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
import net.kwatts.powtools.database.entities.Ride;
import net.kwatts.powtools.util.ProgressDialogHandler;
import net.kwatts.powtools.util.debugdrawer.DebugDrawerAddDummyRide;

import java.util.ArrayList;
import java.util.List;

import io.palaima.debugdrawer.DebugDrawer;
import io.palaima.debugdrawer.commons.SettingsModule;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class RidesListActivity extends AppCompatActivity {

    public static final String TAG = RidesListActivity.class.getSimpleName();
    public static final int MENU_ITEM_DELETE = 0;
    public static final int MENU_ITEM_SELECT_ALL = 1;

    RideListAdapter rideListAdapter;
    private Disposable disposable;
    private ProgressDialogHandler progressDialogHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rides_list);

        setupToolbar();
        progressDialogHandler = new ProgressDialogHandler(this);

        RecyclerView recyclerView = findViewById(R.id.ride_list_view);
        rideListAdapter = new RideListAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        refreshList();

        recyclerView.setAdapter(rideListAdapter);


        DebugDrawerAddDummyRide debugDrawerAddDummyRide = new DebugDrawerAddDummyRide(this);
        new DebugDrawer.Builder(this)
                .withTheme(R.style.Theme_AppCompat_Light)
                .backgroundColorRes(R.color.background_material_light)
                .modules(
                        debugDrawerAddDummyRide,
                        new SettingsModule()
                ).build();

        getLifecycle().addObserver(debugDrawerAddDummyRide);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0, MENU_ITEM_DELETE, 0, "Delete");
        menu.add(0, MENU_ITEM_SELECT_ALL, 1, "Select All");

        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE:
                deleteSelectedRides();

                break;
            case MENU_ITEM_SELECT_ALL:
                selectAll();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void selectAll() {
        for (Ride ride : rideListAdapter.getRideList()) {
            if (!rideListAdapter.getCheckedItems().contains(ride)) {
                rideListAdapter.getCheckedItems().add(ride);
            }
        }
        rideListAdapter.notifyDataSetChanged();
    }

    private void deleteSelectedRides() {
        progressDialogHandler.show();
        App.dbExecute(database -> {
            final List<Ride> checkedItems = rideListAdapter.getCheckedItems();

            rideListAdapter.getRideList().removeAll(checkedItems);
            database.rideDao().delete(checkedItems);
            rideListAdapter.getCheckedItems().clear();

            runOnUiThread(() -> {
                rideListAdapter.notifyDataSetChanged();
                progressDialogHandler.dismiss();
            });
        });
    }


    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setTitle("Logged Rides");
        toolbar.setLogo(R.mipmap.ic_launcher);

        setSupportActionBar(toolbar);

    }


    public void refreshList() {
        disposable = Single.fromCallable(() -> {
                    Timber.d("fetching ride row list");
                    List<Ride> rideRowList = App.INSTANCE.db.rideDao().getAll();
                    Timber.d("rideRowList.size() = " + rideRowList.size());
                    return rideRowList;

                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<Ride>>() {

                    @Override
                    public void onSuccess(List<Ride> rides) {
                        for (Ride ride : rides) {
                            Timber.d("ride = " + ride);
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

    @Override
    protected void onPause() {
        super.onPause();
        disposable.dispose();
    }

    public RideListAdapter getRideListAdapter() {
        return rideListAdapter;
    }

}
