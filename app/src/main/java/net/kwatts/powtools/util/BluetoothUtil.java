package net.kwatts.powtools.util;

import android.bluetooth.BluetoothGattCharacteristic;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

public interface BluetoothUtil {
    void init(MainActivityC mainActivity, OWDevice mOWDevice);
    void reconnect(MainActivityC activity);
    void stopScanning();
    void disconnect();
    boolean isConnected();
    boolean isScanning();
    void startScanning();
    BluetoothGattCharacteristic getCharacteristic(String onewheelCharacteristicLightingMode);
    void writeCharacteristic(BluetoothGattCharacteristic lc);
}
