package net.kwatts.powtools.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import net.kwatts.powtools.App;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class BluetoothUtil {
    private static final String TAG = "BluetoothUtil";

    private static final int REQUEST_ENABLE_BT = 1;

    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private android.bluetooth.BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mGatt;
    private BluetoothGattService owGatService;

    Map<String, String> mScanResults = new HashMap<>();

    private MainActivity mainActivity;
    private OWDevice mOWDevice;

    private ScanSettings settings;
    private boolean mScanning;

    public void init(MainActivity mainActivity, OWDevice mOWDevice) {
        this.mainActivity = mainActivity;
        this.mOWDevice = mOWDevice;

        final BluetoothManager manager = (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        assert manager != null;
        mBluetoothAdapter = manager.getAdapter();
        mOWDevice.bluetoothLe.set("On");
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "BluetoothGattCallback.nConnectionStateChange: status=" + status + " newState=" + newState);
            updateLog("Bluetooth connection state change: status="+ status + " newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (gatt.getDevice().getAddress().equals(mOWDevice.deviceMacAddress.get())) {
                    onOWStateChangedToDisconnected(gatt);
                }
                updateLog("--> Closed " + gatt.getDevice().getAddress());
                Log.d(TAG, "Disconnect:" + gatt.getDevice().getAddress());
            }
        }



        @SuppressLint("WakelockTimeout")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            Log.d(TAG, "Only should be here if connecting to OW:" + gatt.getDevice().getAddress());
            owGatService = gatt.getService(UUID.fromString(OWDevice.OnewheelServiceUUID));

            if (owGatService == null) {
                if (gatt.getDevice().getName() == null) {
                    updateLog("--> " + gatt.getDevice().getAddress() + " not OW, moving on.");
                } else {
                    updateLog("--> " + gatt.getDevice().getName() + " not OW, moving on.");
                }
                return;
            }

            mGatt = gatt;
            updateLog("Hey, I found the OneWheel Service: " + owGatService.getUuid().toString());
            mainActivity.deviceConnectedTimer(true);
            mOWDevice.isConnected.set(true);
            App.INSTANCE.acquireWakeLock();
            mOWDevice.deviceMacAddress.set(mGatt.getDevice().toString());
            mOWDevice.deviceMacName.set(mGatt.getDevice().getName());
            App.INSTANCE.getSharedPreferences().saveMacAddress(
                    mOWDevice.deviceMacAddress.get(),
                    mOWDevice.deviceMacName.get()
            );

            scanLeDevice(false); // We can stop scanning...

            for(OWDevice.DeviceCharacteristic deviceCharacteristic: mOWDevice.getNotifyCharacteristics()) {
                String uuid = deviceCharacteristic.uuid.get();
                if (uuid != null && deviceCharacteristic.enabled.get()) {
                    BluetoothGattCharacteristic localCharacteristic = owGatService.getCharacteristic(UUID.fromString(uuid));
                    if (localCharacteristic != null) {
                        if (isCharacteristicNotifiable(localCharacteristic)) {
                            mGatt.setCharacteristicNotification(localCharacteristic, true);
                            BluetoothGattDescriptor descriptor = localCharacteristic.getDescriptor(UUID.fromString(OWDevice.OnewheelConfigUUID));
                            Log.d(TAG, "descriptorWriteQueue.size:" + descriptorWriteQueue.size());
                            if (descriptor == null) {
                                Log.e(TAG, uuid + " has a null descriptor!");
                            } else {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                descriptorWriteQueue.add(descriptor);
                                if (descriptorWriteQueue.size() == 1) {
                                    mGatt.writeDescriptor(descriptor);
                                }
                                Log.d(TAG, uuid + " has been set for notifications");
                            }
                        }

                    }

                }
            }

            for(OWDevice.DeviceCharacteristic dc : mOWDevice.getReadCharacteristics()) {
                if (dc.uuid.get() != null) {
                    BluetoothGattCharacteristic c = owGatService.getCharacteristic(UUID.fromString(dc.uuid.get()));
                    if (c != null) {
                        if (isCharacteristicReadable(c)) {
                            characteristicReadQueue.add(c);
                            //Read if 1 in the queue, if > 1 then we handle asynchronously in the onCharacteristicRead callback
                            //GIVE PRECEDENCE to descriptor writes.  They must all finish first.
                            Log.i(TAG, "characteristicReadQueue.size =" + characteristicReadQueue.size() + " descriptorWriteQueue.size:" + descriptorWriteQueue.size());
                            if (characteristicReadQueue.size() == 1 && (descriptorWriteQueue.size() == 0)) {
                                Log.i(TAG, dc.uuid.get() + " is readable and added to queue");
                                mGatt.readCharacteristic(c);
                            }
                        }
                    }
                }
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite: " + status);
            descriptorWriteQueue.remove();  //pop the item that we just finishing writing
            //if there is more to write, do it!
            if(descriptorWriteQueue.size() > 0) {
                gatt.writeDescriptor(descriptorWriteQueue.element());
            } else if(characteristicReadQueue.size() > 0) {
                gatt.readCharacteristic(characteristicReadQueue.element());
            }
        }
    };

    private void updateLog(String s) {
        mainActivity.updateLog(s);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            List<ScanFilter> filters_v2 = new ArrayList<>();
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(OWDevice.OnewheelServiceUUID))
                    .build();
            filters_v2.add(scanFilter);
            //c03f7c8d-5e96-4a75-b4b6-333d36230365
            mBluetoothLeScanner.startScan(filters_v2, settings, mScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
            // added 10/23 to try cleanup
            mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
        }
        mainActivity.invalidateOptionsMenu();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String deviceName = result.getDevice().getName();
            String deviceAddress = result.getDevice().getAddress();

            if (!mScanResults.containsKey(deviceAddress)) {
                Log.i(TAG, "ScanCallback.onScanResult:" + deviceName);
                mScanResults.put(deviceAddress, deviceName);

                if (deviceName == null) {
                    updateLog("Found " + deviceAddress);
                } else {
                    updateLog("Found " + deviceAddress + " (" + deviceName + ")");
                }

                if (deviceName != null) {
                    if (deviceName.startsWith("ow")) {
                        updateLog("Looks like we found our OW device (" + deviceName + ") discovering services!");
                        connectToDevice(result.getDevice());
                    }
                }

            }


        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i(TAG,"ScanCallback.onBatchScanResults.each:" + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "ScanCallback.onScanFailed:" + errorCode);
        }
    };


    public void connectToDevice(BluetoothDevice device) {
        Log.e(TAG, "connectToDevice:" + device.getName());
        device.connectGatt(mainActivity, false, mGattCallback);
    }


    private void onOWStateChangedToDisconnected(BluetoothGatt gatt) {
        updateLog("We got disconnected from our Device: " + gatt.getDevice().getAddress());
        mainActivity.deviceConnectedTimer(false);
        mOWDevice.isConnected.set(false);
        App.INSTANCE.releaseWakeLock();
        mScanResults.clear();

        if (App.INSTANCE.getSharedPreferences().shouldAutoReconnect()) {
            updateLog("Attempting to Reconnect to " + mOWDevice.deviceMacAddress.get());
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mOWDevice.deviceMacAddress.get());
            connectToDevice(device);
            //scanLeDevice(true);
        } else {
            gatt.close();

        }
        mainActivity.invalidateOptionsMenu();
    }

    public static boolean isCharacteristicWriteable(BluetoothGattCharacteristic c) {
        return (c.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic c) {
        return ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic c) {
        return (c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }


    // Helpers
    public static int unsignedByte(byte var0) {
        return var0 & 255;
    }

    public static int unsignedShort(byte[] var0) {
        // Short.valueOf(ByteBuffer.wrap(v_bytes).getShort()) also works
        int var1;
        if(var0.length < 2) {
            var1 = -1;
        } else {
            var1 = (unsignedByte(var0[0]) << 8) + unsignedByte(var0[1]);
        }

        return var1;
    }

    public boolean isConnected() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public void reconnect(Activity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public void stopScanning() {
        scanLeDevice(false);
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
        }
        mOWDevice.isConnected.set(false);
        this.mScanResults.clear();
        descriptorWriteQueue.clear();
        // Added stuff 10/23 to clean fix
        owGatService = null;
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void startScanning() {
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanLeDevice(true);
    }

    public void disconnect() {
        scanLeDevice(false);
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
        }
        this.mScanResults.clear();
        descriptorWriteQueue.clear();
        // Added stuff 10/23 to clean fix
        owGatService = null;
    }

    public BluetoothGattCharacteristic getCharacteristic(String uuidLookup) {
        return owGatService.getCharacteristic(UUID.fromString(uuidLookup));
    }

    public void writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        mGatt.writeCharacteristic(bluetoothGattCharacteristic);
    }
}
