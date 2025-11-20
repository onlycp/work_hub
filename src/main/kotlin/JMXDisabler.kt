package data

/**
 * JMX 禁用器
 * 在类加载时立即禁用 JMX 相关功能，避免在 Windows 上出现 MalformedObjectNameException
 * 这必须在任何 JGit 操作之前执行
 */
object JMXDisabler {
    init {
        try {
            // 禁用 JMX 相关功能
            System.setProperty("java.awt.headless", "false")
            System.setProperty("com.sun.management.jmxremote", "false")
            // 禁用 JGit 的 JMX 监控（这是最关键的设置）
            System.setProperty("org.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled", "true")
            
            // 尝试禁用 MBean 服务器（如果已创建）
            try {
                val mbeanServerClass = Class.forName("javax.management.MBeanServer")
                val platformMBeanServerClass = Class.forName("java.lang.management.ManagementFactory")
                val getPlatformMBeanServerMethod = platformMBeanServerClass.getMethod("getPlatformMBeanServer")
                val mbeanServer = getPlatformMBeanServerMethod.invoke(null)
                
                // 如果 MBeanServer 已创建，尝试移除 JGit 相关的 MBean
                try {
                    val objectNameClass = Class.forName("javax.management.ObjectName")
                    val jgitObjectName = objectNameClass.getConstructor(String::class.java)
                        .newInstance("org.eclipse.jgit:type=WindowCache")
                    val unregisterMBeanMethod = mbeanServerClass.getMethod("unregisterMBean", objectNameClass)
                    unregisterMBeanMethod.invoke(mbeanServer, jgitObjectName)
                } catch (e: Exception) {
                    // MBean 可能不存在，忽略
                }
            } catch (e: Exception) {
                // 如果无法访问 MBeanServer，忽略
            }
        } catch (e: Exception) {
            // 忽略所有异常，确保不会阻止应用启动
            println("⚠️ JMXDisabler: 设置系统属性时出现异常: ${e.message}")
        }
    }
}

