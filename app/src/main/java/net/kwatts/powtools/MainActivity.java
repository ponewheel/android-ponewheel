package net.kwatts.powtools;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.graphics.Typeface;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
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

import net.kwatts.powtools.database.Attribute;
import net.kwatts.powtools.database.Moment;
import net.kwatts.powtools.database.Ride;
import net.kwatts.powtools.events.NotificationEvent;
import net.kwatts.powtools.events.VibrateEvent;
import net.kwatts.powtools.loggers.PlainTextFileLogger;
import net.kwatts.powtools.model.OWDevice;
import net.kwatts.powtools.services.VibrateService;
import net.kwatts.powtools.util.BluetoothUtil;
import net.kwatts.powtools.util.BluetoothUtilMockImpl;
import net.kwatts.powtools.util.SharedPreferencesUtil;
import net.kwatts.powtools.view.AlertsMvpController;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.honorato.multistatetogglebutton.MultiStateToggleButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import static net.kwatts.powtools.model.OWDevice.MockOnewheelCharacteristicSpeed;

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

    MultiStateToggleButton mRideModeToggleButton;

    public VibrateService mVibrateService;
    private android.os.Handler mLoggingHandler = new Handler();
    private PlainTextFileLogger mTextFileLogger;

    private Context mContext;
    ScrollView mScrollView;
    Chronometer mChronometer;
    private OWDevice mOWDevice;
    net.kwatts.powtools.databinding.ActivityMainBinding mBinding;
    BluetoothUtil bluetoothUtil;


    PieChart mBatteryChart;
    Ride ride;
    private DisposableObserver<Address> rxLocationObserver;
    Date latestMoment;
    private AlertsMvpController alertsController;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NotificationEvent event){
        Log.d(TAG, event.message + ":" + event.title);
        final String title = event.title;
        final String message = event.message;
        runOnUiThread(() -> {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mContext, "ponewheel")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title)
                            .setColor(0x008000)
                            .setContentText(message);
            android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert mNotifyMgr != null;
            mNotifyMgr.notify(message,0, mBuilder.build());
        });
    }

    public void updateLog(final String msg) {
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

        alertsController.handleChargePercentage(percent);
    }
    public void deviceConnectedTimer(final boolean start) {
        runOnUiThread(() -> {
            if (start) {
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
            } else {
                mChronometer.stop();
            }
        });


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting...");
        super.onCreate(savedInstanceState);

        mContext = this;
        bluetoothUtil = new BluetoothUtilMockImpl();
//        bluetoothUtil = new BluetoothUtilImpl();


        EventBus.getDefault().register(this);
        // TODO unbind in onPause or whatever is recommended by goog
        bindService(new Intent(this, VibrateService.class), mVibrateConnection, Context.BIND_AUTO_CREATE);

        initWakelock();

        // this line won't compile? File -> Invalidate caches https://stackoverflow.com/a/42824662/247325
        mBinding = DataBindingUtil.setContentView(this, net.kwatts.powtools.R.layout.activity_main);

        alertsController = new AlertsMvpController(this);

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
        initLightSettings();
        initRideModeButtons();
    }

    private void initWakelock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        mOWDevice.setupCharacteristics();
        mOWDevice.isConnected.set(false);

        //mOWDevice.bluetoothLe.set("Off");
        //mOWDevice.bluetoothStatus.set("Disconnected");

        bluetoothUtil.init(this, mOWDevice);

        mOWDevice.isConnected.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (
                        ride == null ||
                        latestMoment == null ||
                        TimeUnit.MINUTES.toMillis(1) > getMillisSinceLastMoment()) {

                    ride = new Ride();
                    App.dbExecute(database -> ride.id = database.rideDao().insert(ride));
                }
            }
        });

        mOWDevice.characteristics.get(MockOnewheelCharacteristicSpeed).value.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                alertsController.handleSpeed(mOWDevice.characteristics.get(MockOnewheelCharacteristicSpeed).value.get());
            }
        });
    }

    long getMillisSinceLastMoment() {
        return new Date().getTime() - latestMoment.getTime();
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
        } else if (!bluetoothUtil.isScanning()) {
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
                               bluetoothUtil.startScanning();
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
                bluetoothUtil.stopScanning();
                this.invalidateOptionsMenu();

                break;
            case R.id.menu_disconnect:
                mOWDevice.isConnected.set(false);
                bluetoothUtil.disconnect();
                updateLog("Disconnected from device by user.");
                deviceConnectedTimer(false);
                this.invalidateOptionsMenu();
                break;
            case R.id.menu_about:
                showEula();
                break;
            case R.id.menu_settings:
                Intent i = new Intent(this, MainPreferencesActivity.class);
                startActivity(i);
                break;
            case R.id.menu_refresh:
                createNewRide();
                break;
        }

        return true;
    }

    private void createNewRide() {
        App.dbExecute((database) -> database.rideDao().insert(new Ride()));
    }

    private void startLocationScan() {

        RxLocation rxLocation = new RxLocation(this);

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(TimeUnit.SECONDS.toMillis(5));

        rxLocationObserver = rxLocation.location()
                .updates(locationRequest)
                .subscribeOn(Schedulers.io())
                .flatMap(location -> rxLocation.geocoding().fromLocation(location).toObservable())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableObserver<Address>() {
                    @Override public void onNext(Address address) {

                        boolean isLocationsEnabled = App.INSTANCE.getSharedPreferences().isLocationsEnabled();
                        if (isLocationsEnabled) {
                            mOWDevice.setGpsLocation(address);
                        } else if (rxLocationObserver != null) {
                            rxLocationObserver.dispose();
                        }
                    }

                    @Override public void onError(Throwable e) {
                        Log.e(TAG, "onError: error retreiving location", e);
                    }

                    @Override public void onComplete() {
                        Log.d(TAG, "onComplete: ");
                    }
                });
    }

    private Single<Boolean> getPermissions() {
        // TODO I think this is necessary since changing the target api
        RxPermissions rxPermissions = new RxPermissions(this);
        return rxPermissions
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .firstOrError();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (bluetoothUtil.isConnected()) {
            mOWDevice.bluetoothStatus.set("Connected");
        } else {
            bluetoothUtil.reconnect(this);
        }

        alertsController.recaptureMedia(this);

        this.invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // TODO if we want background support, we don't want to do this here
        if (rxLocationObserver != null) {
            rxLocationObserver.dispose();
        }

        // TODO this is not ideal but is recommended because it saves battery
        alertsController.releaseMedia();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged callback");
        switch (key) {
            case SharedPreferencesUtil.METRIC_UNITS:
                mOWDevice.refreshCharacteristics();
                refreshMetricViews();
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
                    mOWDevice.setGpsLocation(null);
                }
                break;

            default:
                Log.d(TAG, "onSharedPreferenceChanged: " + key);
        }

    }

    private void refreshMetricViews() {
        // TODO Auto convert the speed alert for the user or meh?
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
                            persistMoment();

                        } catch (Exception e) {
                            Log.e(TAG, "unable to write logs", e);
                        }
                    }
                }
            };
            mLoggingHandler.postDelayed(deviceFileLogger, App.INSTANCE.getSharedPreferences().getLoggingFrequency());

        }
    }

    private void persistMoment() throws Exception {
//        mTextFileLogger.write(mOWDevice);
        latestMoment = new Date();
        Moment moment = new Moment(ride.id, latestMoment);
        moment.rideId = ride.id;

        App.dbExecute(database -> {
            long momentId = database.momentDao().insert(moment);
            for (OWDevice.DeviceCharacteristic deviceReadCharacteristic : mOWDevice.getNotifyCharacteristics()) {
                Attribute attribute = new Attribute();
                attribute.setMomentId(momentId);
                attribute.setValue(deviceReadCharacteristic.value.get());
//                attribute.setUuid(deviceReadCharacteristic.uuid.get());
//                attribute.setUiName(deviceReadCharacteristic.uuid.get());
                attribute.setKey(deviceReadCharacteristic.key.get());

                database.attributeDao().insert(attribute);
            }
        });



        if (mOWDevice.getGpsLocation() != null) {
            moment.setGpsLat(mOWDevice.getGpsLocation().getLatitude());
            moment.setGpsLong(mOWDevice.getGpsLocation().getLongitude());
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
                mOWDevice.setCustomLights(bluetoothUtil, 0, 0, 60);
            } else {
                mOWDevice.setCustomLights(bluetoothUtil, 0, 0, 0);
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
                mOWDevice.setCustomLights(bluetoothUtil, 1, 1, 60);
            } else {
                mOWDevice.setCustomLights(bluetoothUtil, 1, 1, 0);
            }
            backBlinkCount++;
        }
    }
    mBackBlinkTaskTimerTask mBackBlinkTimerTask;
    Timer mBackBlinkTimer;




    public void initLightSettings() {
        mMasterLight = this.findViewById(R.id.master_light_switch);
        mCustomLight = this.findViewById(R.id.custom_light_switch);

        mFrontBright = this.findViewById(R.id.front_bright_switch);
        mBackBright = this.findViewById(R.id.back_bright_switch);
        mFrontBlink = this.findViewById(R.id.front_blink_switch);
        mBackBlink = this.findViewById(R.id.back_blink_switch);

        mMasterLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    updateLog("LIGHTS ON");
                    mOWDevice.setLights(bluetoothUtil, 1);
                } else {
                    updateLog("LIGHTS OFF");
                    mOWDevice.setLights(bluetoothUtil, 0);
                }
            }
        });

        mCustomLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    updateLog("CUSTOM LIGHTS ON");
                    mOWDevice.setLights(bluetoothUtil, 2);
                } else {
                    updateLog("CUSTOM LIGHTS OFF");
                    mOWDevice.setLights(bluetoothUtil, 0);

                }
            }
        });


        mFrontBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    mOWDevice.setCustomLights(bluetoothUtil, 0,0,60);
                 } else {
                    mOWDevice.setCustomLights(bluetoothUtil, 0,0,30);
                 }
            }

        });

        mBackBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    mOWDevice.setCustomLights(bluetoothUtil, 1,1,60);
                } else {
                    mOWDevice.setCustomLights(bluetoothUtil, 1,1,30);

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



    public void initRideModeButtons() {
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
                    mOWDevice.setRideMode(bluetoothUtil,position + 4); // ow+ ble value for ridemode 4,5,6,7,8 (delirium)
                } else {
                    updateLog("Ridemode changed to:" + position + 1);
                    mOWDevice.setRideMode(bluetoothUtil,position + 1); // ow uses 1,2,3 (expert)
                }
            } else {
                Toast.makeText(mContext, "Not connected to Device!", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
