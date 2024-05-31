package com.example.screenshotapp.bluetoothscanner

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.screenshotapp.MainActivity
import com.example.screenshotapp.R


class CustomDialog(context: Context) : Dialog(context) , ConnectionCallback {
    private val context: Context = context
    private val deviceItemList: ArrayList<DeviceItem> = ArrayList()
    private val devices: ArrayList<BluetoothDevice> = ArrayList()
    private var bTAdapter: BluetoothAdapter? = null
    private lateinit var mListView: ListView
    private lateinit var device: BluetoothDevice
    private lateinit var mAdapter: DeviceListAdapter
    private lateinit var progressBar: ProgressBar;
   // private lateinit var bluetoothLeScanner : BluetoothLeScanner
    private var scanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_dialog_layout)

        bTAdapter  =  BluetoothAdapter.getDefaultAdapter()
     //   bluetoothLeScanner = bTAdapter!!.bluetoothLeScanner
        //scanLeDevice()
        setUpBluetooth()
        if (deviceItemList.isEmpty()) {
            deviceItemList.add(DeviceItem("No Devices", "", false))
        }
        mAdapter = DeviceListAdapter(context, deviceItemList)
        progressBar = findViewById(R.id.progressBar)
        mListView = findViewById(R.id.listView)
        mListView.adapter = mAdapter

        mListView.setOnItemClickListener { parent, view, position, id ->
            progressBar.visibility = View.VISIBLE
            connectBluetooth(position)
        }
    }



    private fun setUpBluetooth() {
        bTAdapter?.bondedDevices?.let { pairedDevices ->
            for (device in pairedDevices) {
                val newDevice = DeviceItem(device.name, device.address,false)
                deviceItemList.add(newDevice)
                devices.add(device)
            }
        }
    }

        private fun connectBluetooth(position: Int) {
            device = devices[position]
            progressBar.visibility = View.GONE
            if (context is MainActivity) {
                (context as MainActivity).handleBluetoothConnectionResult(true, device)
            }
            dismiss()
//            val connectThread = ConnectThread(device, this)
//            connectThread.start()
        }

        override fun onConnectionResult(connected: Boolean) {
            if (connected) {

                Log.d("status", "connected")
                //   Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show()
                // Handle successful connection here
            } else {
                Log.d("status", "connected")
                //Toast.makeText(context, "Not Connected", Toast.LENGTH_SHORT).show()
                // Handle failed connection here
            }

        }

//        fun isConnected(device: BluetoothDevice): Boolean {
//            Toast.makeText(context,"true",Toast.LENGTH_SHORT).show()
//            return try {
//                val method = device.javaClass.getMethod("isConnected")
//                method.invoke(device) as Boolean
//            } catch (e: Exception) {
//              //  throw IllegalStateException(e)
//                return false
//            }
//        }

//    fun isBluetoothDeviceConnected() {
//        val filter = IntentFilter()
//        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
//        registerReceiver(mReceiver, filter)
//    }
//    private val mReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val action = intent.action
//            bTAdapter = BluetoothAdapter.getDefaultAdapter()
//            bTAdapter?.bondedDevices?.let { pairedDevices ->
//                for (device in pairedDevices) {
//                    val newDevice = DeviceItem(device.name, device.address,)
//                    deviceItemList.add(newDevice)
//                    devices.add(device)
//                }
//            }
//            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//
//            when (action) {
//
//                BluetoothDevice.ACTION_ACL_CONNECTED -> {
//                    // Device is now connected
//                }
//
//                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
//                    // Device has disconnected
//                }
//            }
//        }
//    }

    }

interface ConnectionCallback {
    fun onConnectionResult(connected: Boolean)
}

//bTAdapter?.bondedDevices?.let { pairedDevices ->
//    for (device in pairedDevices) {
//        val newDevice = DeviceItem(device.name, device.address,)
//        deviceItemList.add(newDevice)
//        devices.add(device)
//    }
