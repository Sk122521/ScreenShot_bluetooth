package com.example.screenshotapp.bluetoothscanner;

/**
 * Created by Matt on 5/12/2015.
 */
public class DeviceItem {

    private String deviceName;
    private String address;
    private boolean connected;

    public String getDeviceName() {
        return deviceName;
    }

    public boolean getConnected() {
        return connected;
    }

    public String getAddress() {
        return address;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public DeviceItem(String name, String address, Boolean connected){
        this.deviceName = name;
        this.address = address;
        this.connected = connected;
    }
}
