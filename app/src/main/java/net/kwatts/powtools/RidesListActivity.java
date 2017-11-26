package net.kwatts.powtools;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.kwatts.powtools.database.Moment;
import net.kwatts.powtools.database.Ride;
import net.kwatts.powtools.database.RideRow;

public class RidesListActivity extends AppCompatActivity {

    public static final String TAG = "RidesListActivity";
    public static final int MENU_ITEM_DELETE = 0;
    private static final int MENU_ITEM_ADD_RANDO = 1;
    RideListAdapter rideListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_viewer);

        setupToolbar();

        RecyclerView recyclerView = findViewById(R.id.ride_list_view);
        rideListAdapter = new RideListAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        refreshList();

        recyclerView.setAdapter(rideListAdapter);

    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0, MENU_ITEM_DELETE, 0, "Delete");
        if (BuildConfig.DEBUG) {
            menu.add(0, MENU_ITEM_ADD_RANDO, 0, "Add Dummy");
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 0:
                deleteSelectedRides();

                break;
            case 1:
                insertSampleRidesOrDebug();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedRides() {
        App.dbExecute(database -> {
            final List<RideRow> checkedItems = rideListAdapter.getCheckedItems();
            for (RideRow checkedItem : checkedItems) {
                database.rideDao().delete(checkedItem.rideId);
            }

            refreshList();
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setTitle("Logged Rides");
        toolbar.setLogo(R.mipmap.ic_launcher);

        setSupportActionBar(toolbar);

    }

    private void insertSampleRidesOrDebug() {
        App.dbExecute(database -> {

            for (Ride ride : database.rideDao().getAll()) {
                System.out.println("ride = " + ride);
            }

            // Insert sample rides
            Ride ride = new Ride();
            long rideId = database.rideDao().insert(ride);

            Calendar calendar = Calendar.getInstance();
            Moment moment = new Moment(rideId, calendar.getTime());
            database.momentDao().insert(moment);

            calendar.add(Calendar.MINUTE, (int) (Math.random() * 58));
            Moment moment2 = new Moment(rideId, calendar.getTime());
            database.momentDao().insert(moment2);

            refreshList();
        });
    }

    private void refreshList() {
        Single.fromCallable(() -> App.INSTANCE.db.rideDao().getRideRowList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<RideRow>>() {

                    @Override
                    public void onSuccess(List<RideRow> rides) {
                        for (RideRow ride : rides) {
                            System.out.println("logFile = " + ride);
                        }

                        rideListAdapter.getRideList().clear();
                        rideListAdapter.getRideList().addAll(rides);
                        rideListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: ", e);
                    }
                });
    }
}
