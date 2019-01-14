package net.kwatts.powtools.util;

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

import net.kwatts.powtools.App;
import net.kwatts.powtools.BuildConfig;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.security.DigestInputStream;

import timber.log.Timber;

public class BluetoothUtilImpl implements BluetoothUtil{

    private static final String TAG = BluetoothUtilImpl.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;

    Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();
    Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private android.bluetooth.BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    BluetoothGatt mGatt;
    BluetoothGattService owGatService;

    private Map<String, String> mScanResults = new HashMap<>();

    private MainActivity mainActivity;
    OWDevice mOWDevice;

    private ScanSettings settings;
    private boolean mScanning;
    private long mDisconnected_time;
    private int mRetryCount = 0;




    @Override
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
            Timber.d( "Bluetooth connection state change: address=" + gatt.getDevice().getAddress()+ " status=" + status + " newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.d("There was a BluetoothProfile.STATE_DISCONNECTED: name=" + gatt.getDevice().getName() + " address=" + gatt.getDevice().getAddress());
                if (gatt.getDevice().getAddress().equals(mOWDevice.deviceMacAddress.get())) {
                    onOWStateChangedToDisconnected(gatt);
                }
                //updateLog("--> Closed " + gatt.getDevice().getAddress());
                //Timber.d( "Disconnect:" + gatt.getDevice().getAddress());
            }
        }



        //@SuppressLint("WakelockTimeout")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Timber.d("Only should be here if connecting to OW:" + gatt.getDevice().getAddress());
            owGatService = gatt.getService(UUID.fromString(OWDevice.OnewheelServiceUUID));

            if (owGatService == null) {
                if (gatt.getDevice().getName() == null) {
                    Timber.i("--> " + gatt.getDevice().getAddress() + " not OW, moving on.");
                } else {
                    Timber.i("--> " + gatt.getDevice().getName() + " not OW, moving on.");
                }
                return;
            }

            mGatt = gatt;
            Timber.i("Hey, I found the OneWheel Service: " + owGatService.getUuid().toString());
            mainActivity.deviceConnectedTimer(true);
            mOWDevice.isConnected.set(true);
            App.INSTANCE.acquireWakeLock();
            String deviceMacAddress = mGatt.getDevice().toString();
            String deviceMacName = mGatt.getDevice().getName();
            mOWDevice.deviceMacAddress.set(deviceMacAddress);
            mOWDevice.deviceMacName.set(deviceMacName);
            App.INSTANCE.getSharedPreferences().saveMacAddress(
                    mOWDevice.deviceMacAddress.get(),
                    mOWDevice.deviceMacName.get()
            );


            scanLeDevice(false); // We can stop scanning...




            for(OWDevice.DeviceCharacteristic deviceCharacteristic: mOWDevice.getNotifyCharacteristics()) {
                String uuid = deviceCharacteristic.uuid.get();
                if (uuid != null && deviceCharacteristic.isNotifyCharacteristic) {
                    BluetoothGattCharacteristic localCharacteristic = owGatService.getCharacteristic(UUID.fromString(uuid));
                    if (localCharacteristic != null) {
                        if (isCharacteristicNotifiable(localCharacteristic) && deviceCharacteristic.isNotifyCharacteristic) {
                            mGatt.setCharacteristicNotification(localCharacteristic, true);
                            BluetoothGattDescriptor descriptor = localCharacteristic.getDescriptor(UUID.fromString(OWDevice.OnewheelConfigUUID));
                            Timber.d( "descriptorWriteQueue.size:" + descriptorWriteQueue.size());
                            if (descriptor == null) {
                                Timber.e( uuid + " has a null descriptor!");
                            } else {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                descriptorWriteQueue.add(descriptor);
                                if (descriptorWriteQueue.size() == 1) {
                                    mGatt.writeDescriptor(descriptor);
                                }
                                Timber.d( uuid + " has been set for notifications");
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
                            Timber.d( "characteristicReadQueue.size =" + characteristicReadQueue.size() + " descriptorWriteQueue.size:" + descriptorWriteQueue.size());
                            if (characteristicReadQueue.size() == 1 && (descriptorWriteQueue.size() == 0)) {
                                Timber.i( dc.uuid.get() + " is readable and added to queue");
                                mGatt.readCharacteristic(c);
                            }
                        }
                    }
                }
            }


        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
            String characteristic_uuid = c.getUuid().toString();
            Timber.d( "BluetoothGattCallback.onCharacteristicRead: CharacteristicUuid=" + characteristic_uuid + "status=" + status);
            characteristicReadQueue.remove();

            if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicFirmwareRevision)) {
                Timber.d("GEMINI step #1: trigger sending the 20 byte input key over multiple serial ble notifications");
                gatt.writeCharacteristic(c);
            } else if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicBatteryRemaining)) {
                mainActivity.updateBatteryRemaining(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            } else if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicRidingMode)) {
                 Timber.d( "Got ride mode from the main UI thread:" + c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            } else if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicUartSerialRead)) {
                unlockKeyGemini(gatt, c.getValue());
            }



            if (BuildConfig.DEBUG) {
                byte[] v_bytes = c.getValue();


                StringBuilder sb = new StringBuilder();
                for (byte b : c.getValue()) {
                    sb.append(String.format("%02x", b));
                }

                Timber.d( "HEX %02x: " + sb);
                Timber.d( "Arrays.toString() value: " + Arrays.toString(v_bytes));
                Timber.d( "String value: " + c.getStringValue(0));
                Timber.d( "Unsigned short: " + unsignedShort(v_bytes));
                Timber.d( "getIntValue(FORMAT_UINT8,0) " + c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                Timber.d( "getIntValue(FORMAT_UINT8,1) " + c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            }

            mOWDevice.processUUID(c);
            // Callback to make sure the queue is drained
            if (characteristicReadQueue.size() > 0) {
                gatt.readCharacteristic(characteristicReadQueue.element());
            }


        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic c) {
            //Timber.d( "BluetoothGattCallback.onCharacteristicChanged: CharacteristicUuid=" + c.getUuid().toString());

            // https://github.com/ponewheel/android-ponewheel/issues/86
            if (c.getUuid().toString().equals(OWDevice.OnewheelCharacteristicUartSerialRead)) {
                unlockKeyGemini(gatt,c.getValue());
            } else if(c.getUuid().toString().equals(OWDevice.OnewheelCharacteristicBatteryRemaining)) {
                mainActivity.updateBatteryRemaining(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            }

            mOWDevice.processUUID(c);
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Timber.i( "onCharacteristicWrite: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Timber.i( "onDescriptorWrite: " + status);
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


    void scanLeDevice(final boolean enable) {
        Timber.d("scanLeDevice enable = " + enable);
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

            Timber.i( "ScanCallback.onScanResult: " + mScanResults.entrySet());
            if (!mScanResults.containsKey(deviceAddress)) {
                Timber.i( "ScanCallback.deviceName:" + deviceName);
                mScanResults.put(deviceAddress, deviceName);

                if (deviceName == null) {
                    Timber.i("Found " + deviceAddress);
                } else {
                    Timber.i("Found " + deviceAddress + " (" + deviceName + ")");
                }

                if (deviceName != null && (deviceName.startsWith("ow") || deviceName.startsWith("Onewheel"))) {
                    mRetryCount = 0;
                    Timber.i("Looks like we found our OW device (" + deviceName + ") discovering services!");
                    connectToDevice(result.getDevice());
                } else {
                    Timber.d("onScanResult: found another device:" + deviceName + "-" + deviceAddress);
                }

            } else {
                Timber.d("onScanResult: mScanResults already had our key, still connecting to OW services or something is up with the BT stack.");
              //  Timber.d("onScanResult: mScanResults already had our key," + "deviceName=" + deviceName + ",deviceAddress=" + deviceAddress);
                // still connect
                //connectToDevice(result.getDevice());
            }


        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Timber.i("ScanCallback.onBatchScanResults.each:" + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Timber.e( "ScanCallback.onScanFailed:" + errorCode);
        }
    };


    public void connectToDevice(BluetoothDevice device) {
        Timber.d( "connectToDevice:" + device.getName());
        device.connectGatt(mainActivity, false, mGattCallback);
    }

    public void connectToGatt(BluetoothGatt gatt) {
        Timber.d( "connectToGatt:" + gatt.getDevice().getName());
        gatt.connect();
    }

    private void onOWStateChangedToDisconnected(BluetoothGatt gatt) {
        Timber.i("We got disconnected from our Device: " + gatt.getDevice().getAddress());
        mainActivity.deviceConnectedTimer(false);
        mOWDevice.isConnected.set(false);
        App.INSTANCE.releaseWakeLock();
        mScanResults.clear();

        if (App.INSTANCE.getSharedPreferences().shouldAutoReconnect()) {
            mRetryCount++;
            Timber.i("mRetryCount=" + mRetryCount);

            try {
                if (mRetryCount == 20) {
                    Timber.i("Reached too many retries, stopping search");
                    gatt.close();
                    stopScanning();
                    disconnect();
                    //mainActivity.invalidateOptionsMenu();
                } else {
                    Timber.i("Waiting for 5 seconds until trying to connect to OW again.");
                    TimeUnit.SECONDS.sleep(5);
                    Timber.i("Trying to connect to OW at " + mOWDevice.deviceMacAddress.get());
                    //BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mOWDevice.deviceMacAddress.get());
                    //connectToDevice(device);
                    gatt.connect();
                }
            } catch (InterruptedException e) {
                Timber.d("Connection to OW got interrupted:" + e.getMessage());
            }
        } else {
            gatt.close();
            mainActivity.invalidateOptionsMenu();
        }

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

    public static ByteArrayOutputStream inkey = new ByteArrayOutputStream();

    public void unlockKeyGemini(BluetoothGatt gatt, byte[] c) {
        try {
            inkey.write(c);

            if (inkey.toByteArray().length == 20) {
                ByteArrayOutputStream outkey = new ByteArrayOutputStream();


                outkey.write(Util.StringToByteArrayFastest("43:52:58"));

                // Take almost all of the bytes from the input array. This is almost the same as the last part as
                // we are ignoring the first 3 and the last bytes.
                byte[] arrayToMD5_part1 = Arrays.copyOfRange(inkey.toByteArray(), 3, 19);
                byte[] arrayToMD5_part2 = Util.StringToByteArrayFastest("D9255F0F23354E19BA739CCDC4A91765");

                // New byte array we are going to MD5 hash. Part of the input string, part of this static string.
                ByteBuffer arrayToMD5 = ByteBuffer.allocate(arrayToMD5_part1.length + arrayToMD5_part2.length);
                arrayToMD5.put(arrayToMD5_part1);
                arrayToMD5.put(arrayToMD5_part2);

                // Start prepping the MD5 hash
                MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
                DigestInputStream digestInputStream = new DigestInputStream(new ByteArrayInputStream(arrayToMD5.array()), localMessageDigest);

                // This is actually the byte that represents a space character. ¯\_(ツ)_/¯
                byte[] arrayOfByte = new byte[]{101};
                while (digestInputStream.read(arrayOfByte) != -1) {
                }
                digestInputStream.close();
                byte[] md5Hash = localMessageDigest.digest();

                // Add it to the 3 bytes we already have.
                outkey.write(md5Hash);

                // Validate the check byte.
                byte checkByte = 0;
                int j = outkey.toByteArray().length;
                int i = 0;
                while (i < j) {
                    checkByte = ((byte) (outkey.toByteArray()[i] ^ checkByte));
                    i += 1;
                }
                outkey.write(checkByte);


                // Finally, write out to the OW serial UART characteristic
                Timber.d("GEMINI Step #1: write outkey=" + Arrays.toString(outkey.toByteArray()));
                BluetoothGattCharacteristic lc = owGatService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicUartSerialWrite));
                lc.setValue(outkey.toByteArray());
                gatt.writeCharacteristic(lc);

                // cleanup and stop notifications to serial read
                outkey.reset();
                inkey.reset();
                BluetoothGattCharacteristic lcn = owGatService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicUartSerialRead));
                gatt.setCharacteristicNotification(lcn, false);
            }

        } catch(Exception e){
                Timber.e("Exception with GEMINI obfuckstation:" + e.getMessage());
        }

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


    @Override
    public boolean isConnected() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }


    @Override
    public void reconnect(MainActivity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
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
        this.mRetryCount = 0;
        // Added stuff 10/23 to clean fix
        owGatService = null;
        // Added more 3/12/2018
        this.characteristicReadQueue.clear();

    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }


    @Override
    public void startScanning() {
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanLeDevice(true);
    }


    @Override
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

    @Override
    public BluetoothGattCharacteristic getCharacteristic(String uuidLookup) {
        return owGatService.getCharacteristic(UUID.fromString(uuidLookup));
    }

    @Override
    public void writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        mGatt.writeCharacteristic(bluetoothGattCharacteristic);
    }
}
