package net.kwatts.powtools.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.IntRange;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
/**
 * Created by kwatts on 6/15/16.
 */
public class Util {


    public static String getUsername(Context c) {
        AccountManager manager = AccountManager.get(c);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");

            if (parts.length > 1)
                return parts[0];
        }
        return null;
    }

    /* Helper methods. For dealing with bytes see:
       http://www.roseindia.net/java/master-java/bitwise-bitshift-operators.shtml
       https://calleerlandsson.com/2014/02/06/rubys-bitwise-operators/
     */
    public static short byteToShort(byte[] v) {
        return (short)((v[1] << 8) + (v[0] & 0xff));
    }

    private static final byte[] twoBytes = new byte[2];
    public static byte[] intToShortBytes(@IntRange(from = -32768, to = 32767) int v) {

        twoBytes[0] = (byte) (v >> 8 & 0xff);
        twoBytes[1] = (byte) (v & 0xff);

        return twoBytes;
    }
    public static byte[] boolToShortBytes(boolean b) {
        return new byte[0];
    }

    // 1 byte/8-bit signed two's complement (-128 to 127)
    // returns an int, 32-bit signed two's complement
    public static int unsignedByte(byte var0) {
        return var0 & 255;
    }

    // short = 2 bytes/16-bit signed two's complement (-32,768 to 32,767)
    public static int unsignedShort(byte[] var0) {
        int var1;
        if(var0.length < 2) {
            var1 = -1;
        } else {
            var1 = (unsignedByte(var0[0]) << 8) + unsignedByte(var0[1]);
        }
        // ByteBuffer.wrap(v_bytes).getShort() also works
        // or
        // ByteBuffer bb = ByteBuffer.allocate(2);
        // bb.order(ByteOrder.LITTLE_ENDIAN);
        // bb.put(var0[0]);
        // bb.put(var0[1]);
        // short shortVal = bb.getShort(0);
        return var1;
    }

    // double is 64 bit and used for decimals
    public static double cel2far(int celsius) {
        return (9.0/5.0)*celsius + 32;
    }
    public static double far2cel(int far) {
        return (5.0/9.0)*(far - 32);
    }
    public static double milesToKilometers(double paramDouble) {
        return paramDouble * 1.609344;

    }
    public static double revolutionsToKilometers(double paramDouble)
    {
        return paramDouble * 35.0D / 39370.099999999999D;
    }

    public static double revolutionsToMiles(double paramDouble)
    {
        return paramDouble * 35.0D / 63360.0D;
    }
    public static double rpmToKilometersPerHour(double paramDouble)
    {
        return 60.0D * (35.0D * paramDouble) / 39370.099999999999D;
    }

    public static double rpmToMilesPerHour(double paramDouble)
    {
        return 60.0D * (35.0D * paramDouble) / 63360.0D;
    }

    public static void printBits(String prompt, BitSet b, int numBits) {
        System.out.print(prompt + " ");
        for (int i = 0; i < numBits; i++) {
            System.out.print(b.get(i) ? "1" : "0");
        }
        System.out.println();
    }

    public static String bytesToHex(byte c[]) {
        StringBuilder sb = new StringBuilder();
        for (byte b : c) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * If your number X falls between A and B, and you would like Y to fall between C and D, you can apply the following linear transform:
     *
     * Y = (X-A)/(B-A) * (D-C) + C
     *
     * https://stackoverflow.com/a/345204/247325
     */
    public static float linearTransform(float x, float a, float b, float c, float d) {
        return (x-a)/(b-a) * (d-c) + c;
    }
    /**
     * If your number X falls between A and B, and you would like Y to fall between C and D, you can apply the following linear transform:
     *
     * Y = (X-A)/(B-A) * (D-C) + C
     *
     * https://stackoverflow.com/a/345204/247325
     */
    public static double linearTransform(double x, double a, double b, double c, double d) {
        return (x-a)/(b-a) * (d-c) + c;
    }

    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }


    public static byte[] StringToByteArrayFastest(String hex)
    {
        hex = hex.replace(":", "");
        hex = hex.replace("-", "");
        hex = hex.toUpperCase();

        if (hex.length() % 2 == 1) {
            return null;
            // throw new Exception("The binary key cannot have an odd number of digits");
        }

        byte[] arr = new byte[hex.length() >> 1];

        char[] c = hex.toCharArray();

        for (int i = 0; i < hex.length() >> 1; ++i)
        {
            arr[i] = (byte)((GetHexVal(c[i << 1]) << 4) + (GetHexVal(c[(i << 1) + 1])));
        }

        return arr;
    }

    public static int GetHexVal(char hex)
    {
        int val = (int)hex;
        //For uppercase A-F letters:
        return val - (val < 58 ? 48 : 55);
        //For lowercase a-f letters:
        //return val - (val < 58 ? 48 : 87);
        //Or the two combined, but a bit slower:
        //return val - (val < 58 ? 48 : (val < 97 ? 55 : 87));
    }
}


