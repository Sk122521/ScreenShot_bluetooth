package com.example.screenshotapp.bluetoothscanner;



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.example.screenshotapp.R;
import com.example.screenshotapp.bluetoothscanner.Connecting.SendReceive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;


public class ConnectThread extends Thread {

    private static ConnectThread instance = null;
    private  BluetoothSocket socket;
    private final BluetoothDevice device;
    private final ConnectionCallback callback;
    private final BluetoothAdapter bluetoothAdapter;

    private OutputStream outputStream;

    private Bitmap bitmap;

    private Boolean isConnected =  false;

    private Handler Handler = new Handler(Looper.getMainLooper());

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;


    int REQUEST_ENABLE_BLUETOOTH=1;

    private static final String APP_NAME = "BTChat";
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    SendReceive sendReceive;


    public ConnectThread(BluetoothDevice device,ConnectionCallback callback) {
        BluetoothSocket tmp = null;
        this.device = device;
        this.callback = callback;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
       // this.socket = socket;

        try {
            UUID uuid = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB");
            tmp = device.createRfcommSocketToServiceRecord(uuid);

        } catch (IOException e) {
            Log.e("BluetoothConnection", "Socket's create() method failed", e);
        }

        socket = tmp;
    }

    public void run()  {
        // Cancel discovery because it otherwise slows down the connection.
      //  bluetoothAdapter.cancelDiscovery();

        try {

            socket.connect();
            startKeepAlive(socket);
            isConnected = true;
            Handler.post(new Runnable() {
                @Override
                public void run() {
                   callback.onConnectionResult(true); // Notify success
                }
            });
            // Connection successful
            Log.d("BluetoothConnection", "Connected to " + device.getName());

        } catch (IOException connectException) {
            isConnected = false;
            Log.e("BluetoothConnection", "Unable to connect; closing the socket", connectException);
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e("BluetoothConnection", "Could not close the client socket", closeException);
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                   callback.onConnectionResult(false); // Notify success
                }
            });
        }

        // Manage the connection (in a separate thread)
      //  manageConnectedSocket(socket);
    }

    public void cancel() {
        try {
            if (isConnected){
                socket.close();
            }
           // mainHandler.removeCallbacks(keepAliveRunnable);
        } catch (IOException e) {
            Log.e("BluetoothConnection", "Could not close the client socket", e);
        }
    }

    private Handler mainHandler = new Handler(Looper.getMainLooper());
//
   private Runnable keepAliveRunnable;

    private void startKeepAlive(final BluetoothSocket socket) {
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    // Send a simple keep-alive message
                    outputStream.write("keep-alive".getBytes());
                    outputStream.flush();


                    Log.e("KeepAlive", " keep-getting-alive");
                } catch (IOException e) {
                    isConnected = false;
                    Log.e("KeepAlive", "Failed to send keep-alive", e);
                    // Handle error: stop keep-alive and close socket
                    stopKeepAlive();
                    cancel();
                }
                // Schedule the next keep-alive message
                mainHandler.postDelayed(this, 1000); // 10 seconds interval
            }
        };
        // Start the first keep-alive message
        mainHandler.post(keepAliveRunnable);
    }
//
    private void stopKeepAlive() {
        if (mainHandler != null && keepAliveRunnable != null) {
            mainHandler.removeCallbacks(keepAliveRunnable);
        }
    }

    public void sendBitmapOverBluetooth( Bitmap bitmap) {

//            try {
//                // Convert Bitmap to byte array
//                OutputStream outputStream = socket.getOutputStream();
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//                byte[] byteArray = byteArrayOutputStream.toByteArray();
//
//                // Write byte array to Bluetooth output stream
//                outputStream.write(byteArray);
//                outputStream.flush();
//                outputStream.close();
//                Log.e("KeepAlive", " keep-getting-alive");
//            } catch (IOException e) {
//                // Handle IOException
//               // Log.e("KeepAlive", "Failed to send keep-alive", e);
//                e.printStackTrace();
//            }

    }

    public static synchronized ConnectThread getInstance(BluetoothDevice device,ConnectionCallback callback) {
        if (instance == null) {
            instance = new ConnectThread(device,callback);
        }
        return instance;
    }
    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_LISTENING:
                    // status.setText("Listening");
                    //will try callback here
                    break;
                case STATE_CONNECTING:
                  //  status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                   // status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                  //  status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:

                    byte[] readBuff= (byte[]) msg.obj;
                    Bitmap bitmap=BitmapFactory.decodeByteArray(readBuff,0,msg.arg1);

                  //  imageView.setImageBitmap(bitmap);

                    break;
            }
            return true;
        }
    });
//    public void sendImage(Bitmap bitmap) {
//        ByteArrayOutputStream stream=new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG,50,stream);
//        byte[] imageBytes=stream.toByteArray();
//
//        int subArraySize=400;
//
//        sendReceive.write(String.valueOf(imageBytes.length).getBytes());
//
//        for(int i=0;i<imageBytes.length;i+=subArraySize){
//            byte[] tempArray;
//            tempArray= Arrays.copyOfRange(imageBytes,i,Math.min(imageBytes.length,i+subArraySize));
//            sendReceive.write(tempArray);
//        }
//    }
}





