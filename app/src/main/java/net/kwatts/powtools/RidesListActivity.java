package net.kwatts.powtools;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.kwatts.powtools.database.Ride;

import java.util.List;

public class RidesListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_viewer);

        ListView listView = findViewById(R.id.ride_list_view);
        ArrayAdapter<Ride> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        final List<Ride> rides = App.INSTANCE.db.rideDao().getAll();
        for (Ride ride : rides) {
            System.out.println("logFile = " + ride);
        }
        if (listView != null) {
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(RidesListActivity.this, MapActivity.class);
                intent.putExtra(RideDetailActivity.FILE_NAME, rides.get(position).id);
                startActivity(intent);
            });
        }

        adapter.addAll(rides);
    }


}
