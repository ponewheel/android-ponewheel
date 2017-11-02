package net.kwatts.powtools;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.kwatts.powtools.loggers.PlainTextFileLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

public class RideDetailActivity extends AppCompatActivity {

    public static final String FILE_NAME = "EXTRA_DATA_FILE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);

        LineChart lineChart = (LineChart) findViewById(R.id.ride_detail_speed_chart);


        String fileName = getIntent().getStringExtra(FILE_NAME);

        String logFileLocation = PlainTextFileLogger.getLoggingPath() + "/" + fileName;
        System.out.println("logFile = " + logFileLocation);

        BufferedReader bufferedReader = null;
        FileReader fileReader = null;
        ArrayList<Entry> values = new ArrayList<>();

        try {

            fileReader = new FileReader(logFileLocation);
            bufferedReader = new BufferedReader(fileReader);

            String currentLine;

            String headerLine = bufferedReader.readLine();
            Long referenceTime = null;
            while ((currentLine = bufferedReader.readLine()) != null) {
//                System.out.println(currentLine);
                StringTokenizer stringTokenizer = new StringTokenizer(currentLine, ",");
                Date date = OWDevice.SIMPLE_DATE_FORMAT.parse(stringTokenizer.nextToken());

                String speed = stringTokenizer.nextToken();
                long time = date.getTime();
                if (referenceTime == null) {
                    referenceTime = time;
                }
                time = time - referenceTime;
                values.add(new Entry(time, Float.valueOf(speed)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                if (bufferedReader != null)
                    bufferedReader.close();

                if (fileReader != null)
                    fileReader.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
        System.out.println("values.size() = " + values.size());
        LineDataSet dataSet = new LineDataSet(values, "Label");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(ColorTemplate.getHoloBlue());
        dataSet.setValueTextColor(ColorTemplate.getHoloBlue());
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(ColorTemplate.getHoloBlue());
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawCircleHole(false);


        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();



        lineChart.getDescription().setEnabled(false);

        // enable touch gestures
        lineChart.setTouchEnabled(true);

        lineChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setViewPortOffsets(0f, 0f, 0f, 0f);

        // add data
        lineChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = lineChart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
//        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                long minutes = TimeUnit.MILLISECONDS.toMinutes((long) value);
                return minutes +"m";
            }
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
//        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
//        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }
}
