package com.example.screenshotapp.bluetoothscanner.Connecting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendReceive extends Thread
{
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;


    int REQUEST_ENABLE_BLUETOOTH=1;

    public SendReceive (BluetoothSocket socket)
    {
        bluetoothSocket=socket;
        InputStream tempIn=null;

        OutputStream tempOut=null;

        try {
            tempIn= bluetoothSocket.getInputStream();
            tempOut= bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        inputStream=  tempIn;
        outputStream= tempOut;
    }

    public void run() {

        byte[] buffer = null;
        int numberOfBytes = 0;
        int index = 0;
        boolean flag = true;

        while (true) {
            if (flag) {
                try {
                    byte[] temp = new byte[inputStream.available()];
                    if (inputStream.read(temp) > 0) {
                        numberOfBytes = Integer.parseInt(new String(temp, "UTF-8"));
                        buffer = new byte[numberOfBytes];
                        flag = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    byte[] data = new byte[inputStream.available()];
                    int numbers = inputStream.read(data);

                    System.arraycopy(data, 0, buffer, index, numbers);
                    index = index + numbers;

                    if (index == numberOfBytes) {
                        handler.obtainMessage(STATE_MESSAGE_RECEIVED, numberOfBytes, -1, buffer).sendToTarget();
                        flag = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void write(byte[] bytes)
    {
        try {
            outputStream.write(bytes);
            outputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   public  Handler handler=new Handler(new Handler.Callback() {
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
