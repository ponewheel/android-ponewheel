package net.kwatts.powtools;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ScrollView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.location.LocationRequest;
import com.patloew.rxlocation.RxLocation;
import com.tbruyelle.rxpermissions2.RxPermissions;

import net.kwatts.powtools.events.NotificationEvent;
import net.kwatts.powtools.events.VibrateEvent;
import net.kwatts.powtools.loggers.PlainTextFileLogger;
import net.kwatts.powtools.services.VibrateService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.honorato.multistatetogglebutton.MultiStateToggleButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

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

    private static final boolean ONEWHEEL_LOGGING = true;
    private static final int REQUEST_ENABLE_BT = 1;

    MultiStateToggleButton mRideModeToggleButton;

    public VibrateService mVibrateService;
    private android.os.Handler mLoggingHandler = new android.os.Handler();
    private PlainTextFileLogger mTextFileLogger;
    private android.bluetooth.BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mGatt;
    private BluetoothGattService owGatService;
    private ScanSettings settings;
    private Context mContext;
    private boolean mScanning;
    ScrollView mScrollView;
    Chronometer mChronometer;
    WakeLock mWakeLock;
    private OWDevice mOWDevice;
    net.kwatts.powtools.databinding.ActivityMainBinding mBinding;

    Map<String, String> mScanResults = new HashMap<>();

    PieChart mBatteryChart;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NotificationEvent event){
        Log.d(TAG, event.message + ":" + event.title);
        final String t = event.title;
        final String m = event.message;
        runOnUiThread(() -> {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mContext, "ponewheel")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(t)
                            .setColor(0x008000)
                            .setContentText(m);
            android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert mNotifyMgr != null;
            mNotifyMgr.notify(m,0, mBuilder.build());
        });
    }

    private void updateLog(final String msg) {
        Log.d(TAG, msg);
        runOnUiThread(() -> {
            //mBinding.owLog.setMovementMethod(new ScrollingMovementMethod());
            mBinding.owLog.append("\n" + msg);
            mScrollView.fullScroll(View.FOCUS_DOWN);
        });

    }

    //battery level alerts
    public static SparseBooleanArray batteryAlertLevels = new SparseBooleanArray(){{
        put(75,false); //1
        put(50, false); //2
        put(25, false); //3
        put(5, false); // 4
    }};

    public void updateBatteryRemaining(final int percent) {
        runOnUiThread(() -> {
            try {
                //mBatteryPieData.removeDataSet(0);
                //updateLog("Got battery event with " + percent + " remaining!");
                ArrayList<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(percent, 0));
                entries.add(new PieEntry(100 - percent, 1));
                PieDataSet dataSet = new PieDataSet(entries, "battery percentage");
                ArrayList<Integer> mColors = new ArrayList<>();
                mColors.add(ColorTemplate.rgb("#2E7D32")); //green
                mColors.add(ColorTemplate.rgb("#C62828")); //red
                dataSet.setColors(mColors);

                PieData newPieData = new PieData( dataSet);
                mBatteryChart.setCenterText(percent + "%");
                mBatteryChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
                mBatteryChart.setCenterTextColor(ColorTemplate.rgb("#616161"));
                mBatteryChart.setCenterTextSize(20f);
                mBatteryChart.setDescription(null);

                mBatteryChart.setData(newPieData);
                mBatteryChart.notifyDataSetChanged();
                mBatteryChart.invalidate();


                if (batteryAlertLevels.indexOfKey(percent) > -1) {
                    if (!(batteryAlertLevels.get(percent))) {
                        switch (percent) {
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
                        batteryAlertLevels.put(percent,true);
                    }
                }

            } catch (Exception e) {
                updateLog("Got an exception updating battery:" + e.getMessage());

            }
        });
    }
    private void deviceConnectedTimer(final boolean start) {
        runOnUiThread(() -> {
            if (start) {
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
            } else {
                mChronometer.stop();
            }
        });


    }
    private void updateOptionsMenu() {
        runOnUiThread(this::invalidateOptionsMenu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting...");
        super.onCreate(savedInstanceState);

        mContext = this;


        EventBus.getDefault().register(this);
        // TODO unbind in onPause or whatever is recommended by goog
        bindService(new Intent(this, VibrateService.class), mVibrateConnection, Context.BIND_AUTO_CREATE);

        initWakelock();

        mBinding = DataBindingUtil.setContentView(this, net.kwatts.powtools.R.layout.activity_main);

        setupDarkModes(savedInstanceState);

        App.INSTANCE.getSharedPreferences().registerListener(this);

        if (!App.INSTANCE.getSharedPreferences().isEulaAgreed()) {
            showEula();
        }

        startService(new Intent(getApplicationContext(), VibrateService.class));

        setupOWDevice();

        setupToolbar();

        mScrollView = findViewById(R.id.logScroller);

        if (App.INSTANCE.getSharedPreferences().isLoggingEnabled()) {
            initLogging();
        }

        mChronometer = findViewById(R.id.chronometer);
        initBatteryChart();
        initLightSettings(getWindow().getDecorView().getRootView());
        initRideModeButtons(getWindow().getDecorView().getRootView());

    }

    private void initWakelock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        assert powerManager != null;
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pOWToolsWakeLock");
    }

    private void initBatteryChart() {
        mBatteryChart = findViewById(R.id.batteryPieChart);
        // configure pie chart
        mBatteryChart.setUsePercentValues(true);
        mBatteryChart.setDescription(new Description());
        // enable hole and configure
        mBatteryChart.setDrawHoleEnabled(true);
        Legend legend = mBatteryChart.getLegend();
        legend.setEnabled(false);
    }

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.tool_bar);
        mToolbar.setTitle("POWheel");
        mToolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(mToolbar);
    }

    private void setupOWDevice() {
        mOWDevice = new OWDevice();
        mBinding.setOwdevice(mOWDevice);

        mOWDevice.showDebugWindow.set(App.INSTANCE.getSharedPreferences().isDebugging());
        mOWDevice.isOneWheelPlus.set(App.INSTANCE.getSharedPreferences().isOneWheelPlus());

//        mOWDevice.isConnected.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
//            @Override
//            public void onPropertyChanged(Observable observable, int i) {
//                Log.d(TAG, "onPropertyChanged: " + mOWDevice.isConnected.get());
//                Log.d(TAG, "onPropertyChanged: " + observable.toString() + "i" + i);
//            }
//        });

        mOWDevice.refresh();
        mOWDevice.isConnected.set(false);

        //mOWDevice.bluetoothLe.set("Off");
        //mOWDevice.bluetoothStatus.set("Disconnected");


        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert manager != null;
        mBluetoothAdapter = manager.getAdapter();
        mOWDevice.bluetoothLe.set("On");
    }

    private void showEula() {
        new MaterialDialog.Builder(this)
                .title("WARNING")
                .content(R.string.eula)
                .positiveText("AGREE")
                .onPositive((dialog, which) ->
                        App.INSTANCE.getSharedPreferences().setEulaAgreed(true))
                .show();
    }

    private void setupDarkModes(Bundle savedInstanceState) {
        if (App.INSTANCE.getSharedPreferences().isDayNightMode()) {
            if (savedInstanceState == null) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                recreate();
            }
        }
        if (App.INSTANCE.getSharedPreferences().isDarkNightMode()) {
            if (savedInstanceState == null) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                recreate();
            }
        }
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
        App.INSTANCE.getSharedPreferences().removeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (mOWDevice.isConnected.get()) {
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            //menu.findItem(R.id.menu_ow_light_on).setVisible(true);
            //menu.findItem(R.id.menu_ow_ridemode).setVisible(true);
        } else if (!mScanning) {
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
            case R.id.menu_logs:
                startActivity(new Intent(MainActivity.this, RidesListActivity.class));
                break;
            case R.id.menu_scan:
                //mLeDeviceListAdapter.clear();
//                mTracker.send(new HitBuilders.EventBuilder().setCategory("Actions").setAction("Scan").build());

                getPermissions().subscribe(new DisposableSingleObserver<Boolean>() {
                           @Override
                           public void onSuccess(Boolean aBoolean) {
                               mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                               settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                               scanLeDevice(true);

                               // TODO move this to where we're actually connected to device? (or maybe its better here so we can achieve a location lock before logging)
                               if (App.INSTANCE.getSharedPreferences().isLoggingEnabled()) {
                                   startLocationScan();
                               }
                           }

                           @Override
                           public void onError(Throwable e) {
                                e.printStackTrace();
                           }
                });

                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                if (mGatt != null) {
                    mGatt.disconnect();
                    mGatt.close();
                    mGatt = null;
                }
                mOWDevice.isConnected.set(false);
                this.mScanResults.clear();
                descriptorWriteQueue.clear();
                this.invalidateOptionsMenu();

                // Added stuff 10/23 to clean fix
                owGatService = null;



                break;
            case R.id.menu_disconnect:
                scanLeDevice(false);
                if (mGatt != null) {
                    mGatt.disconnect();
                    mGatt.close();
                    mGatt = null;
                }
                mOWDevice.isConnected.set(false);
                this.mScanResults.clear();
                descriptorWriteQueue.clear();
                updateLog("Disconnected from device by user.");
                deviceConnectedTimer(false);
                this.invalidateOptionsMenu();
                // Added stuff 10/23 to clean fix
                owGatService = null;
                break;
            case R.id.menu_about:
                showEula();
                break;
            case R.id.menu_settings:
                Intent i = new Intent(this, MainPreferencesActivity.class);
                startActivity(i);
                break;
        }

        return true;
    }

    private void startLocationScan() {

        RxLocation rxLocation = new RxLocation(this);

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(TimeUnit.SECONDS.toMillis(5));

        rxLocation
                .location()
                .updates(locationRequest)
                .subscribeOn(Schedulers.io())
                .flatMap(location -> rxLocation.geocoding().fromLocation(location).toObservable())
                .observeOn(Schedulers.io())
                .subscribe(address -> mOWDevice.setLocation(address.getLongitude() + "," + address.getLatitude()));
    }

    private Single<Boolean> getPermissions() {
        // TODO I think this is necessary since changing the target api
        RxPermissions rxPermissions = new RxPermissions(this);
        return rxPermissions
                .request(
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .firstOrError();
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
            case SharedPreferencesUtil.METRIC_UNITS:
                boolean metricUnitsState = sharedPreferences.getBoolean(key,false);
                mOWDevice.metricUnits.set(metricUnitsState);
                mOWDevice.refresh();
//                mTracker.send(new HitBuilders.EventBuilder().setCategory("SharedPreferences").setAction("metricUnits")
//                        .setLabel((metricUnitsState) ? "on" : "off").build());
                break;

            case SharedPreferencesUtil.DARK_NIGHT_MODE:
                boolean checkDarkNightMode = sharedPreferences.getBoolean(key, false);
                getDelegate().setLocalNightMode(
                        checkDarkNightMode ?
                                AppCompatDelegate.MODE_NIGHT_YES
                                : AppCompatDelegate.MODE_NIGHT_NO);

                recreate();
//                mTracker.send(new HitBuilders.EventBuilder().setCategory("SharedPreferences").setAction("darkNightMode")
//                        .setLabel((checkDarkNightMode) ? "on" : "off").build());
                break;

            case SharedPreferencesUtil.LOG_LOCATIONS:
                boolean checkLogLocations = sharedPreferences.getBoolean(key, false);
                if (!checkLogLocations && mOWDevice != null) {
                    mOWDevice.setLocation(null);
                }
                break;

            default:
                Log.d(TAG, "onSharedPreferenceChanged: " + key);
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
        device.connectGatt(this, false, mGattCallback);
    }


    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<>();
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();


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
            deviceConnectedTimer(true);
            mOWDevice.isConnected.set(true);
            mWakeLock.acquire();
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
                            BluetoothGattDescriptor descriptor = localCharacteristic.getDescriptor(UUID.fromString(mOWDevice.OnewheelConfigUUID));
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
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
            String characteristic_uuid = c.getUuid().toString();
            Log.i(TAG, "BluetoothGattCallback.onCharacteristicRead: CharacteristicUuid=" + characteristic_uuid + "status=" + status);
            characteristicReadQueue.remove();


            //XXX until we figure out what's going on
            if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicBatteryRemaining)) {
                updateBatteryRemaining(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            } /* else if (characteristic_uuid.equals(OWDevice.OnewheelCharacteristicRidingMode)) {
                Log.d(TAG, "Got ride mode from the main UI thread:" + c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
            } */

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

    private void onOWStateChangedToDisconnected(BluetoothGatt gatt) {
        updateLog("We got disconnected from our Device: " + gatt.getDevice().getAddress());
        deviceConnectedTimer(false);
        mOWDevice.isConnected.set(false);
        mWakeLock.release();
        mScanResults.clear();

        if (App.INSTANCE.getSharedPreferences().shouldAutoReconnect()) {
            updateLog("Attempting to Reconnect to " + mOWDevice.deviceMacAddress.get());
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mOWDevice.deviceMacAddress.get());
            connectToDevice(device);
            //scanLeDevice(true);
        } else {
            gatt.close();

        }
        updateOptionsMenu();
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

    public void initLogging() {
        if (ONEWHEEL_LOGGING) {
            String dateTimeString = new SimpleDateFormat("yyyy-MM-dd_HH_mm", Locale.US).format(new Date());

            File owLogFile = new File( PlainTextFileLogger.getLoggingPath() + "/owlogs_" + dateTimeString + ".csv");
            updateLog("Logging device to " + owLogFile.getAbsolutePath());
            Toast.makeText(
                    mContext,
                    String.format(
                            Locale.getDefault(),
                            "All OW activity will be logged to %s every %d ms.",
                            owLogFile.getAbsolutePath(),
                            App.INSTANCE.getSharedPreferences().getLoggingFrequency()
                    ),
                    Toast.LENGTH_LONG)
                .show();
            mTextFileLogger = new PlainTextFileLogger(owLogFile);
            Runnable deviceFileLogger = new Runnable() {
                @Override
                public void run() {
                    int mLoggingFrequency = App.INSTANCE.getSharedPreferences().getLoggingFrequency();
                    mLoggingHandler.postDelayed(this, mLoggingFrequency);
                    if (mOWDevice.isConnected.get()) {
                        try {
                            mTextFileLogger.write(mOWDevice);
                        } catch (Exception e) {
                            Log.e(TAG, "unable to write logs");
                        }
                    }
                }
            };
            mLoggingHandler.postDelayed(deviceFileLogger, App.INSTANCE.getSharedPreferences().getLoggingFrequency());

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
        mMasterLight = v.findViewById(R.id.master_light_switch);
        mCustomLight = v.findViewById(R.id.custom_light_switch);

        mFrontBright = v.findViewById(R.id.front_bright_switch);
        mBackBright = v.findViewById(R.id.back_bright_switch);
        mFrontBlink = v.findViewById(R.id.front_blink_switch);
        mBackBlink = v.findViewById(R.id.back_blink_switch);

        mMasterLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    updateLog("LIGHTS ON");
                    mOWDevice.setLights(owGatService, mGatt, 1);
                } else {
                    updateLog("LIGHTS OFF");
                    mOWDevice.setLights(owGatService, mGatt, 0);
                }
            }
        });

        mCustomLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    updateLog("CUSTOM LIGHTS ON");
                    mOWDevice.setLights(owGatService, mGatt, 2);
                } else {
                    updateLog("CUSTOM LIGHTS OFF");
                    mOWDevice.setLights(owGatService, mGatt, 0);

                }
            }
        });


        mFrontBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    mOWDevice.setCustomLights(owGatService, mGatt, 0,0,60);
                 } else {
                    mOWDevice.setCustomLights(owGatService, mGatt, 0,0,30);
                 }
            }

        });

        mBackBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    mOWDevice.setCustomLights(owGatService, mGatt, 1,1,60);
                } else {
                    mOWDevice.setCustomLights(owGatService, mGatt, 1,1,30);

                }
            }

        });


        mFrontBlink.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
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

        });

        mBackBlink.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
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
        });

    }



    public void initRideModeButtons(View v) {
        mRideModeToggleButton = this.findViewById(R.id.mstb_multi_ridemodes);
        if (mOWDevice.isOneWheelPlus.get()) {
            mRideModeToggleButton.setElements(getResources().getStringArray(R.array.owplus_ridemode_array));
        } else {
            mRideModeToggleButton.setElements(getResources().getStringArray(R.array.ow_ridemode_array));
        }

        mRideModeToggleButton.setOnValueChangedListener(position -> {
            if (mOWDevice.isConnected.get()) {
                Log.d(TAG, "mOWDevice.setRideMode button pressed:" + position);
                if (mOWDevice.isOneWheelPlus.get()) {
                    updateLog("Ridemode changed to:" + position + 4);
                    mOWDevice.setRideMode(owGatService, mGatt,position + 4); // ow+ ble value for ridemode 4,5,6,7,8 (delirium)
                } else {
                    updateLog("Ridemode changed to:" + position + 1);
                    mOWDevice.setRideMode(owGatService, mGatt,position + 1); // ow uses 1,2,3 (expert)
                }
            } else {
                Toast.makeText(mContext, "Not connected to Device!", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
