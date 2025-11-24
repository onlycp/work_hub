package service

import utils.Logger
import java.io.File
import java.lang.Runtime
import kotlin.system.measureTimeMillis

/**
 * 系统代理设置器
 * 支持自动配置系统级HTTP/SOCKS代理
 */
object SystemProxySetter {

    /**
     * 设置系统代理
     */
    fun setProxy(host: String, port: Int, enable: Boolean): Result<Unit> {
        return try {
            val os = System.getProperty("os.name").lowercase()

            when {
                os.contains("windows") -> setWindowsProxy(host, port, enable)
                os.contains("mac") || os.contains("darwin") -> setMacProxy(host, port, enable)
                os.contains("linux") -> setLinuxProxy(host, port, enable)
                else -> Result.failure(Exception("不支持的操作系统: $os"))
            }
        } catch (e: Exception) {
            Logger.error("设置系统代理失败", e)
            Result.failure(e)
        }
    }

    /**
     * Windows系统代理设置
     */
    private fun setWindowsProxy(host: String, port: Int, enable: Boolean): Result<Unit> {
        return try {
            val proxyServer = if (enable) "$host:$port" else ""

            // 使用PowerShell设置代理
            val commands = listOf(
                "Set-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings' -Name ProxyServer -Value '$proxyServer'",
                "Set-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings' -Name ProxyEnable -Value ${if (enable) 1 else 0}"
            )

            commands.forEach { command ->
                executeCommand("powershell", "-Command", command)
            }

            // 通知系统代理设置已更改
            executeCommand("reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", "/v", "ProxySettingsPerUser", "/t", "REG_DWORD", "/d", "1", "/f")

            Logger.info("Windows代理设置完成: ${if (enable) "启用" else "禁用"} $host:$port")
            Result.success(Unit)

        } catch (e: Exception) {
            Logger.error("Windows代理设置失败", e)
            Result.failure(e)
        }
    }

    /**
     * macOS系统代理设置
     */
    private fun setMacProxy(host: String, port: Int, enable: Boolean): Result<Unit> {
        return try {
            val networks = getMacNetworkServices()

            if (networks.isEmpty()) {
                Logger.warn("macOS: 未找到可用的网络服务，无法设置代理")
                return Result.failure(Exception("未找到可用的网络服务"))
            }

            networks.forEach { service ->
                Logger.debug("macOS: 为网络服务 '$service' 设置代理")

                // 设置HTTP代理
                executeCommand("networksetup", "-setwebproxy", service, host, port.toString())
                executeCommand("networksetup", "-setwebproxystate", service, if (enable) "on" else "off")

                // 设置HTTPS代理
                executeCommand("networksetup", "-setsecurewebproxy", service, host, port.toString())
                executeCommand("networksetup", "-setsecurewebproxystate", service, if (enable) "on" else "off")

                // 设置SOCKS代理
                executeCommand("networksetup", "-setsocksfirewallproxy", service, host, port.toString())
                executeCommand("networksetup", "-setsocksfirewallproxystate", service, if (enable) "on" else "off")

                // 设置代理绕过列表 - 排除本地地址和私有网络，避免循环代理
                if (enable) {
                    val bypassList = "127.0.0.1,localhost,*.local,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16"
                    executeCommand("networksetup", "-setproxybypassdomains", service, bypassList)
                } else {
                    // 清除绕过列表
                    executeCommand("networksetup", "-setproxybypassdomains", service, "")
                }
            }

            Logger.info("macOS代理设置完成: ${if (enable) "启用" else "禁用"} $host:$port (处理了 ${networks.size} 个网络服务)")
            Result.success(Unit)

        } catch (e: Exception) {
            Logger.error("macOS代理设置失败", e)
            Result.failure(e)
        }
    }