Main




        10:15:24-kevin_watkins@M288427QKHV2L:/grindhouse/workspace/android-ponewheel$ git diff app/src/main/java/net/kwatts/powtools/MainActivity.java
        diff --git a/app/src/main/java/net/kwatts/powtools/MainActivity.java b/app/src/main/java/net/kwatts/powtools/MainActivity.java
        index e2c1666..2675e23 100644
        --- a/app/src/main/java/net/kwatts/powtools/MainActivity.java
        +++ b/app/src/main/java/net/kwatts/powtools/MainActivity.java
@@ -387,8 +387,6 @@ public class MainActivity extends AppCompatActivity implements SharedPreferences
         mOWDevice.isConnected.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
@Override
public void onPropertyChanged(Observable observable, int i) {
        -
        -
        if (mOWDevice.isConnected.get() && isNewOrNotContinuousRide()) {
        ride = new Ride();
        App.dbExecute(database -> ride.id = database.rideDao().insert(ride));
@@ -513,6 +511,8 @@ public class MainActivity extends AppCompatActivity implements SharedPreferences
             menu.findItem(R.id.menu_disconnect).setVisible(true);
        menu.findItem(R.id.menu_stop).setVisible(false);
        menu.findItem(R.id.menu_scan).setVisible(false);
        +            Timber.d("Connected, sending the key/challenge kickoff...");
        +            mOWDevice.sendKeyChallengeForGemini(getBluetoothUtil());
        //menu.findItem(R.id.menu_ow_light_on).setVisible(true);
        //menu.findItem(R.id.menu_ow_ridemode).setVisible(true);
        } else if (!getBluetoothUtil().isScanning()) {
@@ -649,6 +649,8 @@ public class MainActivity extends AppCompatActivity implements SharedPreferences

         alertsController.recaptureMedia(this);

        +
        +
        this.invalidateOptionsMenu();
        }

        10:15:44-kevin_watkins@M288427QKHV2L:/grindhouse/workspace/android-ponewheel$ git diff app/src/main/java/net/kwatts/powtools/model/OWDevice.java
        diff --git a/app/src/main/java/net/kwatts/powtools/model/OWDevice.java b/app/src/main/java/net/kwatts/powtools/model/OWDevice.java
        index 6b8e50c..51f4e17 100644
        --- a/app/src/main/java/net/kwatts/powtools/model/OWDevice.java
        +++ b/app/src/main/java/net/kwatts/powtools/model/OWDevice.java
@@ -78,6 +78,7 @@ public class OWDevice extends BaseObservable implements DeviceInterface {
    public static final String KEY_TILT_ANGLE_ROLL = "tilt_angle_roll";
    public static final String KEY_RIDE_MODE = "ride_mode";
    public static final String KEY_BATTERY_TEMP = "battery_temp";
+    public static final String KEY_SERIAL_READ = "serial_read";

    public static SparseArray<String> ERROR_CODE_MAP = new SparseArray<>();
    {
        @@ -117,6 +118,9 @@ public class OWDevice extends BaseObservable implements DeviceInterface {

        public static final String OnewheelServiceUUID = "e659f300-ea98-11e3-ac10-0800200c9a66";
        public static final String OnewheelConfigUUID= "00002902-0000-1000-8000-00805f9b34fb";
+
        +
        +    // 00002a04-0000-1000-8000-00805f9b34fb
        public static final String OnewheelCharacteristicSerialNumber = "e659F301-ea98-11e3-ac10-0800200c9a66"; //2085
        public static final String OnewheelCharacteristicRidingMode = "e659f302-ea98-11e3-ac10-0800200c9a66";
        public static final String OnewheelCharacteristicBatteryRemaining = "e659f303-ea98-11e3-ac10-0800200c9a66";
        @@ -211,6 +215,7 @@ public class OWDevice extends BaseObservable implements DeviceInterface {
            In manual mode (0x0045=02) 0x0049 is front lights and 0x004d is back lights
            For both, the first byte is the level of light for white and second byte for red. Levels are 00 (off) to 75 (super bright)
            SETS FRONT TO BRIGHT RED AND BACK TO BRIGHT WHITE:
                    +hcitool lescan | grep 'ow' to get device address, e.g D0:39:72:BE:0A:32 ow059062
            gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=0002 --handle=0x0045
            gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=0075 --handle=0x0049
            gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=7500 --handle=0x004d
            @@ -268,6 +273,7 @@ gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=7500 --handle=0x004
                    deviceReadCharacteristics.clear();
         deviceNotifyCharacteristics.clear();

+
        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicHardwareRevision, KEY_HARDWARE_REVISION,   "HARDWARE REVISION"));            // 0
         deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicFirmwareRevision, KEY_FIRMWARE_REVISION,   "FIRMWARE REVISION"));            // 1
         deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicLifetimeOdometer, KEY_LIFETIME_ODOMETER,   ""));                             // 2
            @@ -277,7 +283,7 @@ gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=7500 --handle=0x004
                    deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryTemp,      KEY_BATTERY_TEMP,        "BATTERY TEMP"));                 // 6
         deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicRidingMode,       KEY_RIDE_MODE,           "RIDING MODE"));                  // 7

