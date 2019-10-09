package net.kwatts.powtools.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.databinding.BaseObservable;
import android.databinding.ObservableDouble;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.location.Address;
import android.util.SparseArray;

import net.kwatts.powtools.App;
import net.kwatts.powtools.DeviceInterface;
import net.kwatts.powtools.events.DeviceStatusEvent;
import net.kwatts.powtools.util.BluetoothUtil;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.util.Battery;
import net.kwatts.powtools.util.SharedPreferencesUtil;

import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

import static net.kwatts.powtools.util.Util.cel2far;
import static net.kwatts.powtools.util.Util.milesToKilometers;
import static net.kwatts.powtools.util.Util.revolutionsToKilometers;
import static net.kwatts.powtools.util.Util.revolutionsToMiles;
import static net.kwatts.powtools.util.Util.rpmToKilometersPerHour;
import static net.kwatts.powtools.util.Util.rpmToMilesPerHour;
import static net.kwatts.powtools.util.Util.unsignedByte;
import static net.kwatts.powtools.util.Util.unsignedShort;

/**
 * Created by kwatts on 3/23/16.
 */
public class OWDevice extends BaseObservable implements DeviceInterface {
    private static final String NAME = "ONEWHEEL";


    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static final SimpleDateFormat OLD_SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    public static final String KEY_MOTOR_TEMP = "motor_temp";
    public static final String KEY_CONTROLLER_TEMP = "controller_temp";
    public static final String KEY_SPEED_MAX = "speed_max";
    public static final String KEY_SPEED = "speed";

    public static final String KEY_HARDWARE_REVISION = "hardware_revision";
    public static final String KEY_FIRMWARE_REVISION = "firmware_revision";
    public static final String KEY_LIFETIME_ODOMETER = "lifetime_odometer";
    public static final String KEY_LIGHTING_MODE = "lighting_mode";
    public static final String KEY_BATTERY_INITIAL = "battery_initial";
    public static final String KEY_LAST_ERROR_CODE = "last_error_code";
    public static final String KEY_BATTERY = "battery";
    public static final String KEY_RIDER_DETECTED = "rider_detected";
    public static final String KEY_RIDER_DETECTED_PAD_1 = "rider_detected_pad1";
    public static final String KEY_RIDER_DETECTED_PAD_2 = "rider_detected_pad2";
    public static final String KEY_ODOMETER = "odometer";
    public static final String KEY_ODOMETER_TIRE_REVS = "odometer_tire_revs";
    public static final String KEY_TRIP_AMPS = "trip_amps";
    public static final String KEY_TRIP_AMPS_REGEN = "trip_amps_regen";
    public static final String KEY_SPEED_RPM = "speed_rpm";
    public static final String KEY_BATTERY_VOLTAGE = "battery_voltage";
    public static final String KEY_BATTERY_CELLS = "battery_cells";
    public static final String KEY_CURRENT_AMPS = "current_amps";
    public static final String KEY_TILT_ANGLE_PITCH = "tilt_angle_pitch";
    public static final String KEY_TILT_ANGLE_ROLL = "tilt_angle_roll";
    public static final String KEY_RIDE_MODE = "ride_mode";
    public static final String KEY_BATTERY_TEMP = "battery_temp";
    public static final String KEY_SERIAL_READ = "serial_read";

    public static SparseArray<String> ERROR_CODE_MAP = new SparseArray<>();
    {
        ERROR_CODE_MAP.append(1, "ErrorBMSLowBattery");
        ERROR_CODE_MAP.append(2, "ErrorVoltageLow");
        ERROR_CODE_MAP.append(3, "ErrorVoltageHigh");
        ERROR_CODE_MAP.append(4, "ErrorFallDetected");
        ERROR_CODE_MAP.append(5, "ErrorPickupDetected");
        ERROR_CODE_MAP.append(6, "ErrorOverCurrentDetected");
        ERROR_CODE_MAP.append(7, "ErrorOverTemperature");
        ERROR_CODE_MAP.append(8, "ErrorBadGyro");
        ERROR_CODE_MAP.append(9, "ErrorBadAccelerometer");
        ERROR_CODE_MAP.append(10, "ErrorBadCurrentSensor");
        ERROR_CODE_MAP.append(11, "ErrorBadHallSensors");
        ERROR_CODE_MAP.append(12, "ErrorBadMotor");
        ERROR_CODE_MAP.append(13, "ErrorOvercurrent13");
        ERROR_CODE_MAP.append(14, "ErrorOvercurrent14");
        ERROR_CODE_MAP.append(15, "ErrorRiderDetectZone");
    }

    public final ObservableField<Boolean> isConnected = new ObservableField<>();
    public final ObservableField<Boolean> showDebugWindow = new ObservableField<>();

    public final ObservableField<Boolean> isOneWheelPlus = new ObservableField<>();

    public final ObservableInt speedRpm = new ObservableInt();
    public final ObservableDouble maxSpeedRpm = new ObservableDouble();
    public final ObservableInt maxTiltAnglePitch = new ObservableInt();
    public final ObservableInt maxTiltAngleRoll = new ObservableInt();
    public final ObservableInt lifetimeOdometer = new ObservableInt();
    public final ObservableInt lightMode = new ObservableInt();


