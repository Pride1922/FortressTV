package com.fortresstv.player.transfer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.util.ArrayDeque

data class DiscoveredFortressTv(
    val serviceName: String,
    val addresses: List<InetAddress>,
    val port: Int,
    val protocolVersion: Int,
    val sessionToken: String? = null,
    val backupKey: String? = null
) {
    val displayName: String = serviceName.removePrefix("FortressTV - ").ifBlank { serviceName }

    fun endpoint(): String? {
        val address = addresses.firstOrNull { it.hostAddress?.contains(':') == false }
            ?: addresses.firstOrNull()
            ?: return null
        val host = address.hostAddress?.substringBefore('%') ?: return null
        return if (host.contains(':')) "http://[$host]:$port" else "http://$host:$port"
    }
}

/** Lifecycle-owned DNS-SD advertiser/discoverer for FortressTV devices on the LAN. */
class TvNsdManager(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val pendingResolution = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private val found = linkedMapOf<String, DiscoveredFortressTv>()
    private val _devices = MutableStateFlow<List<DiscoveredFortressTv>>(emptyList())
    val devices: StateFlow<List<DiscoveredFortressTv>> = _devices.asStateFlow()

    fun advertise(port: Int, deviceName: String) {
        stopAdvertising()
        if (port <= 0) return
        val info = NsdServiceInfo().apply {
            serviceName = "FortressTV - $deviceName"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("protocol", TRANSFER_PROTOCOL_VERSION.toString())
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListener = listener
        runCatching { nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { registrationListener = null }
    }

    fun stopAdvertising() {
        registrationListener?.let { listener -> runCatching { nsdManager.unregisterService(listener) } }
        registrationListener = null
    }

    fun startDiscovery() {
        if (discoveryListener != null) return
        acquireMulticastLock()
        found.clear()
        _devices.value = emptyList()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.startsWith("_fortresstv._tcp")) enqueueResolution(serviceInfo)
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(found) {
                    found.remove(serviceInfo.serviceName)
                    publishDevices()
                }
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { stopDiscovery() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { stopDiscovery() }
        }
        discoveryListener = listener
        runCatching { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { stopDiscovery() }
    }

    fun stopDiscovery() {
        val listener = discoveryListener
        discoveryListener = null
        if (listener != null) runCatching { nsdManager.stopServiceDiscovery(listener) }
        synchronized(pendingResolution) { pendingResolution.clear() }
        resolving = false
        releaseMulticastLock()
    }

    fun close() {
        stopDiscovery()
        stopAdvertising()
    }

    private fun enqueueResolution(info: NsdServiceInfo) {
        synchronized(pendingResolution) {
            if (pendingResolution.none { it.serviceName == info.serviceName }) pendingResolution.add(info)
        }
        resolveNext()
    }

    private fun resolveNext() {
        val next: NsdServiceInfo? = synchronized(pendingResolution) {
            if (resolving || pendingResolution.isEmpty()) null
            else pendingResolution.removeFirst().also { resolving = true }
        }
        next ?: return
        @Suppress("DEPRECATION")
        nsdManager.resolveService(next, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = resolutionFinished()
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val addresses = if (Build.VERSION.SDK_INT >= 34) {
                    serviceInfo.hostAddresses
                } else {
                    @Suppress("DEPRECATION")
                    listOfNotNull(serviceInfo.host)
                }
                val protocol = serviceInfo.attributes["protocol"]
                    ?.toString(Charsets.UTF_8)?.toIntOrNull() ?: 1
                if (addresses.isNotEmpty() && serviceInfo.port > 0) {
                    synchronized(found) {
                        found[serviceInfo.serviceName] = DiscoveredFortressTv(
                            serviceName = serviceInfo.serviceName,
                            addresses = addresses,
                            port = serviceInfo.port,
                            protocolVersion = protocol
                        )
                        publishDevices()
                    }
                }
                resolutionFinished()
            }
        })
    }

    private fun resolutionFinished() {
        synchronized(pendingResolution) { resolving = false }
        resolveNext()
    }

    private fun publishDevices() {
        _devices.value = found.values.sortedBy { it.displayName.lowercase() }
    }

    @Suppress("DEPRECATION")
    private fun acquireMulticastLock() {
        if (multicastLock?.isHeld == true) return
        multicastLock = wifiManager?.createMulticastLock("fortresstv-nsd")?.apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { if (it.isHeld) runCatching { it.release() } }
        multicastLock = null
    }

    companion object {
        const val SERVICE_TYPE = "_fortresstv._tcp."
    }
}

/** Isolates the future Android 17 runtime-permission change from discovery UI. */
object LocalNetworkPermission {
    const val ANDROID_17_API = 37
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

    fun isRuntimePermissionRequired(targetSdk: Int, deviceSdk: Int = Build.VERSION.SDK_INT): Boolean =
        targetSdk >= ANDROID_17_API && deviceSdk >= ANDROID_17_API
}
