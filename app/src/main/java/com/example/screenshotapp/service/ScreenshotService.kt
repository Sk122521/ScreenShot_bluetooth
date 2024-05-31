package com.example.screenshotapp.service


import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.screenshotapp.MainActivity
import com.example.screenshotapp.bluetoothscanner.ConnectThread
import com.example.screenshotapp.bluetoothscanner.Connecting.SerialListener
import com.example.screenshotapp.bluetoothscanner.Connecting.SerialSocket
import com.example.screenshotapp.bluetoothscanner.ConnectionCallback
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayDeque


class ScreenService() : Service() , ConnectionCallback ,SerialListener {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    //private var displayMetrics: DisplayMetrics? = null
    private var windowManager: WindowManager? = null
    private var screenDensity: Int = 0
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private lateinit var handler: Handler
    private var captureInterval : Long?  = null
    private var bDevice : BluetoothDevice? = null
    //var socket: BluetoothSocket? = null
    private var connected = false
    private var socket: SerialSocket? = null
    private val listener: SerialListener? = null
    private var mainLooper: Handler? = null
    private var binder: IBinder? = null
    private var queue1: ArrayDeque<QueueItem>? = null
    private var queue2: ArrayDeque<QueueItem>? = null
    private var lastRead: QueueItem? = null
    var connectThread : ConnectThread? = null
    var isConnected : Boolean =  false




    private val captureRunnable : Runnable = object : Runnable {
        override fun run() {
            startCapture()
            captureInterval?.let { handler?.postDelayed(this, it*1000) }
        }
    }


  init {
      mainLooper = Handler(Looper.getMainLooper())
      queue1 = ArrayDeque()
      queue2 = ArrayDeque()
      lastRead = QueueItem(QueueType.Read)
  }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val resultData = intent?.getParcelableExtra<Intent>("resultData")
        captureInterval = intent?.getLongExtra("frequency", 5)
        val address = intent?.getStringExtra("deviceaddress")

        if (address != null){
            bDevice =  BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)
            Toast.makeText(this,address,Toast.LENGTH_SHORT).show()
        }


       // connect()
        connectThread = ConnectThread.getInstance(bDevice,this)
        connectThread!!.start()
        mediaProjection = resultCode?.let { resultData?.let { it1 -> mediaProjectionManager?.getMediaProjection(it, it1) } }

        captureInterval?.let { handler.postDelayed(captureRunnable , it*1000) }
        return START_STICKY
    }

    private fun startCapture() {

        imageReader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 2)
        mediaProjection?.createVirtualDisplay("ScreenCapture", displayWidth, displayHeight, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader?.acquireLatestImage()
            if (image!=null){
                //Toast.makeText(this,"gotand gpat",Toast.LENGTH_SHORT).show()
                image?.let {
                    val buffer = it.planes[0].buffer
                    buffer.rewind()

                    val pixelStride = it.planes[0].pixelStride
                    val rowStride = it.planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * image.width

                  // val bitmap = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_4444)
                    val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)

                    bitmap.copyPixelsFromBuffer(buffer)
                  //  sendScreenshot(bitmap)


//                    val connectThread = ConnectThread(bDevice, this, bitmap,socket)
//                    connectThread.start()

//                    val handler = Handler(Looper.getMainLooper())
//                    val sendBitmapRunnable = object : Runnable {
//                        override fun run() {
//
//                        }
//                    }
//                    handler.post(sendBitmapRunnable)

                    //  connectThread!!.sendImage(bitmap)



                    // Process the bitmap (e.g., apply filters, resize, etc.)
                  //  val processedBitmap = processBitmap(bitmap)

                    // Save the processed bitmap to external storage
                    saveBitmapToStorage(bitmap)
                }
            }
            //else{
//                Toast.makeText(this,"null image",Toast.LENGTH_SHORT).show()
//                Log.d("MSG","MSG")
//            }
            // Process and save the image here
            image?.close()
            imageReader?.close()
        }, handler)
    }

