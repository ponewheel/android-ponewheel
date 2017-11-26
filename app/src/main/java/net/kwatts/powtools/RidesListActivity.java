package net.kwatts.powtools;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import net.kwatts.powtools.database.Ride;
import net.kwatts.powtools.database.RideRow;

public class RidesListActivity extends AppCompatActivity {

    public static final String TAG = "RidesListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_viewer);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        mToolbar.setTitle("Logged Rides");
        mToolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(mToolbar);

        RecyclerView recyclerView = findViewById(R.id.ride_list_view);
        RideListAdapter rideListAdapter = new RideListAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Single.fromCallable(() -> App.INSTANCE.db.rideDao().getRideRowList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<RideRow>>() {

                    @Override
                    public void onSuccess(List<RideRow> rides) {
                        for (RideRow ride : rides) {
                            System.out.println("logFile = " + ride);
                        }
                        rideListAdapter.getRideList().addAll(rides);
                        rideListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: ", e);
                    }
                });

            recyclerView.setAdapter(rideListAdapter);
            //recyclerView.clicksetOnItemClickListener((parent, view, position, id) -> {
            //    Intent intent = new Intent(RidesListActivity.this, MapActivity.class);
            //
            //    RideRow ride = rideListAdapter.getItem(position);
            //    assert ride != null;
            //    //FIXME intent.putExtra(RidesListActivity.FILE_NAME, ride.id);
            //    startActivity(intent);
            //});



        App.dbExecute(database -> {

            for (Ride ride : database.rideDao().getAll()) {
                System.out.println("ride = " + ride);
            }

            // Insert sample rides
            //Ride ride = new Ride();
            //Ride ride2 = new Ride();
            //long rideId = database.rideDao().insert(ride);
            //database.rideDao().insert(ride2);
            //
            //Calendar calendar = Calendar.getInstance();
            //Moment moment = new Moment(rideId, calendar.getTime());
            //database.momentDao().insert(moment);
            //
            //calendar.add(Calendar.MINUTE, 2);
            //Moment moment2 = new Moment(rideId, calendar.getTime());
            //database.momentDao().insert(moment2);
        });


    }


}
