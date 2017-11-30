package net.kwatts.powtools.util.debugdrawer;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.kwatts.powtools.App;
import net.kwatts.powtools.R;
import net.kwatts.powtools.RidesListActivity;
import net.kwatts.powtools.database.Attribute;
import net.kwatts.powtools.database.Moment;
import net.kwatts.powtools.database.Ride;
import net.kwatts.powtools.database.RideRow;

import java.util.Calendar;

import io.palaima.debugdrawer.base.DebugModule;

public class DebugDrawerAddDummyRide implements DebugModule {
    private RidesListActivity ridesListActivity;

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
        App.dbExecute(database -> {

            //for (Ride ride : database.rideDao().getAll()) {
            //    Log.d(TAG, "ride = " + ride);
            //}

            // Insert sample rides
            Ride ride = new Ride();
            long rideId = database.rideDao().insert(ride);
            RideRow newRideRow = new RideRow();
            newRideRow.rideId = rideId;

            Calendar calendar = Calendar.getInstance();
            Moment moment;
            int rideLength = (int) (Math.random() * 58);
            for (int i = 0; i < rideLength; i++) {
                calendar.add(Calendar.MINUTE, 1);
                moment = new Moment(rideId, calendar.getTime());

                moment.setGpsLat(37.7891223 + i * .001);
                moment.setGpsLong(-122.4118449 + Math.sin(i) * .001);

                long momentId = database.momentDao().insert(moment);

                Attribute attribute = new Attribute();
                attribute.setMomentId(momentId);
                attribute.setKey(Attribute.KEY_SPEED);
                attribute.setValue("" + i);
                database.attributeDao().insert(attribute);

                attribute = new Attribute();
                attribute.setMomentId(momentId);
                attribute.setKey(Attribute.KEY_PAD1);
                attribute.setValue(Math.random() > .3 ? "true" : null);
                database.attributeDao().insert(attribute);


                attribute = new Attribute();
                attribute.setMomentId(momentId);
                attribute.setKey(Attribute.KEY_PAD2);
                attribute.setValue(Math.random() > .3 ? "true" : null);
                database.attributeDao().insert(attribute);

                if (i == 0) {
                    newRideRow.minEventDate = calendar.getTime();
                } else if (i == rideLength - 1) {
                    newRideRow.maxEventDate = calendar.getTime();
                }
            }

            ridesListActivity.refreshList();

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

    @Override
    public void onPause() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }
}
