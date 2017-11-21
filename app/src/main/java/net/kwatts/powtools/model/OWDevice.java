package net.kwatts.powtools.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.databinding.BaseObservable;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableDouble;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import net.kwatts.powtools.App;
import net.kwatts.powtools.DeviceInterface;
import net.kwatts.powtools.events.DeviceStatusEvent;

import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by kwatts on 3/23/16.
 */




public class OWDevice extends BaseObservable implements DeviceInterface {
    private static final String TAG = "OWTOOLS";
    private static final String NAME = "ONEWHEEL";


    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static final SimpleDateFormat OLD_SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);


    public final ObservableField<Boolean> isConnected = new ObservableField<>();
    public final ObservableField<Boolean> showDebugWindow = new ObservableField<>();

    public final ObservableField<Boolean> isOneWheelPlus = new ObservableField<>();

    public final ObservableDouble maxSpeed = new ObservableDouble();
    public final ObservableInt maxTiltAnglePitch = new ObservableInt();
    public final ObservableInt maxTiltAngleRoll = new ObservableInt();
    public final ObservableInt lifetimeOdometer = new ObservableInt();
    public final ObservableInt lightMode = new ObservableInt();



    public static final String OnewheelServiceUUID = "e659f300-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelConfigUUID= "00002902-0000-1000-8000-00805f9b34fb";
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
    public static final String OnewheelCharacteristicSpeed = "e659f30b-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicTripRegenAmpHours = "e659f314-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicTripTotalAmpHours = "e659f313-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUartSerialRead = "e659f3fe-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUartSerialWrite = "e659f3ff-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN1 = "e659f31d-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN2 = "e659f31e-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN3 = "e659f31f-ea98-11e3-ac10-0800200c9a66";
    public static final String OnewheelCharacteristicUNKNOWN4 = "e659f320-ea98-11e3-ac10-0800200c9a66";
    private String location;

    public void setLocation(String location) {
        this.location = location;
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
gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=0002 --handle=0x0045
gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=0075 --handle=0x0049
gatttool --device=D0:39:72:BE:0A:32 --char-write-req --value=7500 --handle=0x004d
*/

    public static class DeviceCharacteristic {
        public final ObservableField<String> uuid = new ObservableField<>();
        public final ObservableField<String> key = new ObservableField<>();
        public final ObservableField<String> value = new ObservableField<>();
        public final ObservableField<Boolean> enabled = new ObservableField<>();
        public final ObservableField<Boolean> ui_enabled = new ObservableField<>();
        public final ObservableField<String> ui_name = new ObservableField<>();
    }

    public void setDeviceCharacteristicDisplay(String key,String value) {
        for (DeviceCharacteristic dc : deviceReadCharacteristics) {
            if (dc.value.get().equals(key)) {
                dc.ui_name.set(value);
            }
        }
        for (DeviceCharacteristic dc : deviceNotifyCharacteristics) {
            if (dc.value.get().equals(key)) {
                dc.ui_name.set(value);
            }
        }
    }


    public ObservableArrayList<DeviceCharacteristic> deviceReadCharacteristics = new ObservableArrayList<DeviceCharacteristic>();
    public ObservableArrayList<DeviceCharacteristic> deviceNotifyCharacteristics = new ObservableArrayList<DeviceCharacteristic>();

    public ObservableArrayList<DeviceCharacteristic> getReadCharacteristics() {
        return deviceReadCharacteristics;
    }
    public ObservableArrayList<DeviceCharacteristic> getNotifyCharacteristics() {
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
    public void refresh() {
        deviceReadCharacteristics.clear();
        deviceNotifyCharacteristics.clear();

        deviceReadCharacteristics.add(new DeviceCharacteristic() {{
            uuid.set(OnewheelCharacteristicHardwareRevision);
            key.set("hardware_revision");
            value.set("");
            ui_name.set("HARDWARE REVISION");
            ui_enabled.set(true);
        }});

        deviceReadCharacteristics.add(new DeviceCharacteristic() {{
            uuid.set(OnewheelCharacteristicFirmwareRevision);
            key.set("firmware_revision");
            value.set("");
            ui_name.set("FIRMWARE REVISION");
            ui_enabled.set(true);
        }});

        deviceReadCharacteristics.add(new DeviceCharacteristic() {{
            uuid.set(OnewheelCharacteristicLifetimeOdometer);
            key.set("lifetime_odometer");
            value.set("");
            if (App.INSTANCE.getSharedPreferences().isMetric()) {
                ui_name.set("LIFETIME ODOMETER (KM)");
            } else {
                ui_name.set("LIFETIME ODOMETER (MILES)");
            }
            ui_enabled.set(true);
        }});
/*
                deviceReadCharacteristics.add(new DeviceCharacteristic() {{
                    uuid.set(OnewheelCharacteristicLifetimeAmpHours);
                    key.set("lifetime_amps");
                    value.set("");
                    ui_name.set("LIFETIME AMPS (HOURS)");
                    ui_enabled.set(true);
                }}); */
                deviceReadCharacteristics.add(new DeviceCharacteristic() {{
                    uuid.set(OnewheelCharacteristicLightingMode);
                    key.set("lighting_mode");
                    value.set("");
                    ui_name.set("LIGHTS");
                    ui_enabled.set(true);
                }}); /*
                deviceReadCharacteristics.add(new DeviceCharacteristic() {{
                    uuid.set(OnewheelCharacteristicRidingMode);
                    key.set("ride_mode");
                    value.set("");
                    ui_name.set("RIDING MODE");
                    ui_enabled.set(false);
                }}); */
                deviceReadCharacteristics.add(new DeviceCharacteristic() {{
                    uuid.set(OnewheelCharacteristicBatteryRemaining);
                    key.set("battery_initial");
                    value.set("");
                    ui_name.set("BATTERY AT START (%)");
                    ui_enabled.set(true);
                }});


        deviceReadCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicLastErrorCode);
            key.set("last_error_code");
            value.set("");
            ui_name.set("LAST ERROR CODE");
            ui_enabled.set(true);
            enabled.set(true);
        }});


        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("speed");
            value.set("0.0");
            if (App.INSTANCE.getSharedPreferences().isMetric()) {
                ui_name.set("(KMH)");
            } else {
                ui_name.set("(MPH)");
            }
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicBatteryRemaining);
            key.set("battery");
            value.set("0");
            ui_name.set("Battery");
            ui_enabled.set(true);
            enabled.set(true);
        }});
        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicStatusError);
            key.set("rider_detected");
            value.set("");
            ui_name.set("RIDER");
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("rider_detected_pad1");
            value.set("");
            ui_name.set("PAD1");
            ui_enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("rider_detected_pad2");
            value.set("");
            ui_name.set("PAD2");
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("speed_max");
            value.set("");
            if (App.INSTANCE.getSharedPreferences().isMetric()) {
                ui_name.set("Trip Top Speed: ");
            } else {
                ui_name.set("Trip Top Speed: ");
            }
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("odometer");
            value.set("");
            if (App.INSTANCE.getSharedPreferences().isMetric()) {
                ui_name.set("TRIP ODOMETER (KMS)");
            } else {
                ui_name.set("TRIP ODOMETER (MILES)");
            }
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicOdometer);
            key.set("odometer_tire_revs");
            value.set("");
            ui_name.set("TRIP ODOMETER (TIRE REVS)");
            ui_enabled.set(true);
            enabled.set(true);
        }});


        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicTripTotalAmpHours);
            key.set("trip_amps");
            value.set("");
            ui_name.set("TRIP USED Ah (Amp hours)");
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicTripRegenAmpHours);
            key.set("trip_amps_regen");
            value.set("");
            ui_name.set("TRIP GAINED Ah (Amp hours)");
            ui_enabled.set(true);
            enabled.set(true);
        }});
        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicSpeed);
            key.set("speed_rpm");
            value.set("");
            ui_name.set("SPEED (RPM)");
            ui_enabled.set(true);
            enabled.set(true);
        }});


        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicBatteryVoltage);
            key.set("battery_voltage");
            value.set("");
            ui_name.set("BATTERY (Voltage)");
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicBatteryCells);
            key.set("battery_cells");
            value.set("");
            ui_name.set("BATTERY CELLS (Voltage)");
            ui_enabled.set(true);
            enabled.set(true);
        }});
        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicCurrentAmps);
            key.set("current_amps");
            value.set("");
            ui_name.set("BATTERY CURRENT (Amps)");
            ui_enabled.set(true);
            enabled.set(true);
        }});


        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicTiltAnglePitch);
            key.set("tilt_angle_pitch");
            value.set("");
            ui_name.set("TILT ANGLE PITCH");
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicTiltAngleRoll);
            key.set("tilt_angle_roll");
            value.set("");
            ui_name.set("TILT ANGLE ROLL");
            ui_enabled.set(true);
            enabled.set(true);
        }});


        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            uuid.set(OnewheelCharacteristicTemperature);
            key.set("controller_temp");
            value.set("");
            if (App.INSTANCE.getSharedPreferences().isMetric()) {
                ui_name.set("CONTROLLER TEMP (C)");
            } else {
                ui_name.set("CONTROLLER TEMP (F)");
            }
            ui_enabled.set(true);
            enabled.set(true);
        }});

        deviceNotifyCharacteristics.add(new DeviceCharacteristic()
        {{
            key.set("motor_temp");
            value.set("");
            if (App.INSTANCE.getSharedPreferences().isMetric()) {
                ui_name.set("MOTOR TEMP (C)");
            } else {
                ui_name.set("MOTOR TEMP (F)");
            }
            ui_enabled.set(true);
            enabled.set(true);
        }});



        deviceNotifyCharacteristics.add(new DeviceCharacteristic() {{
            uuid.set(OnewheelCharacteristicRidingMode);
            key.set("ride_mode");
            value.set("");
            ui_name.set("RIDING MODE");
            ui_enabled.set(true);
            enabled.set(true);
        }});

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



    }


    // Status fields
    public boolean lightState = false;
    public final ObservableField<String> bluetoothLe = new ObservableField<>();
    public final ObservableField<String> bluetoothStatus = new ObservableField<>();
    public final ObservableField<String> deviceMacName = new ObservableField<>();
    public final ObservableField<String> deviceMacAddress = new ObservableField<>();
    public final ObservableField<String> log = new ObservableField<>();

    @Override
    public void processUUID(BluetoothGattCharacteristic c) {
        String c_uuid = c.getUuid().toString();
        byte[] c_value = c.getValue();


        for(DeviceCharacteristic dc : deviceReadCharacteristics) {
            String dev_uuid = dc.uuid.get();
            if(dev_uuid != null && dev_uuid.equals(c_uuid)) {
                switch(dev_uuid) {
                    case OnewheelCharacteristicHardwareRevision:
                        dc.value.set(Integer.toString(unsignedShort(c_value)));
                        break;
                    case OnewheelCharacteristicFirmwareRevision:
                        dc.value.set(Integer.toString(unsignedShort(c_value)));
                        break;
                    case OnewheelCharacteristicLifetimeOdometer:
                        int i_lifetime = unsignedShort(c_value);
                        lifetimeOdometer.set(i_lifetime);
                        if (App.INSTANCE.getSharedPreferences().isMetric()) {
                            dc.value.set(String.format(Locale.getDefault(),"%.2f",milesToKilometers(i_lifetime)));
                        } else {
                            dc.value.set(Integer.toString(i_lifetime));
                        }
                        break;
                    /*
                    case OnewheelCharacteristicRidingMode:
                        int ridemode = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                        Log.d(TAG, "Got the ridemode from BLE:" + ridemode);
                        dc.value.set("initial ridemode " + ridemode);
                        // Let the UI know initial ridemode
                        EventBus.getDefault().post(new DeviceStatusEvent("Initial ridemode set to: " + ridemode));
                        EventBus.getDefault().post(new RideModeEvent(ridemode));
                        break; */
                    case OnewheelCharacteristicBatterySerial:
                        // Battery: Lithium Iron Phosphate (LiFePo4) 48V
                        //batterySerialNumber.set(Integer.toString(unsignedShort(c_value)));
                        break;
                    case OnewheelCharacteristicLightingMode:
                        switch (unsignedShort(c_value)) {
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
                        break;
                    case OnewheelCharacteristicLastErrorCode:
                        int error_code  = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        int error_code2  = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                        String s_error_code = " " + error_code + ":" + error_code2 + "";


                        switch (error_code) {
                        case 1:
                            dc.value.set("ErrorBMSLowBattery"+s_error_code);
                            break;
                        case 2:
                            dc.value.set("ErrorVoltageLow"+s_error_code);
                            break;
                        case 3:
                            dc.value.set("ErrorVoltageHigh"+s_error_code);
                            break;
                        case 4:
                            dc.value.set("ErrorFallDetected"+s_error_code);
                            break;
                        case 5:
                            dc.value.set("ErrorPickupDetected"+s_error_code);
                            break;
                        case 6:
                            dc.value.set("ErrorOverCurrentDetected"+s_error_code);
                            break;
                        case 7:
                            dc.value.set("ErrorOverTemperature"+s_error_code);
                            break;
                        case 8:
                            dc.value.set("ErrorBadGyro"+s_error_code);
                            break;
                        case 9:
                            dc.value.set("ErrorBadAccelerometer"+s_error_code);
                            break;
                        case 10:
                            dc.value.set("ErrorBadCurrentSensor"+s_error_code);
                            break;
                        case 11:
                            dc.value.set("ErrorBadHallSensors"+s_error_code);
                            break;
                        case 12:
                            dc.value.set("ErrorBadMotor"+s_error_code);
                            break;
                        case 13:
                            dc.value.set("ErrorOvercurrent13"+s_error_code);
                            break;
                        case 14:
                            dc.value.set("ErrorOvercurrent14"+s_error_code);
                            break;
                        case 15:
                            dc.value.set("ErrorRiderDetectZone"+s_error_code);
                            break;
                            default:
                                dc.value.set(s_error_code);
                                break;
                    }
                    default:
                }
            }
        }

        for(DeviceCharacteristic dc : deviceNotifyCharacteristics) {
            if (dc.uuid.get() != null && dc.uuid.get().equals(c_uuid)) {
                switch(dc.uuid.get()) {
                    case OnewheelCharacteristicBatteryVoltage:
                        //double d_value = Double.valueOf((double) c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1) / 10.0D);
                        int d_volts = unsignedShort(c_value);
                        double d_value = Double.valueOf((double) d_volts / 10.0D);
                        dc.value.set(Double.toString(d_value));
                        break;
                    case OnewheelCharacteristicBatteryRemaining:
                        int batteryLevel  = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);

                        //EventBus.getDefault().post(new DeviceBatteryRemainingEvent(batteryLevel));
                        dc.value.set(Integer.toString(batteryLevel));
                        break;
                    case OnewheelCharacteristicTiltAnglePitch:
                        int i_tiltAnglePitch = unsignedShort(c_value);

                        if (i_tiltAnglePitch > maxTiltAnglePitch.get()) {
                            maxTiltAnglePitch.set(i_tiltAnglePitch);

                        }
                        dc.value.set(Integer.toString(i_tiltAnglePitch));
                        break;
                    case OnewheelCharacteristicTiltAngleRoll:
                        int i_tiltAngleRoll = unsignedShort(c_value);

                        if (i_tiltAngleRoll > maxTiltAngleRoll.get()) {
                            maxTiltAngleRoll.set(i_tiltAngleRoll);

                        }
                        dc.value.set(Integer.toString(i_tiltAngleRoll));
                        break;
                    case OWDevice.OnewheelCharacteristicTiltAngleYaw:
                        dc.value.set(Integer.toString(unsignedShort(c_value)));
                        break;

                    case OnewheelCharacteristicStatusError:
                        DeviceStatus deviceStatus = DeviceStatus.from(c_value);
                        //charging.set(Boolean.toString(deviceStatus.charging));
                        //bmsCtrlComms.set(Boolean.toString(deviceStatus.bmsCtrlComms));
                        //icsuFault.set(Boolean.toString(deviceStatus.icsuFault));
                        //icsvFault.set(Boolean.toString(deviceStatus.icsvFault));
                        dc.value.set(Boolean.toString(deviceStatus.riderDetected));
                        for (DeviceCharacteristic dc2 : deviceNotifyCharacteristics) {
                            if (dc2.key.get().equals("rider_detected_pad1")) {
                                dc2.value.set(Boolean.toString(deviceStatus.riderDetectPad1));
                            }
                            if (dc2.key.get().equals("rider_detected_pad2")) {
                                dc2.value.set(Boolean.toString(deviceStatus.riderDetectPad2));
                            }
                            if (dc2.key.get().equals("charging")) {
                                dc2.value.set(Boolean.toString(deviceStatus.charging));
                            }
                        }
                        break;
                    case OnewheelCharacteristicOdometer:
                        int i_odometer = unsignedShort(c_value);
                        DeviceCharacteristic dc_odometer = getDeviceCharacteristicByKey("odometer");
                        if (dc_odometer != null) {
                                if (App.INSTANCE.getSharedPreferences().isMetric()) {
                                    dc_odometer.value.set(String.format("%3.2f", revolutionsToKilometers((double) i_odometer)));
                                } else {
                                    dc_odometer.value.set(String.format("%3.2f", revolutionsToMiles((double) i_odometer)));
                                }
                        }

                        dc.value.set(Integer.toString(i_odometer));
                        break;

                    case OnewheelCharacteristicSpeed:
                        int i_speed = unsignedShort(c_value);
                        dc.value.set(Integer.toString(i_speed));
                        DeviceCharacteristic dc_speed = getDeviceCharacteristicByKey("speed");
                        if (dc_speed != null) {
                            if (App.INSTANCE.getSharedPreferences().isMetric()) {
                                dc_speed.value.set(String.format(Locale.getDefault(),"%3.2f", rpmToKilometersPerHour((double) i_speed)));
                            } else {
                                dc_speed.value.set(String.format(Locale.getDefault(),"%3.2f", rpmToMilesPerHour((double) i_speed)));
                            }
                        }
                        DeviceCharacteristic dc_speed_max = getDeviceCharacteristicByKey("speed_max");
                        if (dc_speed_max != null) {
                            if (i_speed > maxSpeed.get()) {
                                if (App.INSTANCE.getSharedPreferences().isMetric()) {
                                    dc_speed_max.value.set(String.format(Locale.getDefault(),"%3.2f", rpmToKilometersPerHour((double) i_speed)));
                                } else {
                                    dc_speed_max.value.set(String.format(Locale.getDefault(),"%3.2f", rpmToMilesPerHour((double) i_speed)));
                                }
                                maxSpeed.set(i_speed);
                            }
                        }

                        break;
                    case OnewheelCharacteristicCurrentAmps:
                        // Wh = mAh × V / 1000
                        // battery is 3.5Amps
                        int i_cells_1 = unsignedByte(c_value[0]);
                        double[] amp_cells = new double[15];
                        if(i_cells_1 < amp_cells.length && i_cells_1 >= 0) {
                            int var3 = unsignedByte(c_value[1]);
                            amp_cells[i_cells_1] = (double)var3 / 50.0D;
                        }
                        StringBuilder amps_string = new StringBuilder();
                        for (int x = 0;x < amp_cells.length;++x) {
                            if (amp_cells[x] == 0) {
                                amps_string.append('-');
                            } else {
                                amps_string.append(amp_cells[x]);
                            }
                            if(x != -1 + amp_cells.length) {
                                amps_string.append('|');
                            }
                        }
                        dc.value.set(amps_string.toString());
                        //dc.value.set(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1).toString());
                        //int camps = unsignedByte(c_value[1]);
                        //double d_camps = (double)camps / 50.0D;
                        //currentAmps.set("cells:" + i_cells_1 + " value:" + d_camps);
                        //int i_currentamps = unsignedShort(c_value);
                        //double d_currentapps = Double.valueOf((double) i_currentamps / 50.0D);
                        //currentAmps.set(Double.toString(d_currentapps));
                        break;
                    case OnewheelCharacteristicBatteryCells:
                        int i_cells = unsignedByte(c_value[0]);
                        double[] cells = new double[15];
                        if(i_cells < cells.length && i_cells >= 0) {
                            int var3 = unsignedByte(c_value[1]);
                            cells[i_cells] = (double)var3 / 50.0D;
                        }
                        StringBuilder var1 = new StringBuilder();
                        for(int var3 = 0; var3 < cells.length; ++var3) {
                            if (cells[var3] == 0) {
                                var1.append('-');
                            } else {
                                var1.append(cells[var3]);
                            }
                            if(var3 != -1 + cells.length) {
                                var1.append('|');
                            }
                        }
                        dc.value.set(var1.toString());
                        break;
                    case OnewheelCharacteristicTemperature:
                        int controllerTemp = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        int motorTemp = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                        for (DeviceCharacteristic dc2 : deviceNotifyCharacteristics) {
                            if (dc2.key.get().equals("controller_temp")) {
                                if (App.INSTANCE.getSharedPreferences().isMetric()) {
                                    dc2.value.set(String.format(Locale.getDefault(),"%.2f", (double)controllerTemp));
                                } else {
                                    dc2.value.set(String.format(Locale.getDefault(),"%.2f", cel2far(controllerTemp)));
                                }
                            }

                            if  (dc2.key.get().equals("motor_temp")) {
                                if (App.INSTANCE.getSharedPreferences().isMetric()) {
                                    dc2.value.set(String.format(Locale.getDefault(),"%.2f", (double)motorTemp));
                                } else {
                                    dc2.value.set(String.format(Locale.getDefault(),"%.2f", cel2far(motorTemp)));
                                }
                            }
                        }
                        break;
                    case OnewheelCharacteristicBatteryTemp:
                        int batteryTemp = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                        if (App.INSTANCE.getSharedPreferences().isMetric()) {
                            dc.value.set(String.format(Locale.getDefault(),"%.2f", (double) batteryTemp));
                        } else {
                            dc.value.set(String.format(Locale.getDefault(),"%.2f", cel2far(batteryTemp)));
                        }
                        break;
                    case OnewheelCharacteristicSafetyHeadroom:
                        dc.value.set(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString());
                        break;
                    case OnewheelCharacteristicTripTotalAmpHours:
                        //tripTotalAmpHours.set(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1).toString());
                        //tripTotalAmpHours.set(Integer.toString(unsignedShort(c_value)));
                        //int i_cells_2 = unsignedByte(c_value[0]);
                        //int camps2 = unsignedByte(c_value[1]);
                        //double d_camps2 = (double)camps2 / 50.0D;
                        //tripTotalAmpHours.set("cells:" + i_cells_2 + " value:" + d_camps2);
                        int i_amphours = unsignedShort(c_value);
                        double d_amphours = Double.valueOf((double) i_amphours / 50.0D);
                        dc.value.set(Double.toString(d_amphours));
                        break;
                    case OnewheelCharacteristicTripRegenAmpHours:
                        int i_tripregenamp = unsignedShort(c_value);
                        double d_tripregenamp = Double.valueOf((double) i_tripregenamp / 50.0D);
                        dc.value.set(Double.toString(d_tripregenamp));
                        break;
                    case OnewheelCharacteristicLifetimeAmpHours:
                        dc.value.set(Integer.toString(unsignedShort(c_value)));
                        break;
                    case OnewheelCharacteristicRidingMode:
                        int ridemode = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                        dc.value.set(Integer.toString(ridemode));
                        // Let the UI know initial ridemode
                        EventBus.getDefault().post(new DeviceStatusEvent("Ridemode changed to: " + ridemode));
                        //EventBus.getDefault().post(new RideModeEvent(ridemode));
                        break;

                    default:
                        StringBuilder sb = new StringBuilder();
                        for (byte b : c_value) {
                            sb.append(String.format("%02x", b));
                        }
                        //this.unknownUUID.set(c_uuid);
                        //this.unknownValue.set("hex:" + sb.toString() + " (" + Integer.toString(unsignedShort(c_value)) + ")");
                        EventBus.getDefault().post(new DeviceStatusEvent("UNKNOWN " + c_uuid + ":" +
                                "hex:" + sb.toString() + " (" + Integer.toString(unsignedShort(c_value)) + ")"));
                        Log.i(TAG, "UNKNOWN Device characteristic:" + c_uuid + " value=" + sb.toString() + "|" + Integer.toString(unsignedShort(c_value)));


                }

            }
        }

    }


    /* These are helper methods used to set BLE Device characteristic values */
    public void setCharacteristicValue(BluetoothGattService gattService,BluetoothGatt gatt,String k,int v) {
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

    public void setLights(BluetoothGattService gattService,BluetoothGatt gatt,int state) {

        lightMode.set(state);
        BluetoothGattCharacteristic lc = null;

        ByteBuffer v = ByteBuffer.allocate(2);
        lc = gattService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicLightingMode));

        v.putShort((short) state);
        if (lc != null) {
            lc.setValue(v.array());
            lc.setWriteType(2);
            gatt.writeCharacteristic(lc);
            EventBus.getDefault().post(new DeviceStatusEvent("LIGHTS SET TO STATE:" + state));
        }

    }

    public final ObservableInt frontLightsWhite = new ObservableInt();
    public final ObservableInt frontLightsRed = new ObservableInt();
    public final ObservableInt backLightsWhite = new ObservableInt();
    public final ObservableInt backLightsRed = new ObservableInt();

    public void setCustomLights(BluetoothGattService gattService,BluetoothGatt gatt,int position, int color, int colorLevel) {
        BluetoothGattCharacteristic lc = null;
        // front lights
        if (position == 0) {
            if (color == 0) {
                frontLightsWhite.set(colorLevel);
            }
            if (color == 1) {
                frontLightsRed.set(colorLevel);
            }
            lc = gattService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicLightsFront));
            if (lc != null) {
               // lc.setValue(new byte[] { (byte)frontLightsWhite.get(), (byte) frontLightsRed.get() });
                int x = frontLightsWhite.get();
                int y = frontLightsRed.get();
                lc.setValue(new byte[] { (byte) x, (byte) y });
                lc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                gatt.writeCharacteristic(lc);
            }

        }

        if (position == 1) {
            if (color == 0) {
                backLightsWhite.set(colorLevel);
            }
            if (color == 1) {
                backLightsRed.set(colorLevel);
            }

            lc = gattService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicLightsBack));
            if (lc != null) {
                int x = backLightsWhite.get();
                int y = backLightsRed.get();
                lc.setValue(new byte[] { (byte) x, (byte) y });
                lc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                gatt.writeCharacteristic(lc);
            }

        }
    }

    public void setRideMode(BluetoothGattService gattService,BluetoothGatt gatt,int ridemode) {
        Log.d(TAG,"setRideMode() called for gatt:" + ridemode);
        BluetoothGattCharacteristic lc = gattService.getCharacteristic(UUID.fromString(OWDevice.OnewheelCharacteristicRidingMode));
        if (lc != null) {
            ByteBuffer var2 = ByteBuffer.allocate(2);
            var2.putShort((short) ridemode);
            lc.setValue(var2.array());
            lc.setWriteType(2);
            gatt.writeCharacteristic(lc);
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

        if (location != null) {
            values.append(",LOC=(").append(location).append(")");
        }
        return header + values.toString() + '\n';
    }

    public String toString() {
        return "";
    }

    @Override
    public String getName() { return NAME; }
    /* Helper methods. For dealing with bytes see:
       http://www.roseindia.net/java/master-java/bitwise-bitshift-operators.shtml
       https://calleerlandsson.com/2014/02/06/rubys-bitwise-operators/
     */
    public static short byteToShort(byte[] v) {
        return (short)((v[1] << 8) + (v[0] & 0xff));
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

    public static String bytesToHex(byte c[]) {
        StringBuilder sb = new StringBuilder();
        for (byte b : c) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
