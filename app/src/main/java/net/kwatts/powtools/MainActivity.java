package net.kwatts.powtools;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.graphics.Typeface;
import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ScrollView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.github.anastr.speedviewlib.ProgressiveGauge;
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
import io.palaima.debugdrawer.DebugDrawer;
import io.palaima.debugdrawer.commons.SettingsModule;
import io.palaima.debugdrawer.timber.TimberModule;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import net.kwatts.powtools.database.entities.Attribute;
import net.kwatts.powtools.database.entities.Moment;
import net.kwatts.powtools.database.entities.Ride;
import net.kwatts.powtools.events.NotificationEvent;
import net.kwatts.powtools.events.VibrateEvent;
import net.kwatts.powtools.model.OWDevice;
import net.kwatts.powtools.services.VibrateService;
import net.kwatts.powtools.util.BluetoothUtil;
import net.kwatts.powtools.util.MainActivityDelegate;
import net.kwatts.powtools.util.SharedPreferencesUtil;
import net.kwatts.powtools.util.SpeedAlertResolver;
import net.kwatts.powtools.util.Util;
import net.kwatts.powtools.util.debugdrawer.DebugDrawerMockBle;
import net.kwatts.powtools.view.AlertsMvpController;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static net.kwatts.powtools.model.OWDevice.KEY_RIDE_MODE;
import static net.kwatts.powtools.model.OWDevice.MockOnewheelCharacteristicSpeed;
import static net.kwatts.powtools.util.Util.rpmToKilometersPerHour;
import static net.kwatts.powtools.util.Util.rpmToMilesPerHour;


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

    private static final boolean ONEWHEEL_LOGGING = true;
    private static final int REQUEST_ENABLE_BT = 1;


    MultiStateToggleButton mRideModeToggleButton;
    int mRideModePosition;
    boolean mRideModePositionSetOnceFlag;

    public VibrateService mVibrateService;
    private android.os.Handler mLoggingHandler = new Handler();
    private SpeedAlertResolver speedAlertResolver = new SpeedAlertResolver(App.INSTANCE.getSharedPreferences());

    private Context mContext;
    ScrollView mScrollView;
    Chronometer mChronometer;
    OWDevice mOWDevice;
    net.kwatts.powtools.databinding.ActivityMainBinding mBinding;

    private NotificationCompat.Builder mStatusNotificationBuilder;
    private static final String POW_NOTIF_CHANNEL_STATUS = "pow_status";
    private static final String POW_NOTIF_TAG_STATUS = "statusNotificationTag";

    PieChart mBatteryChart;
    Ride ride;
    private DisposableObserver<Address> rxLocationObserver;
    private AlertsMvpController alertsController;
    //private SpeedView mSpeedBar;
    public ProgressiveGauge mProgressiveGauge;
    private boolean connectionServiceIsBound;
    @Nullable
    private Disposable connectionStatusDisposable;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NotificationEvent event) {
        Timber.d(event.message + ":" + event.title);
        final String title = event.title;
        final String message = event.message;
        runOnUiThread(() -> {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mContext, "ponewheel")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title)
                            .setColor(0x008000)
                            .setContentText(message);

            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                    new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(contentIntent);

            android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert mNotifyMgr != null;
            mNotifyMgr.notify(message, 0, mBuilder.build());
        });
    }

    public void updateLog(final String msg) {
        Timber.i(msg);
        runOnUiThread(() -> {
            //mBinding.owLog.setMovementMethod(new ScrollingMovementMethod());
            mBinding.owLog.append("\n" + msg);
            mScrollView.fullScroll(View.FOCUS_DOWN);
        });

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

    //battery level alerts
    public static SparseBooleanArray batteryAlertLevels = new SparseBooleanArray() {{
        put(75, false); //1
        put(50, false); //2
        put(25, false); //3
        put(5, false); // 4
    }};


    public void updateBatteryRemaining(final int percent) {
        // Update ongoing notification
        mStatusNotificationBuilder.setContentText("battery: " + percent + "%");
        android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(POW_NOTIF_TAG_STATUS, 0, mStatusNotificationBuilder.build());

        runOnUiThread(() -> {
            try {
                ArrayList<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(percent, 0));
                entries.add(new PieEntry(100 - percent, 1));
                PieDataSet dataSet = new PieDataSet(entries, "battery percentage");
                ArrayList<Integer> mColors = new ArrayList<>();
                mColors.add(ColorTemplate.rgb("#2E7D32")); //green
                mColors.add(ColorTemplate.rgb("#C62828")); //red
                dataSet.setColors(mColors);

                PieData newPieData = new PieData(dataSet);
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
                                EventBus.getDefault().post(new VibrateEvent(1000, 1));
                                onEvent(new NotificationEvent("OW Battery", "75%"));
                                break;
                            case 50:
                                EventBus.getDefault().post(new VibrateEvent(1000, 2));
                                onEvent(new NotificationEvent("OW Battery", "50%"));
                                break;
                            case 25:
                                EventBus.getDefault().post(new VibrateEvent(1000, 3));
                                onEvent(new NotificationEvent("OW Battery", "25%"));
                                break;
                            case 5:
                                EventBus.getDefault().post(new VibrateEvent(1000, 4));
                                onEvent(new NotificationEvent("OW Battery", "5%"));
                                break;
                            default:
                        }
                        batteryAlertLevels.put(percent, true);
                    }
                }

            } catch (Exception e) {
                Timber.e("Got an exception updating battery:" + e.getMessage());
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
        Timber.d("Starting...");
        super.onCreate(savedInstanceState);

        mContext = this;

        startStatusNotification();

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

        doBindService();

        setupToolbar();

        mScrollView = findViewById(R.id.logScroller);

//        if (App.INSTANCE.getSharedPreferences().isLoggingEnabled()) {
//            initLogging();
//        } //TODO

        mChronometer = findViewById(R.id.chronometer);
        mProgressiveGauge = findViewById(R.id.speedbar);
    }

    private void startStatusNotification() {
        mStatusNotificationBuilder =
                new NotificationCompat.Builder(mContext, POW_NOTIF_CHANNEL_STATUS)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Status")
                        .setColor(0x008000)
                        .setContentText("Waiting for connection...")
                        .setOngoing(true)
                        .setAutoCancel(true);
        android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert mNotifyMgr != null;
        mNotifyMgr.notify(POW_NOTIF_TAG_STATUS,0, mStatusNotificationBuilder.build());
    }

    private void stopStatusNotification() {
        android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert mNotifyMgr != null;
        mNotifyMgr.cancel(POW_NOTIF_TAG_STATUS, 0);
    }

    public BluetoothUtil getBluetoothUtil() {
        return bluetoothConnectionService.getBluetoothUtil();
    }

    public void overrideBluetoothUtil(BluetoothUtil bluetoothUtil) {
        bluetoothConnectionService.setBluetoothUtil(bluetoothUtil);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothUtil.init(mainActivityC, mOWDevice, bluetoothManager);
        doSubscribeToBtStatus();
    }

    private void initWakelock() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
