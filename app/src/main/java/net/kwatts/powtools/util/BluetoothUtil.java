package net.kwatts.powtools.util;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import io.reactivex.Observable;
import net.kwatts.powtools.model.ConnectionStatus;
import net.kwatts.powtools.model.OWDevice;

public interface BluetoothUtil {
    void init(MainActivityDelegate mainActivity, OWDevice mOWDevice, BluetoothManager btManager);
    void stopScanning();
    void disconnect();
    boolean isConnected();
    boolean isScanning();
    void startScanning();
    BluetoothGattCharacteristic getCharacteristic(String onewheelCharacteristicLightingMode);
    void writeCharacteristic(BluetoothGattCharacteristic lc);
    boolean isBtAdapterAvailable(Context context);
    Observable<ConnectionStatus> getConnectionStatus();
}
