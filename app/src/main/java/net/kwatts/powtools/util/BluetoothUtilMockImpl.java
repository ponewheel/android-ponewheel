package net.kwatts.powtools.util;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;

import net.kwatts.powtools.App;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

import java.util.Random;
import java.util.UUID;

import timber.log.Timber;

import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicFirmwareRevision;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicHardwareRevision;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicLifetimeOdometer;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicSpeed;

public class BluetoothUtilMockImpl implements BluetoothUtil{
    private MainActivity mainActivity;
    private OWDevice mOWDevice;
    Handler mockLoopHandler = new Handler();
    private boolean isScanning = false;


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
        setIsScanning(false);
    }

    @Override
    public void disconnect() {
        Timber.d("disconnect");
        mockLoopHandler.removeCallbacksAndMessages(null);
        setIsScanning(false);
    }

    private void setIsScanning(boolean shouldBeScanning) {
        isScanning = shouldBeScanning;
        mainActivity.invalidateOptionsMenu();
    }

    @Override
    public boolean isConnected() {
        Timber.d("isConnected");
        return mOWDevice.isConnected.get();
    }

    @Override
    public boolean isScanning() {
        Timber.d("isScanning " + isScanning);
        return isScanning;
    }

    @Override
    public void startScanning() {
        Timber.d("setIsScanning");
        setIsScanning(true);
        updateLog("connected");
        onConnected();
    }

    private void onConnected() {
        mOWDevice.isConnected.set(true);
        mainActivity.updateBatteryRemaining(100);

        String deviceMacAddress = "MAC1234";
        String deviceMacName = "Ocho";
        mOWDevice.deviceMacAddress.set(deviceMacAddress);
        mOWDevice.deviceMacName.set(deviceMacName);

        setIntCharacteristic(OnewheelCharacteristicHardwareRevision, 1);
        setIntCharacteristic(OnewheelCharacteristicFirmwareRevision, 2);
        setIntCharacteristic(OnewheelCharacteristicLifetimeOdometer, 1000);

        Random random = new Random();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Timber.d("on Mock Loop");
                setIntCharacteristic(OnewheelCharacteristicSpeed, random.nextInt(600));
                mockLoopHandler.postDelayed(this, App.INSTANCE.getSharedPreferences().getLoggingFrequency());
            }
        };
        mockLoopHandler.postDelayed(runnable, App.INSTANCE.getSharedPreferences().getLoggingFrequency());
    }

    void setIntCharacteristic(String characteristic2, int v) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(characteristic2), 0, 0);
        characteristic.setValue(Util.intToShortBytes(v));
        mOWDevice.processUUID(characteristic);
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

    private void updateLog(String s) {
        mainActivity.updateLog("mock: " + s);
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status){
        setIsScanning(false);
    }
}