//                Timber.d( "onPropertyChanged: " + mOWDevice.isConnected.get());
//                Timber.d( "onPropertyChanged: " + observable.toString() + "i" + i);
//            }
//        });

        mOWDevice.setupCharacteristics();
        mOWDevice.isConnected.set(false);

        //mOWDevice.bluetoothLe.set("Off");
        //mOWDevice.bluetoothStatus.set("Disconnected");

        mOWDevice.isConnected.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (mOWDevice.isConnected.get() && isNewOrNotContinuousRide()) {
                    ride = new Ride();
                    App.dbExecute(database -> ride.id = database.rideDao().insert(ride));
                }
            }
        });

        mOWDevice.characteristics.get(MockOnewheelCharacteristicSpeed).value.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                String speedString = mOWDevice.characteristics.get(MockOnewheelCharacteristicSpeed).value.get();
                alertsController.handleSpeed(speedString);
                updateGaugeOnSpeedChange(mProgressiveGauge, speedString);
            }
        });
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothConnectionService.getBluetoothUtil().init(mainActivityC, mOWDevice, bluetoothManager);
    }

    private void updateGaugeOnSpeedChange(ProgressiveGauge gauge, @NonNull String speedString) {
        if (speedAlertResolver.isAlertThresholdExceeded(speedString)) {
            gauge.setSpeedometerColor(ColorTemplate.rgb("#800000"));
        } else {
            gauge.setSpeedometerColor(ColorTemplate.rgb("#2E7D32"));
        }
    }

    private void logOnChange(OWDevice.DeviceCharacteristic deviceCharacteristic) {
        deviceCharacteristic.value.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                Timber.d(deviceCharacteristic.key.get() + " = " + deviceCharacteristic.value.get());
            }
        });
    }

    boolean isNewOrNotContinuousRide() {
        if (ride == null) {
            return true;
        }
        if (ride.end == null) {
            Timber.e("isNewOrNotContinuousRide: unexpected state, ride.end not set");
            return true;
        }

        long millisSinceLastMoment = new Date().getTime() - ride.end.getTime();
        // Not continuous is defined as 1 min break. Maybe configurable in the future.
        return TimeUnit.MINUTES.toMillis(1) > millisSinceLastMoment;
    }

    private void showEula() {
        new MaterialDialog.Builder(this)
                .theme(Theme.LIGHT)
                .title("WARNING")
                .content(R.string.eula)
                .positiveText("AGREE")
                .onPositive((dialog, which) ->
                        App.INSTANCE.getSharedPreferences().setEulaAgreed(true))
                .show();
    }

    private void showDonation() {
        new MaterialDialog.Builder(this)
                .theme(Theme.LIGHT)
                .title("Donate")
                .content("This app is openly developed and maintained by contributors in their spare time. Show support and help fuel their OneWheel addictions :)")
                .items(R.array.donation_options)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=kwatkins%40gmail%2ecom&lc=US&item_name=pOneWheel%20Android%20App&no_note=1&no_shipping=1&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
                            startActivity(browserIntent);
                        } else if (which == 1) {
                            //TODO: Google Play
                        }

                    }
                })
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
    protected void onStart() {
        super.onStart();
        doSubscribeToBtStatus();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        doUnsubscribeFromBtStatus();
    }

    @Override
    public void onDestroy() {
        if (mVibrateService != null) {
            unbindService(mVibrateConnection);
        }
        App.INSTANCE.getSharedPreferences().removeListener(this);
        stopStatusNotification();
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (bluetoothConnectionService != null && mOWDevice.isConnected.get()) {
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            //menu.findItem(R.id.menu_ow_light_on).setVisible(true);
            //menu.findItem(R.id.menu_ow_ridemode).setVisible(true);
        } else if (bluetoothConnectionService != null && getBluetoothUtil().isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
            //menu.findItem(R.id.menu_ow_light_on).setVisible(false);
            //menu.findItem(R.id.menu_ow_ridemode).setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
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
                getPermissions().subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if (getBluetoothUtil().isConnected()) {
                            BluetoothConnectionService.Companion.startBtConnection(MainActivity.this);
                            if (App.INSTANCE.getSharedPreferences().isLoggingEnabled()) {
                                startLocationScan();
                            }
                        } else if (getBluetoothUtil().isBtAdapterAvailable(MainActivity.this)) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.bt_is_not_supported), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }
                });
                break;
            case R.id.menu_stop:
                BluetoothConnectionService.Companion.stopBtConnection(this);
                this.invalidateOptionsMenu();
                break;
            case R.id.menu_disconnect:
                mOWDevice.isConnected.set(false);
                getBluetoothUtil().disconnect();
                Timber.i("Disconnected from device by user.");
                deviceConnectedTimer(false);
                mLoggingHandler.removeCallbacksAndMessages(null);
                this.invalidateOptionsMenu();
                break;
            case R.id.menu_about:
                showEula();
                break;
            case R.id.menu_donate:
                showDonation();
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
                    @Override
                    public void onNext(Address address) {

                        boolean isLocationsEnabled = App.INSTANCE.getSharedPreferences().isLocationsEnabled();
                        if (isLocationsEnabled) {
                            mOWDevice.setGpsLocation(address);
                        } else if (rxLocationObserver != null) {
                            rxLocationObserver.dispose();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e("onError: error retrieving location", e);
                    }

                    @Override
                    public void onComplete() {
                        Timber.d("onComplete: ");
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

        alertsController.recaptureMedia(this);
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
        Timber.i("onSharedPreferenceChanged callback");
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
                Timber.d("onSharedPreferenceChanged: " + key);
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
            Runnable deviceFileLogger = new Runnable() {
                @Override
                public void run() {
                    int mLoggingFrequency = App.INSTANCE.getSharedPreferences().getLoggingFrequency();
                    mLoggingHandler.postDelayed(this, mLoggingFrequency);
                    if (mOWDevice.isConnected.get()) {
                        try {
                            persistMoment();

                        } catch (Exception e) {
                            Timber.e("unable to write logs", e);
                        }
                    }
                }
            };
            mLoggingHandler.postDelayed(deviceFileLogger, App.INSTANCE.getSharedPreferences().getLoggingFrequency());

        }
    }

    private void persistMoment() throws Exception {
        App.dbExecute(database -> {
            Date latestMoment = new Date();

            if (ride.start == null) {
                ride.start = latestMoment;
            }
            ride.end = latestMoment;
            database.rideDao().updateRide(ride);

            Moment moment = new Moment(ride.id, latestMoment);
            moment.rideId = ride.id;
            long momentId = database.momentDao().insert(moment);
            List<Attribute> attributes = new ArrayList<>();
            for (OWDevice.DeviceCharacteristic deviceReadCharacteristic : mOWDevice.getNotifyCharacteristics()) {
                Attribute attribute = new Attribute();
                attribute.setMomentId(momentId);
                attribute.setValue(deviceReadCharacteristic.value.get());
                attribute.setKey(deviceReadCharacteristic.key.get());

                attributes.add(attribute);
            }
            database.attributeDao().insertAll(attributes);
            if (mOWDevice.getGpsLocation() != null) {
                moment.setGpsLat(mOWDevice.getGpsLocation().getLatitude());
                moment.setGpsLong(mOWDevice.getGpsLocation().getLongitude());
            }
        });
    }

    private static final int SPEED_ANIMATION_DURATION = 0;

    private void initSpeedBar() {
        mOWDevice.speedRpm.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                runOnUiThread(() -> {
                    try {
                        //TODO: add notes for things like top speed https://github.com/anastr/SpeedView/wiki/Notes
                        double speed = (double) mOWDevice.speedRpm.get();
                        boolean isMetric = App.INSTANCE.getSharedPreferences().isMetric();
                        if (isMetric) {
                            mProgressiveGauge.setMaxSpeed(25);
                            mProgressiveGauge.setUnit("km/h");
                            mProgressiveGauge.speedTo((float) Util.round(rpmToKilometersPerHour(speed), 1), SPEED_ANIMATION_DURATION);
                        } else {
                            mProgressiveGauge.setMaxSpeed(20);
                            mProgressiveGauge.setUnit("mph");
                            mProgressiveGauge.speedTo((float) Util.round(rpmToMilesPerHour(speed), 1), SPEED_ANIMATION_DURATION);
                        }
                    } catch (Exception e) {
                        Timber.e("Got an exception updating speed:" + e.getMessage());
                    }
                });
            }
        });
    }


    SwitchCompat mMasterLight;
    SwitchCompat mCustomLight;
    SwitchCompat mFrontBright;
    SwitchCompat mBackBright;
    SwitchCompat mFrontBlink;
    SwitchCompat mBackBlink;

    int frontBlinkCount = 0;
    int backBlinkCount = 0;

    public class mFrontBlinkTaskTimerTask extends TimerTask {
        @Override
        public void run() {
            if ((frontBlinkCount % 2) == 0) {
                mOWDevice.setCustomLights(getBluetoothUtil(), 0, 0, 60);
            } else {
                mOWDevice.setCustomLights(getBluetoothUtil(), 0, 0, 0);
            }
            frontBlinkCount++;
        }
    }

    mFrontBlinkTaskTimerTask mFrontBlinkTimerTask;
    Timer mFrontBlinkTimer;

    public class mBackBlinkTaskTimerTask extends TimerTask {
        @Override
        public void run() {
            if ((backBlinkCount % 2) == 0) {
                mOWDevice.setCustomLights(getBluetoothUtil(), 1, 1, 60);
            } else {
                mOWDevice.setCustomLights(getBluetoothUtil(), 1, 1, 0);
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
                    Timber.i("Lights turned on");
                    mOWDevice.setLights(getBluetoothUtil(), 1);
                } else {
                    Timber.i("Lights turned off");
                    mOWDevice.setLights(getBluetoothUtil(), 0);
                }
            }
        });

        mCustomLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    Timber.i("Custom lights turned on");
                    mOWDevice.setLights(getBluetoothUtil(), 2);
                } else {
                    Timber.i("Custom lights turned off");
                    mOWDevice.setLights(getBluetoothUtil(), 0);

                }
            }
        });


        mFrontBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    mOWDevice.setCustomLights(getBluetoothUtil(), 0, 0, 60);
                } else {
                    mOWDevice.setCustomLights(getBluetoothUtil(), 0, 0, 30);
                }
            }

        });

        mBackBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mOWDevice.isConnected.get()) {
                if (isChecked) {
                    mOWDevice.setCustomLights(getBluetoothUtil(), 1, 1, 60);
                } else {
                    mOWDevice.setCustomLights(getBluetoothUtil(), 1, 1, 30);

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

        final ToggleButton.OnValueChangedListener onToggleValueChangedListener = position -> {
            if (mOWDevice.isConnected.get()) {
                Timber.d("mOWDevice.setRideMode button pressed:" + position);
                double mph = net.kwatts.powtools.util.Util.rpmToMilesPerHour(mOWDevice.speedRpm.get());
                if (mph > 12) {
                    Toast.makeText(mContext, "Unable to change riding mode, your going too fast! (" + mph + " mph)", Toast.LENGTH_SHORT).show();
                    //TODO: fix, kicks back to change listener & creates an infinite amount of evil whirling dervishes
                    //mRideModeToggleButton.setValue(mRideModePosition);
                } else {
                    mRideModePosition = position;
                    if (mOWDevice.isOneWheelPlus.get()) {
                        Timber.i("Ridemode changed to:" + position + 4);
                        mOWDevice.setRideMode(getBluetoothUtil(), position + 4); // ow+ ble value for ridemode 4,5,6,7,8 (delirium)
                    } else {
                        Timber.i("Ridemode changed to:" + position + 1);
                        mOWDevice.setRideMode(getBluetoothUtil(), position + 1); // ow uses 1,2,3 (expert)
                    }
                }
            } else {
                Toast.makeText(mContext, "Not connected to Device!", Toast.LENGTH_SHORT).show();
            }
        };
        mRideModeToggleButton.setOnValueChangedListener(onToggleValueChangedListener);
        mOWDevice.getDeviceCharacteristicByKey(KEY_RIDE_MODE).value.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                runOnUiThread(() -> {

                    String rideMode = mOWDevice.getDeviceCharacteristicByKey(KEY_RIDE_MODE).value.get();
                    int rideModeInt = Integer.parseInt(rideMode);
                    if (mOWDevice.isOneWheelPlus.get()) {
                        rideModeInt -= 4;
                    } else {
                        rideModeInt -= 1;
                    }

                    mRideModeToggleButton.setOnValueChangedListener(null);
                    mRideModeToggleButton.setValue(rideModeInt);
                    mRideModeToggleButton.setOnValueChangedListener(onToggleValueChangedListener);

                });
            }
        });


    }

    private BluetoothConnectionService bluetoothConnectionService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            bluetoothConnectionService = ((BluetoothConnectionService.LocalBinder) iBinder).getService();
            setupOWDevice();
            initSpeedBar();
            initBatteryChart();
            initLightSettings();
            initRideModeButtons();

            doSubscribeToBtStatus();

            new DebugDrawer.Builder(MainActivity.this)
                    .modules(
                            new DebugDrawerMockBle(MainActivity.this),
                            new SettingsModule(MainActivity.this),
                            new TimberModule()
                    ).build();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            doUnsubscribeFromBtStatus();
            bluetoothConnectionService = null;
        }
    };

    private void doSubscribeToBtStatus() {
        if (bluetoothConnectionService != null) {
            if (connectionStatusDisposable != null && !connectionStatusDisposable.isDisposed()) {
                Timber.w("connectionStatusDisposable was not disposed. Disposing...");
                connectionStatusDisposable.dispose();
            }
            connectionStatusDisposable = bluetoothConnectionService.getBluetoothUtil().getConnectionStatus()
                    .subscribe(
                            connectionStatus -> invalidateOptionsMenu(),
                            Timber::e
                    );
        }
    }

    private void doUnsubscribeFromBtStatus() {
        if (connectionStatusDisposable != null) {
            connectionStatusDisposable.dispose();
            connectionStatusDisposable = null;
        }
    }

    private void doBindService() {
        bindService(new Intent(this, BluetoothConnectionService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        connectionServiceIsBound = true;
    }

    private void doUnbindService() {
        if (connectionServiceIsBound) {
            unbindService(serviceConnection);
            connectionServiceIsBound = false;
        }
    }

    private MainActivityDelegateImpl mainActivityC = new MainActivityDelegateImpl();

    private class MainActivityDelegateImpl implements MainActivityDelegate {

        @Override
        public void updateBatteryRemaining(int percent) {
            MainActivity.this.updateBatteryRemaining(percent);
        }

        @Override
        public void deviceConnectedTimer(boolean timer) {
            MainActivity.this.deviceConnectedTimer(timer);
        }

    }

}
