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
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.databinding.ObservableField;
import net.kwatts.powtools.App;
import net.kwatts.powtools.BuildConfig;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;
import net.kwatts.powtools.model.OWDevice.DeviceCharacteristic;
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
    public static ByteArrayOutputStream inkey = new ByteArrayOutputStream();
    public static ObservableField<String> isOWFound = new ObservableField<>();
    public Context mContext;
    Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();
    Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private android.bluetooth.BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    BluetoothGatt mGatt;
    BluetoothGattService owGatService;

    private Map<String, String> mScanResults = new HashMap<>();

    private MainActivity mainActivity;
    OWDevice mOWDevice;
    public boolean sendKey = true;
    private ScanSettings settings;
    private boolean mScanning;
    private long mDisconnected_time;
    private int mRetryCount = 0;
    private int statusMode = 0;

    private Handler handler;

    public static boolean isGemini = false;

    //TODO: decouple this crap from the UI/MainActivity
    @Override
    public void init(MainActivity mainActivity, OWDevice mOWDevice) {
        this.mainActivity = mainActivity;
        this.mContext = mainActivity.getApplicationContext();
        this.mOWDevice = mOWDevice;

        this.mBluetoothAdapter = ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        //final BluetoothManager manager = (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        //assert manager != null;
        //mBluetoothAdapter = manager.getAdapter();
        mOWDevice.bluetoothLe.set("On");

        handler = new Handler(Looper.getMainLooper());

        final int repeatTime = 60000; //every minute
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (statusMode == 2) {
                    walkReadQueue(1);
                }
                handler.postDelayed(this, repeatTime);
            }
        }, repeatTime);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Timber.d( "Bluetooth connection state change: address=" + gatt.getDevice().getAddress()+ " status=" + status + " newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Timber.d("STATE_CONNECTED: name=" + gatt.getDevice().getName() + " address=" + gatt.getDevice().getAddress());
                BluetoothUtilImpl.isOWFound.set("true");
                gatt.discoverServices();
                Battery.initStateTwoX(App.INSTANCE.getSharedPreferences());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.d("STATE_DISCONNECTED: name=" + gatt.getDevice().getName() + " address=" + gatt.getDevice().getAddress());
                statusMode = 0;
                BluetoothUtilImpl.isOWFound.set("false");
                if (gatt.getDevice().getAddress().equals(mOWDevice.deviceMacAddress.get())) {
                    BluetoothUtilImpl bluetoothUtilImpl = BluetoothUtilImpl.this;
                    onOWStateChangedToDisconnected(gatt,bluetoothUtilImpl.mContext);
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
            scanLeDevice(false);

            // Stability updates per https://github.com/ponewheel/android-ponewheel/issues/86#issuecomment-460033659
            // Step 1: In OnServicesDiscovered, JUST read the firmware version.
            Timber.d("Stability Step 1: Only reading the firmware version!");
            //new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            handler.postDelayed(new Runnable() {
                public void run() {
                    BluetoothUtilImpl.this.mGatt.readCharacteristic(BluetoothUtilImpl.this.owGatService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicFirmwareRevision)));
                }
            }, 500);