//    private fun sendScreenshot(bitmap: Bitmap) {
//        try {
//            val byteArrayOutputStream = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
//            val byteArray = byteArrayOutputStream.toByteArray()
//
//            outputStream?.write(byteArray)
//            Log.d(TAG, "Screenshot sent over Bluetooth")
//        } catch (e: IOException) {
//            Log.e(TAG, "Error sending screenshot over Bluetooth: ${e.message}")
//        }
//    }

    fun sendBitmapOverBluetooth(bluetoothSocket: BluetoothSocket?, bitmap: Bitmap) {
        try {
            bluetoothSocket?.let { socket ->
                // Convert Bitmap to byte array
                val outputStream: OutputStream = socket.outputStream
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray: ByteArray = byteArrayOutputStream.toByteArray()

                // Write byte array to Bluetooth output stream
                outputStream.write(byteArray)
                outputStream.flush()
                outputStream.close()
            }
        } catch (e: IOException) {
            // Handle IOException
        }
    }
    private fun processBitmap(bitmap: Bitmap): Bitmap {
        // Placeholder for image processing logic
        // Example: Resize the image
        val targetWidth = 512
        val targetHeight = (bitmap.height.toFloat() / bitmap.width.toFloat() * targetWidth).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
    }
    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        val fileName = "screenshot_${System.currentTimeMillis()}.png"
        //val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(dir, fileName)

        try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                return Uri.fromFile(imageFile)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private fun saveBitmapToStorage(bitmap: Bitmap) {
      //  Log.d("MSG","MSG")
        val uri = saveBitmapToUri(bitmap)

        Toast.makeText(this,"ScreenShot taken",Toast.LENGTH_SHORT).show()

        // Send the URI to the MainActivity
        uri?.let { imageUri ->
            val intent = Intent("com.yourpackage.ACTION_SCREENSHOT")
            intent.putExtra("imageUri", imageUri.toString())
            intent.putExtra("connect", isConnected.toString())
            sendBroadcast(intent)
        }

//        val fileName = "screenshot_${System.currentTimeMillis()}.png"
//        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//        val imageFile = File(dir, fileName)
//
//        try {
//            FileOutputStream(imageFile).use { fos ->
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
//            }
//            // Notify the user or update UI that the screenshot is saved
//        } catch (e: IOException) {
//            e.printStackTrace()
//            // Handle the exception (e.g., show error message to the user)
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        imageReader?.close()
        handler?.removeCallbacks(captureRunnable)
//        connectThread!!.stop()
//        connectThread = null
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    override fun onCreate() {
        super.onCreate()


        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)

      //  displayMetrics = resources.displayMetrics
        screenDensity = displayMetrics.densityDpi ?: DisplayMetrics.DENSITY_DEFAULT
        displayWidth = displayMetrics.widthPixels ?: 720
        displayHeight = displayMetrics.heightPixels ?: 1280

        handler = Handler(Looper.getMainLooper())

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        }
        // startScreenCapture(mediaProjectionData)


        // Check if Bluetooth is enabled, if not, request to turn it on

        // Initialize handler for Bluetooth communication
//        handler = object : Handler(Looper.getMainLooper()) {
//            override fun handleMessage(msg: Message) {
//                if (msg.what == MESSAGE_WRITE) {
//                    // Handle write messages if needed
//                }
//            }
//        }


    }

    private fun connect() {
        try {
           // status("connecting...")
          //  connected = Connected.Pending
            val socket = SerialSocket(this, bDevice)
            socket.connect(this);
            socket.start()
            this.socket = socket;
            Toast.makeText(this, "yes", Toast.LENGTH_SHORT)
                .show()
          //  connected  = true
        } catch (e: Exception) {
            Toast.makeText(this, "no", Toast.LENGTH_SHORT)
                .show()
           onSerialConnectError(e)
        }
    }

    override fun onSerialConnect() {
        Toast.makeText(this,"connected....", Toast.LENGTH_SHORT).show()
        connected = true
    }

    override fun onSerialConnectError(e: java.lang.Exception?) {
        if (connected) {
            synchronized(this) {
                if (listener != null) {
                    mainLooper!!.post {
                        if (listener != null) {
                            listener.onSerialConnectError(e)
                        } else {
                            queue1!!.add(QueueItem(QueueType.ConnectError, e))
                            disconnect()
                        }
                    }
                } else {
                    queue2!!.add(QueueItem(QueueType.ConnectError, e))
                    disconnect()
                }
            }
        }else{
//            Toast.makeText(this,"connected....", Toast.LENGTH_SHORT).show()
        }
    }
    fun disconnect() {
        connected = false // ignore data,errors while disconnecting
       // cancelNotification()
        if (socket != null) {
           // socket!!.disconnect()
            socket = null
        }
    }
    override fun onSerialRead(data: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun onSerialRead(datas: ArrayDeque<ByteArray>?) {
        TODO("Not yet implemented")
    }

    override fun onSerialIoError(e: java.lang.Exception?) {
        TODO("Not yet implemented")
    }

    override fun onConnectionResult(connected: Boolean) {
        if (connected){
            isConnected =  true
            Toast.makeText(this,"Connected", Toast.LENGTH_SHORT).show()
        }else{
            isConnected = false
            Toast.makeText(this,"Failed to connect", Toast.LENGTH_SHORT).show()
        }
    }

    private enum class QueueType {
        Connect, ConnectError, Read, IoError
    }

    private class QueueItem {
        var type: QueueType
        var datas: ArrayDeque<ByteArray>? = null
        var e: java.lang.Exception? = null

        internal constructor(type: QueueType) {
            this.type = type
            if (type == QueueType.Read) init()
        }

        internal constructor(type: QueueType, e: java.lang.Exception?) {
            this.type = type
            this.e = e
        }

        internal constructor(type: QueueType, datas: ArrayDeque<ByteArray>?) {
            this.type = type
            this.datas = datas
        }

        fun init() {
            datas = ArrayDeque()
        }

        fun add(data: ByteArray) {
            datas!!.add(data)
        }
    }

}

