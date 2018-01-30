package com.k.matthias.corneringassistanceapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.k.matthias.corneringassistanceapplication.detection.CurveDetectedListener;
import com.k.matthias.corneringassistanceapplication.detection.UpcomingCurveDetection;
import com.k.matthias.corneringassistanceapplication.grpc.PointDTO;
import com.k.matthias.corneringassistanceapplication.grpc.RecommendationListener;
import com.k.matthias.corneringassistanceapplication.grpc.RequestDTO;
import com.k.matthias.corneringassistanceapplication.model.CachedCurve;
import com.k.matthias.corneringassistanceapplication.task.GrpcTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;

public class AssistantActivity extends AppCompatActivity implements RecommendationListener, LocationListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, CurveDetectedListener {

    private static final String TAG = AssistantActivity.class.getName();
    private UpcomingCurveDetection upcomingCurveDetection;
    private TextView tvCurrentSpeed;
    private TextView tvRecommendedSpeed;
    private static Polyline nearestCurvePolyline = null;
    private BoundingBox latestBB = null;
    private Polyline currentBBPolyline = null;
    private TextView tvUpcomingCurveMeters;
    private float mLastKnownSpeed = 0f;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        initGoogleMaps(savedInstanceState);
        initAndroidUI();
        initDetection();

    }

    private void initAndroidUI() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        tvCurrentSpeed = (TextView) findViewById(R.id.tv_current_current_speed);
        tvRecommendedSpeed = (TextView) findViewById(R.id.tv_recommended_speed);
        tvUpcomingCurveMeters = (TextView) findViewById(R.id.tv_upcoming_curve_meters);
    }

    private void initDetection() {
        this.upcomingCurveDetection = new UpcomingCurveDetection(this, getApplicationContext());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed to: [" + location.getLatitude() + ","  + location.getLongitude() + "]");
        if (driverStartedOrMovedOutOfBoundingBox(location.getLatitude(), location.getLongitude())) {
            Log.d(TAG, "Driver moved out of Bounding Box");
            boolean callServer = mPrefs.getBoolean(SettingsActivity.KEY_PREF_CALL_SERVER, false);
            if (callServer) {
                callRecommendation(location.getLatitude(), location.getLongitude());
            }
            drawBoundingBox();
        }

        upcomingCurveDetection.driverLocationUpdate(location.getLatitude(), location.getLongitude());
        tvCurrentSpeed.setText(String.format(Locale.GERMAN, "%.2f km/h", location.getSpeed() * 3.6));
        mLastKnownSpeed = location.getSpeed() * 3.6f;

        boolean autoMoveMap = mPrefs.getBoolean(SettingsActivity.KEY_PREF_AUTO_MOVE_MAP, false);
        if (autoMoveMap) {
            LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 16);
            mMap.animateCamera(yourLocation);
        }
    }

    private boolean driverStartedOrMovedOutOfBoundingBox(double latitude, double longitude) {
        BoundingBox currentBB = GeoHash.withCharacterPrecision(latitude, longitude, 6).getBoundingBox();
        if (currentBB.equals(latestBB)) {
            return false;
        } else {
            this.latestBB = currentBB;
            return true;
        }
    }

    private void callRecommendation(double lat, double lon) {
        RequestDTO request = RequestDTO.newBuilder()
                .setLocation(PointDTO.newBuilder().setLat(lat).setLon(lon).build())
                .build();
        Log.d(TAG, "Sending new request to recommendation server");
        GrpcTask task = new GrpcTask(this, request, false, getApplicationContext());
        task.execute();
    }

    @Override
    public void onRecommendationResult() {
    }

    @Override
    public void onApproachingCurveDetected(CachedCurve curve, int distanceLeft) {
        tvRecommendedSpeed.setText(String.format(Locale.GERMAN,"%dkm/h", curve.getRecommendedSpeed()));
        tvUpcomingCurveMeters.setText(String.format(Locale.GERMAN,"%dm", distanceLeft));
        drawCurve(curve);
    }

    @Override
    public void onEnteringCurve(CachedCurve curve, int distanceLeft) {
        tvUpcomingCurveMeters.setText(String.format(Locale.GERMAN,"%dm", distanceLeft));
        updateCurve(curve);
    }

    @Override
    public void onPassedCurve() {
        boolean hide = mPrefs.getBoolean(SettingsActivity.KEY_PREF_HIDE_PASSED_CURVES, false);
        if (hide) {
            if (nearestCurvePolyline != null) {
                nearestCurvePolyline.remove();
                nearestCurvePolyline = null;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.show_options:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void drawCurve(CachedCurve curve) {
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(curve.getStartPoint().getLatitude(), curve.getStartPoint().getLongitude()));
        points.add(new LatLng(curve.getCenterPoint().getLatitude(), curve.getCenterPoint().getLongitude()));
        points.add(new LatLng(curve.getEndPoint().getLatitude(), curve.getEndPoint().getLongitude()));

        int color = getCurveColor(curve);
        if (nearestCurvePolyline == null) {
            PolylineOptions rectOptions = new PolylineOptions();
            rectOptions.color(color);
            rectOptions.width(17.5f);
            rectOptions.addAll(points);
            nearestCurvePolyline = mMap.addPolyline(rectOptions);
        } else {
            nearestCurvePolyline.setPoints(points);
            nearestCurvePolyline.setColor(color);
        }
    }

    public void updateCurve(CachedCurve curve) {
        if (nearestCurvePolyline != null) {
            nearestCurvePolyline.setColor(getCurveColor(curve));
        }
    }

    public void drawBoundingBox() {
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(latestBB.getMaxLat(), latestBB.getMinLon())); // lat = north/sout
        points.add(new LatLng(latestBB.getMaxLat(), latestBB.getMaxLon()));
        points.add(new LatLng(latestBB.getMinLat(), latestBB.getMaxLon()));
        points.add(new LatLng(latestBB.getMinLat(), latestBB.getMinLon()));
        points.add(new LatLng(latestBB.getMaxLat(), latestBB.getMinLon()));

        if (currentBBPolyline == null) {
            PolylineOptions rectOptions = new PolylineOptions();
            rectOptions.addAll(points);
            currentBBPolyline = mMap.addPolyline(rectOptions);
        } else {
            currentBBPolyline.setPoints(points);
        }
    }

    /**
     * Returns GREEN if the driver is below the recommended speed and RED otherwise
     * @param curve The approaching curve
     * @return
     */
    private int getCurveColor(CachedCurve curve) {
        if (mLastKnownSpeed <= curve.getRecommendedSpeed()) {
            return Color.GREEN;
        } else {
            return Color.RED;
        }
    }

    /**
     *
     *
     * GOOGLE MAPS Fields and Methods
     *
     *
     */

    /**
     * GOOGLE MAPS fields
     */
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;
    private GoogleApiClient mGoogleApiClient;
    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(48.099881, 15.422875);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private LocationRequest mLocationRequest;
    //The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            //mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
    }

    private void initGoogleMaps(Bundle savedInstanceState) {
        // MAPS inits
        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_assistant);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        createLocationRequest();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
}
