package net.kwatts.powtools.util;

import android.bluetooth.BluetoothGattCharacteristic;

import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

import java.util.UUID;

import timber.log.Timber;

public class BluetoothUtilMockImpl implements BluetoothUtil{
    private MainActivity mainActivity;
    private OWDevice mOWDevice;

    @Override
    public void init(MainActivity mainActivity, OWDevice mOWDevice) {
        Timber.d("init");
        this.mainActivity = mainActivity;
        this.mOWDevice = mOWDevice;
    }

    @Override
    public void reconnect(MainActivity activity) {
        mainActivity = activity;
        Timber.d("reconnect");

    }

    @Override
    public void stopScanning() {
        Timber.d("stopScanning");
    }

    @Override
    public void disconnect() {
        Timber.d("disconnect");

    }

    @Override
    public boolean isConnected() {
        Timber.d("isConnected");
        return false;
    }

    @Override
    public boolean isScanning() {
        Timber.d("isScanning");

        return false;
    }

    @Override
    public void startScanning() {
        Timber.d("startScanning");

        mainActivity.updateBatteryRemaining(100);
    }

    @Override
    public BluetoothGattCharacteristic getCharacteristic(String onewheelCharacteristicLightingMode) {
        Timber.d("getCharacteristic" + onewheelCharacteristicLightingMode);
        return new BluetoothGattCharacteristic(UUID.fromString("asdf"),1,1);
    }

    @Override
    public void writeCharacteristic(BluetoothGattCharacteristic lc) {
        Timber.d("writeChar" + lc);
    }
}
