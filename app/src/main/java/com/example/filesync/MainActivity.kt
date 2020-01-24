package com.example.filesync


import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder

import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat

import androidx.fragment.app.Fragment
import com.example.filesync.database.DeviceStatus
import com.example.filesync.fileproceder.UriDeserializer
import com.example.filesync.fileproceder.UriSerializer
import com.example.filesync.networking.FileSyncService
import com.example.filesync.networking.WifiWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import kotlin.collections.ArrayList
import android.net.wifi.WifiManager
import android.content.Intent
import android.content.BroadcastReceiver
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.R.attr.orientation
import android.content.res.Configuration
import android.net.InetAddresses
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.text.format.Formatter
import android.widget.ImageView
import kotlinx.android.synthetic.main.members_header.*
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.text.Format
import java.util.*


data class DeviceModel(
    val adjName : String,
    val status : DeviceStatus
)

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, NavigationView.OnNavigationItemSelectedListener{
    private lateinit var receiveFragment : ReceivedFragment
    private lateinit var sendFragment : SendFragment
    private lateinit var tasksFragment: TasksFragment
    private var isSoft = false
    private var indFragment : Int = 0

    private val wifiStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val wifiStateExtra = intent.getIntExtra(
                WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN
            )
            val netStatusHeader : View = top_nv_main.getHeaderView(0)
            when (wifiStateExtra) {
                WifiManager.WIFI_STATE_ENABLED -> {
                    netStatusHeader.findViewById<ImageView>(R.id.iv_connection_status)
                        .setImageResource(R.drawable.ic_wifi_24px)
                    val nInfo = (applicationContext.
                        getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
                    if (nInfo.supplicantState != SupplicantState.COMPLETED){
                        netStatusHeader.findViewById<TextView>(R.id.tv_ip_address).
                            text = getString(R.string.msg_nocon)
                        netStatusHeader.findViewById<TextView>(R.id.tv_bandwidth).text = ""
                    }
                    else{
                        @SuppressWarnings("deprecation")
                        netStatusHeader.findViewById<TextView>(R.id.tv_ip_address)
                            .text = Formatter.formatIpAddress(nInfo.ipAddress)
                        netStatusHeader.findViewById<TextView>(R.id.tv_bandwidth)
                            .text = String.format(getString(R.string.msg_spd),
                            nInfo.frequency.toFloat() / 1000,
                            nInfo.linkSpeed)
                    }

                }
                WifiManager.WIFI_STATE_DISABLED -> {
                    netStatusHeader.findViewById<TextView>(R.id.tv_ip_address).
                        text = getString(R.string.msg_wfdis)
                    netStatusHeader.findViewById<TextView>(R.id.tv_bandwidth).text = ""
                    netStatusHeader.findViewById<ImageView>(R.id.iv_connection_status)
                        .setImageResource(R.drawable.ic_wifi_off_24px)
                }
            }
        }
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        if (!hasPermissions(applicationContext, perms)){
            perms.forEach {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, it))
                {

                }
            }
            ActivityCompat.requestPermissions(this, perms, 1);
        }
    }

    fun hasPermissions(context: Context, permissions : Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    inline fun <reified T> genericType() = object: TypeToken<T>() {}.type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*val dbh = DBHelper(this)
        dbh.addDeviceToRequest("ANDIE")
        dbh.setDeviceStatus("ANDIE", DeviceStatus.DEVICE_REQUEST_SENDED)
        dbh.getDevices().forEach{
            Log.d("Devices", it)
        }
        dbh.setDeviceStatus("ANDIE", DeviceStatus.DEVICE_CONNECTED)
        dbh.deleteDevice("ANDIE")
        dbh.getDevices().forEach{
            Log.d("Devices", it)
        }
        dbh.deleteDevice("ANDIE")
        dbh.getAllTaskFiles().forEach{
            Log.d("TASKS", it)
        }
        dbh.setNewAdjustedName("ANDIE", "ANDIE_SUPER")

        dbh.addTasks(arrayOf("TESTURI1", "TESTURI2"), arrayOf("ANDIE"))
        dbh.getAllFiles().forEach{
            Log.d("FILES", it)
        }
        dbh.getAllTaskFiles().forEach{
            Log.d("TASKS", it)
        }
        dbh.close()*/

        setContentView(R.layout.activity_main)
        checkPermissions()
        val prefs = getPreferences(Context.MODE_PRIVATE);
        val gson = GsonBuilder()
            .registerTypeAdapter(Uri::class.java, UriDeserializer())
            .create()

        try{
            indFragment = prefs.getInt("ind_frag",0)
                receiveFragment = ReceivedFragment.build {
                    appContext = applicationContext
                    downloadsList = gson.fromJson(
                        prefs.getString("dl_files", gson.toJson(ArrayList<DownloadModel>())),
                        genericType<ArrayList<DownloadModel>>()
                    ) ?: ArrayList()
                    pauseDay = prefs.getInt("up_day", 0)
                    pauseMonth = prefs.getInt("up_month", 0)
                    pauseYear = prefs.getInt("up_year", 0)
                }
                sendFragment = SendFragment.build {
                    appContext = applicationContext
                    activity = this@MainActivity
                    filesUri = gson.fromJson(
                        prefs.getString("uri_files", gson.toJson(ArrayList<Uri>())),
                        genericType<ArrayList<Uri>>()
                    ) ?: ArrayList()
                    selectedUri = gson.fromJson(
                        prefs.getString("uri_selected", gson.toJson(ArrayList<Uri>())),
                        genericType<ArrayList<Uri>>()
                    ) ?: ArrayList()

                }
                tasksFragment = TasksFragment.build { appContext = applicationContext }
        }
        catch (ex : Exception){
            prefs.edit().clear().apply()
            sendFragment = SendFragment.build{appContext = applicationContext
                activity = this@MainActivity }
            tasksFragment = TasksFragment.build {appContext = applicationContext}
            receiveFragment = ReceivedFragment.build {appContext = applicationContext}
        }

        when(indFragment){
            1 -> {loadFragment(sendFragment)}
            2 -> {loadFragment(tasksFragment)}
            else -> {loadFragment(receiveFragment)}
        }

        val toolbar : Toolbar = findViewById(R.id.tb_main)
        setSupportActionBar(toolbar)
        val toggle =  ActionBarDrawerToggle(this, dl_drawer_main, toolbar,R.string.drawer_open,
            R.string.drawer_close)
        dl_drawer_main.addDrawerListener(toggle)
        toggle.syncState()
        bottom_nv_main.setOnNavigationItemSelectedListener(this)
        top_nv_main.setNavigationItemSelectedListener(this)
        val netStatusHeader : View = top_nv_main.getHeaderView(0)
            netStatusHeader.findViewById<TextView>(R.id.tv_device_name).text =
                String.format("%s %s-%s",
                android.os.Build.BRAND.toUpperCase(Locale.ENGLISH),
                    android.os.Build.MODEL.toUpperCase(Locale.ENGLISH),
                    android.os.Build.ID)
        val wfMng = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wfMng.isWifiEnabled){
            wifiStateReceiver.onReceive(this,Intent().apply {
                putExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_ENABLED)
            })
        }
        else {
            wifiStateReceiver.onReceive(this,Intent().apply {
                putExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED)
            })
        }
        /*val wifiW = WifiWorker()
        wifiW.checkNetworkConnection(applicationContext)
        wifiW.listenUdpDevices()
        wifiW.pollDevicesUdp()*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    super.onPause()
                    super.onStop()
                    super.onDestroy()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onBackPressed() {
        if (dl_drawer_main.isDrawerOpen(GravityCompat.START)){
            dl_drawer_main.closeDrawer(GravityCompat.START)
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiStateReceiver,
            IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val fragment : OrientationSensable? =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as OrientationSensable
        fragment?.onOrientationChanged(resources.configuration.orientation)
    }

    override fun onPause() {
        unregisterReceiver(wifiStateReceiver)
        val gson = GsonBuilder().registerTypeAdapter(Uri::class.java, UriSerializer()).create()
        val prefs = getPreferences(Context.MODE_PRIVATE)
        val tst = sendFragment.getAllUri()
        val str = gson.toJson(tst, genericType<ArrayList<Uri>>())
        prefs.edit().run {
            this.apply {
                putInt("ind_frag", indFragment)
                putInt("up_day", receiveFragment.getUpDay())
                putInt("up_month", receiveFragment.getUpMonth())
                putInt("up_year", receiveFragment.getUpYear())
                putString("dl_files", gson.toJson(receiveFragment.getDownloadModels(),
                    genericType<ArrayList<DownloadModel>>()))
                putString("uri_files", gson.toJson(sendFragment.getAllUri(),
                    genericType<ArrayList<Uri>>()))
                putString("uri_selected", gson.toJson(sendFragment.getSelectedUris(),
                    genericType<ArrayList<Uri>>()))
            }
            this.apply()
        }
        super.onPause()
    }



    override fun onNavigationItemSelected(menuItem : MenuItem) : Boolean{
        when(menuItem.itemId){
            R.id.mi_received -> {
                loadFragment(receiveFragment)
                indFragment = 0
            }
            R.id.mi_tosend -> {
                loadFragment(sendFragment)
                indFragment = 1
            }
            R.id.mi_tasks -> {
                loadFragment(tasksFragment)
                indFragment = 2
            }
            R.id.mi_synchronized -> Toast.makeText(this, "Synchronized", Toast.LENGTH_SHORT).show()
            R.id.mi_requests -> Toast.makeText(this, "Requests", Toast.LENGTH_SHORT).show()
            else -> {Toast.makeText(this, "Network devices", Toast.LENGTH_SHORT).show()}
        }
        return true
    }

    private fun loadFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment).commit()
    }
}
