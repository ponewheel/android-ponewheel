package net.kwatts.powtools.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import net.kwatts.powtools.R;
import net.kwatts.powtools.RideDetailActivity;
import net.kwatts.powtools.database.RideRow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class RideListAdapter extends RecyclerView.Adapter<RideListAdapter.RideViewHolder> {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.ENGLISH);
    final Context context;
    private final List<RideRow> rideRows;
    final List<RideRow> checkedRides = new ArrayList<>();

    public RideListAdapter(Context context, List<RideRow> rideRows) {
        this.context = context;
        this.rideRows = rideRows;
    }
    @Override
    public RideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View ridesListRow = LayoutInflater.from(context).inflate(R.layout.rides_list_row, parent, false);

        return new RideViewHolder(ridesListRow);
    }

    @Override
    public void onBindViewHolder(RideViewHolder holder, int position) {
        RideRow rideRow = rideRows.get(position);

        holder.bind(rideRow);
    }

    @Override
    public int getItemCount() {
        return rideRows.size();
    }

    public List<RideRow> getRideList() {
        return rideRows;
    }

    public List<RideRow> getCheckedItems() {
        return checkedRides;
    }

    class RideViewHolder extends RecyclerView.ViewHolder {

        private TextView dateView;
        private TextView rideLengthView;
        private CheckBox checkbox;

        RideViewHolder(View itemView) {
            super(itemView);

            dateView = itemView.findViewById(R.id.rides_row_date);
            rideLengthView = itemView.findViewById(R.id.rides_row_length);
            checkbox = itemView.findViewById(R.id.rides_row_checkbox);
        }

        void bind(RideRow rideRow) {
            Timber.d("rideId" + rideRow.rideId + " minDate= " + rideRow.minEventDate + " max=" + rideRow.maxEventDate);
            // TODO only show id in debug builds?
            if (rideRow.getMinDate() != null) {
                dateView.setText(SIMPLE_DATE_FORMAT.format(rideRow.getMinDate()));
                //dateView.setText(SIMPLE_DATE_FORMAT.format(rideRow.getMinDate())+ " ("+rideRow.rideId + ")");
            }
            rideLengthView.setText(
                    String.format(Locale.getDefault(),"%d%s", rideRow.getMinuteDuration(), context.getString(R.string.min_duration)));

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, RideDetailActivity.class);
                intent.putExtra(RideDetailActivity.RIDE_ID, rideRow.rideId);
                context.startActivity(intent);
            });

            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(checkedRides.contains(rideRow));

            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    checkedRides.add(rideRow);
                } else {
                    checkedRides.remove(rideRow);
                }
            });
        }
    }

}