/*
            BluetoothGattCharacteristic c = owGatService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicFirmwareRevision));
            if (c != null) {
                if (isCharacteristicReadable(c)) {
                    characteristicReadQueue.add(c);
                    Timber.d( "characteristicReadQueue.size =" + characteristicReadQueue.size() + " descriptorWriteQueue.size:" + descriptorWriteQueue.size());
                    if (characteristicReadQueue.size() == 1 && (descriptorWriteQueue.size() == 0)) {
                        mGatt.readCharacteristic(c);
                    }
                }
            }

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

*/
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
            String characteristic_uuid = c.getUuid().toString();
            Timber.d( "BluetoothGattCallback.onCharacteristicRead: CharacteristicUuid=" +
                    characteristic_uuid +
                    ",status=" + status +
                    ",isGemini=" + isGemini);
            if (characteristicReadQueue.size() > 0) {
                characteristicReadQueue.remove();
            }

            // Stability Step 2: In OnCharacteristicRead, if the value is of the char firmware version, parse it's value.
            // If its >= 4034, JUST write the descriptor for the Serial Read characteristic to Enable notifications,
            // and set notify to true with gatt. Otherwise its Andromeda or lower and we can call the method to
            // read & notify all the characteristics we want. (Although I learned doing this that some android devices
            // have a max of 12 notify characteristics at once for some reason. At least I'm pretty sure.)
            // I also set a class-wide boolean value isGemini to true here so I don't have to keep checking if its Andromeda
            // or Gemini later on.
            if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicFirmwareRevision)) {
                Timber.d("We have the firmware revision! Checking version.");
                if (unsignedShort(c.getValue()) >= 4034) {
                    Timber.d("It's Gemini!");
                    isGemini = true;
                    Timber.d("Stability Step 2.1: JUST write the descriptor for the Serial Read characteristic to Enable notifications");
                    BluetoothGattCharacteristic gC = owGatService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicUartSerialRead));
                    gatt.setCharacteristicNotification(gC, true);
                    Timber.d("and set notify to true with gatt...");
                    BluetoothGattDescriptor descriptor = gC.getDescriptor(UUID.fromString(OWDevice.OnewheelConfigUUID));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                } else {
                    Timber.d("It's before Gemini, likely Andromeda - calling read and notify characteristics");
                    isGemini = false;
                    whenActuallyConnected();
                }
            } else if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicRidingMode)) {
                 Timber.d( "Got ride mode from the main UI thread:" + c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            }

            //else if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicUartSerialRead)) {
            //    Timber.d("Got OnewheelCharacteristicUartSerialRead, calling unlockKeyGemini! ");
             //   unlockKeyGemini(gatt, c.getValue());
           // }



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

            mOWDevice.setBatteryRemaining(mainActivity);

            // Callback to make sure the queue is drained

            if (characteristicReadQueue.size() > 0) {
                gatt.readCharacteristic(characteristicReadQueue.element());
            }

        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic c) {
            //Timber.d( "BluetoothGattCallback.onCharacteristicChanged: CharacteristicUuid=" + c.getUuid().toString());

            // https://github.com/ponewheel/android-ponewheel/issues/86
            //if (isGemini && (c.getUuid().toString().equals(OWDevice.OnewheelCharacteristicUartSerialRead))) {
                // Step 4: In OnCharacteristicChanged, if isGemini and characteristic is serial read,
                // do the gemini hash crap and stuff and setNotify for serial read to false.
            //    Timber.d("Stability Step 4: Gemini unlock & setting setNotify for serial read to false");
            //    unlockKeyGemini(gatt,c.getValue());

            //}
            BluetoothGatt bluetoothGatt = gatt;
            BluetoothGattCharacteristic bluetoothGattCharacteristic = c;

            if (isGemini && (c.getUuid().toString().equals(OWDevice.OnewheelCharacteristicUartSerialRead))) {                try {
                    Timber.d("Setting up inkey!");
                    inkey.write(c.getValue());
                    if (inkey.toByteArray().length >= 20 && sendKey) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("GEMINI Step #2: convert inkey=");
                        sb.append(Arrays.toString(inkey.toByteArray()));
                        Timber.d("GEMINI:" +  sb.toString());
                        ByteArrayOutputStream outkey = new ByteArrayOutputStream();
                        outkey.write(Util.StringToByteArrayFastest("43:52:58"));
                        byte[] arrayToMD5_part1 = Arrays.copyOfRange(BluetoothUtilImpl.inkey.toByteArray(), 3, 19);
                        byte[] arrayToMD5_part2 = Util.StringToByteArrayFastest("D9255F0F23354E19BA739CCDC4A91765");
                        ByteBuffer arrayToMD5 = ByteBuffer.allocate(arrayToMD5_part1.length + arrayToMD5_part2.length);
                        arrayToMD5.put(arrayToMD5_part1);
                        arrayToMD5.put(arrayToMD5_part2);
                        MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
                        DigestInputStream digestInputStream = new DigestInputStream(new ByteArrayInputStream(arrayToMD5.array()), localMessageDigest);
                        while (digestInputStream.read(new byte[]{101}) != -1) {
                        }
                        digestInputStream.close();
                        outkey.write(localMessageDigest.digest());
                        byte checkByte = 0;
                        for (byte b : outkey.toByteArray()) {
                            checkByte = (byte) (b ^ checkByte);
                        }
                        outkey.write(checkByte);
                        StringBuilder sb2 = new StringBuilder();
                        byte[] bArr = arrayToMD5_part1;
                        sb2.append("GEMINI Step #3: write outkey=");
                        sb2.append(Arrays.toString(outkey.toByteArray()));
                        Timber.d("GEMINI" +  sb2.toString());
                        BluetoothGattCharacteristic lc = owGatService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicUartSerialWrite));
                        lc.setValue(outkey.toByteArray());
                        if (!bluetoothGatt.writeCharacteristic(lc)) {
                            BluetoothGattCharacteristic bluetoothGattCharacteristic2 = lc;
                            sendKey = true;
                        } else {
                            sendKey = false;
                            bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, false);
                        }
                        outkey.reset();
                    }
                } catch (Exception e) {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("Exception with GEMINI obfuckstation:");
                    sb3.append(e.getMessage());
                    Timber.d("GEMINI" + sb3.toString());
                }
            }


            mOWDevice.processUUID(bluetoothGattCharacteristic);

            mOWDevice.setBatteryRemaining(mainActivity);
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
            Timber.i( "onCharacteristicWrite: " + status + ", CharacteristicUuid=" + c.getUuid().toString());
            // Step 5: In OnCharacteristicWrite, if isGemini & characteristic is Serial Write, NOW setNotify
            // and read all the characteristics you want. its also only now that I start the
            // repeated handshake clock thing but I don't think it really matters, this all happens pretty quick.
            if (isGemini && (c.getUuid().toString().equals(OWDevice.OnewheelCharacteristicUartSerialWrite))) {
                Timber.d("Step 5: Gemini and serial write, kicking off all the read and notifies...");
                whenActuallyConnected();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Timber.i( "onDescriptorWrite: " + status + ",descriptor=" + descriptor.getUuid().toString() +
                    ",descriptor_characteristic=" + descriptor.getCharacteristic().getUuid().toString());

            if (isGemini && descriptor.getCharacteristic().getUuid().toString().equals(OWDevice.OnewheelCharacteristicUartSerialRead)) {
                Timber.d("Stability Step 3: if isGemini and the characteristic descriptor that was written was Serial Write" +
                        "then trigger the 20 byte input key over multiple serial ble notification stream by writing the firmware version onto itself");
                gatt.writeCharacteristic(owGatService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicFirmwareRevision)));
            }

            //DeviceCharacteristic dc = mOWDevice.characteristics.get(descriptor.getCharacteristic().getUuid().toString());
            //if (dc != null && (dc.state == 0 || dc.state == 1)) {
            //    gatt.setCharacteristicNotification(  owGatService.getCharacteristic(UUID.fromString(dc.uuid.get())), true);
            //
            // }

            if (descriptorWriteQueue.size() > 0) {
                descriptorWriteQueue.remove();
                if (descriptorWriteQueue.size() > 0) {
                    gatt.writeDescriptor(descriptorWriteQueue.element());
                } else if (characteristicReadQueue.size() > 0) {
                    gatt.readCharacteristic(characteristicReadQueue.element());
                }
            }

            // Step 3: In OnDescriptorWrite, if isGemini and the characteristic descriptor that was
            // written was Serial Write, then trigger the byte stream by writing the firmware version
            // onto itself.
            /*
            if (isGemini && (descriptor.equals(OWDevice.OnewheelCharacteristicUartSerialWrite))) {
                Timber.d("Step 3: Is Gemini, writing the descriptor onto itself");
                gatt.writeDescriptor(descriptor);
            }
            */
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

    private void onOWStateChangedToDisconnected(BluetoothGatt gatt, Context context) {
        //TODO: we really should have a BluetoothService we kill and restart
        Timber.i("We got disconnected from our Device: " + gatt.getDevice().getAddress());
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Toast.makeText(mainActivity, "We got disconnected from our device: " + gatt.getDevice().getAddress(), Toast.LENGTH_SHORT).show();

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


/*
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
    */


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
    public boolean isGemini() {
        return this.isGemini;
    }

    @Override
    public void reconnect(MainActivity activity) {
        activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
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
        inkey.reset();
        isOWFound.set("false");
        this.sendKey = true;

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
        inkey.reset();
        isOWFound.set("false");
        this.sendKey = true;
        statusMode = 0;
    }

    @Override
    public BluetoothGattCharacteristic getCharacteristic(String uuidLookup) {
        return owGatService.getCharacteristic(UUID.fromString(uuidLookup));
    }

    @Override
    public void writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        mGatt.writeCharacteristic(bluetoothGattCharacteristic);
    }

    @Override
    public int getStatusMode() {
        return statusMode;
    }

    private void walkNotifyQueue(int state) {
        for(OWDevice.DeviceCharacteristic dc: mOWDevice.getNotifyCharacteristics()) {
            String uuid = dc.uuid.get();
            if (uuid != null && dc.isNotifyCharacteristic && dc.state == state) {
                BluetoothGattCharacteristic localCharacteristic = owGatService.getCharacteristic(UUID.fromString(uuid));
                if (localCharacteristic != null) {
                    if (isCharacteristicNotifiable(localCharacteristic) && dc.isNotifyCharacteristic) {
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
    }

    private void walkReadQueue(int state) {
        for(OWDevice.DeviceCharacteristic dc : mOWDevice.getReadCharacteristics()) {
            if (dc.uuid.get() != null && !dc.isNotifyCharacteristic && dc.state == state) {
                Timber.d("uuid:%s, state:%d", dc.uuid.get(), dc.state);
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

    public void whenActuallyConnected() {
        walkNotifyQueue(0);
        walkReadQueue(0);
        walkReadQueue(1);

        statusMode = 2;

     /*
        for (DeviceCharacteristic dc : mOWDevice.getNotifyCharacteristics()) {
            String uuid = dc.uuid.get();
            if (uuid != null && dc.state == 0) {
                try {
                    BluetoothGattCharacteristic gC = owGatService.getCharacteristic(UUID.fromString(uuid));
                    if (gC != null) {
                        BluetoothGattDescriptor descriptor = gC.getDescriptor(UUID.fromString(OWDevice.OnewheelConfigUUID));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        descriptorWriteQueue.add(descriptor);
                        if (this.descriptorWriteQueue.size() == 1) {
                            this.mGatt.writeDescriptor(descriptor);
                        }
                        Timber.d( uuid + " has been set for notifications");
                        //this.characteristicReadQueue.add(gC);
                    }
                } catch (Exception e) {
                    Timber.e("Exception trying to set notification for: " + uuid);
                }
            }
        }
        for (DeviceCharacteristic dc2 : this.mOWDevice.getReadCharacteristics()) {
            String uuid = dc2.uuid.get();
            if (uuid != null && dc2.state == 3) {
                BluetoothGattCharacteristic gC2 = this.owGatService.getCharacteristic(UUID.fromString(uuid));
                if (gC2 != null) {
                    this.characteristicReadQueue.add(gC2);
                }
            }
        }


        for(OWDevice.DeviceCharacteristic deviceCharacteristic: mOWDevice.getNotifyCharacteristics()) {
            String uuid = deviceCharacteristic.uuid.get();
            if (uuid != null && deviceCharacteristic.state == 0) {
                BluetoothGattCharacteristic localCharacteristic = owGatService.getCharacteristic(UUID.fromString(uuid));
                if (localCharacteristic != null) {
                    if (isCharacteristicNotifiable(localCharacteristic)) {
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
                if (c != null && dc.state == 3) {
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
*/
    }
}
