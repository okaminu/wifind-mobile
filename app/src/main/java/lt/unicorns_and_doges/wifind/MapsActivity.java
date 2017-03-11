package lt.unicorns_and_doges.wifind;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import lt.unicorns_and_doges.wifind.backend.WiFindClient;
import lt.unicorns_and_doges.wifind.model.Location;
import lt.unicorns_and_doges.wifind.model.WifiSpot;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    //Wifi scanner specific
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    StringBuilder sb = new StringBuilder();

    private volatile Handler showGUIMessageHandler;



    private static final int MY_PERMISSIONS_REQUEST_LOCATION_PERMISSIONS = 10;
    private static final long LOCATION_REFRESH_TIME = 1000;
    private static final float LOCATION_REFRESH_DISTANCE = 1;

    private lt.unicorns_and_doges.wifind.model.Location currentLocation =
            new lt.unicorns_and_doges.wifind.model.Location(1, 1);

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createHandlers();
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        setupGoogleApiClient();
//        scanWifiSpots();
    }

    @Override
    protected void onStart() {
        super.onStart();
        new WifiSpotGetTask().execute();
    }

    @Override
    protected void onResume() {
//        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
//        unregisterReceiver(receiverWifi);
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
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
    }

    private void setupGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * This is where we can add markers or lines.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        if (Build.VERSION.SDK_INT > 22 && hasPermissions()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION_PERMISSIONS);
        } else {
            continueMapSetup();
        }
    }

    @SuppressWarnings({"ResourceType"})
    private void continueMapSetup() {
        mMap.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE, mLocationListener);


    }

    private void zoomToLocation(android.location.Location location) {
        currentLocation.setLongitude(location.getLatitude());
        currentLocation.setLatitude(location.getLongitude());

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 17));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        new WifiSpotPutTask(new Location(location.getLatitude(), location.getLongitude()),
                "Some example2").execute();
//        scanWifiSpots();
    }

//    private void scanWifiSpots() {
//
//        // Initiate wifi service manager
//        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//
//        // Check for wifi is disabled
//        if (!mainWifi.isWifiEnabled()) {
//            // If wifi disabled then enable it
//            Toast.makeText(getApplicationContext(), "Wifi is disabled .. making it enabled",
//                    Toast.LENGTH_LONG).show();
//
//            mainWifi.setWifiEnabled(true);
//        }
//        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
//        mainWifi.startScan();
//        System.out.println("--------------------------------");
//        System.out.println("Starting Scan...");
//        System.out.println("--------------------------------");
//    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Work with backend
     */

    private class WifiSpotGetTask extends AsyncTask<Integer, Integer, WifiSpot[]> {
        @Override
        protected WifiSpot[] doInBackground(Integer... integers) {
            ApiClientFactory apiClientFactory = new ApiClientFactory();
            WifiSpot[] spots = new WifiSpot[0];
            try
            {
                spots = apiClientFactory.build(WiFindClient.class).getAll();
            }
            catch (Exception ex)
            {
                Message message = showGUIMessageHandler.obtainMessage
                        (0, "Failed to retrieve spots: service unreachable");
                message.sendToTarget();
            }
            return spots;
        }

        @Override
        protected void onPostExecute(WifiSpot[] spots) {
            super.onPostExecute(spots);
            for (WifiSpot spot : spots) {
                float color = BitmapDescriptorFactory.HUE_GREEN;
                double longitude = spot.getLocation().getLongitude();
                double latitude = spot.getLocation().getLatitude();
                String SSID = spot.getSsid();
                long lastUpdated = spot.getLastUpdated();


                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT"));
                time.setTimeInMillis(lastUpdated);
                String timeString = time.get(Calendar.YEAR)
                        + "-" + (time.get(Calendar.MONTH) + 1)
                        + "-" + time.get(Calendar.DAY_OF_MONTH);

                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title("SSID: " + SSID + ", Last updated: " + timeString)
                        .icon(BitmapDescriptorFactory.defaultMarker(color)));
            }
        }
    }

    private class WifiSpotPutTask extends AsyncTask<Integer, Integer, Boolean> {
        Location location;
        String ssid;

        public WifiSpotPutTask(Location location, String ssid) {
            this.location = location;
            this.ssid = ssid;
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {
            WifiSpot wifiSpot = new WifiSpot(ssid, System.currentTimeMillis(), location);
            Message message;
            try
            {
                return new ApiClientFactory().build(WiFindClient.class).save(wifiSpot);
            }
            catch (Exception ex)
            {
                ex.getMessage();
                message = showGUIMessageHandler.obtainMessage
                        (0, "Failed to submit spot info: service unreachable");
                message.sendToTarget();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            mMap.clear();
            new WifiSpotGetTask().execute();
        }
    }


    /**
     * Asynchronous listeners
     */

    private final LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(android.location.Location location) {
            zoomToLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    continueMapSetup();

                } else {
                    Toast.makeText(getApplicationContext(), "Location permissions not granted",
                            Toast.LENGTH_SHORT).show();
                    this.finishAffinity();
                }
                return;
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        android.location.Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Toast.makeText(
                    getApplicationContext(),
                    "Please enable Location services from settings to get current location",
                    Toast.LENGTH_LONG).show();
        } else {
            zoomToLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {

            sb = new StringBuilder();
            wifiList = mainWifi.getScanResults();
            sb.append("\nNumber Of Wifi connections :" + wifiList.size() + "\n\n");
            for (int i = 0; i < wifiList.size(); i++) {
                sb.append(new Integer(i + 1).toString() + ". ");
                sb.append((wifiList.get(i)).toString());
                sb.append("\n\n");
            }
            System.out.println("--------------------------------");
            System.out.println(sb);
            System.out.println("--------------------------------");
        }

    }

    public void createHandlers (){
        showGUIMessageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if(message.what == 0)
                    Toast.makeText(getApplicationContext(), message.obj.toString(), Toast.LENGTH_LONG)
                            .show();
            }
        };
    }


}