    public int firmwareVersion;

    private double[] ampCells = new double[16];
    private double[] batteryVoltageCells = new double[16];

    private static boolean updateBatteryChanges = true;
    private static String updateBatteryMethod = "";


    public static final String OnewheelServiceUUID = "e659f300-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelConfigUUID= "00002902-0000-1000-8000-00805f9b34fb";

    // 00002a04-0000-1000-8000-00805f9b34fb
    public static final String OnewheelCharacteristicSerialNumber = "e659F301-ea98-11e3-ac10-0800200c9a66"; //2085
    public static final String OnewheelCharacteristicRidingMode = "e659f302-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicBatteryRemaining = "e659f303-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicBatteryLow5 = "e659f304-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicBatteryLow20 = "e659f305-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicBatterySerial = "e659f306-ea98-11e3-ac10-0800200c9a66"; //22136
    public static final String OnewheelCharacteristicTiltAnglePitch = "e659f307-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicTiltAngleRoll = "e659f308-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicTiltAngleYaw = "e659f309-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicTemperature = "e659f310-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicStatusError = "e659f30f-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicBatteryCells = "e659f31b-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicBatteryTemp = "e659f315-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicBatteryVoltage = "e659f316-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicCurrentAmps = "e659f312-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicCustomName = "e659f3fd-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicFirmwareRevision = "e659f311-ea98-11e3-ac10-0800200c9a66"; //3034
    public static final String OnewheelCharacteristicHardwareRevision = "e659f318-ea98-11e3-ac10-0800200c9a66"; //2206
    public static final String OnewheelCharacteristicLastErrorCode = "e659f31c-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicLifetimeAmpHours = "e659f31a-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicLifetimeOdometer = "e659f319-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicLightingMode = "e659f30c-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicLightsBack = "e659f30e-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicLightsFront = "e659f30d-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicOdometer = "e659f30a-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicSafetyHeadroom = "e659f317-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicSpeedRpm = "e659f30b-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicTripRegenAmpHours = "e659f314-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicTripTotalAmpHours = "e659f313-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUartSerialRead = "e659f3fe-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUartSerialWrite = "e659f3ff-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN1 = "e659f31d-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN2 = "e659f31e-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN3 = "e659f31f-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN4 = "e659f320-ea98-11e3-ac10-0800200c9a66";

    // These 'dummy' fields don't really sync with the device, but maintain consistency throughout the app.
    public static final String MockOnewheelCharacteristicMotorTemp = "MockOnewheelCharacteristicMotorTemp";
    public static final String MockOnewheelCharacteristicOdometer = "MockOnewheelCharacteristicOdometer";
    public static final String MockOnewheelCharacteristicSpeed = "MockOnewheelCharacteristicSpeed";
    public static final String MockOnewheelCharacteristicMaxSpeed = "MockOnewheelCharacteristicMaxSpeed";
    public static final String MockOnewheelCharacteristicPad1 = "MockOnewheelCharacteristicPad1";
    public static final String MockOnewheelCharacteristicPad2 = "MockOnewheelCharacteristicPad2";

    private Address gpsLocation;

