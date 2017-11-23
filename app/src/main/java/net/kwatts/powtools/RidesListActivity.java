package net.kwatts.powtools;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import net.kwatts.powtools.database.Ride;

public class RidesListActivity extends AppCompatActivity {

    public static final String TAG = "RidesListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_viewer);

        ListView listView = findViewById(R.id.ride_list_view);
        ArrayAdapter<Ride> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        Single.fromCallable(() -> App.INSTANCE.db.rideDao().getAll())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<Ride>>() {

                    @Override
                    public void onSuccess(List<Ride> rides) {
                        for (Ride ride : rides) {
                            System.out.println("logFile = " + ride);
                        }
                        adapter.addAll(rides);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: ", e);
                    }
                });

        if (listView != null) {
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(RidesListActivity.this, MapActivity.class);

                Ride ride = adapter.getItem(position);
                assert ride != null;
                //FIXME intent.putExtra(RidesListActivity.FILE_NAME, ride.id);
                startActivity(intent);
            });
        }

    }


}
