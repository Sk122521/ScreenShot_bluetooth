package com.example.screenshotapp.bluetoothscanner.Connecting;

import static com.example.screenshotapp.bluetoothscanner.Connecting.SendReceive.STATE_CONNECTED;
import static com.example.screenshotapp.bluetoothscanner.Connecting.SendReceive.STATE_CONNECTING;
import static com.example.screenshotapp.bluetoothscanner.Connecting.SendReceive.STATE_CONNECTION_FAILED;
import static com.example.screenshotapp.bluetoothscanner.Connecting.SendReceive.STATE_LISTENING;
import static com.example.screenshotapp.bluetoothscanner.Connecting.SendReceive.STATE_MESSAGE_RECEIVED;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;


public class ServerClass extends Thread
{
    private BluetoothServerSocket serverSocket;
    private  BluetoothAdapter bluetoothAdapter;

    private static final String APP_NAME = "BTChat";
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    SendReceive sendReceive;

    public ServerClass(){
        try {
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run()
    {
        BluetoothSocket socket=null;

        while (socket==null)
        {
            try {
                Message message=Message.obtain();
                message.what=STATE_CONNECTING;
                handler.sendMessage(message);

                socket=serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }

            if(socket!=null)
            {
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive=new SendReceive(socket);
                sendReceive.start();

                break;
            }
        }
    }
    public Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_LISTENING:
                    //status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    // status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    //   status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    // status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:

                    byte[] readBuff= (byte[]) msg.obj;
                    Bitmap bitmap= BitmapFactory.decodeByteArray(readBuff,0,msg.arg1);


                    break;
            }
            return true;
        }
    });
}
