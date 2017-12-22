package net.kwatts.powtools.util.debugdrawer;

import android.app.ProgressDialog;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.kwatts.powtools.App;
import net.kwatts.powtools.R;
import net.kwatts.powtools.RidesListActivity;
import net.kwatts.powtools.database.entities.Attribute;
import net.kwatts.powtools.database.entities.Moment;
import net.kwatts.powtools.database.entities.Ride;
import net.kwatts.powtools.model.OWDevice;
import net.kwatts.powtools.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.palaima.debugdrawer.base.DebugModule;
import timber.log.Timber;

public class DebugDrawerAddDummyRide implements DebugModule, LifecycleObserver {
    private RidesListActivity ridesListActivity;
    private ProgressDialog progressDialog;

    public DebugDrawerAddDummyRide(RidesListActivity mainActivity) {
        this.ridesListActivity = mainActivity;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        View view = inflater.inflate(R.layout.debug_drawer_add_ride, parent, false);

        Button mockBle = view.findViewById(R.id.debug_drawer_add_ride);
        mockBle.setOnClickListener(v -> {
            insertSampleRidesOrDebug();
        });

        return view;
    }


    private void insertSampleRidesOrDebug() {
        progressDialog = Util.showProgressDialog(ridesListActivity);

        App.dbExecute(database -> {

            //for (Ride ride : database.rideDao().getAll()) {
            //    Log.d(TAG, "ride = " + ride);
            //}

            // Insert sample rides
            Ride ride = new Ride();
            long rideId = database.rideDao().insert(ride);
            ride.id = rideId;
            Timber.d("rideId = " + rideId);
            Calendar calendar = Calendar.getInstance();
            Moment moment;
            int rideLength = 900;
//            int rideLength = (int) (Math.random() * 90);
            Timber.d("rideLength = " + rideLength);
            List<Attribute> attributes = new ArrayList<>();
            List<Moment> moments = new ArrayList<>();
            for (int i = 0; i < rideLength; i++) {
                calendar.add(Calendar.MINUTE, 1);
                moment = new Moment(rideId, calendar.getTime());

                moment.setGpsLat(37.7891223 + i * .001);
                moment.setGpsLong(-122.4118449 + Math.sin(i) * .001);

                moments.add(moment);
                Attribute attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_SPEED);
                attribute.setValue("" + i);
                attributes.add(attribute);

                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_RIDER_DETECTED_PAD_1);
                attribute.setValue(Math.random() > .3 ? "true" : null);
                attributes.add(attribute);

                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_RIDER_DETECTED_PAD_2);
                attribute.setValue(Math.random() > .3 ? "true" : null);
                attributes.add(attribute);


                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_CONTROLLER_TEMP);
                attribute.setValue("" + (Math.sin(i) * 10.0 + 80));
                attributes.add(attribute);


                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_MOTOR_TEMP);
                attribute.setValue("" + (Math.sin(i) * 20.0 + 90));
                attributes.add(attribute);


                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_BATTERY);
                attribute.setValue("" + Util.linearTransform(i, 0, rideLength, 100, 0));
                attributes.add(attribute);


                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_BATTERY_VOLTAGE);
                attribute.setValue("" + Util.linearTransform(i, 0, rideLength, 53.6, 43.1));
                attributes.add(attribute);


                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_TRIP_AMPS);
                attribute.setValue("" + Util.linearTransform(i, 0, rideLength, 0, 3000));
                attributes.add(attribute);


                attribute = new Attribute();
                attribute.setMoment(moment);
                attribute.setKey(OWDevice.KEY_CURRENT_AMPS);
                attribute.setValue("" + Util.linearTransform(i, 0, rideLength, 0, 3000));
                attributes.add(attribute);

                if (i == 0) {
                    ride.start = calendar.getTime();
                } else if (i == rideLength - 1) {
                    ride.end = calendar.getTime();
                }
            }
            database.rideDao().updateRide(ride);
            database.momentDao().insertAll(moments);
            database.attributeDao().insertAllCascadeMoment(attributes);

            List<Ride> rideList = ridesListActivity.getRideListAdapter().getRideList();
            rideList.add(ride);

            ridesListActivity.runOnUiThread(() -> {
                ridesListActivity.getRideListAdapter().notifyItemInserted(rideList.size()-1);
                progressDialog.dismiss();
            });
        });
    }

    @Override
    public void onOpened() {

    }

    @Override
    public void onClosed() {

    }

    @Override
    public void onResume() {

    }

    @Override @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Timber.d("onPause: ");
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onStart() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Override
    public void onStop() {
        Timber.d("onStop: ");
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