    public void setGpsLocation(Address gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public Address getGpsLocation() {
        return gpsLocation;
    }

/*
0x0000 = e659F301-ea98-11e3-ac10-0800200c9a66 (OnewheelServiceUUID)
0x001a = e659F301-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicSerialNumber)
0x001d = e659f302-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicRidingMode)
0x0021 = e659f303-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicBatteryRemaining)
0x0025 = e659f304-ea98-11e3-ac10-0800200c9a66
0x0029 = e659f305-ea98-11e3-ac10-0800200c9a66
0x003d = e659f306-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicBatterySerial)
0x0041 = 659f307-ea98-11e3-ac10-0800200c9a66
0x0045 = e659f308-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicTiltAngleRoll)
0x0049 = e659f309-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicTiltAngleYaw)
0x003e = e659f30a-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicOdometer)
0x0041 = e659f30b-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicSpeed)
0x0045 = e659f30c-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicLightingMode)
0x0049 = e659f30d-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicLightsFront)
0x004d = e659f30e-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicLightsBack)
0x0051 = e659f30f-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicStatusError)
0x0055 = e659f310-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicTemperature)
0x0059 = e659f311-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicFirmwareRevision)
0x005d = e659f312-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicCurrentAmps)
0x0061 = e659f313-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicTripTotalAmpHours)
0x0065 = e659f314-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicTripRegenAmpHours)
0x0069 = e659f315-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicBatteryTemp)
0x006d = e659f316-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicBatteryVoltage)
0x0071 = e659f317-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicSafetyHeadroom)
0x0075 = e659f318-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicHardwareRevision)
0x0079 = e659f319-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicLifetimeOdometer)
0x007d = e659f31a-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicLifetimeAmpHours)
0x0081 = e659f31b-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicBatteryCells)
0x0085 = e659f31c-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicLastErrorCode)
0x0089 = e659f31d-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicUNKNOWN1)
0x009d = e659f31e-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicUNKNOWN2)
0x0101 = e659f31f-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicUNKNOWN3)
0x0105 = e659f320-ea98-11e3-ac10-0800200c9a66 (OnewheelCharacteristicUNKNOWN4)
0x0045=00 then lights are OFF
0x0045=01 is default lights
0x0045=02 is manual mode for lights
In manual mode (0x0045=02) 0x0049 is front lights and 0x004d is back lights
For both, the first byte is the level of light for white and second byte for red. Levels are 00 (off) to 75 (super bright)
SETS FRONT TO BRIGHT RED AND BACK TO BRIGHT WHITE:
hcitool lescan | grep 'ow' to get device address, e.g D0:39:72:BE:0A:32 ow059062
gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=0002 --handle=0x0045
gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=0075 --handle=0x0049
gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=7500 --handle=0x004d
*/

    public static class DeviceCharacteristic {
        public final ObservableField<String> uuid = new ObservableField<>();
        public final int state;
        public final ObservableField<String> key = new ObservableField<>();
        public final ObservableField<String> value = new ObservableField<>();
        public final ObservableField<String> ui_name = new ObservableField<>();
        public final boolean isNotifyCharacteristic;

        public DeviceCharacteristic(String uuid, String key, String ui_name) {
            this(uuid, key, ui_name, 0,true);
        }

        public DeviceCharacteristic(String uuid, String key, String ui_name, int state, boolean isNotifyCharacteristic) {
            this.uuid.set(uuid);
            this.key.set(key);
            this.ui_name.set(ui_name);
            this.state = state;
            this.isNotifyCharacteristic = isNotifyCharacteristic;
        }
    }

    public List<DeviceCharacteristic> deviceReadCharacteristics = new ArrayList<>();
    public List<DeviceCharacteristic> deviceNotifyCharacteristics = new ArrayList<>();
    public Map<String, DeviceCharacteristic> characteristics = new HashMap<>();

    public List<DeviceCharacteristic> getReadCharacteristics() {
        return deviceReadCharacteristics;
    }
    public List<DeviceCharacteristic> getNotifyCharacteristics() {
        return deviceNotifyCharacteristics;
    }

    public DeviceCharacteristic getDeviceCharacteristicByKey(String key) {
        for (DeviceCharacteristic dc : deviceReadCharacteristics) {
            if (dc.key.get().equals(key)) {
                return dc;
            }
        }
        for (DeviceCharacteristic dc : deviceNotifyCharacteristics) {
            if (dc.key.get().equals(key)) {
                return dc;
            }
        }
        return null;
    }


    /* This method is the main dictionary for the BLE Device. It contains the map for each
       Device attribute and contains the UUID, value, and display string for the UI.
     */
    public void setupCharacteristics() {
        deviceReadCharacteristics.clear();
        deviceNotifyCharacteristics.clear();


        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicHardwareRevision, KEY_HARDWARE_REVISION,   "HARDWARE REVISION",0,false));            // 0
        //deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicFirmwareRevision, KEY_FIRMWARE_REVISION,   "FIRMWARE REVISION"));            // 1
        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicLifetimeOdometer, KEY_LIFETIME_ODOMETER,   "",0,false));                             // 2
        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicLightingMode,     KEY_LIGHTING_MODE,       "LIGHTS",0,false));                       // 3
        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryRemaining, KEY_BATTERY_INITIAL,     "BATTERY AT START (%)",0,false));         // 4
        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicLastErrorCode,    KEY_LAST_ERROR_CODE,     "LAST ERROR CODE",0,false));              // 5
        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryTemp,      KEY_BATTERY_TEMP,        "BATTERY TEMP",0,false));                 // 6
        deviceReadCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicRidingMode,       KEY_RIDE_MODE,           "RIDING MODE",0,false));                  // 7

        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicUartSerialRead,   KEY_SERIAL_READ,         "",0,false));// 18
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicSpeed,        KEY_SPEED,               "",0,false));               // 0
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryRemaining, KEY_BATTERY,             "Battery",0,true));                   // 1
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicStatusError,      KEY_RIDER_DETECTED,      "RIDER",0,true));                     // 2
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicPad1,         KEY_RIDER_DETECTED_PAD_1,"PAD1", 0, false));             // 3
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicPad2,         KEY_RIDER_DETECTED_PAD_2,"PAD2", 0, false));             // 4
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicMaxSpeed,     KEY_SPEED_MAX,           "TRIP TOP SPEED", 0, false)); // 5
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicOdometer,     KEY_ODOMETER,            "", 0, false));                 // 6
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicOdometer,         KEY_ODOMETER_TIRE_REVS,  "TRIP ODOMETER (TIRE REVS)",0,true)); // 7
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicTripTotalAmpHours,KEY_TRIP_AMPS,           "TRIP USED Ah (Amp hours)",0,true));  // 8
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicTripRegenAmpHours,KEY_TRIP_AMPS_REGEN,     "TRIP GAINED Ah (Amp hours)",0,true));// 9
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicSpeedRpm,         KEY_SPEED_RPM,           "SPEED (RPM)", 0,true));// 10
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryVoltage,   KEY_BATTERY_VOLTAGE,     "BATTERY (Voltage)",0,true));         // 11
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryCells,     KEY_BATTERY_CELLS,       "BATTERY CELLS (Voltage)",0,true));   // 12
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicCurrentAmps,      KEY_CURRENT_AMPS,        "BATTERY CURRENT (Amps)",0,true));    // 13
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicTiltAnglePitch,   KEY_TILT_ANGLE_PITCH,    "TILT ANGLE PITCH",0,true));          // 14
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicTiltAngleRoll,    KEY_TILT_ANGLE_ROLL,     "TILT ANGLE ROLL",0,true));           // 15
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicTemperature,      KEY_CONTROLLER_TEMP,     "",0,true));                          // 16
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(MockOnewheelCharacteristicMotorTemp,    KEY_MOTOR_TEMP,          "", 0,false));// 17
        deviceNotifyCharacteristics.add(new DeviceCharacteristic(OnewheelCharacteristicBatteryTemp,      KEY_BATTERY_TEMP,        "BATTERY TEMP",0,true));                 // 18

