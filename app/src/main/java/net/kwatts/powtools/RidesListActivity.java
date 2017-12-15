package net.kwatts.powtools;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import net.kwatts.powtools.adapters.RideListAdapter;
import net.kwatts.powtools.database.RideRow;
import net.kwatts.powtools.util.debugdrawer.DebugDrawerAddDummyRide;

import java.util.ArrayList;
import java.util.List;

import io.palaima.debugdrawer.DebugDrawer;
import io.palaima.debugdrawer.commons.SettingsModule;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class RidesListActivity extends AppCompatActivity {

    public static final String TAG = "RidesListActivity";
    public static final int MENU_ITEM_DELETE = 0;
    private static final int MENU_ITEM_ADD_RANDO = 1;
    RideListAdapter rideListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rides_list);

        setupToolbar();

        RecyclerView recyclerView = findViewById(R.id.ride_list_view);
        rideListAdapter = new RideListAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        refreshList();

        recyclerView.setAdapter(rideListAdapter);


        new DebugDrawer.Builder(this)
                .modules(
                        new DebugDrawerAddDummyRide(this),
                        new SettingsModule(this)
                ).build();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0, MENU_ITEM_DELETE, 0, "Delete");

        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE:
                deleteSelectedRides();

                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedRides() {
        App.dbExecute(database -> {
            final List<RideRow> checkedItems = rideListAdapter.getCheckedItems();
            for (int i = 0; i < checkedItems.size(); i++) {
                RideRow checkedItem = checkedItems.get(i);
                database.rideDao().delete(checkedItem.rideId);

                int removedIndex = rideListAdapter.getRideList().indexOf(checkedItem);
                rideListAdapter.getRideList().remove(checkedItem);
                runOnUiThread(() -> rideListAdapter.notifyItemRemoved(removedIndex));
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setTitle("Logged Rides");
        toolbar.setLogo(R.mipmap.ic_launcher);

        setSupportActionBar(toolbar);

    }


    public void refreshList() {
        Single.fromCallable(() -> App.INSTANCE.db.rideDao().getRideRowList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<RideRow>>() {

                    @Override
                    public void onSuccess(List<RideRow> rides) {
                        for (RideRow ride : rides) {
                            Timber.d("logFile = " + ride);
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
