package net.kwatts.powtools;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by kwatts on 4/22/16.
 */
public interface DeviceInterface {
    void processUUID(BluetoothGattCharacteristic c);
    String getName();
    String getCSVHeader();
    String toCSV();
}