-
        +        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicUartSerialRead,   KEY_SERIAL_READ,         ""));// 18
         deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicSpeed,        KEY_SPEED,               "",false));               // 0
         deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryRemaining, KEY_BATTERY,             "Battery"));                   // 1
         deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicStatusError,      KEY_RIDER_DETECTED,      "RIDER"));                     // 2
            @@ -297,7 +303,6 @@ gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=7500 --handle=0x004
                    deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicTemperature,      KEY_CONTROLLER_TEMP,     ""));                          // 16
         deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicMotorTemp,    KEY_MOTOR_TEMP,          "", false));// 17

-
 /*
         deviceNotifyCharacteristics.add(new DeviceCharacteristic()
         {{
@@ -700,6 +705,16 @@ gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=7500 --handle=0x004
         }
     }

+
+    //Needed for Gemini, kick off the key/challenge workflow
+    public void sendKeyChallengeForGemini(BluetoothUtil bluetoothUtil) {
+        BluetoothGattCharacteristic lc = null;
+        lc = bluetoothUtil.getCharacteristic(OWDevice.OnewheelCharacteristicFirmwareRevision);
+        Timber.d("GEMINI #1: (exposed) trigger the key by sending board firmware version back to board");
+        lc.setValue(new byte[] { 16, 38 });
+        bluetoothUtil.writeCharacteristic(lc);
+    }
+
     public void setLights(BluetoothUtil bluetoothUtil,int state) {

         lightMode.set(state);
