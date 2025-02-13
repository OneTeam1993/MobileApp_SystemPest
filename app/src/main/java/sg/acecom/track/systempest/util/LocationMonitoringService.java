package sg.acecom.track.systempest.util;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by jmingl on 12/1/18.
 */

public class LocationMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = LocationMonitoringService.class.getSimpleName();
    GoogleApiClient mLocationClient;
    LocationRequest mLocationRequest = new LocationRequest();
    private Location mLastLocation;
    private Location startLocation = new Location("");
    private Location endLocation = new Location("");

    public static final String ACTION_LOCATION_BROADCAST = LocationMonitoringService.class.getName() + "LocationBroadcast";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_SPEED = "extra_speed";
    public static final String EXTRA_DIRECTION = "extra_direction";
    public static final String EXTRA_MILEAGE = "extra_mileage";

    Double start_lat;
    Double start_lng;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest.setInterval(20);
        mLocationRequest.setFastestInterval(10);


        int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY; //by default
        //PRIORITY_BALANCED_POWER_ACCURACY, PRIORITY_LOW_POWER, PRIORITY_NO_POWER are the other priority modes


        mLocationRequest.setPriority(priority);
        mLocationClient.connect();

        //Make it stick to the notification panel so it is less prone to get cancelled by the Operating System.
        return START_NOT_STICKY ;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * LOCATION CALLBACKS
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.d(TAG, "== Error On onConnected() Permission not granted");
            //Permission not granted by user so cancel the further execution.

            return;
        }
        if(mLocationClient.isConnected())
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
            try{
                if(mLastLocation.getLatitude() != 0.0 || mLastLocation != null){
                    start_lat = mLastLocation.getLatitude();
                    start_lng = mLastLocation.getLongitude();
                    startLocation.setLatitude(start_lat);
                    startLocation.setLongitude(start_lng);
                    startLocation.set(startLocation);
                }
                else{
                    return;
                }
            }catch(Exception e){
                Log.e("Last Location : ", String.valueOf(e));
            }


        }

        //Log.d(TAG, "Connected to Google API");
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
    }

    //to get the location change
    @Override
    public void onLocationChanged(Location location) {
        //Log.d(TAG, "Location changed");


        if (location != null) {
            //Log.d(TAG, "== location != null");

            endLocation.setLatitude(location.getLatitude());
            endLocation.setLongitude(location.getLongitude());
            endLocation.set(endLocation);
            double meters = endLocation.distanceTo(startLocation);
            double miles = (meters*0.000621371192237334);
            //Send result to activities
            sendMessageToUI(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), String.valueOf(location.getSpeed()), String.valueOf(location.getBearing()), String.valueOf(miles));
        }
    }

    private void sendMessageToUI(String lat, String lng, String spd, String dir, String mil) {

        //Log.d(TAG, "Sending info...");

        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_LATITUDE, lat);
        intent.putExtra(EXTRA_LONGITUDE, lng);
        intent.putExtra(EXTRA_SPEED, spd);
        intent.putExtra(EXTRA_DIRECTION, dir);
        intent.putExtra(EXTRA_MILEAGE, mil);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to Google API");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationClient.isConnected()) {
            mLocationClient.disconnect();
        }
        //Log.e(TAG, "onCreate() , service stopped...");
    }

    private String getUTCDateAndTime() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new Date());

        return utcTime;
    }
}
