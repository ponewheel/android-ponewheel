package net.kwatts.powtools;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import net.kwatts.powtools.loggers.PlainTextFileLogger;

import java.util.ArrayList;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    public static final String TAG = MapActivity.class.getSimpleName();
    ArrayList<LatLng> latLngs = new ArrayList<>();
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.maps_activity);

        String fileName = getIntent().getStringExtra(RideDetailActivity.FILE_NAME);

        ArrayList<Entry> values = new ArrayList<>();

        // TODO convert to async
        PlainTextFileLogger.getEntriesFromFile(fileName, values, latLngs);



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
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.

        Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .add( latLngs.toArray(new LatLng[latLngs.size()]) )
        );
        LatLngBounds.Builder latLongBounds = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) {
            latLongBounds.include(latLng);
        }

        // TODO convert dp to px
        LatLngBounds latLngBounds = latLongBounds.build();
        Log.d(TAG, "onMapReady: latLngBounds.northeast" + latLngBounds.northeast);
        Log.d(TAG, "onMapReady: latLngBounds.southwest" + latLngBounds.southwest);

        mapFragment.getView().post(() -> googleMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, 150)));
    }
}