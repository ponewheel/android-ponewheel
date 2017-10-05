package net.kwatts.powtools;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.*;

import android.util.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.widget.*;
import android.os.*;
import android.os.PowerManager.*;
import android.view.*;
import android.text.method.*;
import android.databinding.*;
import android.preference.PreferenceManager;
import android.preference.Preference;

import com.afollestad.materialdialogs.*;
import org.greenrobot.eventbus.*;
import org.honorato.multistatetogglebutton.MultiStateToggleButton;

import java.io.*;
import org.json.JSONObject;
import net.kwatts.powtools.events.*;
import net.kwatts.powtools.loggers.*;
import net.kwatts.powtools.services.*;
import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.components.Legend;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

// http://blog.davidvassallo.me/2015/09/02/ble-health-devices-first-steps-with-android/
// https://github.com/alt236/Bluetooth-LE-Library---Android
// https://github.com/Fakher-Hakim/android-BluetoothLeGatt
// https://developer.android.com/tools/data-binding/guide.html
// https://github.com/iDevicesInc/SweetBlue
// https://www.evilsocket.net/2015/01/29/nike-fuelband-se-ble-protocol-reversed/
// mapping to google maps with geojson? https://developers.google.com/maps/documentation/android-api/utility/geojson#style-feature
// OW stats: 58VDC charger, 3.5Amp with 130Wh (LiFEPO4 Nano-phosphate Litium) and 500W motor
// Other stats: Likely 7500 mah = 7500/58 is 130Wh
// Calculations: 130wh/48v = 2.7AH  - a 2.7AH battery would take 54 minutes to charge.... (2.7/3.5 = 0.9 x 60 minutes)
// AMP hours = a battery with 1 amp-hour supplies 1 amp to load for 1 hour. 2 amps for 1/2 hour, etc
// I (current measured in Amps) = V (Volts) / R (resistance,ohms)
// The consumed power of motor is P (input power, measured in Watts) = I (Amps) * V (applied Voltage)
// Should show stats with,
// - Speed
// - Consumed power of motor (W)
// - Consumed power total
// 12.8V 6.9Ah 88.32Wh

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "POWTOOLS";
    private static final boolean SCANNER = false;
    private static final boolean POWER_USER = false;
    private static final boolean ONEWHEEL = true;
    private static final boolean ONEWHEEL_LOGGING = true;
    private static final String ONEWHEEL_LOGGING_PATH = "powlogs";
    private static final int ONEWHEEL_LOGGING_DELAY = 1000;
    private static final int REQUEST_ENABLE_BT = 1;


    public VibrateService mVibrateService;
    private android.os.Handler mLoggingHandler = new android.os.Handler();
    private Runnable deviceFileLogger;
    private int logging_seconds = 0;
    private PlainTextFileLogger mTextFileLogger;
    private android.bluetooth.BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mGatt;
    private BluetoothGattService owGatService;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private Context mContext;
    SharedPreferences mSharedPref;
    MaterialDialog.Builder mAboutDialog;
    private boolean mOWConnected;
    private String mDeviceMacAddress;
    private String mDeviceMacName;
    private boolean mScanning;
    private Toolbar mToolbar;
    Spinner mKeySpinner;
    Spinner mValueSpinner;
    ArrayAdapter<String> mKeyAdapter;
    ArrayAdapter<String> mValueAdapter;
    ScrollView mScrollView;
    Chronometer mChronometer;
    MaterialDialog.Builder mDeviceLightDialog;
    WakeLock mWakeLock;
    private OWDevice mOWDevice;
    net.kwatts.powtools.databinding.ActivityMainBinding mBinding;

    Map<String, String> mScanResults = new HashMap<>();
    JSONObject mCurrentDevice = new JSONObject();

    // defaults from preferences
    int mLoggingFrequency = 1000;

    //ArrayList<Integer> mColors;
    //ArrayList<String> mLabels;
    PieChart mBatteryChart;
    //PieData mBatteryPieData;


    int mCount1;

    @Subscribe
    public void onDeviceStatusEvent(DeviceStatusEvent event){
        updateLog(event.message);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceBatteryRemainingEvent event){
        //updateLog("Got battery remaining event");
        updateBatteryRemaining(event.percentage);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RideModeEvent event){
        updateRideMode(event.rideMode);
    }


    @Subscribe
    public void onEvent(DeviceCrashEvent event){
        //TODO: updateCrashEvent()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NotificationEvent event){
        Log.d(TAG, event.message + ":" + event.title);
        final String t = event.title;
        final String m = event.message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.support.v4.app.NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(mContext)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(t)
                                .setContentText(m);
                android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(m,0, mBuilder.build());
            }
        });
    }

    private void updateLog(final String msg) {
        Log.d(TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mBinding.owLog.setMovementMethod(new ScrollingMovementMethod());
                mBinding.owLog.append("\n" + msg);
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });

    }



    public void updateRideMode(final int ridemode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRideModeToggleButton.setValue(ridemode -1);
            }
        });

    }


    public void sendAnalyticsStats() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    double max_speed_revs = OWDevice.rpmToMilesPerHour(mOWDevice.maxSpeed.get());
                    String speed_max_mph = Double.toString(max_speed_revs);
                    String lifetime_odometer = Integer.toString(mOWDevice.lifetimeOdometer.get());
                    String max_tilt_angle_pitch = Integer.toString(mOWDevice.maxTiltAnglePitch.get());
                    String max_tilt_angle_roll = Integer.toString(mOWDevice.maxTiltAngleRoll.get());
                    mTracker.send(new HitBuilders.EventBuilder().setCategory("DeviceStats").setAction("speed_max_mph").setLabel(speed_max_mph).build());
                    mTracker.send(new HitBuilders.EventBuilder().setCategory("DeviceStats").setAction("lifetime_odometer_miles").setLabel(lifetime_odometer).build());
                    mTracker.send(new HitBuilders.EventBuilder().setCategory("DeviceStats").setAction("max_tilt_angle_pitch").setLabel(max_tilt_angle_pitch).build());
                    mTracker.send(new HitBuilders.EventBuilder().setCategory("DeviceStats").setAction("max_tilt_angle_roll").setLabel(max_tilt_angle_roll).build());
                } catch (Exception e) {
                }
            }
        });

    }

    //battery level alerts
    public static Map<Integer, Boolean> batteryAlertLevels = new HashMap<>();
    static {
        batteryAlertLevels.put(75,false); //1
        batteryAlertLevels.put(50, false); //2
        batteryAlertLevels.put(25, false); //3
        batteryAlertLevels.put(5, false); // 4
    }
    public void updateBatteryRemaining(final int perc) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //mBatteryPieData.removeDataSet(0);
                    updateLog("Got battery event with " + perc + " remaining!");
                    ArrayList<Entry> entries = new ArrayList<>();
                    entries.add(new Entry(perc, 0));
                    entries.add(new Entry(100 - perc, 1));
                    PieDataSet dataSet = new PieDataSet(entries, "battery percentage");
                    ArrayList<Integer> mColors = new ArrayList<>();
                    mColors.add(ColorTemplate.rgb("#2E7D32")); //green
                    mColors.add(ColorTemplate.rgb("#C62828")); //red
                    dataSet.setColors(mColors);

                    ArrayList<String> labels = new ArrayList<>();
                    labels.add("");
                    labels.add("");


                    PieData newPieData = new PieData(labels, dataSet);
                    mBatteryChart.setCenterText(perc + "%");
                    mBatteryChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
                    mBatteryChart.setCenterTextColor(ColorTemplate.rgb("#616161"));
                    mBatteryChart.setCenterTextSize(20f);

                    mBatteryChart.setData(newPieData);
                    mBatteryChart.notifyDataSetChanged();
                    mBatteryChart.invalidate();


                    if (batteryAlertLevels.containsKey(perc)) {
                        if ((batteryAlertLevels.get(perc)) == false) {
                            switch (perc) {
                                case 75:
                                    EventBus.getDefault().post(new VibrateEvent(1000,1));
                                    onEvent(new NotificationEvent("OW Battery", "75%"));
                                    break;
                                case 50:
                                    EventBus.getDefault().post(new VibrateEvent(1000,2));
                                    onEvent(new NotificationEvent("OW Battery", "50%"));
                                    break;
                                case 25:
                                    EventBus.getDefault().post(new VibrateEvent(1000,3));
                                    onEvent(new NotificationEvent("OW Battery", "25%"));
                                    break;
                                case 5:
                                    EventBus.getDefault().post(new VibrateEvent(1000,4));
                                    onEvent(new NotificationEvent("OW Battery", "5%"));
                                    break;
                                default:
                            }
                            batteryAlertLevels.put(perc,true);
                        }
                    }

                } catch (Exception e) {
                    updateLog("Got an exception updating battery:" + e.getMessage());

                }
            }
        });
    }
    private void deviceConnectedTimer(final boolean start) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (start) {
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                } else {
                    mChronometer.stop();
                }
            }
        });


    }
    private void updateOptionsMenu() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
            }
        });
    }


    private Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting...");
        super.onCreate(savedInstanceState);

        mContext = this;

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.enableAdvertisingIdCollection(true);
        mTracker.setScreenName("MainActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());


        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mSharedPref.registerOnSharedPreferenceChangeListener(this);

        String loggingFrequencyString = mSharedPref.getString("loggingFrequency","1000");
        //TODO check for int
        try {
            mLoggingFrequency = Integer.parseInt(loggingFrequencyString);
        } catch (Exception e) {
            Log.d(TAG,"Use a number for polling frequency already! Going with the default for now.");
        }

        EventBus.getDefault().register(this);
        bindService(new Intent(this, VibrateService.class), mVibrateConnection, Context.BIND_AUTO_CREATE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pOWToolsWakeLock");

        mBinding = DataBindingUtil.setContentView(this, net.kwatts.powtools.R.layout.activity_main);

        if (mSharedPref.getBoolean("dayNightMode", false)) {
            if (savedInstanceState == null) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                recreate();
            }
        }
        if (mSharedPref.getBoolean("darkNightMode", false)) {
            if (savedInstanceState == null) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                recreate();
            }
        }



        mAboutDialog = new MaterialDialog.Builder(this).title("WARNING").content(R.string.eula).positiveText("AGREE")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        SharedPreferences.Editor editor = mSharedPref.edit();
                        editor.putBoolean("eula_agree", true);
                        editor.commit();
                    }
                });

        if ((mSharedPref.getBoolean("eula_agree", false)) == false) {
            mAboutDialog.show();
        }

        startService(new Intent(getApplicationContext(), VibrateService.class));


        mOWDevice = new OWDevice();
        mBinding.setOwdevice(mOWDevice);

        if (mSharedPref.getBoolean("debugWindow", false)) {
            mOWDevice.showDebugWindow.set(true);
        } else {
            mOWDevice.showDebugWindow.set(false);
        }

        mOWDevice.refresh();
        mOWConnected = false;
        mOWDevice.isConnected.set(false);

        //mOWDevice.bluetoothLe.set("Off");
        //mOWDevice.bluetoothStatus.set("Disconnected");


        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mOWDevice.bluetoothLe.set("On");
        mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        mToolbar.setTitle("POWheel");
        mToolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(mToolbar);


        mScrollView = (ScrollView) findViewById(R.id.logScroller);


        if ((mSharedPref.getBoolean("tripLogging", false)) == true) {
            initLogging();
        }


        mChronometer = (Chronometer)findViewById(R.id.chronometer);


        mBatteryChart = (PieChart) findViewById(R.id.batteryPieChart);
        // configure pie chart
        mBatteryChart.setUsePercentValues(true);
        mBatteryChart.setDescription("");
        // enable hole and configure
        mBatteryChart.setDrawHoleEnabled(true);
        Legend legend = mBatteryChart.getLegend();
        legend.setEnabled(false);
       // mBatteryChart.setHoleRadius(7);
       // mBatteryChart.setTransparentCircleRadius(10);


        initLightSettings(getWindow().getDecorView().getRootView());
        initRideModeButtons(getWindow().getDecorView().getRootView());

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
    @Override
    public void onDestroy() {
        if (mVibrateService != null) {
            unbindService(mVibrateConnection);
        }
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (mOWConnected == true) {
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            //menu.findItem(R.id.menu_ow_light_on).setVisible(true);
            //menu.findItem(R.id.menu_ow_ridemode).setVisible(true);
        }
        else if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            //menu.findItem(R.id.menu_ow_light_on).setVisible(false);
            //menu.findItem(R.id.menu_ow_ridemode).setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
            //menu.findItem(R.id.menu_ow_light_on).setVisible(false);
            //menu.findItem(R.id.menu_ow_ridemode).setVisible(false);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                //mLeDeviceListAdapter.clear();
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("Scan").build());
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                filters = new ArrayList<ScanFilter>();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                if (mGatt != null) {
                    mGatt.disconnect();
                    mGatt.close();
                    mGatt = null;
                }
                mOWConnected = false;
                mOWDevice.isConnected.set(false);
                this.mScanResults.clear();
                descriptorWriteQueue.clear();
                this.invalidateOptionsMenu();
                break;
            case R.id.menu_disconnect:
                scanLeDevice(false);
                if (mGatt != null) {
                    mGatt.disconnect();
                    mGatt.close();
                    mGatt = null;
                }
                mOWConnected = false;
                mOWDevice.isConnected.set(false);
                this.mScanResults.clear();
                descriptorWriteQueue.clear();
                updateLog("Disconnected from device by user.");
                deviceConnectedTimer(false);
                sendAnalyticsStats();
                this.invalidateOptionsMenu();
                break;
            case R.id.menu_about:
                mAboutDialog.show();
                break;
            case R.id.menu_settings:
                Intent i = new Intent(this, MainPreferencesActivity.class);
                startActivity(i);
                break;
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mOWDevice.bluetoothStatus.set("Connected");
        }

        this.invalidateOptionsMenu();




    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged callback");
        switch (key) {
            case "metricUnits":
                boolean metricUnitsState = sharedPreferences.getBoolean("metricUnits",false);
                mOWDevice.metricUnits.set(metricUnitsState);
                mOWDevice.refresh();
                mTracker.send(new HitBuilders.EventBuilder().setCategory("SharedPreferences").setAction("metricUnits")
                        .setLabel((metricUnitsState) ? "on" : "off").build());
                break;

            case "darkNightMode":
                boolean checkDarkNightMode = mSharedPref.getBoolean("darkNightMode", false);
                if (checkDarkNightMode) {
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                recreate();
                mTracker.send(new HitBuilders.EventBuilder().setCategory("SharedPreferences").setAction("darkNightMode")
                        .setLabel((checkDarkNightMode) ? "on" : "off").build());
                break;

            default:
                //XXX right now, all preferences are bools, but this could change in the future
              try {
                  boolean checkState = sharedPreferences.getBoolean(key, false);
                  mTracker.send(new HitBuilders.EventBuilder().setCategory("SharedPreferences").setAction(key)
                          .setLabel((checkState) ? "on" : "off").build());
                  break;
              } catch (Exception e) {}

        }

    }

    // Services
    private ServiceConnection mVibrateConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mVibrateService = ((VibrateService.MyBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mVibrateService = null;
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
        this.invalidateOptionsMenu();
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
                connectToDevice(result.getDevice());
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
        device.connectGatt(this, false, mGattCallback);
    }


    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "BluetoothGattCallback.nConnectionStateChange: status=" + status + " newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (gatt.getDevice().getAddress() == mDeviceMacAddress) {
                    updateLog("We got disconnected from our Device: " + gatt.getDevice().getAddress());
                    deviceConnectedTimer(false);
                    mOWConnected = false;
                    mOWDevice.isConnected.set(false);
                    mWakeLock.release();
                    mScanResults.clear();

                    if (mSharedPref.getBoolean("deviceReconnect",false)) {
                        updateLog("Attempting to Reconnect to " + mDeviceMacAddress);
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceMacAddress);
                        connectToDevice(device);
                        //scanLeDevice(true);
                    } else {
                        gatt.close();

                    }
                    updateOptionsMenu();

                }
                updateLog("--> Closed " + gatt.getDevice().getAddress());
                Log.d(TAG, "Disconnect:" + gatt.getDevice().getAddress());
            }
        }



        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){

                BluetoothGattService checkGattService = gatt.getService(UUID.fromString(OWDevice.OnewheelServiceUUID));
                if (checkGattService != null) {
                    owGatService = checkGattService;
                    mGatt = gatt;
                    updateLog("Hey, I found the OneWheel!");
                    deviceConnectedTimer(true);
                    mOWConnected = true;
                    mOWDevice.isConnected.set(true);
                    mWakeLock.acquire();
                    mDeviceMacAddress = mGatt.getDevice().toString();
                    mDeviceMacName    = mGatt.getDevice().getName();
                    mOWDevice.deviceMacAddress.set(mDeviceMacAddress);
                    mOWDevice.deviceMacName.set(mDeviceMacName);
                    SharedPreferences.Editor editor = mSharedPref.edit();
                    editor.putString("ow_mac_address", mDeviceMacAddress);
                    editor.putString("ow_mac_name",mDeviceMacName);
                    editor.commit();
                    scanLeDevice(false); // We can stop scanning...

                    for(OWDevice.DeviceCharacteristic dc: mOWDevice.getNotifyCharacteristics()) {
                        if (dc.uuid.get() != null && (dc.enabled.get() == true)) {
                            BluetoothGattCharacteristic c = owGatService.getCharacteristic(UUID.fromString(dc.uuid.get()));
                            if (c != null) {
                                if (isCharacteristicNotifiable(c)) {
                                    mGatt.setCharacteristicNotification(c, true);
                                    BluetoothGattDescriptor descriptor = c.getDescriptor(UUID.fromString(mOWDevice.OnewheelConfigUUID));
                                    Log.d(TAG, "descriptorWriteQueue.size:" + descriptorWriteQueue.size());
                                    if (descriptor == null) {
                                        Log.e(TAG, dc.uuid.get() + " has a null descriptor!");
                                    } else {
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        descriptorWriteQueue.add(descriptor);
                                        if (descriptorWriteQueue.size() == 1) {
                                            mGatt.writeDescriptor(descriptor);
                                        }
                                        Log.d(TAG, dc.uuid.get()+ " has been set for notifications");
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


                } else {
                    if (gatt.getDevice().getName() == null) {
                        updateLog("--> " + gatt.getDevice().getAddress() + " not OW, moving on.");
                    } else {
                        updateLog("--> " + gatt.getDevice().getName() + " not OW, moving on.");
                    }
                    //mOWConnected = false;
                }



        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
            String characteristic_uuid = c.getUuid().toString();
            Log.i(TAG, "BluetoothGattCallback.onCharacteristicRead: CharacteristicUuid=" + characteristic_uuid + "status=" + status);
            characteristicReadQueue.remove();


            //XXX until we figure out what's going on
            if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicBatteryRemaining)) {
                updateBatteryRemaining(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            } else if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicRidingMode)) {
                updateRideMode(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            }

            mOWDevice.processUUID(c);

            if (BuildConfig.DEBUG) {
                byte[] v_bytes = c.getValue();


                StringBuilder sb = new StringBuilder();
                for (byte b : c.getValue()) {
                    sb.append(String.format("%02x", b));
                }

                Log.d(TAG, "HEX %02x: " + sb);
                Log.d(TAG, "Arrays.toString() value: " + Arrays.toString(v_bytes));
                Log.d(TAG, "String value: " + c.getStringValue(0));
                Log.d(TAG, "Unsigned short: " + unsignedShort(v_bytes));
                Log.d(TAG, "getIntValue(FORMAT_UINT8,0) " + c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                Log.d(TAG, "getIntValue(FORMAT_UINT8,1) " + c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            }
            // Callback to make sure the queue is drained
            if(characteristicReadQueue.size() > 0) {
                gatt.readCharacteristic(characteristicReadQueue.element());
            }


        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic c) {
            //XXX until we figure out what's going on
            if (c.getUuid().toString().equals(OWDevice.OnewheelCharacteristicBatteryRemaining)) {
                updateBatteryRemaining(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            }

            mOWDevice.processUUID(c);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite: " + status);
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

    public void initLogging() {
        if (ONEWHEEL_LOGGING) {
            String dateTimeString = new SimpleDateFormat("yyyy-MM-dd_HH_mm").format(new Date());
            String logPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + ONEWHEEL_LOGGING_PATH;
            PlainTextFileLogger.createDirIfNotExists(logPath);
            File owLogFile = new File(logPath + "/owlogs_" + dateTimeString + ".csv");
            updateLog("Logging device to " + owLogFile.getAbsolutePath());
            Toast.makeText(mContext, "All OW activity will be logging to " + owLogFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            mTextFileLogger = new PlainTextFileLogger(owLogFile);
            deviceFileLogger = new Runnable() {
                @Override
                public void run() {
                    logging_seconds++;
                    if (mOWConnected) {
                        if (mOWDevice != null) {
                            try {
                                mTextFileLogger.write(mOWDevice);
                            } catch (Exception e) {
                                Log.e(TAG,"unable to write logs");
                            }
                        }
                    }
                    mLoggingHandler.postDelayed(this, mLoggingFrequency);
                }
            };
            mLoggingHandler.postDelayed(deviceFileLogger, mLoggingFrequency);

        }
    }




    SwitchCompat mMasterLight;
    SwitchCompat mCustomLight;
    SwitchCompat mFrontBright;
    SwitchCompat mBackBright;
    SwitchCompat mFrontBlink;
    SwitchCompat mBackBlink;

    int frontBlinkCount = 0;
    int backBlinkCount = 0;

    public class mFrontBlinkTaskTimerTask extends TimerTask
    {
        @Override
        public void run() {
            if ((frontBlinkCount % 2) == 0) {
                mOWDevice.setCustomLights(owGatService, mGatt, 0, 0, 60);
            } else {
                mOWDevice.setCustomLights(owGatService, mGatt, 0, 0, 0);
            }
            frontBlinkCount++;
        }
    }
    mFrontBlinkTaskTimerTask mFrontBlinkTimerTask;
    Timer mFrontBlinkTimer;

    public class mBackBlinkTaskTimerTask extends TimerTask
    {
        @Override
        public void run() {
            if ((backBlinkCount % 2) == 0) {
                mOWDevice.setCustomLights(owGatService, mGatt, 1, 1, 60);
            } else {
                mOWDevice.setCustomLights(owGatService, mGatt, 1, 1, 0);
            }
            backBlinkCount++;
        }
    }
    mBackBlinkTaskTimerTask mBackBlinkTimerTask;
    Timer mBackBlinkTimer;




    public void initLightSettings(View v) {
        mMasterLight = (SwitchCompat) v.findViewById(R.id.master_light_switch);
        mCustomLight = (SwitchCompat) v.findViewById(R.id.custom_light_switch);

        mFrontBright = (SwitchCompat) v.findViewById(R.id.front_bright_switch);
        mBackBright = (SwitchCompat) v.findViewById(R.id.back_bright_switch);
        mFrontBlink = (SwitchCompat) v.findViewById(R.id.front_blink_switch);
        mBackBlink = (SwitchCompat) v.findViewById(R.id.back_blink_switch);

        mMasterLight.setOnCheckedChangeListener(new android.support.v7.widget.SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        updateLog("LIGHTS ON");
                        mOWDevice.setLights(owGatService, mGatt, 1);
                    } else {
                        updateLog("LIGHTS OFF");
                        mOWDevice.setLights(owGatService, mGatt, 0);
                    }
                }
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("Lights").setLabel((isChecked) ? "on" : "off").build());
            }
        });

        mCustomLight.setOnCheckedChangeListener(new android.support.v7.widget.SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        updateLog("CUSTOM LIGHTS ON");
                        mOWDevice.setLights(owGatService, mGatt, 2);
                    } else {
                        updateLog("CUSTOM LIGHTS OFF");
                        mOWDevice.setLights(owGatService, mGatt, 0);

                    }
                }
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("CustomLights").setLabel((isChecked) ? "on" : "off").build());
            }
        });


        mFrontBright.setOnCheckedChangeListener(new android.support.v7.widget.SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        mOWDevice.setCustomLights(owGatService, mGatt, 0,0,60);
                     } else {
                        mOWDevice.setCustomLights(owGatService, mGatt, 0,0,30);
                     }
                }
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("CustomLightsFrontBright").setLabel((isChecked) ? "on" : "off").build());

            }
        });

        mBackBright.setOnCheckedChangeListener(new android.support.v7.widget.SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        mOWDevice.setCustomLights(owGatService, mGatt, 1,1,60);
                    } else {
                        mOWDevice.setCustomLights(owGatService, mGatt, 1,1,30);

                    }
                }
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("CustomLightsBackBright").setLabel((isChecked) ? "on" : "off").build());

            }
        });


        mFrontBlink.setOnCheckedChangeListener(new android.support.v7.widget.SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        mFrontBlinkTimerTask = new mFrontBlinkTaskTimerTask();
                        mFrontBlinkTimer = new Timer();
                        mFrontBlinkTimer.scheduleAtFixedRate(mFrontBlinkTimerTask, 0, 500);

                    } else {
                        if (mFrontBlinkTimer != null) {
                            mFrontBlinkTimer.cancel();
                            mFrontBlinkTimer.purge();
                            mFrontBlinkTimer = null;
                        }
                        if (mFrontBlinkTimerTask != null) {
                            mFrontBlinkTimerTask.cancel();
                            mFrontBlinkTimerTask = null;
                        }
                    }

                }
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("CustomLightsFrontBlink").setLabel((isChecked) ? "on" : "off").build());

            }
        });

        mBackBlink.setOnCheckedChangeListener(new android.support.v7.widget.SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        mBackBlinkTimerTask = new mBackBlinkTaskTimerTask();
                        mBackBlinkTimer = new Timer();
                        mBackBlinkTimer.scheduleAtFixedRate(mBackBlinkTimerTask, 0, 500);

                    } else {
                        if (mBackBlinkTimer != null) {
                            mBackBlinkTimer.cancel();
                            mBackBlinkTimer.purge();
                            mBackBlinkTimer = null;
                        }
                        if (mBackBlinkTimerTask != null) {
                            mBackBlinkTimerTask.cancel();
                            mBackBlinkTimerTask = null;
                        }
                    }

                }
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("CustomLightsBackBlink").setLabel((isChecked) ? "on" : "off").build());
            }
        });

    }


    MultiStateToggleButton mRideModeToggleButton;
    MultiStateToggleButton mRideModeToggleButtonOWplus;
    public void initRideModeButtons(View v) {


        mRideModeToggleButton = (MultiStateToggleButton) this.findViewById(R.id.mstb_multi_ridemodes);
        mRideModeToggleButtonOWplus = (MultiStateToggleButton) this.findViewById(R.id.mstb_multi_ridemodes_owplus);


        mRideModeToggleButton.setOnValueChangedListener(new MultiStateToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int position) {
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("SetRideMode").setLabel(Integer.toString(position + 1)).build());

                if (mOWConnected) {
                    mOWDevice.setRideMode(owGatService, mGatt, position + 1);
                } else {
                    Toast.makeText(mContext, "Not connected to Device!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mRideModeToggleButtonOWplus.setOnValueChangedListener(new MultiStateToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int position) {
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("SetRideMode").setLabel(Integer.toString(position + 4)).build());
                if (mOWConnected) {
                    mOWDevice.setRideMode(owGatService, mGatt, position + 4);
                } else {
                    Toast.makeText(mContext, "Not connected to Device!", Toast.LENGTH_SHORT).show();
                }
            }
        });



    }

    /* Took this out, t'was ahead of its time ;) */
    /*
    Switch mMasterLightSwitch;
    Switch mCustomLightSwitch;
    Slider mFrontLightWhiteSlider;
    Slider mFrontLightRedSlider;
    Slider mBackLightRedSlider;
    Slider mBackLightWhiteSlider;

    public void initLightDialog(View v) {

        mMasterLightSwitch = (Switch) v.findViewById(R.id.master_light_switch);
        mMasterLightSwitch.setOnCheckedChangeListener(new com.rey.material.widget.Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(com.rey.material.widget.Switch buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        mCustomLightSwitch.setVisibility(View.VISIBLE);
                        mOWDevice.setLights(owGatService, mGatt, 1);
                    } else {
                        mCustomLightSwitch.setVisibility(View.INVISIBLE);
                        mFrontLightWhiteSlider.setVisibility(View.INVISIBLE);
                        mFrontLightRedSlider.setVisibility(View.INVISIBLE);
                        mBackLightRedSlider.setVisibility(View.INVISIBLE);
                        mBackLightWhiteSlider.setVisibility(View.INVISIBLE);
                        mCustomLightSwitch.setChecked(false);
                        mOWDevice.setLights(owGatService, mGatt, 0);
                    }
                }

            }

        });



        mCustomLightSwitch = (Switch) v.findViewById(R.id.custom_light_switch);
        final Handler handler = new Handler();

        mCustomLightSwitch.setOnCheckedChangeListener(new com.rey.material.widget.Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(com.rey.material.widget.Switch buttonView,boolean isChecked) {
                if (mOWConnected) {
                    if (isChecked == true) {
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                mCount1++;
                                if ( ( mCount1 % 2 ) == 0 ) {
                                    mOWDevice.setCustomLights(owGatService, mGatt,0,0,50);
                                } else {
                                    mOWDevice.setCustomLights(owGatService, mGatt,0,0,0);
                                }

                                handler.postDelayed(this, 1000);
                            }
                        };
                        handler.post(task);


                    }

                }

                if (isChecked == false)  {
                    handler.removeCallbacksAndMessages(null);
                }

            }

        });


        mFrontLightWhiteSlider  = (Slider) v.findViewById(R.id.white_front_lights_slider);
        mFrontLightWhiteSlider.setOnPositionChangeListener(new Slider.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(Slider view, boolean fromUser, float oldPos, float newPos, int oldValue, int newValue) {
                //Toast.makeText(mContext, String.format("pos=%.1f value=%d", newPos, newValue), Toast.LENGTH_SHORT).show();
                if (mOWConnected) {
                    mOWDevice.setCustomLights(owGatService, mGatt, 0,0,newValue);
                }
            }
        });

        mFrontLightRedSlider = (Slider) v.findViewById(R.id.red_front_lights_slider);
        mFrontLightRedSlider.setOnPositionChangeListener(new Slider.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(Slider view, boolean fromUser, float oldPos, float newPos, int oldValue, int newValue) {
                if (mOWConnected) {
                    mOWDevice.setCustomLights(owGatService, mGatt, 0,1,newValue);
                }
            }
        });

        mBackLightWhiteSlider = (Slider) v.findViewById(R.id.white_back_lights_slider);
        mBackLightWhiteSlider.setOnPositionChangeListener(new Slider.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(Slider view, boolean fromUser, float oldPos, float newPos, int oldValue, int newValue) {
                if (mOWConnected) {
                    mOWDevice.setCustomLights(owGatService, mGatt, 1,0,newValue);
                }
            }
        });

        mBackLightRedSlider = (Slider) v.findViewById(R.id.red_back_lights_slider);
        mBackLightRedSlider.setOnPositionChangeListener(new Slider.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(Slider view, boolean fromUser, float oldPos, float newPos, int oldValue, int newValue) {
                if (mOWConnected) {
                    mOWDevice.setCustomLights(owGatService, mGatt, 1,1,newValue);
                }
            }
        });

    }

*/
}
