package com.example.filesync.networking

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.provider.ContactsContract
import android.widget.Toast
import java.net.*
import java.nio.ByteBuffer
import java.util.*

enum class DeviceMessage(val code : Byte){
    MSG_OK(0),
    MSG_HERE(1),
    MSG_REQUESTED(2),
    MSG_DECLINE(3),
    MSG_ACCEPT(4),
    MSG_SYNCED(5),
    MSG_SEND_FILE(6);
    companion object {
        fun getMessage(code: Short): DeviceMessage? {
            when (code.toInt()) {
                0 -> return MSG_OK
                1 -> return MSG_HERE
                2 -> return MSG_REQUESTED
                3 -> return MSG_DECLINE
                4 -> return MSG_ACCEPT
                5 -> return MSG_SYNCED
                6 -> return MSG_SEND_FILE
            }
            return null
        }
    }
}

data class NetworkDevice(
    val netName : String,
    var ipAddress : InetAddress
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkDevice

        if (netName != other.netName) return false
        return true
    }

    override fun hashCode(): Int {
        var result = netName.hashCode()
        result = 31 * result + ipAddress.hashCode()
        return result
    }
}




class FileSyncService : IntentService("FileSyncService"){

    companion object{
        const val APP_RECV_PORT = 8350
        const val APP_SEND_PORT = 8351
        const val MSG_HEADER = "FSync"
        const val ACTION_NET_CONNECTED = "com.example.filesync.action.ACTION_NET_CONNECTED"
        const val ACTION_SERVICE_STARTED = "com.example.filesync.action.ACTION_SERVICE_STARTED"
        fun startActionNetConnected(context : Context){
            val intent = Intent(context, FileSyncService::class.java)
            intent.action = ACTION_NET_CONNECTED
            context.startService(intent)
        }
    }
    private lateinit var devNetName: String
    private lateinit var msgDeviceHeader : String
    private lateinit var thisIp : ByteArray
    private lateinit var broadcastAddress: InetAddress
    private lateinit var broadcastMessage : ByteArray
    private lateinit var udpBroadcastDS : DatagramSocket
    private lateinit var udpListenerDS : DatagramSocket

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this,"Service starting", Toast.LENGTH_LONG).show()
        devNetName = String.format("%s %s-%s",android.os.Build.BRAND.toUpperCase(Locale.ENGLISH),
            android.os.Build.MODEL.toUpperCase(Locale.ENGLISH),
            android.os.Build.ID
            )
        val msgTemp = String.format("%s:%s", MSG_HEADER, devNetName)
        msgDeviceHeader = "$msgTemp:%s"
        broadcastMessage = msgTemp.toByteArray()
        udpListenerDS = DatagramSocket(APP_RECV_PORT)
        udpBroadcastDS = DatagramSocket(APP_SEND_PORT, InetAddress.getByAddress(thisIp)) //!!
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            when(it.action){
                ACTION_SERVICE_STARTED -> {
                    checkNetworkConnection()

                }
                ACTION_NET_CONNECTED -> {
                    checkNetworkConnection()

                }
                else -> {

                }
            }
        }
    }

    fun pollDevicesUdp(){
        udpBroadcastDS.send(
            DatagramPacket(broadcastMessage, broadcastMessage.size, broadcastAddress, APP_RECV_PORT)
        )
    }

    fun listenUdpDevices(){
        try{
            var buffer = ByteArray(2024)
            val dPacket = DatagramPacket(buffer, buffer.size)
            while (true){
                udpListenerDS.receive(dPacket)
                if (!dPacket.address.address.equals(thisIp))
                 getUdpPollerDevice(dPacket.data, dPacket.address)
            }
        }
        finally {

        }

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

    fun responseToPoller(device : NetworkDevice){
        val msg = getInitialMessage(DeviceMessage.MSG_HERE)
        try {
            val socket = Socket(device.ipAddress, APP_RECV_PORT)
            socket.getOutputStream().write(msg)

        }
        finally {

        }
    }

    fun getInitialMessage(message: DeviceMessage) : ByteArray{
        return String.format(msgDeviceHeader, message).toByteArray()
    }

    fun checkNetworkConnection(){
        val nInfo = (applicationContext.
            getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
        if (nInfo.supplicantState == SupplicantState.COMPLETED){
            setBroadcastAddr(nInfo.ipAddress)
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
                            thisIp = address.address
                            for (ifAddress in this.interfaceAddresses){
                                ifIntAddr = ByteBuffer.wrap(ifAddress.address.address).int
                                if (ifIntAddr == netIp){
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