/*
        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("charging");
            value.set("");
            ui_name.set("CHARGING STATE");
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("crash_state");
            value.set("false");
            ui_name.set("CRASH STATE");
            ui_enabled.set(false);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicUartSerialRead);
            key.set("uart_serial_read");
            value.set("");
            ui_name.set("");
            ui_enabled.set(false);
            enabled.set(false);
        }});
        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicUartSerialWrite);
            key.set("uart_serial_write");
            value.set("");
            ui_name.set("");
            ui_enabled.set(false);
            enabled.set(false);
        }}); */


        characteristics.clear();
        for (DeviceCharacteristic deviceNotifyCharacteristic : deviceNotifyCharacteristics) {
            characteristics.put(deviceNotifyCharacteristic.uuid.get(), deviceNotifyCharacteristic);
        }
        for (DeviceCharacteristic deviceReadCharacteristic : deviceReadCharacteristics) {
            characteristics.put(deviceReadCharacteristic.uuid.get(), deviceReadCharacteristic);
        }

        refreshCharacteristics();
    }

    public void refreshCharacteristics() {
        boolean isMetric = App.INSTANCE.getSharedPreferences().isMetric();
        characteristics.get(MockOnewheelCharacteristicSpeed).ui_name.set(isMetric ? "(KMH)" : "(MPH)");
        characteristics.get(MockOnewheelCharacteristicOdometer).ui_name.set("TRIP ODOMETER " + (isMetric ? "(KM)" : "(MILES)"));
        characteristics.get(OnewheelCharacteristicLifetimeOdometer).ui_name.set("LIFETIME ODOMETER " + (isMetric ? "(KM)" : "(MILES)"));
        characteristics.get(OnewheelCharacteristicTemperature).ui_name.set("CONTROLLER TEMP " + (isMetric ? "(C)" : "(F)"));
        characteristics.get(MockOnewheelCharacteristicMotorTemp).ui_name.set("MOTOR TEMP " + (isMetric ? "(C)" : "(F)"));
    }


    // Status fields
    public boolean lightState = false;
    public final ObservableField<String> bluetoothLe = new ObservableField<>();
    public final ObservableField<String> bluetoothStatus = new ObservableField<>();
    public final ObservableField<String> deviceMacName = new ObservableField<>();
    public final ObservableField<String> deviceMacAddress = new ObservableField<>();
    public final ObservableField<String> log = new ObservableField<>();

    public static float m14598a(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        byte[] value = bluetoothGattCharacteristic.getValue();
        if (value == null || value.length != 2) {
            return 0.0f;
        }
        return (float) ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getChar();
    }

    @Override
    public void processUUID(BluetoothGattCharacteristic incomingCharacteristic) {
        String incomingUuid = incomingCharacteristic.getUuid().toString();
        byte[] incomingValue = incomingCharacteristic.getValue();

        DeviceCharacteristic dc = characteristics.get(incomingUuid);

        String dev_uuid = dc.uuid.get();

        if(dev_uuid != null && dev_uuid.equals(incomingUuid)) {
            switch(dev_uuid) {
                case OnewheelCharacteristicHardwareRevision:
                    int hver = unsignedShort(incomingValue);
                    dc.value.set(Integer.toString(hver));
                    Battery.setHardware(hver);
                    break;
                case OnewheelCharacteristicFirmwareRevision:
                    int fver = unsignedShort(incomingValue);
                    this.firmwareVersion = fver;
                    //dc.value.set(Integer.toString(fver));
                    break;
                case OnewheelCharacteristicLifetimeOdometer:
                    processLifetimeOdometer(incomingValue, dc);
                    break;
                case OnewheelCharacteristicBatterySerial:
                    // Battery: Lithium Iron Phosphate (LiFePo4) 48V
                    //batterySerialNumber.set(Integer.toString(unsignedShort(c_value)));
                    break;
                case OnewheelCharacteristicLightingMode:
                    processLightingMode(incomingValue, dc);
                    break;
                case OnewheelCharacteristicLastErrorCode:
                    processErrorCode(incomingCharacteristic, dc);
                    break;
                case OnewheelCharacteristicBatteryVoltage:
                    processBatteryVoltage(incomingValue, dc);
                    break;
                case OnewheelCharacteristicBatteryRemaining:
                    processBatteryRemaining(incomingCharacteristic, dc);
                    break;
                case OnewheelCharacteristicTiltAnglePitch:
                    processPitch(incomingValue, dc);
                    break;
                case OnewheelCharacteristicTiltAngleRoll:
                    processRoll(incomingValue, dc);
                    break;
                case OWDevice.OnewheelCharacteristicTiltAngleYaw:
                    dc.value.set(Integer.toString(unsignedShort(incomingValue)));
                    break;
                case OnewheelCharacteristicStatusError:
                    processStatusError(incomingValue, dc);
                    break;
                case OnewheelCharacteristicOdometer:
                    processOdometer(incomingValue, dc);
                    break;
                case OnewheelCharacteristicSpeedRpm:
                    processSpeedRpm(incomingValue, dc);
                    break;
                case OnewheelCharacteristicCurrentAmps:
                    processCurrentAmps(incomingValue, dc);
                    break;
                case OnewheelCharacteristicBatteryCells:
                    processBatteryCellsVoltage(incomingValue, dc);
                    break;
                case OnewheelCharacteristicTemperature:
                    processControllerAndMotorTemp(incomingCharacteristic);
                    break;
                case OnewheelCharacteristicBatteryTemp:
                    processBatteryTemp(incomingCharacteristic, dc);
                    break;
                case OnewheelCharacteristicSafetyHeadroom:
                    dc.value.set(incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString());
                    break;
                case OnewheelCharacteristicTripTotalAmpHours:
                    processTripTotalAmpHours(incomingValue, dc);
                    break;
                case OnewheelCharacteristicTripRegenAmpHours:
                    processTripRegenHours(incomingValue, dc);
                    break;
                case OnewheelCharacteristicLifetimeAmpHours:
                    dc.value.set(Integer.toString(unsignedShort(incomingValue)));
                    break;
                case OnewheelCharacteristicRidingMode:
                    processRidingMode(incomingValue, dc, incomingCharacteristic);
                    break;
                default:
                    processUnknownUuid(incomingUuid, incomingValue);
            }
        }

    }

    public void processBatteryVoltage(byte[] incomingValue, DeviceCharacteristic dc) {
        //double d_value = Double.valueOf((double) c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1) / 10.0D);
        int d_volts = unsignedShort(incomingValue);
        double d_value = Double.valueOf((double) d_volts / 10.0D);
        dc.value.set(Double.toString(d_value));
        updateBatteryChanges |= Battery.setOutput(d_value);
    }

    public void processBatteryRemaining(BluetoothGattCharacteristic incomingCharacteristic, DeviceCharacteristic dc) {
        int batteryLevel  = incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);

        //EventBus.getDefault().post(new DeviceBatteryRemainingEvent(batteryLevel));
        //dc.value.set(Integer.toString(batteryLevel));
        updateBatteryChanges |= Battery.setRemaining(batteryLevel);
    }

    public void processPitch(byte[] incomingValue, DeviceCharacteristic dc) {
        int i_tiltAnglePitch = unsignedShort(incomingValue);

        if (i_tiltAnglePitch > maxTiltAnglePitch.get()) {
            maxTiltAnglePitch.set(i_tiltAnglePitch);

        }
        dc.value.set(Integer.toString(i_tiltAnglePitch));
    }

    public void processRoll(byte[] incomingValue, DeviceCharacteristic dc) {
        int i_tiltAngleRoll = unsignedShort(incomingValue);

        if (i_tiltAngleRoll > maxTiltAngleRoll.get()) {
            maxTiltAngleRoll.set(i_tiltAngleRoll);

        }
        dc.value.set(Integer.toString(i_tiltAngleRoll));
    }

    public void processStatusError(byte[] incomingValue, DeviceCharacteristic dc) {
        DeviceStatus deviceStatus = DeviceStatus.from(incomingValue);
        //charging.set(Boolean.toString(deviceStatus.charging));
        //bmsCtrlComms.set(Boolean.toString(deviceStatus.bmsCtrlComms));
        //icsuFault.set(Boolean.toString(deviceStatus.icsuFault));
        //icsvFault.set(Boolean.toString(deviceStatus.icsvFault));

        characteristics.get(MockOnewheelCharacteristicPad1).value.set(Boolean.toString(deviceStatus.riderDetectPad1));
        characteristics.get(MockOnewheelCharacteristicPad2).value.set(Boolean.toString(deviceStatus.riderDetectPad2));
        dc.value.set(Boolean.toString(deviceStatus.riderDetected));
//        for (DeviceCharacteristic dc2 : deviceNotifyCharacteristics) {
//            // TODO 'charging' is commented out, I think @kwatkins said its not working
//            if (dc2.key.get().equals("charging")) {
//                dc2.value.set(Boolean.toString(deviceStatus.charging));
//            }
//        }
    }

    public void processOdometer(byte[] incomingValue, DeviceCharacteristic dc) {
        int i_odometer = unsignedShort(incomingValue);
        DeviceCharacteristic dc_odometer = getDeviceCharacteristicByKey(KEY_ODOMETER);
        if (dc_odometer != null) {
                if (App.INSTANCE.getSharedPreferences().isMetric()) {
                    dc_odometer.value.set(String.format(Locale.getDefault(),"%3.2f", revolutionsToKilometers((double) i_odometer)));
                } else {
                    dc_odometer.value.set(String.format(Locale.getDefault(),"%3.2f", revolutionsToMiles((double) i_odometer)));
                }
        }

        dc.value.set(Integer.toString(i_odometer));
    }

    public void processSpeedRpm(byte[] incomingValue, DeviceCharacteristic dc) {
        int i_speedRpm = unsignedShort(incomingValue);
        speedRpm.set(i_speedRpm);
        dc.value.set(Integer.toString(i_speedRpm));
        updateBatteryChanges |= Battery.setSpeedRpm(i_speedRpm);
        DeviceCharacteristic speedCharacteristic = characteristics.get(MockOnewheelCharacteristicSpeed);
        DeviceCharacteristic maxSpeedCharacteristic = characteristics.get(MockOnewheelCharacteristicMaxSpeed);
        setFormattedSpeedWithMetricPreference(speedCharacteristic, i_speedRpm);
        if (i_speedRpm > maxSpeedRpm.get()) {
            setFormattedSpeedWithMetricPreference(maxSpeedCharacteristic, i_speedRpm);
            maxSpeedRpm.set(i_speedRpm);
        }
    }

    public void processControllerAndMotorTemp(BluetoothGattCharacteristic incomingCharacteristic) {
        int controllerTemp = incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        int motorTemp = incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);

        setFormattedTempWithMetricPreference(characteristics.get(OnewheelCharacteristicTemperature), controllerTemp);
