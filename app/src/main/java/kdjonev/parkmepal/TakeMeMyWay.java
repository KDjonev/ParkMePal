package kdjonev.parkmepal;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TakeMeMyWay extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    public static final String TAG = TakeMeMyWay.class.getSimpleName();

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final long INTERVAL = 1000*1; //milliseconds
    private final long FASTEST_INTERVAL = 1000*1; //milliseconds
    private final float SMALLEST_DISPLACEMENT = 5f; //meters

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    Marker mCurrLocationMarker = null;
    Marker mStartLocationMarker = null;
    Location mCurrentLocation = null;
    LatLng mParkedLatLng = null;
    boolean foreground = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("runnin", "onCreate start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_me_my_way);
        double latExtra = getIntent().getExtras().getDouble("lat", 0);
        double lonExtra = getIntent().getExtras().getDouble("lon", 0);
        if(latExtra !=0 && lonExtra !=0)
        {
            mParkedLatLng = new LatLng(latExtra,lonExtra);
        }
        setUpMapIfNeeded();
        buildGoogleApiClient();
        loadAdd();

        Log.i("runnin", "onCreate finished");

    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
    }

    private void loadAdd()
    {
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient");

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "unable to connect to google play services.");
        }
    }

    private void createLocationRequest()
    {
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(SMALLEST_DISPLACEMENT)
                .setInterval(INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        try {
            mMap.setMyLocationEnabled(true);
        }
        catch(SecurityException e)
        {
            e.printStackTrace();
        }
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


    @Override
    protected void onResume() {
        super.onResume();
        Log.i("runnin", "onResume start");
        gpsCheck();
        setUpMapIfNeeded();
        restoreRescourses();
        foreground = true;

        Log.i("runnin", "onResume finish");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("runnin", "onPause start");
        foreground = false;
        returnRescourses();
        Log.i("runnin", "onPause finsished");
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        Log.i("runnin", "setUpMapIFneeded start");
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
        Log.i("runnin", "setUpMapIFneeded finish");
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        if(mParkedLatLng != null)
        {
            placeMarker(mParkedLatLng, Icon.CAR);
        }
        Log.i("runnin", "setUpMap start");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("runnin", "onConnected start");
        createLocationRequest();
        startLocationRequest();
        fixZoom();
        Log.i("runnin", "onConnected finsihed");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("runnin", "onConnectionSuspended start");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    /*
     * Google Play services can resolve some errors it detects.
     * If the error has a resolution, try sending an Intent to
     * start a Google Play services activity that can resolve
     * error.
     */
        Log.i("runnin", "onConnectionFailed start");
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            /*
             * Thrown if Google Play services canceled the original
             * PendingIntent
             */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
        /*
         * If no resolution is available, display a dialog to the
         * user with the error.
         */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
        Log.i("runnin", "onConnectionFailed finsih");
    }



    @Override
    public void onLocationChanged(Location location) {
        Log.i("runnin", "********onLocationChanged start");
        handleNewLocation(location);

        Log.i("runnin", "********onLocationChanged finish");

    }

    public void handleNewLocation(Location location) {
        Log.i("runnin", "handleNewLocation start");
        Log.d(TAG, location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        placeMarker(latLng, Icon.PERSON);
        Log.i("runnin", "handleNewLocation finish");
    }


    public void placeMarker(LatLng latLng, Icon i)
    {
        int deviceW = getResources().getDisplayMetrics().widthPixels;
        int deviceH = getResources().getDisplayMetrics().heightPixels;
        int scale = Math.min(deviceH,deviceW);
        Log.i("size", "width = " + deviceW + " and height = " + deviceH);
        int newWidth = scale/10;
        int newHeight = scale/10;
        Log.i("size", "width = " + newWidth + " and height = " + newHeight);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("You are Here");
        Bitmap b;

        switch(i)
        {
            case CAR:
                b = resizeBitmap(R.drawable.biker, newWidth, newHeight);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(b));
                markerOptions.title("Car Parked Here");
                mStartLocationMarker = mMap.addMarker(markerOptions);
                return;
            case PERSON:
                b = resizeBitmap(R.drawable.hiker, newWidth, newHeight);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(b));
                mCurrLocationMarker = mMap.addMarker(markerOptions);
                return;
        }

    }

    private Bitmap resizeBitmap(int id, int w, int h)
    {
        Bitmap bitMap = BitmapFactory.decodeResource(getResources(), id, null);
        Bitmap resized = Bitmap.createScaledBitmap(bitMap, w, h, true);
        return resized;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        returnRescourses();

    }

    private void returnRescourses()
    {


        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void startLocationRequest()
    {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        } catch (SecurityException e) {
            e.printStackTrace();
        }
        if(mCurrentLocation != null) {
            handleNewLocation(mCurrentLocation);
        }
    }

    private void restoreRescourses()
    {
        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    public void takeMeGoogleMaps(View v)
    {

        if( mParkedLatLng == null)
        {
            Toast.makeText(this,"No Parked Location set", Toast.LENGTH_SHORT).show();
            return;
        }
        double destinationLatitude = mParkedLatLng.latitude;
        double destinationLongitude = mParkedLatLng.longitude;
        String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%f,%f (%s)", destinationLatitude, destinationLongitude, "Where the party is at");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        try
        {
            startActivity(intent);
        }
        catch(ActivityNotFoundException ex)
        {
            try
            {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(unrestrictedIntent);
            }
            catch(ActivityNotFoundException innerEx)
            {
                Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fixZoom()
    {
        List<LatLng> list = new ArrayList<LatLng>();
        if(mParkedLatLng != null)
        {
            list.add(mParkedLatLng);
        }
        LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
        list.add(currentLatLng);
        LatLngBounds.Builder bc = new LatLngBounds.Builder();

        for (LatLng item : list) {
            bc.include(item);
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 50));
    }

}