package net.kwatts.powtools.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;

import net.kwatts.powtools.App;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.DeviceStatus;
import net.kwatts.powtools.model.OWDevice;

import java.util.Random;
import java.util.UUID;

import timber.log.Timber;

import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicBatteryTemp;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicFirmwareRevision;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicHardwareRevision;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicLifetimeOdometer;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicSpeedRpm;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicStatusError;
import static net.kwatts.powtools.model.OWDevice.OnewheelCharacteristicTemperature;

public class BluetoothUtilMockImpl implements BluetoothUtil{
    MainActivity mainActivity;
    private OWDevice owDevice;
    Handler mockLoopHandler = new Handler();
    private boolean isScanning = false;


    @Override
    public void init(MainActivity mainActivity, OWDevice mOWDevice) {
        Timber.d("init");
        this.mainActivity = mainActivity;
        this.owDevice = mOWDevice;
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
        return owDevice.isConnected.get();
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
        owDevice.isConnected.set(true);

        String deviceMacAddress = "MAC1234";
        String deviceMacName = "Ocho";
        owDevice.deviceMacAddress.set(deviceMacAddress);
        owDevice.deviceMacName.set(deviceMacName);

        setIntCharacteristic(OnewheelCharacteristicHardwareRevision, 1);
        setIntCharacteristic(OnewheelCharacteristicFirmwareRevision, 2);
        setIntCharacteristic(OnewheelCharacteristicLifetimeOdometer, 1000);

        Random random = new Random();
        Runnable runnable = new Runnable() {
            @SuppressLint("VisibleForTests")
            @Override
            public void run() {
                Timber.d("on Mock Loop");
                setIntCharacteristic(OnewheelCharacteristicSpeedRpm, random.nextInt(600));

                byte[] temp = new byte[2];
                int controllerTemp = (int) Util.far2cel(random.nextInt(30) + 90);
                temp[0] = (byte) controllerTemp;  // controller temp
                temp[1] = (byte) ((int) Util.far2cel(random.nextInt(30) + 120)); // motor temp
                setByteCharacteristic(OnewheelCharacteristicTemperature, temp);

                temp[0] = 0; // unused?
                temp[1] = (byte) Util.far2cel(random.nextInt(30) + 60); // battery temp
                setByteCharacteristic(OnewheelCharacteristicBatteryTemp, temp);

                byte[] deviceStatus = DeviceStatus.toByteArray(
                        random.nextBoolean(),
                        random.nextBoolean(),
                        random.nextBoolean(),
                        random.nextBoolean(),

                        random.nextBoolean(),
                        random.nextBoolean(),
                        random.nextBoolean(),
                        random.nextBoolean()
                );

                setByteCharacteristic(OnewheelCharacteristicStatusError, deviceStatus);
                mainActivity.updateBatteryRemaining(random.nextInt(20) + 80);

                mockLoopHandler.postDelayed(this, App.INSTANCE.getSharedPreferences().getLoggingFrequency());
            }
        };
        mockLoopHandler.postDelayed(runnable, App.INSTANCE.getSharedPreferences().getLoggingFrequency());
    }

    void setBoolCharacteristic(String mockOnewheelCharacteristic, boolean v) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(mockOnewheelCharacteristic), 0, 0);
        characteristic.setValue(Util.boolToShortBytes(v));
        owDevice.processUUID(characteristic);
    }

    void setByteCharacteristic(String mockOnewheelCharacteristic, byte[] v) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(mockOnewheelCharacteristic), 0, 0);
        characteristic.setValue(v);
        owDevice.processUUID(characteristic);
    }

    void setIntCharacteristic(String mockOnewheelCharacteristic, int v) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(mockOnewheelCharacteristic), 0, 0);
        characteristic.setValue(Util.intToShortBytes(v));
        owDevice.processUUID(characteristic);
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
