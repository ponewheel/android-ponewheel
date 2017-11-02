package net.kwatts.powtools;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import net.kwatts.powtools.loggers.PlainTextFileLogger;

import java.io.File;

public class RidesListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_viewer);

        ListView listView = (ListView) findViewById(R.id.ride_list_view);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        String loggingPath = PlainTextFileLogger.getLoggingPath();
        File owLogFileDir = new File(loggingPath);
        final String[] logFiles = owLogFileDir.list();
        for (String logFile : logFiles) {
            System.out.println("logFile = " + logFile);
        }
        adapter.addAll(logFiles);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(RidesListActivity.this, RideDetailActivity.class);
                intent.putExtra(RideDetailActivity.FILE_NAME, logFiles[position]);
                startActivity(intent);
            }
        });

    }


}
