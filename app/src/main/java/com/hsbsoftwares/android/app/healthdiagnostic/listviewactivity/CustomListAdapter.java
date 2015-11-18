package com.hsbsoftwares.android.app.healthdiagnostic.listviewactivity;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hsbsoftwares.android.app.healthdiagnostic.R;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.Crisi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Bigsony on 18/11/2015.
 */
public class CustomListAdapter extends BaseAdapter {
    private Activity activity;
    private LayoutInflater inflater;
    private List<Crisi> crisiItems;

    public CustomListAdapter(Activity activity, List<Crisi> crisiItems) {
        this.activity = activity;
        this.crisiItems = crisiItems;
    }

    @Override
    public int getCount() {
        return crisiItems.size();
    }

    @Override
    public Object getItem(int position) {
        return crisiItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.list_row, null);

        /*
        if (imageLoader == null)
            imageLoader = AppController.getInstance().getImageLoader();
        NetworkImageView thumbNail = (NetworkImageView) convertView
                .findViewById(R.id.thumbnail);
                */
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView rating = (TextView) convertView.findViewById(R.id.rating);
        TextView genre = (TextView) convertView.findViewById(R.id.genre);
        TextView year = (TextView) convertView.findViewById(R.id.releaseYear);

        //SimpleDateFormat  sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.getDefault());
        SimpleDateFormat  sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // getting movie data for the row
        Crisi c = crisiItems.get(getCount()-position-1);

        // thumbnail image
        //thumbNail.setImageUrl(m.getThumbnailUrl(), imageLoader);

        // title
        title.setText("Crisi: " + c.getId());

        // rating
        //rating.setText("Start date: " + String.valueOf(c.getStartDate()));
        try {
            Date date = sdf.parse(c.getStartDate());
            SimpleDateFormat sdfOut = new SimpleDateFormat("EEE d MMM yyyy, HH:mm:ss");
            rating.setText("Start date: " + sdfOut.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // genre
        Date d1 = null;
        Date d2 = null;
        try {

            d1 = sdf.parse(c.getStartDate());
            d2 = sdf.parse(c.getEndDate());

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);
            Log.i("CustomListAdapter ", "Elapsed Time: d1=" + d1 + " d2=" + d2 + " diff =" + diff);

            //genre.setText("Elapsed Time: " + diff);
            //genre.setText("Elapsed Time: " + diffHours + "h" + diffMinutes + "m" + diffSeconds + "s");
            genre.setText("Elapsed Time: " + getElapsedTime(d1, d2));

            //genre.setText("Elapsed Time: " + duree);
            //genre.setText("Elapsed Time: " + heure + "h" + minute + "m" + seconde + "s");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //genre.setText("Elapsed Time: "+ String.valueOf(getElapsedTime(c.getStartDate(), c.getEndDate())));
        /*
        String genreStr = "";
        for (String str : m.getGenre()) {
            genreStr += str + ", ";
        }
        genreStr = genreStr.length() > 0 ? genreStr.substring(0,
                genreStr.length() - 2) : genreStr;
        genre.setText(genreStr);
        */

        // release year
        //year.setText("End Date: " + String.valueOf(c.getEndDate()));
        try {
            Date date = sdf.parse(c.getEndDate());
            SimpleDateFormat sdfOut = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss");
            year.setText("End Date: " + sdfOut.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return convertView;
    }

    public Date convertStringToDate(String dateString){
        Date date = null;
        SimpleDateFormat  sdf = new SimpleDateFormat("dd-MMM-yyyy");
        try{
            date = sdf.parse(dateString);
            //formatteddate = sdf.format(date);
        }
        catch ( Exception ex ){
            //System.out.println(ex);
        }
        return date;
    }

    public static String getElapsedTime(Date startDate, Date endDate){
        //N'oublie pas de tester si les dates ne sont pas null et que la fin soit supérieur au début.
        /*SimpleDateFormat  sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startDate = null;
        Date endDate = null;

        try {
            startDate = sdf.parse(_startDate);
            endDate = sdf.parse(_startDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        */
        StringBuilder dateDiff = new StringBuilder();
        boolean hasPrev = false;

        int diffMilliSec = (int)(endDate.getTime() - startDate.getTime()) ;
        int diffSeconds = diffMilliSec / 1000;


        int diffDays =  diffSeconds / (24 * 60 * 60);
        if ( diffDays > 0 )
        {
            dateDiff.append(diffDays + " Jours");
            hasPrev = true;
        }

        int diffHours =  (diffSeconds / (60 * 60)) % 24;
        if ( diffHours > 0 || hasPrev )
        {
            dateDiff.append(" " + diffHours + "h");
            hasPrev = true;
        }

        int diffMinutes =  (diffSeconds / 60) % 60;

        if ( diffMinutes > 0  || hasPrev )
        {
            dateDiff.append(" " + diffMinutes + "m");
            hasPrev = true;
        }

        diffSeconds =  diffSeconds  % 60;
        if ( diffSeconds > 0 || hasPrev )
        {
            dateDiff.append(" " + diffSeconds + "s");
            hasPrev = true;
        }

        return dateDiff.toString();
    }
}

