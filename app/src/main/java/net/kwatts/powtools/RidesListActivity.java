package net.kwatts.powtools;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.kwatts.powtools.database.Ride;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RidesListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_viewer);

        ListView listView = findViewById(R.id.ride_list_view);
        ArrayAdapter<Ride> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        Single.fromCallable(() -> App.INSTANCE.db.rideDao().getAll())
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleObserver<List<Ride>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

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

                    }
                });

        if (listView != null) {
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(RidesListActivity.this, MapActivity.class);

                Ride ride = adapter.getItem(position);
                assert ride != null;
                intent.putExtra(RideDetailActivity.FILE_NAME, ride.id);
                startActivity(intent);
            });
        }

    }


}
