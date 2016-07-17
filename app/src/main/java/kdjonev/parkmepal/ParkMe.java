package kdjonev.parkmepal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Krassi on 3/3/2016.
 */
public class ParkMe {
    private final float ZOOM_LEVEL = 18.5f; //goes up to 21
    private GoogleMap map;
    private Context context;
    private LatLng startLocation = null;

    public ParkMe(GoogleMap m, Context c)
    {
        map = m;
        m.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        context = c;
        Intent i = new Intent(c, StartLocationGathererService.class);
        context.startService(i);

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver,
                new IntentFilter("LocationChanged"));
    }

    public ParkMe(GoogleMap m, Context c, LatLng latLng)
    {
        map = m;
        m.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        context = c;
        startLocation = latLng;
        placeMarker(latLng);
    }

    // Our handler for received Intents. This will be called whenever an Intent
// with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Double lat = intent.getDoubleExtra("lat", 0);
            Double lon = intent.getDoubleExtra("lon", 0);
            startLocation = new LatLng(lat,lon);
            placeMarker(startLocation);
            stopLocations();

            Log.i("locationtesting", "location recieved: " + " lat: " + lat + " lon: " + lon);
        }
    };

    public void placeMarker(LatLng latLng)
    {
        int deviceW = context.getResources().getDisplayMetrics().widthPixels;
        int deviceH = context.getResources().getDisplayMetrics().heightPixels;
        int scale = Math.min(deviceH,deviceW);
        Log.i("size", "width = " + deviceW + " and height = " + deviceH);
        int newWidth = scale/10;
        int newHeight = scale/10;
        Log.i("size", "width = " + newWidth + " and height = " + newHeight);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        Bitmap b = resizeBitmap(R.drawable.biker, newWidth, newHeight);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(b));
        markerOptions.title("Parked Here");
        map.clear();

        map.addMarker(markerOptions);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL));
    }

    private Bitmap resizeBitmap(int id, int w, int h)
    {
        Bitmap bitMap = BitmapFactory.decodeResource(context.getResources(), id, null);
        Bitmap resized = Bitmap.createScaledBitmap(bitMap, w, h, true);
        return resized;
    }


    private void stopLocations()
    {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
        context.stopService(new Intent(context, StartLocationGathererService.class));
    }

    public LatLng getParkedLocation()
    {
        return startLocation;
    }
}