//        Timber.d("controllerTemp = " + controllerTemp);
        setFormattedTempWithMetricPreference(characteristics.get(MockOnewheelCharacteristicMotorTemp), motorTemp);
    }

    public void setFormattedTempWithMetricPreference(DeviceCharacteristic deviceCharacteristic, int temp) {
        boolean isMetric = App.INSTANCE.getSharedPreferences().isMetric();
        deviceCharacteristic.value.set(String.format(Locale.getDefault(), "%.2f", isMetric ? (double) temp : cel2far(temp)));
    }

    public void setFormattedSpeedWithMetricPreference(DeviceCharacteristic deviceCharacteristic, double speedRpm) {
        boolean isMetric = App.INSTANCE.getSharedPreferences().isMetric();
        double currentSpeed = 0;
        if (App.INSTANCE.getSharedPreferences().isMetric()) {
            currentSpeed = rpmToKilometersPerHour(speedRpm);
        } else {
            currentSpeed = rpmToMilesPerHour(speedRpm);
        }
        // "%2.1f" will be in a format of decimal xx.x
        String speed = String.format(Locale.getDefault(), "%2.0f", currentSpeed);
        deviceCharacteristic.value.set(speed);
    }

    public void processBatteryTemp(BluetoothGattCharacteristic incomingCharacteristic, DeviceCharacteristic dc) {
        int batteryTemp = incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

        Timber.d("batteryTemp = " + batteryTemp);

        setFormattedTempWithMetricPreference(dc, batteryTemp);
        updateBatteryChanges |= Battery.setBatteryTemp(batteryTemp);
    }

    public void processUnknownUuid(String incomingUuid, byte[] incomingValue) {
        StringBuilder sb = new StringBuilder();
        for (byte b : incomingValue) {
            sb.append(String.format("%02x", b));
        }
        //this.unknownUUID.set(c_uuid);
        //this.unknownValue.set("hex:" + sb.toString() + " (" + Integer.toString(unsignedShort(c_value)) + ")");
        EventBus.getDefault().post(new DeviceStatusEvent("UNKNOWN " + incomingUuid + ":" +
                "hex:" + sb.toString() + " (" + Integer.toString(unsignedShort(incomingValue)) + ")"));
        Timber.i( "UNKNOWN Device characteristic:" + incomingUuid + " value=" + sb.toString() + "|" + Integer.toString(unsignedShort(incomingValue)));
    }

    public void processTripRegenHours(byte[] incomingValue, DeviceCharacteristic dc) {
        int i_tripregenamp = unsignedShort(incomingValue);
        double d_tripregenamp = Double.valueOf((double) i_tripregenamp / 50.0D);
        dc.value.set(Double.toString(d_tripregenamp));
        updateBatteryChanges |= Battery.setRegenAmpHrs(d_tripregenamp);
    }

    public void processTripTotalAmpHours(byte[] incomingValue, DeviceCharacteristic dc) {
        //tripTotalAmpHours.set(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1).toString());
        //tripTotalAmpHours.set(Integer.toString(unsignedShort(c_value)));
        //int i_cells_2 = unsignedByte(c_value[0]);
        //int camps2 = unsignedByte(c_value[1]);
        //double d_camps2 = (double)camps2 / 50.0D;
        //tripTotalAmpHours.set("batteryVoltageCells:" + i_cells_2 + " value:" + d_camps2);
        int i_amphours = unsignedShort(incomingValue);
        double d_amphours = Double.valueOf((double) i_amphours / 50.0D);
        dc.value.set(Double.toString(d_amphours));
        updateBatteryChanges |= Battery.setUsedAmpHrs(d_amphours);
    }

    private void processRidingMode(byte[] incomingValue, DeviceCharacteristic dc, BluetoothGattCharacteristic incomingCharacteristic) {

        int ridemode = incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
        String rideMode1 = Integer.toString(ridemode);
        Timber.d("rideMode1 = " + rideMode1);

        dc.value.set(rideMode1);
    }

    private void processLifetimeOdometer(byte[] incomingValue, DeviceCharacteristic dc) {
        int i_lifetime = unsignedShort(incomingValue);
        lifetimeOdometer.set(i_lifetime);
        if (App.INSTANCE.getSharedPreferences().isMetric()) {
            dc.value.set(String.format(Locale.getDefault(),"%.2f",milesToKilometers(i_lifetime)));
        } else {
            dc.value.set(Integer.toString(i_lifetime));
        }
    }

    public void processLightingMode(byte[] incomingValue, DeviceCharacteristic dc) {
        switch (unsignedShort(incomingValue)) {
            case 0:
                lightState = false;
                dc.value.set("0 (Off)");
                break;
            case 1:
                lightState = true;
                dc.value.set("1 (On)");
                break;
            case 2:
                lightState = false;
                dc.value.set("2 (Off)");
                break;
            case 3:
                lightState = false;
                dc.value.set("3 (Off)");
        }
    }

    public void processErrorCode(BluetoothGattCharacteristic incomingCharacteristic, DeviceCharacteristic dc) {
        int error_code  = incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        int error_code2  = incomingCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);

        dc.value.set(ERROR_CODE_MAP.get(error_code, " UNKNOWN") + " " + error_code + ":" + error_code2 + "");
    }

    public void processBatteryCellsVoltage(byte[] incomingValue, DeviceCharacteristic dc) {
        int cellIdentifier = unsignedByte(incomingValue[0]);
        int count = 0;
        double volts = 0.0;

        if(cellIdentifier < batteryVoltageCells.length && cellIdentifier >= 0) {
            int var3 = unsignedByte(incomingValue[1]);
            batteryVoltageCells[cellIdentifier] = (double)var3 / 50.0D;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < batteryVoltageCells.length; ++i) {
            if (batteryVoltageCells[i] < 0.1) {
                stringBuilder.append("--");
            } else {
                count++;
                volts+=batteryVoltageCells[i];
                stringBuilder.append(String.format(Locale.ENGLISH, "%.02f",
                    batteryVoltageCells[i]));
            }

            if ((i+1) != batteryVoltageCells.length) {
                if ((i+1) % 4 == 0) {
                    stringBuilder.append("\n");
                } else {
                    stringBuilder.append(',');
                }
            }

        }
        String batteryCellsVoltage = stringBuilder.toString();
        dc.value.set(batteryCellsVoltage);
        if (Battery.checkCells(count)) {
            updateBatteryChanges |= Battery.setCells(volts);
        }
    }

    public void processCurrentAmps(byte[] incomingValue, DeviceCharacteristic dc) {
        float incoming = ByteBuffer.wrap(incomingValue).getShort();
        float multiplier;
        // TODO reference datasheet of chips/sensors
        if (isOneWheelPlus.get()) {
            multiplier = 1.8f;
        } else {
            multiplier = 0.9f;
        }
        final float amps = incoming / 1000.0f * multiplier;
        dc.value.set(String.format(Locale.ENGLISH, "%.2f",amps));
        updateBatteryChanges |= Battery.setAmps(amps);
    }

    public void setBatteryRemaining(MainActivity mainActivity) {
        SharedPreferencesUtil prefs = App.INSTANCE.getSharedPreferences();

        if (! prefs.getBatteryMethod().equals(updateBatteryMethod)) {
            updateBatteryMethod=prefs.getBatteryMethod();
            updateBatteryChanges=true;
        }

        if (updateBatteryChanges) {
            DeviceCharacteristic dc = characteristics.get(OnewheelCharacteristicBatteryRemaining);
            int remaining = 0;

            if (prefs.isRemainOutput()) {
                remaining = Battery.getRemainingOutput();
            } else if (prefs.isRemainCells()) {
                remaining = Battery.getRemainingCells();
            } else if (prefs.isRemainTwoX()) {
                remaining = Battery.getRemainingTwoX();
            } else {
                remaining = Battery.getRemainingDefault();
            }

            dc.value.set(Integer.toString(remaining));
            mainActivity.updateBatteryRemaining(remaining);

            updateBatteryChanges = false;
        }
    }


    /* These are helper methods used to set BLE Device characteristic values */
    public void setCharacteristicValue(BluetoothGattService gattService, BluetoothGatt gatt, String k, int v) {
        DeviceCharacteristic dc = getDeviceCharacteristicByKey(k);
        if (dc != null) {
            BluetoothGattCharacteristic lc = null;
            lc = gattService.getCharacteristic(UUID.fromString(dc.uuid.get()));
            if (lc != null) {
                ByteBuffer var2 = ByteBuffer.allocate(2);
                var2.putShort((short) v);
                lc.setValue(var2.array());
                lc.setWriteType(2);
                gatt.writeCharacteristic(lc);
                EventBus.getDefault().post(new DeviceStatusEvent("SET " + k + " TO " + v));
            }
        }
    }


    //Needed for Gemini, kick off the key/challenge workflow
    public void sendKeyChallengeForGemini(BluetoothUtil bluetoothUtil) {
        Timber.d("GEMINI: Sending firmware revision to OW board triggering key notifications");
        bluetoothUtil.writeCharacteristic(bluetoothUtil.getCharacteristic(OnewheelCharacteristicFirmwareRevision));
    }

    public void setLights(BluetoothUtil bluetoothUtil,int state) {

        lightMode.set(state);
        BluetoothGattCharacteristic lc = null;

        ByteBuffer v = ByteBuffer.allocate(2);
        lc = bluetoothUtil.getCharacteristic(OWDevice.OnewheelCharacteristicLightingMode);

        v.putShort((short) state);
        if (lc != null) {
            lc.setValue(v.array());
            lc.setWriteType(2);
            bluetoothUtil.writeCharacteristic(lc);
            EventBus.getDefault().post(new DeviceStatusEvent("LIGHTS SET TO STATE:" + state));
        }

    }

    public final ObservableInt frontLightsWhite = new ObservableInt();
    public final ObservableInt frontLightsRed = new ObservableInt();
    public final ObservableInt backLightsWhite = new ObservableInt();
    public final ObservableInt backLightsRed = new ObservableInt();

    public void setCustomLights(BluetoothUtil bluetoothUtil, int position, int color, int colorLevel) {
        BluetoothGattCharacteristic lc;
        // front lights
        if (position == 0) {
            if (color == 0) {
                frontLightsWhite.set(colorLevel);
            }
            if (color == 1) {
                frontLightsRed.set(colorLevel);
            }
            lc = bluetoothUtil.getCharacteristic(OWDevice.OnewheelCharacteristicLightsFront);
            if (lc != null) {
               // lc.setValue(new byte[] { (byte)frontLightsWhite.get(), (byte) frontLightsRed.get() });
                int x = frontLightsWhite.get();
                int y = frontLightsRed.get();
                lc.setValue(new byte[] { (byte) x, (byte) y });
                lc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                bluetoothUtil.writeCharacteristic(lc);
            }

        }

        if (position == 1) {
            if (color == 0) {
                backLightsWhite.set(colorLevel);
            }
            if (color == 1) {
                backLightsRed.set(colorLevel);
            }

            lc = bluetoothUtil.getCharacteristic(OWDevice.OnewheelCharacteristicLightsBack);
            if (lc != null) {
                int x = backLightsWhite.get();
                int y = backLightsRed.get();
                lc.setValue(new byte[] { (byte) x, (byte) y });
                lc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                bluetoothUtil.writeCharacteristic(lc);
            }

        }
    }

    public void setRideMode(BluetoothUtil bluetoothUtil, int ridemode) {
        Timber.d("setRideMode() called for gatt:" + ridemode);
        BluetoothGattCharacteristic lc = bluetoothUtil.getCharacteristic(OWDevice.OnewheelCharacteristicRidingMode);
        if (lc != null) {
            ByteBuffer var2 = ByteBuffer.allocate(2);
            var2.putShort((short) ridemode);
            lc.setValue(var2.array());
            lc.setWriteType(2);
            bluetoothUtil.writeCharacteristic(lc);
            //setDeviceCharacteristicDisplay("ride_mode","ridemode: " + ridemode);
        }
    }

    @Override
    public String getCSVHeader() {
        StringBuilder headers = new StringBuilder();
        for(OWDevice.DeviceCharacteristic dc : this.deviceNotifyCharacteristics) {
            headers.append(',').append(dc.key.get());
        }
        return "time" + headers.toString() + '\n';
    }

    @Override
    public String toCSV() {
        String dateTimeString = SIMPLE_DATE_FORMAT.format(new Date());
        String header = String.format(Locale.US, "%s", dateTimeString);
        StringBuilder values = new StringBuilder();
        for(OWDevice.DeviceCharacteristic dc : this.deviceNotifyCharacteristics) {
            values.append(',').append(dc.value.get());
        }

        if (gpsLocation != null) {
            values.append(",LOC=(").append(gpsLocation.getLongitude() + "," + gpsLocation.getLatitude()).append(")");
        }
        return header + values.toString() + '\n';
    }

    public String toString() {
        return "";
    }

    @Override
    public String getName() { return NAME; }

}
