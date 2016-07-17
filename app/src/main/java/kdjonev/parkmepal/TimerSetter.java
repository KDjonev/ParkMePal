package kdjonev.parkmepal;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.Calendar;

/**
 * Created by Krassi on 3/3/2016.
 */
public class TimerSetter extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer_setter);
        loadAdd();
    }

    private void loadAdd()
    {
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    public void donePressed(View v)
    {
        TimePicker t = (TimePicker)findViewById(R.id.timePicker);
        saveTime(t);
        Intent i = new Intent(this, CountdownNotificationService.class);
        this.startService(i);
        finish();
    }

    private long saveTime(TimePicker t)
    {
        int h = t.getCurrentHour();
        int m = t.getCurrentMinute();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long timeStamp = (c.getTimeInMillis() / 1000L); // in seconds
        SharedPreferences settings = getSharedPreferences(MapsActivity.TIME_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("time", timeStamp);
        // Commit the edits!
        editor.commit();
        return timeStamp;
    }

    public void onBackPressed()
    {
        super.onBackPressed();
        finish();
    }
}
