package net.kwatts.powtools;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import net.kwatts.powtools.loggers.PlainTextFileLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String TAG = MapActivity.class.getSimpleName();
    public static final String FILE_NAME = "EXTRA_DATA_FILE_NAME";


    ArrayMap<Long, LatLng> timeLocationMap = new ArrayMap<>();
    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private HashSet<Marker> mapMarkers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.maps_activity);

        String fileName = getIntent().getStringExtra(FILE_NAME);

        ArrayList<Entry> timeSpeedMap = new ArrayList<>();

        timeLocationMap.clear();
        // TODO convert to async
        PlainTextFileLogger.getEntriesFromFile(fileName, timeSpeedMap, timeLocationMap);

        setupChart(timeSpeedMap);

        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .add( timeLocationMap.values().toArray(new LatLng[timeLocationMap.size()]) )
        );
        LatLngBounds.Builder latLongBoundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLng : timeLocationMap.values()) {
            latLongBoundsBuilder.include(latLng);
        }

        View mapFragmentView = mapFragment.getView();
        if (timeLocationMap.size() != 0) {
            LatLngBounds latLngBounds = latLongBoundsBuilder.build();
            double width = latLngBounds.southwest.longitude - latLngBounds.northeast.longitude;
            Log.d(TAG, "onMapReady: mapWidth" + width);

            // TODO apply a min width

            assert mapFragmentView != null;
            mapFragmentView.post(() -> googleMap.moveCamera(
                    // TODO convert dp to px
                    CameraUpdateFactory.newLatLngBounds(latLngBounds, 150)));
        }
    }


    private void setupChart(ArrayList<Entry> values) {
        LineChart lineChart = (LineChart) findViewById(R.id.ride_detail_speed_chart);
        assert lineChart != null;
        LineDataSet dataSet = new LineDataSet(values, "Label");
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

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();

        lineChart.getDescription().setEnabled(false);

        // enable touch gestures
        lineChart.setTouchEnabled(true);
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                Log.d(TAG, "onValueSelected: " + entry.getX());
                Long entryX = (long) entry.getX();
                if (timeLocationMap.containsKey(entryX)) {
                    for (Marker mapMarker : mapMarkers) {
                        mapMarker.remove();
                    }
                    LatLng latLng = timeLocationMap.get(entryX);
                    mapMarkers.add(googleMap.addMarker(new MarkerOptions().position(latLng)));
                }
            }

            @Override
            public void onNothingSelected() {
                for (Marker mapMarker : mapMarkers) {
                    mapMarker.remove();
                }
            }
        });

        lineChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setViewPortOffsets(0f, 0f, 0f, 0f);

        // add data
        lineChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = lineChart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
//        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter((value, axis) -> {
            long minutes = TimeUnit.MILLISECONDS.toMinutes((long) value);
            return minutes +"m";
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
//        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
//        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

}