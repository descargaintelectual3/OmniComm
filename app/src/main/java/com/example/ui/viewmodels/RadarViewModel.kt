package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.discovery.PeerDiscoveryService
import com.example.domain.local.entities.DeviceSystemStateEntity
import com.example.domain.local.entities.UnifiedSessionStateEntity
import com.example.domain.models.*
import com.example.domain.p2p.FrameBufferPacket
import com.example.domain.p2p.InputTunnelStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadarViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val discoveryService = PeerDiscoveryService.getInstance(context)

    val networkNodes: StateFlow<List<NetworkNode>> = discoveryService.networkNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemEvents: StateFlow<List<MeshSystemEvent>> = discoveryService.systemEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSyncTasks: StateFlow<List<ActiveSyncTask>> = discoveryService.activeSyncTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val toastEvents: SharedFlow<TacticalToastNotification> = discoveryService.toastEvents

    val isScanning: StateFlow<Boolean> = discoveryService.isMultiRadioScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSyncAllRunning: StateFlow<Boolean> = discoveryService.isSyncAllRunning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isHeartbeatActive: StateFlow<Boolean> = discoveryService.isHeartbeatActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val networkHealthScore: StateFlow<Int> = discoveryService.networkHealthScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 98)

    val localUserName: StateFlow<String> = discoveryService.localUserName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Comandante")

    val localUserAvatar: StateFlow<String> = discoveryService.localUserAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "🦅")

    val heartbeatModeLabel: StateFlow<String> = discoveryService.heartbeatModeLabel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "⚡ Ráfaga Rápida")

    // Flujos de Control Remoto con Frame-Buffer y Túnel de Entrada
    val currentScreenFrame: StateFlow<FrameBufferPacket?> = discoveryService.frameBufferStreamer.currentFrame
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tunnelStats: StateFlow<InputTunnelStats> = discoveryService.remoteInputTunnel.tunnelStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InputTunnelStats())

    val deviceSystemStates: StateFlow<List<DeviceSystemStateEntity>> = discoveryService.getAllDeviceSystemStates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHandoffSession: StateFlow<UnifiedSessionStateEntity?> = discoveryService.sessionStateManager.activeHandoffAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedNode = MutableStateFlow<NetworkNode?>(null)
    val selectedNode: StateFlow<NetworkNode?> = _selectedNode.asStateFlow()

    private val _selectedEventFilter = MutableStateFlow<SystemEventType?>(null)
    val selectedEventFilter: StateFlow<SystemEventType?> = _selectedEventFilter.asStateFlow()

    private val _radarStatus = MutableStateFlow("🟢 Malla Activa: Multi-Radio BLE + Wi-Fi Direct")
    val radarStatus: StateFlow<String> = _radarStatus.asStateFlow()

    private var activeScreenMirrorNodeId: String? = null

    // Backward compatibility for legacy callers
    val devices: StateFlow<List<DeviceNode>> = MutableStateFlow(emptyList())

    fun selectNode(node: NetworkNode?) {
        _selectedNode.value = node
        if (node != null && !node.isLocalMaster) {
            discoveryService.recordNodeInteraction(node.id)
        }
    }

    fun setEventFilter(filter: SystemEventType?) {
        _selectedEventFilter.value = filter
    }

    fun triggerMultiRadioScan() {
        discoveryService.forceMultiRadioScan()
    }

    fun triggerHeartbeat() {
        discoveryService.broadcastHeartbeatPulse()
    }

    fun triggerSyncAll() {
        discoveryService.triggerSyncAllEncryptedVerification()
    }

    fun executeRemoteControl(nodeId: String, action: RemoteActionType, payload: String = "") {
        discoveryService.executeRemoteControlAction(nodeId, action, payload)
    }

    fun startScreenMirror(nodeId: String, isBrokenScreen: Boolean = true) {
        activeScreenMirrorNodeId = nodeId
        discoveryService.startScreenMirror(nodeId, isBrokenScreen)
    }

    fun stopScreenMirror() {
        discoveryService.stopScreenMirror()
        activeScreenMirrorNodeId = null
    }

    fun sendRemoteTap(normX: Float, normY: Float) {
        activeScreenMirrorNodeId?.let { id ->
            discoveryService.sendRemoteTap(id, normX, normY)
        }
    }

    fun sendRemoteSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        activeScreenMirrorNodeId?.let { id ->
            discoveryService.sendRemoteSwipe(id, startX, startY, endX, endY)
        }
    }

    fun sendRemoteSystemNav(command: String) {
        activeScreenMirrorNodeId?.let { id ->
            discoveryService.sendRemoteSystemNav(id, command)
        }
    }

    fun saveLocalSession(route: String, title: String, draft: String = "", peerId: String = "") {
        discoveryService.sessionStateManager.saveLocalSessionState(
            activeRoute = route,
            routeTitle = title,
            draftMessage = draft,
            activeChatPeerId = peerId
        )
    }

    fun dismissHandoff() {
        discoveryService.sessionStateManager.dismissCurrentHandoff()
    }

    fun togglePriority(nodeId: String) {
        discoveryService.toggleNodePriority(nodeId)
    }

    fun updateIdentity(name: String, avatar: String) {
        discoveryService.updateLocalIdentity(name, avatar)
    }

    fun registerTacticalNode(nodeId: String, name: String, model: String, medium: ConnectionType, avatar: String) {
        discoveryService.registerManualTacticalNode(nodeId, name, model, medium, avatar)
    }

    fun pingDevice(nodeId: String) {
        discoveryService.executeRemoteControlAction(nodeId, RemoteActionType.AUDIO_ALARM_BEACON, "PING")
    }
}
