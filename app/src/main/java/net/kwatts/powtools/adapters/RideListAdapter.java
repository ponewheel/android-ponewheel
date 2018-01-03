package net.kwatts.powtools.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import net.kwatts.powtools.App;
import net.kwatts.powtools.R;
import net.kwatts.powtools.RideDetailActivity;
import net.kwatts.powtools.database.entities.Moment;
import net.kwatts.powtools.database.entities.Ride;
import net.kwatts.powtools.drawables.TrackDrawable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.Pair;
import timber.log.Timber;

import static net.kwatts.powtools.RideDetailActivity.FILE_FORMAT_DATE;

public class RideListAdapter extends RecyclerView.Adapter<RideListAdapter.RideViewHolder> {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.ENGLISH);
    final Context context;
    private final List<Ride> rides;
    final List<Ride> checkedRides = new ArrayList<>();

    public RideListAdapter(Context context, List<Ride> rides) {
        this.context = context;
        this.rides = rides;
    }
    @Override
    public RideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View ridesListRow = LayoutInflater.from(context).inflate(R.layout.rides_list_row, parent, false);

        return new RideViewHolder(ridesListRow);
    }

    @Override
    public void onBindViewHolder(RideViewHolder holder, int position) {
        Ride ride = rides.get(position);

        holder.bind(ride);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    public List<Ride> getRideList() {
        return rides;
    }

    public List<Ride> getCheckedItems() {
        return checkedRides;
    }

    class RideViewHolder extends RecyclerView.ViewHolder {

        private ImageView thumbnailView;
        private TextView dateView;
        private TextView rideLengthView;
        private CheckBox checkbox;

        RideViewHolder(View itemView) {
            super(itemView);

            thumbnailView = itemView.findViewById(R.id.rides_row_thumbnail);
            dateView = itemView.findViewById(R.id.rides_row_date);
            rideLengthView = itemView.findViewById(R.id.rides_row_length);
            checkbox = itemView.findViewById(R.id.rides_row_checkbox);
        }

        void bind(Ride ride) {
            Timber.d("rideId" + ride.id + " minDate= " + ride.start + " max=" + ride.start);

            loadTrack(ride.id, thumbnailView);

            // TODO only show id in debug builds?
            if (ride.start != null) {
                dateView.setText(SIMPLE_DATE_FORMAT.format(ride.start));
                //dateView.setText(SIMPLE_DATE_FORMAT.format(ride.getMinDate())+ " ("+ride.id + ")");
            }
            rideLengthView.setText(
                    String.format(Locale.getDefault(),"%d%s", ride.getMinuteDuration(), context.getString(R.string.min_duration)));

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, RideDetailActivity.class);
                intent.putExtra(RideDetailActivity.RIDE_ID, ride.id);
                intent.putExtra(RideDetailActivity.RIDE_DATE, FILE_FORMAT_DATE.format(ride.start));
                context.startActivity(intent);
            });

            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(checkedRides.contains(ride));

            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    checkedRides.add(ride);
                } else {
                    checkedRides.remove(ride);
                }
            });
        }
    }

    void loadTrack(long rideId, @NonNull ImageView imageView) {
        App.dbExecute(database -> {
            Timber.d("loadTrack: rideId = " + rideId);
            List<Pair<Double, Double>> track = new ArrayList<>();
            List<Moment> moments = database.momentDao().getFromRide(rideId);
            for (Moment moment : moments) {
                // The standard ordering of geo coordinates (according to ISO 6709) is latitude then
                // longitude, but we swap the ordering here as Android's canvas renders with it's
                // axes labeled in x, y ordering (which is the ordering that TrackDrawable expects).
                //
                // https://en.wikipedia.org/wiki/ISO_6709
                track.add(new Pair<>(moment.getGpsLongDouble(), moment.getGpsLatDouble()));
            }

            new Handler(Looper.getMainLooper()).post(() ->
                    imageView.setImageDrawable(new TrackDrawable(track)));
        });
    }

}
