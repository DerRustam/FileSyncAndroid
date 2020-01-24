package com.example.filesync.networking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast

class SysEventReceiver : BroadcastReceiver(){

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action){
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val wifiStateExtra = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN
                )
                if (wifiStateExtra == WifiManager.WIFI_STATE_ENABLED){

                }
            }
        }
    }
}