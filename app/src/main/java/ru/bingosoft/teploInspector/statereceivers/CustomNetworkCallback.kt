package ru.bingosoft.teploInspector.statereceivers

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import ru.bingosoft.teploInspector.util.OtherUtil
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CustomNetworkCallback(): ConnectivityManager.NetworkCallback() {

    lateinit var otherUtil: OtherUtil

    override fun onAvailable(network : Network) {
        Timber.d("The default network is now: $network")
        otherUtil.writeToFile("Logger_default_network_change: $network")
    }

    override fun onLost(network : Network) {
        Timber.d("The application no longer has a default network. The last default network was $network")
        otherUtil.writeToFile("Logger_default_network_Lost_Last_network: $network")
    }

    override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
        Timber.d("The default network changed capabilities: $networkCapabilities")
        otherUtil.writeToFile("Logger_default_network_changed_capabilities: $networkCapabilities")
    }

    override fun onLinkPropertiesChanged(network : Network, linkProperties : LinkProperties) {
        Timber.d("The default network changed link properties: $linkProperties")
        otherUtil.writeToFile("Logger_default_network_changed_link_properties: $linkProperties")
    }
}