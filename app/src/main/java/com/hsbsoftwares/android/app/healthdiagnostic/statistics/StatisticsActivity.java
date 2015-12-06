package com.hsbsoftwares.android.app.healthdiagnostic.statistics;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.hsbsoftwares.android.app.healthdiagnostic.R;
import com.hsbsoftwares.android.app.healthdiagnostic.statistics.webview.*;

public class StatisticsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        setupActionBar();
    }
    public void openNumberCrisisPerDay(View view){
        startActivity(new Intent(this, ChartNumberCrisisPerDay.class));
    }
    public void openNumberCrisisPerMonth(View view){
        startActivity(new Intent(this, ChartNumberCrisisPerMonth.class));
    }
    public void openNumberCrisisPerYear(View view){
        startActivity(new Intent(this, ChartNumberCrisisPerYear.class));
    }
    public void openNumberCrisisPerState(View view){
        startActivity(new Intent(this, ChartNumberCrisisPerState.class));
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure.
            //Back button functionality
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
