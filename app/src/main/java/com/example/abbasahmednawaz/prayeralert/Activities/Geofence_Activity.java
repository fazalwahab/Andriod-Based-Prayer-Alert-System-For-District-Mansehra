package com.example.abbasahmednawaz.prayeralert.Activities;

import android.Manifest;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.abbasahmednawaz.prayeralert.Classes.Background_Service;
import com.example.abbasahmednawaz.prayeralert.Classes.Broadcast_Receiver;
import com.example.abbasahmednawaz.prayeralert.Classes.Notification_IntentService;
import com.example.abbasahmednawaz.prayeralert.Classes.PrefConfig;
import com.example.abbasahmednawaz.prayeralert.R;
import com.example.abbasahmednawaz.prayeralert.Utils.Permission_Requests_Class;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Geofence_Activity extends AppCompatActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        ResultCallback<Status>
{

    private static final String TAG = Geofence_Activity.class.getSimpleName();

    public static GoogleMap map;
    public static GoogleApiClient googleApiClient;
    public Location lastLocation;

    Dialog dialog;
    private LatLng latLng;
    public static PrefConfig prefConfig;

    private EditText txt_ID;
    public Button btn_clear, btn_no, btn_yes;

    private MapFragment mapFragment;
    private Geocoder geocoder;
    private List<Address> addressList;


    private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";
    // Create a Intent send by the notification
    public static Intent makeNotificationIntent(Context context, String msg)
    {
        Intent intent = new Intent( context, Geofence_Activity.class );
        intent.putExtra( NOTIFICATION_MSG, msg );
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence_);

        //----------------------

        //textLat = (TextView) findViewById(R.id.lat);
        //textLong = (TextView) findViewById(R.id.lon);
        btn_clear = (Button)findViewById(R.id.btn_clear);

        // initialize GoogleMaps
        initGMaps();

        // create GoogleApiClient
        createGoogleApi();
        //------------
        Permission_Requests_Class permission = new Permission_Requests_Class(this);
        permission.SMS_Permission();
        permission.CALL_PHONE_Permission();
        permission.READ_PHONE_STATE_Permission();
        //------------
        prefConfig = new PrefConfig(getApplicationContext());
        //----------------------------------------------------------------------------------------
        geocoder = new Geocoder(this, Locale.getDefault());
        //----------------------------
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.confirmation_dialogbox);
        dialog.setTitle("Confirmation!!!");
        //---------
        txt_ID = (EditText) dialog.findViewById(R.id.txt_ID);
        btn_yes = (Button) dialog.findViewById(R.id.btn_yes);
        btn_no = (Button) dialog.findViewById(R.id.btn_no);

        btn_yes.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                if(txt_ID.getText().toString() == null)
              {
                  Toast.makeText(getApplicationContext(), "!! Location ID Required !!", Toast.LENGTH_SHORT).show();
              }
              else
              {
                  dialog.dismiss();
                  markerForGeofence(latLng);
                  drawGeofence();
                  startGeofence();
                  //------getting time, date and location ID...
                  Calendar calendar = Calendar.getInstance();
                  SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                  String time_date = format.format(calendar.getTime());

                  //------adding in arraylist for shared preferences--
                  ArrayList<String> corrdinates = new ArrayList();

                  String combine = String.valueOf(latLng.latitude)+","+String.valueOf(latLng.longitude)+"!"+
                                     txt_ID.getText().toString()+","+time_date.toString();
                  corrdinates.add(combine);
                  saveGeofence(corrdinates);
              }

            }
        });

        btn_no.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        btn_clear.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Main_Activity.prefConfig.writeCorrdinates(null);

                Intent intent = getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finish();
                startActivity(intent);

                Toast.makeText(getApplicationContext(), "Map Refreshed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Create GoogleApiClient instance
    private void createGoogleApi()
    {
        if ( googleApiClient == null )
        {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        googleApiClient.disconnect();
    }


    private final int REQ_PERMISSION = 999;
    // Check for permission to access Location
    private boolean checkPermission()
    {
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED );
    }

    // Asks for permission
    private void askPermission()
    {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQ_PERMISSION);
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch ( requestCode )
        {
            case REQ_PERMISSION:
            {
                if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                {
                    // Permission granted
                    getLastKnownLocation();

                }
                else
                {
                    // Permission denied
                    permissionsDenied();
                    //-----
                    stopService(new Intent(getApplicationContext(), Background_Service.class));
                    finishAndRemoveTask();
                }
                break;
            }

            case 001:
            {
                if ( grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED )
                {
                    stopService(new Intent(getApplicationContext(), Background_Service.class));
                    finishAndRemoveTask();
                }
                break;
            }

            case 002:
            {
                if ( grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED )
                {
                    stopService(new Intent(getApplicationContext(), Background_Service.class));
                    finishAndRemoveTask();
                }
                break;
            }

            case 003:
            {
                if ( grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED )
                {
                    stopService(new Intent(getApplicationContext(), Background_Service.class));
                    finishAndRemoveTask();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied()
    {
        Log.w(TAG, "permissionsDenied()");
    }

    // Initialize GoogleMaps
    private void initGMaps()
    {
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Callback called when Map is ready
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        Log.d(TAG, "onMapReady()");
        map = googleMap;
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng)
    {
        Log.d(TAG, "onMapClick("+latLng +")");
        //---------
        this.latLng = latLng;
        dialog.show();
    }

    @Override
    public boolean onMarkerClick(Marker marker)
    {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition() );
        return false;
    }

    private LocationRequest locationRequest;
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL =  10000;
    private final int FASTEST_INTERVAL = 9000;

    // Start location Updates
    private void startLocationUpdates()
    {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if ( checkPermission() )
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        lastLocation = location;
        writeActualLocation(location);
    }

    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        getLastKnownLocation();
        recoverGeofenceMarker();
        startGeofence();
    }

    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i)
    {
        Log.w(TAG, "onConnectionSuspended()");
    }

    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.w(TAG, "onConnectionFailed()");
    }

    // Get last known location
    private void getLastKnownLocation()
    {
        if ( checkPermission() )
        {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if ( lastLocation != null )
            {
                writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        }
        else askPermission();
    }

    private void writeActualLocation(Location location)
    {
       // textLat.setText( "Lat: " + location.getLatitude() );
       // textLong.setText( "Long: " + location.getLongitude() );

        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void writeLastLocation()
    {
        writeActualLocation(lastLocation);
    }

    private Marker locationMarker;
    private void markerLocation(LatLng latLng)
    {
        //----------setting marker addresses--------------
        String title;
        try
        {
            addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            title = addressList.get(0).getAddressLine(0);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            title = latLng.latitude + ", " + latLng.longitude;
        }

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if ( map!=null )
        {
            if ( locationMarker != null )
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            float zoom = 19f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);
        }
    }


    private Marker geoFenceMarker;
    private void markerForGeofence(LatLng latLng)
    {
        //----------setting marker addresses--------------
        String title;
        try
        {
            addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            title = addressList.get(0).getAddressLine(0);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            title = latLng.latitude + ", " + latLng.longitude;
        }

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                .title(title);
        if ( map!=null )
        {
            geoFenceMarker = map.addMarker(markerOptions);
            //----now starting geofence at this point
            startGeofence();
        }
    }

    // Start Geofence creation process
    private void startGeofence()
    {
        if( geoFenceMarker != null )
        {
            Geofence geofence = createGeofence( geoFenceMarker.getPosition(), GEOFENCE_RADIUS );
            GeofencingRequest geofenceRequest = createGeofenceRequest( geofence );
            addGeofence( geofenceRequest );
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }

    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = " - GeoFence Area";    //----Notification text
    private static final float GEOFENCE_RADIUS = 30.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius )
    {
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration( GEO_DURATION )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest( Geofence geofence )
    {
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;
    private PendingIntent createGeofencePendingIntent()
    {
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, Notification_IntentService.class);
        return PendingIntent.getService(this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request)
    {
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Status status)
    {
        if ( status.isSuccess() )
        {   // saveGeofence();
            // drawGeofence();
        }
        else
        {
            // inform about fail
            // saveGeofence(); // sometimes status return false.
            //drawGeofence();
        }
    }

    // Draw Geofence circle on GoogleMap
    private Circle geoFenceLimits;
    private void drawGeofence()
    {
        // if ( geoFenceLimits != null )
        //   geoFenceLimits.remove();

        CircleOptions circleOptions = new CircleOptions()
                .center( geoFenceMarker.getPosition())
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(100, 150,150,150) )
                .radius( GEOFENCE_RADIUS );
        geoFenceLimits = map.addCircle( circleOptions );
    }

    // Saving GeoFence marker with prefs mng
    private void saveGeofence(ArrayList<String> list)
    {
        Gson newgson = new Gson();
        String newjson_list;

        if( Geofence_Activity.prefConfig.readCorrdinates() != null)
        {
            Gson oldgson = new Gson();
            String oldjson_list = Geofence_Activity.prefConfig.readCorrdinates();

            Type type_list = new TypeToken<ArrayList<String>>() {}.getType();
            ArrayList<String> existedList = oldgson.fromJson(oldjson_list, type_list);

            existedList.addAll(list);

            newjson_list = newgson.toJson(existedList);

            Toast.makeText(getApplicationContext(), String.valueOf("Latest"+existedList.size()), Toast.LENGTH_SHORT).show();

        }
        else
        {
            newjson_list = newgson.toJson(list);
        }
        //-------------
        Geofence_Activity.prefConfig.writeCorrdinates(newjson_list);
    }

    // Recovering last Geofence marker
    private void recoverGeofenceMarker()
    {
        if( Geofence_Activity.prefConfig.readCorrdinates() != null )
        {
            Gson gson = new Gson();
            String json = Geofence_Activity.prefConfig.readCorrdinates();

            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            ArrayList<String> list = gson.fromJson(json, type);

            for (String point : list)
            {
                String[] separated = point.split("!");
                String[] againSeparated = separated[0].split(",");
                String latitude = againSeparated[0].toString();
                String longitude = againSeparated[1].toString();

                markerForGeofence(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                drawGeofence();
            }
        }
    }

    // Clear Geofence
    private void clearGeofence()
    {
        LocationServices.GeofencingApi.removeGeofences(googleApiClient, createGeofencePendingIntent()).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status)
            {
                if ( status.isSuccess() )
                {
                    // remove drawing
                    removeGeofenceDraw();
                }
            }
        });
    }

    private void removeGeofenceDraw()
    {
        if ( geoFenceMarker != null)
            geoFenceMarker.remove();
        if ( geoFenceLimits != null )
            geoFenceLimits.remove();
    }

}