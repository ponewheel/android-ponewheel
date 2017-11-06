package net.kwatts.powtools;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.kwatts.powtools.loggers.PlainTextFileLogger;

import java.util.ArrayList;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.concurrent.TimeUnit;

public class RideDetailActivity extends AppCompatActivity {

    public static final String FILE_NAME = "EXTRA_DATA_FILE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);

        String fileName = getIntent().getStringExtra(FILE_NAME);
        ArrayList<Entry> values = new ArrayList<>();


        // TODO convert to async
        PlainTextFileLogger.getEntriesFromFile(fileName, values, null);
        System.out.println("values.size() = " + values.size());
    }


}
