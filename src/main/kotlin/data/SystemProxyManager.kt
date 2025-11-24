package data

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import service.SystemProxySetter

/**
 * 系统代理状态管理器
 * 管理全局系统代理状态，确保各页面间状态同步
 */
object SystemProxyManager {
    // 系统代理状态
    private val _systemProxyState = MutableStateFlow<SystemProxyState>(SystemProxyState.Disabled)
    val systemProxyState: StateFlow<SystemProxyState> = _systemProxyState.asStateFlow()

    // 当前代理信息
    private val _currentProxy = MutableStateFlow<SystemProxySetter.ProxyInfo?>(null)
    val currentProxy: StateFlow<SystemProxySetter.ProxyInfo?> = _currentProxy.asStateFlow()

    /**
     * 系统代理状态
     */
    sealed class SystemProxyState {
        object Disabled : SystemProxyState()
        data class Enabled(val host: String, val port: Int) : SystemProxyState()
        object Unknown : SystemProxyState()
    }

    /**
     * 刷新系统代理状态
     */
    fun refreshProxyState() {
        try {
            val proxy = SystemProxySetter.getCurrentProxy()
            _currentProxy.value = proxy

            if (proxy != null && proxy.enabled) {
                _systemProxyState.value = SystemProxyState.Enabled(proxy.host, proxy.port)
            } else {
                _systemProxyState.value = SystemProxyState.Disabled
            }
        } catch (e: Exception) {
            println("❌ 获取系统代理状态失败: ${e.message}")
            _systemProxyState.value = SystemProxyState.Unknown
        }
    }

    /**
     * 启用系统代理
     */
    fun enableProxy(host: String, port: Int): Result<Unit> {
        return try {
            val result = SystemProxySetter.setProxy(host, port, true)
            if (result.isSuccess) {
                _systemProxyState.value = SystemProxyState.Enabled(host, port)
                _currentProxy.value = SystemProxySetter.ProxyInfo(host, port, true)
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("设置系统代理失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 禁用系统代理
     */
    fun disableProxy(): Result<Unit> {
        return try {
            val result = SystemProxySetter.setProxy("", 0, false)
            if (result.isSuccess) {
                _systemProxyState.value = SystemProxyState.Disabled
                _currentProxy.value = null
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("禁用系统代理失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查是否为指定代理
     */
    fun isProxyEnabledFor(host: String, port: Int): Boolean {
        return when (val state = _systemProxyState.value) {
            is SystemProxyState.Enabled -> state.host == host && state.port == port
            else -> false
        }
    }

    // 初始化时刷新状态
    init {
        refreshProxyState()
    }
}

