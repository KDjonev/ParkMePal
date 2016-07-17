package kdjonev.parkmepal;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    public static final String LATLNG_PREFS = "LatLngPrefs";
    public static final String TIME_PREF = "TimePrefs";

    private GoogleMap mMap;
    CountDownTimer countDownTimer = null;
    private ParkMe parkMe;
    private boolean foreground = false;

    private final int NOTIFICATION_ID = 001;
    NotificationManager mNotifyMgr;
    //RemoteViews remoteViews;
    NotificationCompat.Builder mBuilder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        loadAdd();


    }

    private void loadAdd()
    {
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.reParkButton:
                rePark();
                return;
            case R.id.setTimerButton:
                setTimer();
                return;
            case R.id.takeMeButton:
                takeMe();
                return;

        }
    }

    private void rePark()
    {
        parkMe = new ParkMe(mMap, getBaseContext());
    }

    private void setTimer()
    {
        startActivity(new Intent(this, TimerSetter.class));
    }

    private void takeMe()
    {
        Intent i = new Intent(this, TakeMeMyWay.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(parkMe.getParkedLocation() != null)
        {
            double lat = parkMe.getParkedLocation().latitude;
            double lon = parkMe.getParkedLocation().longitude;
            i.putExtra("lat", lat);
            i.putExtra("lon", lon);
        }
        startActivity(i);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        returnRescourses();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        foreground = false;
        saveParkedSpot();
        stopCountdown();
        setUpNotification();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        gpsCheck();
        foreground = true;
        restoreTime();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadParkedMap();

    }

    private void loadParkedMap()
    {
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(LATLNG_PREFS, MODE_PRIVATE);
        String lat = settings.getString("lat", "");
        String lon = settings.getString("lon", "");
        //no previus data
        if(lat == "" || lon == "")
        {
            parkMe = new ParkMe(mMap, getBaseContext());
        }
        //reload previus data
        else
        {
            LatLng latlng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
            parkMe = new ParkMe(mMap,getBaseContext(),latlng);
        }
    }

    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        exitOut();
                    }
                }).create().show();
    }


    private void gpsCheck()
    {
        int off = 0;
        try {
            off = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if(off==0){
            new AlertDialog.Builder(this)
                    .setTitle("Turn Location On?")
                    .setMessage("You must turn On your Location.")
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            exitOut();
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(onGPS);
                        }
                    }).create().show();
        }
    }

    private void exitOut()
    {
        returnRescourses();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void startCountdown(long seconds)
    {
        final TextView text = (TextView)findViewById(R.id.timeLeft);
        countDownTimer = new CountDownTimer(seconds*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % TimeUnit.MINUTES.toSeconds(1));
                if(foreground) {
                    text.setText("Time Left (hh:mm:ss): " + hms);
                }
            }

            public void onFinish() {
                text.setText("done!");
            }
        }.start();
    }

    private void stopCountdown()
    {
        if(countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            resetCountdownText();
        }
    }

    private void resetCountdownText()
    {
        final TextView text = (TextView)findViewById(R.id.timeLeft);
        text.setText("Time Left (hh:mm:ss): " + "00:00:00");
    }



    private void returnRescourses()
    {
        mNotifyMgr.cancel(NOTIFICATION_ID);
        stopCountdown();
    }


    private void saveParkedSpot()
    {
        SharedPreferences settings = getSharedPreferences(LATLNG_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("lat", Double.toString(parkMe.getParkedLocation().latitude));
        editor.putString("lon", Double.toString(parkMe.getParkedLocation().longitude));
        // Commit the edits!
        editor.commit();
    }

    private void restoreTime()
    {
        SharedPreferences settings = getSharedPreferences(TIME_PREF, MODE_PRIVATE);
        long time = settings.getLong("time", -1);
        //no previus data
        if(time == -1)
        {
            resetCountdownText();
        }
        //reload previus data
        else
        {
            Calendar c = Calendar.getInstance();
            long currentTime = (c.getTimeInMillis() / 1000L); // in seconds
            long secondsToCount = time - currentTime;
            if(secondsToCount > 0) {
                startCountdown(secondsToCount);
            }
            else
            {
                resetCountdownText();
                getBaseContext().getSharedPreferences(TIME_PREF, MODE_PRIVATE).edit().clear().apply();
            }
        }
    }

    private void setUpNotification()
    {
        SharedPreferences settings = getSharedPreferences(TIME_PREF, MODE_PRIVATE);
        long time = settings.getLong("time", -1);
        Calendar c = Calendar.getInstance();
        long currentTime = (c.getTimeInMillis() / 1000L); // in seconds
        long secondsToCount = time - currentTime;
        String hm;
        String contentText = "No Time Set";
        boolean onGoing = false;
        if(time == -1 || secondsToCount < 0)
        {
        }
        else
        {
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.setTimeInMillis(time * 1000);
            hm = String.format("%02d:%02d", cal.get(Calendar.HOUR),
                    cal.get(Calendar.MINUTE));
            String am_pm = "AM";
            if(cal.get(Calendar.AM_PM) == 1)
            {
                am_pm = "PM";
            }
            contentText = "Time Set for: " + hm + am_pm;
        }
        //remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
        //remoteViews.setTextViewText(R.id.timeLeftNotification,"00:00");
        //remoteViews.setTextColor(R.id.timeLeftNotification, getResources().getColor(R.color.black));
        mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.park_me_icon)
                        .setContentTitle("ParkMePal")
                        .setContentText(contentText)
        //.setContent(remoteViews)
        ;
        final Intent resultIntent = new Intent(this, MapsActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this,0,resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setOngoing(onGoing);

        // Sets an ID for the notification
        int mNotificationId = NOTIFICATION_ID;
        // Gets an instance of the NotificationManager service
        mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }


}
