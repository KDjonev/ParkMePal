package kdjonev.parkmepal;

/**
 * Created by Krassi on 3/3/2016.
 */
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class StartLocationGathererService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "LocatonGathererService";

    private boolean currentlyProcessingLocation = false;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location currentLocation = null;
    private final int BURST_NUMBER = 10;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!currentlyProcessingLocation) {
            currentlyProcessingLocation = true;
            buildGoogleApiClient();
        }

        return START_NOT_STICKY;
    }

    private void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient");

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        } else {
            Log.e(TAG, "unable to connect to google play services.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.e(TAG, "position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());
            currentLocation = location;

            // we have our desired accuracy of 5 meters so lets quit this service,
            // onDestroy will be called and stop our location uodates
            if (location.getAccuracy() <= 50f) {
                Intent i = new Intent("LocationChanged");
                sendLocationBroadcast(i);
            }
        }
    }

    private void stopLocationUpdates() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(20); // milliseconds
        locationRequest.setFastestInterval(10); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        getLocationUpdate();
    }

    private void getLocationUpdate()
    {

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        } catch (SecurityException e) {
            e.printStackTrace();
        }
        if(currentLocation != null && currentLocation.getAccuracy() <= 25f) {
            prepareForBroadcast();
        }
        else {
            Location tmp = null;
            int counter = 0;
            while (counter++ < BURST_NUMBER) {
                try {
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            googleApiClient, locationRequest, this);
                    tmp = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    if (tmp.getAccuracy() > currentLocation.getAccuracy()) {
                        currentLocation = tmp;
                        Log.e(TAG, "********replaced");
                    }

                    Log.e(TAG, "noReplaced");
                    Log.e(TAG, "position: " + getLocation().getLatitude() + ", " + getLocation().getLongitude() + " accuracy: " + getLocation().getAccuracy());


                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            if (currentLocation != null && currentLocation.getAccuracy() <= 50f) {
                prepareForBroadcast();
            }
        }
    }

    private void prepareForBroadcast()
    {
        Log.e(TAG, "position: " + getLocation().getLatitude() + ", " + getLocation().getLongitude() + " accuracy: " + getLocation().getAccuracy());
        Intent i = new Intent("LocationChanged");
        sendLocationBroadcast(i);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");

        stopLocationUpdates();
        stopSelf();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient connection has been suspend");
    }

    private void sendLocationBroadcast(Intent intent){
        intent.putExtra("lat", getLocation().getLatitude());
        intent.putExtra("lon", getLocation().getLongitude());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.i("locationtesting", "location sent: " + " lat: " + getLocation().getLatitude() + " lon: " + getLocation().getLongitude());
        stopLocationUpdates();
        stopSelf();
    }

    public Location getLocation()
    {
        return currentLocation;
    }
}