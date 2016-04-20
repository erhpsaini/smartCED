package com.hsbsoftwares.android.app.healthdiagnostic.listviewactivity;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hsbsoftwares.android.app.healthdiagnostic.R;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.Crisis;

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
    private List<Crisis> crisisItems;

    public CustomListAdapter(Activity activity, List<Crisis> crisisItems) {
        this.activity = activity;
        this.crisisItems = crisisItems;
    }

    @Override
    public int getCount() {
        return crisisItems.size();
    }

    @Override
    public Object getItem(int position) {
        return crisisItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Keep all Images in array
    public String getcurrentPhotoPathList(int position) {
        Crisis c = crisisItems.get(getCount()-position-1);
        return c.getCurrentPhotoPath();
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
        TextView startDate = (TextView) convertView.findViewById(R.id.startDate);
        TextView elapsedTime = (TextView) convertView.findViewById(R.id.elapsedTime);
        TextView endDate = (TextView) convertView.findViewById(R.id.endDate);
        TextView country = (TextView) convertView.findViewById(R.id.country);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.thumbnail);
        //TextView locality = (TextView) convertView.findViewById(R.id.locality);

        //SimpleDateFormat  sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.getDefault());
        SimpleDateFormat  sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // getting movie data for the row
        Crisis c = crisisItems.get(getCount()-position-1);
        //Crisis c = crisisItems.get(position);

        // thumbnail image
        //thumbNail.setImageUrl(m.getThumbnailUrl(), imageLoader);
        imageView.setImageURI(Uri.parse(c.getCurrentPhotoPath()));
        //currentPhotoPathList.add(c.getCurrentPhotoPath());

        // title
        title.setText("Crisis: " + c.getId());
        // country and locality
        country.setText("Country: " + c.getLocality() + ", " + c.getCountry());
        // locality
        //locality.setText("Locality: " + c.getLocality());

        // rating
        //rating.setText("Start date: " + String.valueOf(c.getStartDate()));
        try {
            Date date = sdf.parse(c.getStartDate());
            SimpleDateFormat sdfOut = new SimpleDateFormat("EEE d MMM yyyy, HH:mm:ss");
            startDate.setText("Start date: " + sdfOut.format(date));
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
//            long diff = d2.getTime() - d1.getTime();
//
//            long diffSeconds = diff / 1000 % 60;
//            long diffMinutes = diff / (60 * 1000) % 60;
//            long diffHours = diff / (60 * 60 * 1000) % 24;
//            long diffDays = diff / (24 * 60 * 60 * 1000);
//            Log.i("CustomListAdapter ", "Elapsed Time: d1=" + d1 + " d2=" + d2 + " diff =" + diff);

            //genre.setText("Elapsed Time: " + diff);
            //genre.setText("Elapsed Time: " + diffHours + "h" + diffMinutes + "m" + diffSeconds + "s");
            elapsedTime.setText("Elapsed Time: " + getElapsedTime(d1, d2));

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
            SimpleDateFormat sdfOut = new SimpleDateFormat("EEE d MMM yyyy, HH:mm:ss");
            endDate.setText("End Date: " + sdfOut.format(date));
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

