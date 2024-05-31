package com.example.screenshotapp

//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.app.ActivityManager
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothManager
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.content.pm.PackageManager
//import android.media.projection.MediaProjection
//import android.media.projection.MediaProjectionManager
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.screenshotapp.bluetoothscanner.CustomDialog
import com.example.screenshotapp.databinding.ActivityMainBinding
import com.example.screenshotapp.service.ScreenService
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class MainActivity : AppCompatActivity() {



    private var activityNewsBinding : ActivityMainBinding? = null
    val binding get() = activityNewsBinding!!

    private val REQUEST_CODE_SCREENSHOT : Int = 111;
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private lateinit var mediaProjection: MediaProjection
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var bDevice : BluetoothDevice? = null
    public  var isConnected : Boolean = false

    // UUID for the Bluetooth service (you can generate your own UUID)
   // private val MY_UUID = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")

    // MAC address of the Bluetooth device you want to connect to
 //   private val DEVICE_ADDRESS = "00:00:00:00:00:00" // Replace with your device's MAC address

    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION

    )

    private val REQUEST_ENABLE_BLUETOOTH = 1

    val devicesList: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit  var adapter : ArrayAdapter<String>

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val imageUri = intent?.getStringExtra("imageUri")
            val connect = intent?.getStringExtra("connect")
            imageUri?.let {
                val uri = Uri.parse(imageUri)
                binding.iv.setImageURI(uri)
               if (connect.equals("true")){
                   Toast.makeText(this@MainActivity,"You gets connected ",Toast.LENGTH_SHORT).show()
                   try {
                       val bitmap = uriToBitmap(uri)
                       val imageFile = saveImageToExternalStorage(bitmap!!)
                      // sendImageViaBluetooth(imageFile)
                      // sendImageViaBluetooth(uri)
                   } catch (e: IOException) {
                       e.printStackTrace()
                   }
               }else{
                   Toast.makeText(this@MainActivity,"You still not  connected ",Toast.LENGTH_SHORT).show()
               }

            }
        }
    }
    fun uriToBitmap( uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToFile(bitmap: Bitmap, file: File): File? {
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

//    private val bluetoothAdapter : BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothManager.adapter
//    }

//    private val serviceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            val binder = service as ScreenCaptureService.LocalBinder
//            binder.getService().setCallback(this@MainActivity)
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            // Handle service disconnect
//        }
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityNewsBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot());

        handler = Handler()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


        // Check if Bluetooth is supported on the device
        adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        binding.listView.adapter = adapter


        binding.startBtn.setOnClickListener {
            checkBluetoothConnection()
//            try {
//                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.userapp)
//                val imageFile = saveImageToExternalStorage(bitmap)
//                sendImageViaBluetooth(imageFile)
//                // sendImageViaBluetooth(uri)
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
        }

        binding.stopBtn.setOnClickListener {
            stopScreenshotService()
        }
    }
    private fun checkBluetoothConnection(){
        if (bluetoothAdapter == null) {
            Toast.makeText(this,"Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }else{
             checkBluetoothPermissions()
        }
    }

    private fun checkBluetoothPermissions() {
        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ENABLE_BLUETOOTH)
        } else {
            // Bluetooth permissions are already granted
            // Check if Bluetooth is enabled
            checkBluetoothEnabled()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkBluetoothEnabled(){
        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this,"Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(enableIntent)
        }else{
           checkConnectedBluetoothDevice()
        }
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>?): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Bluetooth permissions granted
                // Check if Bluetooth is enabled
                checkBluetoothEnabled()
            } else {
                Toast.makeText(this,"You need to grant bluetooth permissions", Toast.LENGTH_SHORT).show()

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                showCustomDialog(resultCode,data)
            }
        }else if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth was enabled by the user
                // Continue with your Bluetooth operations
                checkConnectedBluetoothDevice()
            } else {
               Toast.makeText(this,"Please Enable Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopScreenshotService() {
   if (isServiceRunning(this,ScreenService::class.java)){
       val serviceIntent = Intent(this, ScreenService::class.java)
       stopService(serviceIntent)
       unregisterReceiver(broadcastReceiver)
       Toast.makeText(this,"ScreenShot stopped",Toast.LENGTH_SHORT).show()
   }else{
     Toast.makeText(this,"No screenshot happening", Toast.LENGTH_SHORT).show()
   }
}
   override fun onStart() {
        super.onStart()
       registerReceiver(broadcastReceiver, IntentFilter("com.yourpackage.ACTION_SCREENSHOT"))
  }

    override fun onStop() {
        super.onStop()
      //  unregisterReceiver(broadcastReceiver)
    }

    private fun showCustomDialog( resultCode : Int, data : Intent) {
        val dialogView = layoutInflater.inflate(R.layout.frequencyselect, null)
        val spinner: Spinner = dialogView.findViewById(R.id.spinner)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.spinner_items,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val cancelButton: Button = dialogView.findViewById(R.id.cancelButton)
        val startButton: Button = dialogView.findViewById(R.id.startButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        cancelButton.setOnClickListener {
           dialog.dismiss()
        }

        startButton.setOnClickListener {
            // Add your start button logic here
            val serviceIntent = Intent(this, ScreenService::class.java)
            serviceIntent.putExtra("resultCode", resultCode)
            serviceIntent.putExtra("resultData", data)
            serviceIntent.putExtra("frequency",spinner.selectedItem.toString().toLong())
            serviceIntent.putExtra("deviceaddress", bDevice!!.address)

            //spinner.selectedItem.toString().toLong()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Toast.makeText(this, "above", Toast.LENGTH_SHORT).show()
                startForegroundService(serviceIntent)
            } else {
                Toast.makeText(this, "lower", Toast.LENGTH_SHORT).show()
                startService(serviceIntent)
            }
            Toast.makeText(this, "ScreenShot Process Started", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun  checkConnectedBluetoothDevice() {

        val customDialog = CustomDialog(this@MainActivity)
        customDialog.show()

//        if(!isAnyBluetoothDeviceConnected(this)){
//            if (!getConnectedBluetoothDeviceMac().equals(null)){
//                val device = bluetoothAdapter!!.getRemoteDevice(getConnectedBluetoothDeviceMac())
//
//                try {
//                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
//                    bluetoothSocket?.connect()
//                    outputStream = bluetoothSocket?.outputStream
//                    Toast.makeText(this,device.name,Toast.LENGTH_SHORT).show()
////                    mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
////                    startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 111)
////                    registerReceiver(broadcastReceiver, IntentFilter("com.yourpackage.ACTION_SCREENSHOT"))
//                } catch (e: IOException) {
//                    Log.e(ContentValues.TAG, "Error connecting to Bluetooth device: ${e.message}")
//                }
//            }else{
//                Toast.makeText(this,"Could not retrieve Bluetooth address",Toast.LENGTH_SHORT).show()
//            }
//        }else{
////            val intent = Intent(this, ListActivity::class.java)
////            startActivity(intent)
//            val customDialog = CustomDialog(this@MainActivity)
//            customDialog.show()
//
//        }
    }

    fun handleBluetoothConnectionResult(connected: Boolean, device: BluetoothDevice?) {
        if (connected) {
            Toast.makeText(this, "Selected device :  ${device?.name}", Toast.LENGTH_SHORT).show()
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 111)
            bDevice = device
//            registerReceiver(broadcastReceiver, IntentFilter("com.yourpackage.ACTION_SCREENSHOT"))
        } else {
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show()
        }
        // Add any additional logic here if needed
    }


//    fun isBluetoothDeviceConnected(): Boolean {
//        var connected: Boolean? = false
//        val profileManager = bluetoothAdapter?.getProfileProxy(applicationContext, object : BluetoothProfile.ServiceListener {
//            override fun onServiceDisconnected(profile: Int) {
//                // Handle disconnection if needed
//            }
//
//            @SuppressLint("MissingPermission")
//            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
//                val connectedDevices = proxy.connectedDevices
//                for (device in connectedDevices) {
//                    if (device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
//                        // Bluetooth Classic device connected
//                        connected = true
//                        break
//                    }
//                }
//            }
//        }, BluetoothProfile.)
//
//        return connected!!
//    }
@SuppressLint("MissingPermission")
fun isAnyBluetoothDeviceConnected(context: Context): Boolean {

    val bm = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    val devices = bm.getConnectedDevices(BluetoothProfile.GATT)
    return devices.isEmpty()



//    val devices =  bluetoothAdapter.bondedDevices.toMutableList()
//    if (devices.isEmpty()){
//        return false
//    }else{
//
//    }
}

    fun getConnectedBluetoothDeviceMac(): String? {
        var connectedDeviceMac: String? = null

        bluetoothAdapter?.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceDisconnected(profile: Int) {
                // Handle disconnection if needed
            }

            @SuppressLint("MissingPermission")
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val connectedDevices = proxy.connectedDevices
                for (device in connectedDevices) {

                    if (device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                        // Bluetooth Classic device connected
                        connectedDeviceMac = device.address
                        break
                    }
                }
                bluetoothAdapter!!.closeProfileProxy(profile, proxy)
            }
        }, BluetoothProfile.A2DP)

        return connectedDeviceMac
    }


    @Throws(IOException::class)
    private fun saveImageToExternalStorage(bitmap: Bitmap): File {
        val storageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MyApp")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val imageFile = File(storageDir, "shared_image.jpg")
        FileOutputStream(imageFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }

        return imageFile
    }

    private fun sendImageViaBluetooth(imageFile: File) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            //val uri: Uri =  Uri.fromFile(imageFile)

            val uri = FileProvider.getUriForFile(
                this@MainActivity,
                this@MainActivity.getPackageName() + ".provider",imageFile
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//            val uri: Uri = FileProvider.getUriForFile(
//                this@MainActivity, "${BuildConfig.APPLICATION_ID}.provider", imageFile
//            )
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.android.bluetooth")
        }
        startActivityForResult(Intent.createChooser(intent, "Send Image"),11)
    }

//    private fun sendImageViaBluetooth(uri: Uri) {
//        val i = Intent(Intent.ACTION_SEND).apply {
//            type = "image/jpeg"
//
//            putExtra(Intent.EXTRA_STREAM, uri)
//            setPackage("com.android.bluetooth")
//        }
//        startActivity(Intent.createChooser(i, "Send Image"))
//    }
}