    /**
     * Linux系统代理设置
     */
    private fun setLinuxProxy(host: String, port: Int, enable: Boolean): Result<Unit> {
        return try {
            val proxyUrl = if (enable) "http://$host:$port" else ""

            // 设置环境变量 - 包含更全面的本地地址绕过列表
            val envVars = mapOf(
                "http_proxy" to proxyUrl,
                "https_proxy" to proxyUrl,
                "ftp_proxy" to proxyUrl,
                "no_proxy" to "localhost,127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,*.local"
            )

            // GNOME桌面环境
            if (isGnomeAvailable()) {
                setGnomeProxy(host, port, enable)
            }

            // KDE桌面环境
            if (isKdeAvailable()) {
                setKdeProxy(host, port, enable)
            }

            // 设置用户级环境变量
            setUserEnvironmentVariables(envVars)

            Logger.info("Linux代理设置完成: ${if (enable) "启用" else "禁用"} $host:$port")
            Result.success(Unit)

        } catch (e: Exception) {
            Logger.error("Linux代理设置失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取macOS网络服务列表
     */
    private fun getMacNetworkServices(): List<String> {
        return try {
            val result = executeCommand("networksetup", "-listallnetworkservices")
            result.lines()
                .filter { it.isNotBlank() && !it.startsWith("*") && !it.contains("*") }
                .map { it.trim() }
        } catch (e: Exception) {
            listOf("Wi-Fi", "Ethernet") // 默认服务
        }
    }

    /**
     * 检查GNOME是否可用
     */
    private fun isGnomeAvailable(): Boolean {
        return executeCommand("which", "gsettings").isNotBlank()
    }

    /**
     * 检查KDE是否可用
     */
    private fun isKdeAvailable(): Boolean {
        return executeCommand("which", "kwriteconfig5").isNotBlank()
    }

    /**
     * 设置GNOME代理
     */
    private fun setGnomeProxy(host: String, port: Int, enable: Boolean) {
        try {
            executeCommand("gsettings", "set", "org.gnome.system.proxy", "mode", if (enable) "manual" else "none")

            if (enable) {
                executeCommand("gsettings", "set", "org.gnome.system.proxy.http", "host", host)
                executeCommand("gsettings", "set", "org.gnome.system.proxy.http", "port", port.toString())
                executeCommand("gsettings", "set", "org.gnome.system.proxy.https", "host", host)
                executeCommand("gsettings", "set", "org.gnome.system.proxy.https", "port", port.toString())

                // 设置忽略主机列表，避免循环代理
                val ignoreHosts = arrayOf("localhost", "127.0.0.0/8", "::1", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "*.local")
                executeCommand("gsettings", "set", "org.gnome.system.proxy", "ignore-hosts", ignoreHosts.joinToString(prefix = "[", postfix = "]", separator = ","))
            }
        } catch (e: Exception) {
            Logger.warn("GNOME代理设置失败: ${e.message}")
        }
    }

    /**
     * 设置KDE代理
     */
    private fun setKdeProxy(host: String, port: Int, enable: Boolean) {
        try {
            val proxyType = if (enable) "1" else "0" // 0=None, 1=Manual

            executeCommand("kwriteconfig5", "--file", "kioslaverc", "--group", "Proxy Settings", "--key", "ProxyType", proxyType)

            if (enable) {
                val proxyString = "http://$host:$port"
                executeCommand("kwriteconfig5", "--file", "kioslaverc", "--group", "Proxy Settings", "--key", "httpProxy", proxyString)
                executeCommand("kwriteconfig5", "--file", "kioslaverc", "--group", "Proxy Settings", "--key", "httpsProxy", proxyString)

                // 设置不使用代理的主机列表
                val noProxyFor = "localhost,127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,*.local"
                executeCommand("kwriteconfig5", "--file", "kioslaverc", "--group", "Proxy Settings", "--key", "NoProxyFor", noProxyFor)
            } else {
                // 清除代理设置
                executeCommand("kwriteconfig5", "--file", "kioslaverc", "--group", "Proxy Settings", "--key", "NoProxyFor", "")
            }
        } catch (e: Exception) {
            Logger.warn("KDE代理设置失败: ${e.message}")
        }
    }

    /**
     * 设置用户环境变量
     */
    private fun setUserEnvironmentVariables(envVars: Map<String, String>) {
        try {
            val bashrc = File(System.getProperty("user.home"), ".bashrc")
            val zshrc = File(System.getProperty("user.home"), ".zshrc")

            val exportLines = envVars.map { (key, value) ->
                "export $key=\"$value\""
            }

            // 更新.bashrc
            updateShellRcFile(bashrc, exportLines)

            // 更新.zshrc
            updateShellRcFile(zshrc, exportLines)

        } catch (e: Exception) {
            Logger.warn("设置环境变量失败: ${e.message}")
        }
    }

    /**
     * 更新shell配置文件
     */
    private fun updateShellRcFile(rcFile: File, exportLines: List<String>) {
        try {
            var content = if (rcFile.exists()) rcFile.readText() else ""

            // 移除旧的代理设置
            content = content.lines()
                .filter { line -> !exportLines.any { newLine -> line.contains(newLine.substringBefore("=\"")) } }
                .joinToString("\n")

            // 添加新的代理设置
            if (content.isNotEmpty() && !content.endsWith("\n")) {
                content += "\n"
            }
            content += "\n# HubLink proxy settings\n"
            content += exportLines.joinToString("\n") + "\n"

            rcFile.writeText(content)

        } catch (e: Exception) {
            Logger.warn("更新 ${rcFile.name} 失败: ${e.message}")
        }
    }

    /**
     * 执行系统命令
     */
    private fun executeCommand(vararg command: String): String {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                output.trim()
            } else {
                throw Exception("Command failed with exit code $exitCode: $output")
            }
        } catch (e: Exception) {
            Logger.warn("执行命令失败: ${command.joinToString(" ")} - ${e.message}")
            ""
        }
    }

    /**
     * 获取当前代理设置
     */
    fun getCurrentProxy(): ProxyInfo? {
        return try {
            val os = System.getProperty("os.name").lowercase()

            when {
                os.contains("windows") -> getWindowsProxy()
                os.contains("mac") -> getMacProxy()
                os.contains("linux") -> getLinuxProxy()
                else -> null
            }
        } catch (e: Exception) {
            Logger.warn("获取当前代理设置失败: ${e.message}")
            null
        }
    }

    private fun getWindowsProxy(): ProxyInfo? {
        // TODO: 实现Windows代理获取
        return null
    }

    private fun getMacProxy(): ProxyInfo? {
        // TODO: 实现macOS代理获取
        return null
    }

    private fun getLinuxProxy(): ProxyInfo? {
        // TODO: 实现Linux代理获取
        return null
    }

    /**
     * 代理信息数据类
     */
    data class ProxyInfo(
        val host: String,
        val port: Int,
        val enabled: Boolean
    )
}
