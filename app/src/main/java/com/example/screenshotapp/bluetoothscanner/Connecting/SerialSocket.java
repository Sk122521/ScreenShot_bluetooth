package com.example.screenshotapp.bluetoothscanner.Connecting;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;

public class SerialSocket extends Thread {

    private static final UUID BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

   // private final BroadcastReceiver disconnectBroadcastReceiver;

    private final Context context;
    private SerialListener listener;
    private final BluetoothDevice device;
    private BluetoothSocket socket;
    private boolean connected;
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    public SerialSocket(Context context, BluetoothDevice device) {
        Toast.makeText(context.getApplicationContext(), "called yes", Toast.LENGTH_SHORT)
                .show();


        this.context = context;
        this.device = device;
        BluetoothSocket tmp = null;
        try {
            tmp =device.createRfcommSocketToServiceRecord(MY_UUID);
           // tmp = device.createRfcommSocketToServiceRecord(BLUETOOTH_SPP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = tmp;
//        disconnectBroadcastReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if(listener != null)
//                    listener.onSerialIoError(new IOException("background disconnect"));
//                disconnect(); // disconnect now, else would be queued until UI re-attached
//            }
//        };
    }
//
//    String getName() {
//        return device.getName() != null ? device.getName() : device.getAddress();
//    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    public void connect(SerialListener listener) throws IOException {
        this.listener = listener;
      //  ContextCompat.registerReceiver(context, disconnectBroadcastReceiver, new IntentFilter(Constants.INTENT_ACTION_DISCONNECT), ContextCompat.RECEIVER_NOT_EXPORTED);
        Executors.newSingleThreadExecutor().submit(this);
    }

//    public void disconnect() {
//        listener = null; // ignore remaining data and errors
//        // connected = false; // run loop will reset connected
//        if(socket != null) {
//            try {
//                socket.close();
//            } catch (Exception ignored) {
//            }
//            socket = null;
//        }
//        try {
//            context.unregisterReceiver(disconnectBroadcastReceiver);
//        } catch (Exception ignored) {
//        }
//    }

    public void write(byte[] data) throws IOException {
        if (!connected)
            throw new IOException("not connected");
        socket.getOutputStream().write(data);
    }

    public void run() { // connect & read
//        Toast.makeText(context.getApplicationContext(), "called....", Toast.LENGTH_SHORT).show();
        try {
           // Toast.makeText(context.getApplicationContext(), "connectedddd....", Toast.LENGTH_SHORT).show();
            socket.connect();
            Log.d("did  CONNECT" , "connected");
            if(listener != null)
                listener.onSerialConnect();
        } catch (Exception e) {
            Log.d("ERROR TO CONNECT" , e.getMessage());
            if(listener != null)
                listener.onSerialConnectError(e);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
            return;
        }
        connected = true;
        try {
            byte[] buffer = new byte[1024];
            int len;
            //noinspection InfiniteLoopStatement
            while (true) {
                len = socket.getInputStream().read(buffer);
                byte[] data = Arrays.copyOf(buffer, len);
                if(listener != null)
                    listener.onSerialRead(data);
            }
        } catch (Exception e) {
            connected = false;
            if (listener != null)
                listener.onSerialIoError(e);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }

}
