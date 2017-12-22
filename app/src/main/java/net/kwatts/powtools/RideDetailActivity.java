package net.kwatts.powtools;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import net.kwatts.powtools.components.LockScrollbarGestureListener;
import net.kwatts.powtools.components.LockableScrollView;
import net.kwatts.powtools.database.entities.Attribute;
import net.kwatts.powtools.database.entities.Moment;
import net.kwatts.powtools.loggers.PlainTextFileLogger;
import net.kwatts.powtools.model.OWDevice;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class RideDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String TAG = RideDetailActivity.class.getSimpleName();
    public static final String RIDE_ID = "EXTRA_RIDE_ID";
    public static final String RIDE_DATE = "EXTRA_RIDE_DATE";
    public static final SimpleDateFormat FILE_FORMAT_DATE = new SimpleDateFormat("yyyy-MM-dd_HH_mm", Locale.US);


    Map<Long, LatLng> timeLocationMap = new ArrayMap<>();
    private SupportMapFragment mapFragment;
    GoogleMap googleMap;
    Set<Marker> mapMarkers = new HashSet<>();
    private ShareActionProvider shareActionProvider;
    private boolean isDatasetReady = false;
    private boolean isMapReady = false;
    private int mapCameraPadding;
    private MenuItem shareMenuItem;
    private Intent shareFileIntent;
    private LockableScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_ride_detail);

        scrollView = findViewById(R.id.ride_detail_scroll_view);
        timeLocationMap.clear();
        retrieveData();
        setupMap();

        setupToolbar();
    }

    private void setupMap() {
        mapCameraPadding = getResources().getDimensionPixelSize(R.dimen.map_camera_padding);

        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void retrieveData() {
        App.dbExecute(database -> {
            long rideId = getIntent().getLongExtra(RIDE_ID, -1);
            List<Moment> moments = database.momentDao().getFromRide(rideId);
            Long referenceTime = null;

            List<Entry> timeSpeedMap = new ArrayList<>();
            List<Entry> timePad1Map = new ArrayList<>();
            List<Entry> timePad2Map = new ArrayList<>();
            List<Entry> timeControllerTempMap = new ArrayList<>();
            List<Entry> timeMotorTempMap = new ArrayList<>();
            List<Entry> timeBatteryMap = new ArrayList<>();

            for (Moment moment : moments) {
                long time = moment.getDate().getTime();
                if (referenceTime == null) {
                    referenceTime = time;
                }
                time = time - referenceTime;
                if (moment.getGpsLatDouble() != null && moment.getGpsLongDouble() != null) {
                    timeLocationMap.put(time, new LatLng(moment.getGpsLatDouble(), moment.getGpsLongDouble()));
                }
//                Attribute attribute = database.attributeDao().getFromMomentAndKey(moment.id, "speed");
                List<Attribute> attributes = database.attributeDao().getFromMoment(moment.id);
                for (Attribute attribute : attributes) {
                    if (attribute != null
                            && attribute.getValue() != null
                            && !attribute.getValue().equals("")) {
                        String value = attribute.getValue();
                        Timber.d("value" + value + " key " + attribute.getKey());
                        if (OWDevice.KEY_SPEED.equals(attribute.getKey())) {
                            timeSpeedMap.add(new Entry(time, Float.valueOf(value)));
                        } else if (OWDevice.KEY_RIDER_DETECTED_PAD_1.equals(attribute.getKey())) {
                            timePad1Map.add(new Entry(time, "true".equals(value) ? 1 : 0));
                        } else if (OWDevice.KEY_RIDER_DETECTED_PAD_2.equals(attribute.getKey())) {
                            timePad2Map.add(new Entry(time, "true".equals(value) ? 1 : 0));
                        } else if (OWDevice.KEY_CONTROLLER_TEMP.equals(attribute.getKey())) {
                            timeControllerTempMap.add(new Entry(time, Float.valueOf(value)));
                        } else if (OWDevice.KEY_MOTOR_TEMP.equals(attribute.getKey())) {
                            timeMotorTempMap.add(new Entry(time, Float.valueOf(value)));
                        } else if (OWDevice.KEY_BATTERY.equals(attribute.getKey())) {
                            timeBatteryMap.add(new Entry(time, Float.valueOf(value)));
                        }
                    }
                }
            }

            isDatasetReady = true;
            checkDataAndMapReady();

            runOnUiThread( () -> {
                setupSpeedChart(timeSpeedMap);
                setupPadsChart(timePad1Map, timePad2Map);
                setupTempChart(timeControllerTempMap, timeMotorTempMap);
                setupBatteryChart(timeBatteryMap);
            } );

        });
    }



    private synchronized void checkDataAndMapReady() {
        if (isMapReady && isDatasetReady) {
            runOnUiThread(() -> {
                googleMap.addPolyline(
                        new PolylineOptions().clickable(true).add(
                                timeLocationMap.values().toArray(
                                        new LatLng[timeLocationMap.size()]
                                )
                        )
                );
                LatLngBounds.Builder latLongBoundsBuilder = new LatLngBounds.Builder();
                for (LatLng latLng : timeLocationMap.values()) {
                    latLongBoundsBuilder.include(latLng);
                }

                View mapFragmentView = mapFragment.getView();
                if (timeLocationMap.size() != 0) {
                    LatLngBounds latLngBounds = latLongBoundsBuilder.build();

                    if (mapFragmentView != null && googleMap != null) {
                        mapFragmentView.post(() -> googleMap.moveCamera(
                                // TODO Is mapCameraPadding w/ 150dp converted to px a good approach? Seems like maybe we'd prefer a geographic unit, aka 1 mile padding if that's possible?
                                CameraUpdateFactory.newLatLngBounds(latLngBounds, mapCameraPadding)));
                    }
                }
            });

            isMapReady = false;
            isDatasetReady = false;
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setTitle("POWheel");
        toolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(toolbar);


    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_item_prepare) {
            Toast.makeText(
                    this,
                    "Your file is being prepared",
                    Toast.LENGTH_SHORT)
                    .show();
            prepareItemForSharing();
            item.setVisible(false);

            // share icon will be visible after preparation is done
        } else {
            Timber.d("menuItem:"+item.toString());
        }

        return super.onOptionsItemSelected(item);
    }

    private void prepareItemForSharing() {
        App.dbExecute(database -> {
            long rideId = getIntent().getLongExtra(RIDE_ID, -1);
            String rideDate = getIntent().getStringExtra(RIDE_DATE);
            File file = new File( PlainTextFileLogger.getLoggingPath() + "/owlogs_" + rideDate + ".csv");
            if (file.exists()) {
                boolean deleted = file.delete();
                Timber.d("deleted?" + deleted);
            }
            try {
                FileOutputStream writer = new FileOutputStream(file, true);
                BufferedOutputStream output = new BufferedOutputStream(writer);

                StringBuilder stringBuilder = new StringBuilder();
                List<String> headers = database.attributeDao().getDistinctKeysFromRide(rideId);
                HashMap<String, String> keyValueOrderKeeper = new LinkedHashMap<>();
                headers.add(0, "time");
                headers.add("gps_lat");
                headers.add("gps_long");
                for (String header : headers) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(',');
                    }
                    stringBuilder.append(header);
                    keyValueOrderKeeper.put(header, "");
                }
                stringBuilder.append('\n');
                output.write(stringBuilder.toString().getBytes());

                List<Moment> moments = database.momentDao().getFromRide(rideId);
//                Long referenceTime = null;
                for (Moment moment : moments) {
                    stringBuilder.setLength(0); // reset
                    keyValueOrderKeeper.values().clear();

                    long time = moment.getDate().getTime();

                    // do we need relative time?
//                    if (referenceTime == null) {
//                        referenceTime = time;
//                    }
//                    time = time - referenceTime;
                    stringBuilder.append(Long.toString(time));

                    List<Attribute> attributes = database.attributeDao().getFromMoment(moment.id);
                    for (Attribute attribute : attributes) {
                        keyValueOrderKeeper.put(attribute.getKey(), attribute.getValue());
                    }
                    keyValueOrderKeeper.put("gps_lat", moment.getGpsLat());
                    keyValueOrderKeeper.put("gps_long", moment.getGpsLong());

                    for (String value : keyValueOrderKeeper.values()) {
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append(',');
                        }
                        stringBuilder.append(value);
                    }
                    stringBuilder.append('\n');
                    output.write(stringBuilder.toString().getBytes());
                }

                output.flush();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Uri uri = FileProvider.getUriForFile(this, "net.kwatts.powtools.fileprovider", file);
            shareFileIntent.putExtra(Intent.EXTRA_STREAM, uri);
            runOnUiThread(() -> shareMenuItem.setVisible(true));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ride_menu, menu);

        // store action provider
        shareMenuItem = menu.findItem(R.id.menu_item_share);
        shareMenuItem.setVisible(false);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);

        // Create share intent
        shareFileIntent = new Intent();
        shareFileIntent.setAction(Intent.ACTION_SEND);
        shareFileIntent.setType("text/csv");
        shareFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(shareFileIntent);

        return true;
    }

    private void setShareIntent(Intent shareIntent) {
        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(shareIntent);
            // unable to modify the intent?! wtf android
//            shareActionProvider.setOnShareTargetSelectedListener((source, intent) -> false);
        }
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        isMapReady = true;
        checkDataAndMapReady();
    }


    private void setupSpeedChart(List<Entry> values) {
        if (values.isEmpty()) {
            return;
        }
        LineChart speedChart = findViewById(R.id.ride_detail_speed_chart);
        assert speedChart != null;
        LineDataSet dataSet = new LineDataSet(values, "Label");
        setupDatasetWithDefaultValues(dataSet);


        LineData lineData = new LineData(dataSet);
        speedChart.setData(lineData);
        speedChart.notifyDataSetChanged();

        speedChart.getDescription().setEnabled(false);

        // enable touch gestures
        speedChart.setTouchEnabled(true);
        speedChart.setOnChartGestureListener(new LockScrollbarGestureListener(scrollView));
        addDragListenerMarkerMaker(speedChart);
        speedChart.setDoubleTapToZoomEnabled(false);
        speedChart.setDragDecelerationEnabled(false);

        // enable scaling and dragging
        speedChart.setDragEnabled(true);
        speedChart.setScaleEnabled(false);
        speedChart.setDrawGridBackground(false);
        speedChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        speedChart.setBackgroundColor(Color.WHITE);

        // add data
        speedChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = speedChart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = speedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
//        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularity(1f); // one hour?
        xAxis.setValueFormatter((value, axis) -> {
            long minutes = TimeUnit.MILLISECONDS.toMinutes((long) value);
            return minutes +"m";
        });

        YAxis leftAxis = speedChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
//        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
//        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = speedChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void addDragListenerMarkerMaker(LineChart speedChart) {
        speedChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                Long entryX = (long) entry.getX();
                if (timeLocationMap.containsKey(entryX)) {
                    clearAllMarkersFromMap();
                    LatLng latLng = timeLocationMap.get(entryX);
                    mapMarkers.add(googleMap.addMarker(new MarkerOptions().position(latLng)));
                }
            }

            @Override
            public void onNothingSelected() {
                clearAllMarkersFromMap();
            }
        });
    }

    private void setupPadsChart(List<Entry> pad1Values, List<Entry> pad2Values) {
        LineChart lineChart = findViewById(R.id.ride_detail_pads_chart);
//        LineChart lineChart = null;//findViewById(R.id.ride_detail_pads_chart);
        assert lineChart != null;
        Timber.d("pad1 size" + pad1Values.size());
        Timber.d("pad2 size" + pad2Values.size());
        ArrayList<ILineDataSet> sets = new ArrayList<>();
        if (!pad1Values.isEmpty()) {
            LineDataSet dataSet = new LineDataSet(pad1Values, "Pad1");
            setupDatasetWithDefaultValues(dataSet);
            sets.add(dataSet);
        }
        if (!pad2Values.isEmpty()) {
            LineDataSet dataSet2 = new LineDataSet(pad2Values, "Pad2");
            setupDatasetWithDefaultValues(dataSet2);
            dataSet2.setColor(ColorTemplate.MATERIAL_COLORS[1]);
            dataSet2.setFillColor(ColorTemplate.MATERIAL_COLORS[1]);
            sets.add(dataSet2);
        }

        if (pad1Values.isEmpty() && pad2Values.isEmpty()) {
            return;
        }

        LineData lineData = new LineData(sets);
        lineChart.setData(lineData);

        lineChart.notifyDataSetChanged();

        lineChart.getDescription().setEnabled(false);

        // enable touch gestures
        lineChart.setTouchEnabled(true);
        lineChart.setOnChartGestureListener(new LockScrollbarGestureListener(scrollView));
        addDragListenerMarkerMaker(lineChart);
        lineChart.setDoubleTapToZoomEnabled(false);


        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE);

        // add data
        lineChart.invalidate();

        // get the legend (only possible after setting data)
        Legend legend = lineChart.getLegend();
        legend.setEnabled(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
//        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter((value, axis) -> {
            long minutes = TimeUnit.MILLISECONDS.toMinutes((long) value);
            return minutes + "m";
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(1f);
//        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));
        leftAxis.setValueFormatter(((value, axis) -> value > .5f ? "On" : "Off"));

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupBatteryChart(List<Entry> timeBatteryMap) {
        LineChart lineChart = findViewById(R.id.ride_detail_battery_chart);
        assert lineChart != null;
        Timber.d("pad1 size" + timeBatteryMap.size());
//        Timber.d("pad2 size" + pad2Values.size());
        ArrayList<ILineDataSet> sets = new ArrayList<>();
        if (!timeBatteryMap.isEmpty()) {
            LineDataSet dataSet = new LineDataSet(timeBatteryMap, "Battery Percentage");
            setupDatasetWithDefaultValues(dataSet);
            sets.add(dataSet);
        }
//        if (!pad2Values.isEmpty()) {
//            LineDataSet dataSet2 = new LineDataSet(pad2Values, "Pad2");
//            setupDatasetWithDefaultValues(dataSet2);
//            dataSet2.setColor(ColorTemplate.MATERIAL_COLORS[1]);
//            dataSet2.setFillColor(ColorTemplate.MATERIAL_COLORS[1]);
//            sets.add(dataSet2);
//        }

        if (timeBatteryMap.isEmpty()) {
//        if (pad1Values.isEmpty() && pad2Values.isEmpty()) {
            return;
        }

        LineData lineData = new LineData(sets);
        lineChart.setData(lineData);

        lineChart.notifyDataSetChanged();

        lineChart.getDescription().setEnabled(false);

        // enable touch gestures
        lineChart.setTouchEnabled(true);
        lineChart.setOnChartGestureListener(new LockScrollbarGestureListener(scrollView));
        addDragListenerMarkerMaker(lineChart);
        lineChart.setDoubleTapToZoomEnabled(false);


        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE);

        // add data
        lineChart.invalidate();

        // get the legend (only possible after setting data)
        Legend legend = lineChart.getLegend();
        legend.setEnabled(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
//        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter((value, axis) -> {
            long minutes = TimeUnit.MILLISECONDS.toMinutes((long) value);
            return minutes + "m";
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
//        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupTempChart(List<Entry> timeControllerTempMap, List<Entry> timeMotorTempMap) {
        LineChart lineChart = findViewById(R.id.ride_detail_temp_chart);
        assert lineChart != null;
        ArrayList<ILineDataSet> sets = new ArrayList<>();
        if (!timeControllerTempMap.isEmpty()) {
            LineDataSet dataSet = new LineDataSet(timeControllerTempMap, "Controller Temp");
            setupDatasetWithDefaultValues(dataSet);
            sets.add(dataSet);
        }
        if (!timeMotorTempMap.isEmpty()) {
            LineDataSet dataSet2 = new LineDataSet(timeMotorTempMap, "Motor Temp");
            setupDatasetWithDefaultValues(dataSet2);
            dataSet2.setColor(ColorTemplate.MATERIAL_COLORS[1]);
            dataSet2.setFillColor(ColorTemplate.MATERIAL_COLORS[1]);
            sets.add(dataSet2);
        }
        if (timeControllerTempMap.isEmpty() && timeMotorTempMap.isEmpty()) {
            return;
        }

        LineData lineData = new LineData(sets);
        lineChart.setData(lineData);

        lineChart.notifyDataSetChanged();

        lineChart.getDescription().setEnabled(false);

        // enable touch gestures
        lineChart.setTouchEnabled(true);
        lineChart.setOnChartGestureListener(new LockScrollbarGestureListener(scrollView));

        addDragListenerMarkerMaker(lineChart);

        lineChart.setDoubleTapToZoomEnabled(false);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE);

        // add data
        lineChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = lineChart.getLegend();
        l.setEnabled(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
//        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter((value, axis) -> {
            long minutes = TimeUnit.MILLISECONDS.toMinutes((long) value);
            return minutes +"m";
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
//        leftAxis.setGranularityEnabled(true);
//        leftAxis.setAxisMinimum(0f);
//        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    void clearAllMarkersFromMap() {
        for (Marker mapMarker : mapMarkers) {
            mapMarker.remove();
        }
        mapMarkers.clear();
    }

    private void setupDatasetWithDefaultValues(LineDataSet dataSet) {
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(ColorTemplate.getHoloBlue());
        dataSet.setValueTextColor(ColorTemplate.getHoloBlue());
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(ColorTemplate.getHoloBlue());
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawCircleHole(false);
    }

}