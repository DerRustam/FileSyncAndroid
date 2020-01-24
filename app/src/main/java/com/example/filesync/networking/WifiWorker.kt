package com.example.filesync.networking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.example.filesync.MainActivity
import com.example.filesync.R
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.util.*

class WifiWorker {
    private val devNetName = String.format("%s %s-%s",android.os.Build.BRAND.toUpperCase(Locale.ENGLISH),
    android.os.Build.MODEL.toUpperCase(Locale.ENGLISH),
    android.os.Build.ID
    )
    val msgTemp = String.format("%s:%s", FileSyncService.MSG_HEADER, devNetName)
    private lateinit var udpBroadcastDS : DatagramSocket
    private val udpListenerDS = DatagramSocket(FileSyncService.APP_RECV_PORT)
    private lateinit var curIp : InetAddress
    private val broadcastMessage = msgTemp.toByteArray()
    private lateinit var broadcastAddress: InetAddress

    fun checkNetworkConnection(context : Context){
        val nInfo = (context.
            getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
        if (nInfo.supplicantState == SupplicantState.COMPLETED){
            setBroadcastAddr(nInfo.ipAddress)
        }
    }

    fun pollDevicesUdp(){
        udpBroadcastDS = DatagramSocket(8888, curIp)
        Thread(Runnable {
        udpBroadcastDS.send(
            DatagramPacket(broadcastMessage, broadcastMessage.size, broadcastAddress,
                FileSyncService.APP_RECV_PORT
            )
        )
        }).start()
    }

    fun listenUdpDevices(){
        Thread(Runnable {
                try {
                    var buffer = ByteArray(2048)
                    val dPacket = DatagramPacket(buffer, buffer.size)
                    while (true) {
                        udpListenerDS.receive(dPacket)
                        getUdpPollerDevice(dPacket.data, dPacket.address)
                    }
                } finally {

                }
        }).start()
    }

    fun getUdpPollerDevice(data : ByteArray, endPoint : InetAddress) : NetworkDevice?{
        try {
            val message = String(data)
            val components = message.split(':')
            return NetworkDevice(
                String(data.copyOfRange(data.indexOf(':'.toByte()), data.size)),
                endPoint
            )
        }
        finally {
        }
    }


    fun setBroadcastAddr(wfmIp : Int){
        val reversIp = Integer.reverseBytes(wfmIp)
        val interfaces = NetworkInterface.getNetworkInterfaces()
        var isFound = false
        while(interfaces.hasMoreElements() && !isFound){
            with(interfaces.nextElement()){
                if (this.displayName.contains("wlan")){
                    var netIp : Int
                    for (address in this.inetAddresses){
                        netIp = ByteBuffer.wrap(address.address).int
                        if (netIp == reversIp){
                            var ifIntAddr : Int
                            for (ifAddress in this.interfaceAddresses){
                                ifIntAddr = ByteBuffer.wrap(ifAddress.address.address).int
                                if (ifIntAddr == netIp){
                                    curIp = ifAddress.address
                                    broadcastAddress = ifAddress.broadcast
                                    isFound = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}