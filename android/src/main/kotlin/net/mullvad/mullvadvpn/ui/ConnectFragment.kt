package net.mullvad.mullvadvpn.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mullvad.mullvadvpn.R
import net.mullvad.mullvadvpn.model.KeygenEvent
import net.mullvad.mullvadvpn.model.TunnelState
import net.mullvad.mullvadvpn.util.JobTracker
import org.joda.time.DateTime

val KEY_IS_TUNNEL_INFO_EXPANDED = "is_tunnel_info_expanded"

class ConnectFragment : ServiceDependentFragment(OnNoService.GoToLaunchScreen) {
    private val jobTracker = JobTracker()

    private lateinit var actionButton: ConnectActionButton
    private lateinit var switchLocationButton: SwitchLocationButton
    private lateinit var headerBar: HeaderBar
    private lateinit var notificationBanner: NotificationBanner
    private lateinit var status: ConnectionStatus
    private lateinit var locationInfo: LocationInfo

    private lateinit var updateKeyStatusJob: Job
    private var updateLocationInfoJob: Job? = null
    private var updateTunnelStateJob: Job? = null

    private var isTunnelInfoExpanded = false
    private var tunnelStateListener: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isTunnelInfoExpanded =
            savedInstanceState?.getBoolean(KEY_IS_TUNNEL_INFO_EXPANDED, false) ?: false
    }

    override fun onSafelyCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.connect, container, false)
        val resources = parentActivity.resources

        view.findViewById<ImageButton>(R.id.settings).setOnClickListener {
            parentActivity.openSettings()
        }

        headerBar = HeaderBar(view, resources)
        notificationBanner = NotificationBanner(view, parentActivity, appVersionInfoCache, daemon)
        status = ConnectionStatus(view, resources)

        locationInfo = LocationInfo(view, context!!)
        locationInfo.isTunnelInfoExpanded = isTunnelInfoExpanded

        actionButton = ConnectActionButton(view)
        actionButton.apply {
            onConnect = { connectionProxy.connect() }
            onCancel = { connectionProxy.disconnect() }
            onReconnect = { connectionProxy.reconnect() }
            onDisconnect = { connectionProxy.disconnect() }
        }

        switchLocationButton = SwitchLocationButton(view, resources)
        switchLocationButton.onClick = { openSwitchLocationScreen() }

        updateKeyStatusJob = updateKeyStatus(keyStatusListener.keyStatus)

        return view
    }

    override fun onSafelyResume() {
        locationInfo.isTunnelInfoExpanded = isTunnelInfoExpanded

        notificationBanner.onResume()

        keyStatusListener.onKeyStatusChange = { keyStatus ->
            updateKeyStatusJob.cancel()
            updateKeyStatusJob = updateKeyStatus(keyStatus)
        }

        locationInfoCache.onNewLocation = { location ->
            updateLocationInfoJob?.cancel()
            updateLocationInfoJob = GlobalScope.launch(Dispatchers.Main) {
                locationInfo.location = location
            }
        }

        relayListListener.onRelayListChange = { _, selectedRelayItem ->
            locationInfoCache.selectedRelay = selectedRelayItem
            switchLocationButton.location = selectedRelayItem
        }

        tunnelStateListener = connectionProxy.onUiStateChange.subscribe { uiState ->
            updateTunnelStateJob?.cancel()
            updateTunnelStateJob = updateTunnelState(uiState, connectionProxy.state)
        }

        accountCache.onAccountDataChange = { _, expiry ->
            if (expiry?.isBeforeNow() ?: false) {
                openOutOfTimeScreen()
            } else if (expiry != null) {
                scheduleNextAccountExpiryCheck(expiry)
            }
        }
    }

    override fun onSafelyPause() {
        accountCache.onAccountDataChange = null
        keyStatusListener.onKeyStatusChange = null
        locationInfoCache.onNewLocation = null
        relayListListener.onRelayListChange = null

        tunnelStateListener?.let { listener ->
            connectionProxy.onUiStateChange.unsubscribe(listener)
        }

        updateLocationInfoJob?.cancel()
        updateTunnelStateJob?.cancel()
        notificationBanner.onPause()

        isTunnelInfoExpanded = locationInfo.isTunnelInfoExpanded
    }

    override fun onSafelyDestroyView() {
        jobTracker.cancelAllJobs()
        switchLocationButton.onDestroy()
    }

    override fun onSafelySaveInstanceState(state: Bundle) {
        isTunnelInfoExpanded = locationInfo.isTunnelInfoExpanded
        state.putBoolean(KEY_IS_TUNNEL_INFO_EXPANDED, isTunnelInfoExpanded)
    }

    private fun updateTunnelState(uiState: TunnelState, realState: TunnelState) =
        GlobalScope.launch(Dispatchers.Main) {
        notificationBanner.tunnelState = realState
        locationInfo.state = realState
        headerBar.setState(realState)
        status.setState(realState)

        actionButton.tunnelState = uiState
        switchLocationButton.state = uiState
    }

    private fun updateKeyStatus(keyStatus: KeygenEvent?) = GlobalScope.launch(Dispatchers.Main) {
        notificationBanner.keyState = keyStatus
    }

    private fun openSwitchLocationScreen() {
        fragmentManager?.beginTransaction()?.apply {
            setCustomAnimations(
                R.anim.fragment_enter_from_bottom,
                R.anim.do_nothing,
                R.anim.do_nothing,
                R.anim.fragment_exit_to_bottom
            )
            replace(R.id.main_fragment, SelectLocationFragment())
            addToBackStack(null)
            commit()
        }
    }

    private fun openOutOfTimeScreen() {
        jobTracker.newUiJob("openOutOfTimeScreen") {
            fragmentManager?.beginTransaction()?.apply {
                replace(R.id.main_fragment, OutOfTimeFragment())
                commit()
            }
        }
    }

    private fun scheduleNextAccountExpiryCheck(expiration: DateTime) {
        jobTracker.newBackgroundJob("refetchAccountExpiry") {
            val millisUntilExpiration = expiration.millis - DateTime.now().millis

            delay(millisUntilExpiration)
            accountCache.fetchAccountExpiry()

            // If the account ran out of time but is still connected, fetching the expiry again will
            // fail. Therefore, after a timeout of 5 seconds the app will assume the account time
            // really expired and move to the out of time screen. However, if fetching the expiry
            // succeeds, this job is cancelled and replaced with a new scheduled check.
            delay(5_000)
            openOutOfTimeScreen()
        }
    }
}
