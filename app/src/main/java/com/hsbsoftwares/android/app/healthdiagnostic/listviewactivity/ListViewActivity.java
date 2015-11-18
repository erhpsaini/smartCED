package com.hsbsoftwares.android.app.healthdiagnostic.listviewactivity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

import com.hsbsoftwares.android.app.healthdiagnostic.R;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.Crisi;
import com.hsbsoftwares.android.app.healthdiagnostic.db.helper.DatabaseHandler;

import java.util.List;

public class ListViewActivity extends Activity {

    // Log tag
    private static final String TAG = ListViewActivity.class.getSimpleName();
    private List<Crisi> crisis;
    private ListView listView;
    private CustomListAdapter adapter;
    private ProgressDialog pDialog;
    private static DatabaseHandler databaseHandler;
    //App's context
    private static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        setupActionBar();

        //pDialog = new ProgressDialog(this);
        // Showing progress dialog before making http request
        //pDialog.setMessage("Loading...");
        //pDialog.show();
        listView = (ListView) findViewById(R.id.list);

        try{
            databaseHandler = DatabaseHandler.getInstance(this);
            crisis = databaseHandler.getAllCrisis();
            if(!crisis.isEmpty()){
                adapter = new CustomListAdapter(this, crisis);
                listView.setAdapter(adapter);
            }
        }catch (NullPointerException e) {
            throw new IllegalStateException("Data base is empty!", e);
        }

        // changing action bar color
        //getActionBar().setBackgroundDrawable(
        //      new ColorDrawable(Color.parseColor("#1b1b1b")));
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        hidePDialog();
    }
    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }
    public static Context getAppContext(){
        return mContext.getApplicationContext();
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