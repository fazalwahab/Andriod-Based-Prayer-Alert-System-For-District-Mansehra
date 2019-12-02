package com.example.abbasahmednawaz.prayeralert.Classes;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.azan.PrayerTimes;
import com.azan.TimeCalculator;
import com.azan.types.PrayersType;
import com.example.abbasahmednawaz.prayeralert.Activities.Main_Activity;
import com.example.abbasahmednawaz.prayeralert.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import static com.azan.types.AngleCalculationType.KARACHI;

public class Background_Service extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private static final String TAG = Background_Service.class.getSimpleName();
    private CountDownTimer countDownTimer;
    private CountDownTimer countDownTimer_check;
    private boolean check = false;

    @Override
    public void onCreate()
    {
        super.onCreate();

       countDownTimer = new CountDownTimer(60000, 1000) //1 minute and 5 seconds
        {
            @Override
            public void onTick(long millisUntilFinished)
            {

            }

            @Override
            public void onFinish()
            {
               monitorTime();

               this.start();
            }
        }.start();

       //---- check to disable call and messages reject mode ----
       if(check == true)
       {
               countDownTimer_check = new CountDownTimer(120000 , 1000) //5 minutes
               {
                   @Override
                   public void onTick(long millisUntilFinished)
                   {
                        Toast.makeText(getApplicationContext(), String.valueOf(millisUntilFinished), Toast.LENGTH_SHORT).show();
                   }

                   @Override
                   public void onFinish()
                   {
                       PackageManager packageManager  = getPackageManager();
                       ComponentName componentName = new ComponentName(getApplicationContext(), Broadcast_Receiver.class);
                       packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                       //-----------

                       if(audioManager.getRingerMode() != 2)
                       {
                           audioManager.setRingerMode(2);
                           Toast.makeText(getApplicationContext(), "Ring Mode", Toast.LENGTH_SHORT).show();
                       }
                       check = false;
                   }
               }.start();
       }

    }

    GoogleApiClient mLocationClient;
    LocationRequest mLocationRequest = new LocationRequest();

    public static PrefConfig prefConfig;
    private AudioManager audioManager;
    private String fajr, zuhr, asr, maghrib, isha, currentTime, currentDate;

    public static final String ACTION_LOCATION_BROADCAST = Background_Service.class.getName() + "LocationBroadcast";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);

        int priority = LocationRequest.PRIORITY_HIGH_ACCURACY; //by default
        //PRIORITY_BALANCED_POWER_ACCURACY, PRIORITY_LOW_POWER, PRIORITY_NO_POWER are the other priority modes

        mLocationRequest.setPriority(priority);
        mLocationClient.connect();

        //------------
        prefConfig = new PrefConfig(getApplicationContext());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //--------------
        //Make it stick to the notification panel so it is less prone to get cancelled by the Operating System.
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onConnected(Bundle dataBundle)
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);

        Log.d(TAG, "Connected to Google API");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
    }

    //to get the location change
    @Override
    public void onLocationChanged(Location location)
    {
        //-------------
       // Toast.makeText( getApplicationContext(), String.valueOf(location.getLatitude()) +" -- "+ String.valueOf(location.getLongitude()), Toast.LENGTH_SHORT).show();
        monitorArea(location);
        setNamazTime(location);
    }

    private void sendMessageToUI(String lat, String lng)
    {
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_LATITUDE, lat);
        intent.putExtra(EXTRA_LONGITUDE, lng);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.d(TAG, "Failed to connect to Google API");
    }



    //--------------=====================--------------

    public void monitorArea(Location location)
    {
        if (prefConfig.readCorrdinates() != null)
        {
            Gson gson = new Gson();
            String json = prefConfig.readCorrdinates();

            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            ArrayList<String> list = gson.fromJson(json, type);

            for (String point : list)
            {
                String[] separated = point.split("!");
                String[] againSeparated = separated[0].split(",");
                double latitude = Double.parseDouble(againSeparated[0].toString());
                double longitude = Double.parseDouble(againSeparated[1].toString());

                float[] results = new float[1];
                Location.distanceBetween(latitude, longitude, location.getLatitude(), location.getLongitude(), results);

                float value = results[0];

                if (value <= 30.0f)
                {
                    if(audioManager.getRingerMode() != 0 && audioManager.getRingerMode() != 1)
                    {
                        audioManager.setRingerMode(0);
                        audioManager.setRingerMode(1);
                        //------------Broadcast Receiver-----
                        PackageManager packageManager  = this.getPackageManager();
                        ComponentName componentName = new ComponentName(this, Broadcast_Receiver.class);
                        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                        if( check == false)
                        {
                            check = true;
                            onCreate();
                        }

                        Toast.makeText(getApplicationContext(), "Broadcast Started", Toast.LENGTH_LONG).show();
                        //------------
                        Toast.makeText(this, "Silent Mode", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        } else
        {
            Toast.makeText(getApplicationContext(), "No Geofenced Area", Toast.LENGTH_SHORT).show();
        }
    }

    //--------------- namaz Timings-=-------------

    public void setNamazTime(Location location)
    {
        GregorianCalendar date = new GregorianCalendar();
        System.out.println(date.getTimeInMillis());
        PrayerTimes prayerTimes = new TimeCalculator().date(date).location(location.getLatitude(), location.getLongitude(),0, 0).timeCalculationMethod(KARACHI).calculateTimes();

        prayerTimes.setUseSecond(true);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");

        //---------Fajr time-----
        try
        {
            String[] Info = (String.valueOf(prayerTimes.getPrayTime(PrayersType.FAJR))).split(" ");
            Date defaultTime = format.parse(Info[3].toString());

            calendar.setTime(defaultTime);
            calendar.add(Calendar.HOUR, 0);
            calendar.add(Calendar.MINUTE, 0);

           //------ desired time-----
            fajr = format.format(calendar.getTime());
        }
        catch (ParseException e)
        {
            Toast.makeText(getApplicationContext(), "Fajr (Failed)", Toast.LENGTH_SHORT).show();
        }

        //---------Zuhr time-----
        try
        {
            String[] Info = (String.valueOf(prayerTimes.getPrayTime(PrayersType.ZUHR))).split(" ");
            Date defaultTime = format.parse(Info[3].toString());

            calendar.setTime(defaultTime);
            calendar.add(Calendar.HOUR, 1);
            calendar.add(Calendar.MINUTE, 8);

            //------ desired time-----
            zuhr = format.format(calendar.getTime());
        }
        catch (ParseException e)
        {
            Toast.makeText(getApplicationContext(), "Zuhr (Failed)", Toast.LENGTH_SHORT).show();
        }

        //---------Asr time-----
        try
        {
            String[] Info = (String.valueOf(prayerTimes.getPrayTime(PrayersType.ASR))).split(" ");
            Date defaultTime = format.parse(Info[3].toString());

            calendar.setTime(defaultTime);
            calendar.add(Calendar.HOUR, 1);
            calendar.add(Calendar.MINUTE, 14);

            //------ desired time-----
            asr = format.format(calendar.getTime());
        }
        catch (ParseException e)
        {
            Toast.makeText(getApplicationContext(), "Asr (Failed)", Toast.LENGTH_SHORT).show();
        }

        //---------Maghrib time-----
        try
        {
            String[] Info = (String.valueOf(prayerTimes.getPrayTime(PrayersType.MAGHRIB))).split(" ");
            Date defaultTime = format.parse(Info[3].toString());

            calendar.setTime(defaultTime);
            calendar.add(Calendar.HOUR, 0);
            calendar.add(Calendar.MINUTE, 0);

            //------ desired time-----
            maghrib = format.format(calendar.getTime());
        }
        catch (ParseException e)
        {
            Toast.makeText(getApplicationContext(), "Maghrib (Failed)", Toast.LENGTH_SHORT).show();
        }

        //---------Isha time-----
        try
        {
            String[] Info = (String.valueOf(prayerTimes.getPrayTime(PrayersType.ISHA))).split(" ");
            Date defaultTime = format.parse(Info[3].toString());

            calendar.setTime(defaultTime);
            calendar.add(Calendar.HOUR, 1);
            calendar.add(Calendar.MINUTE, 0);

            //------ desired time-----
            isha = format.format(calendar.getTime());
        }
        catch (ParseException e)
        {
            Toast.makeText(getApplicationContext(), "Isha (Failed)", Toast.LENGTH_SHORT).show();
        }
    }

    public void monitorTime()
    {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String Time = format.format(calendar.getTime());
        String[] separate = Time.split(" ");
        currentDate = separate[0].toString();       //-----for events--------
        currentTime = separate[1].toString();       //-----for events--------
        //SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
       // currentDate = dateFormat.format(calendar.getTime());
        //-----------------------
        if(currentTime.compareTo(fajr.toString()) == 0 || currentTime.compareTo(zuhr.toString()) == 0
                || currentTime.compareTo(asr.toString()) == 0 || currentTime.compareTo(maghrib.toString()) == 0
                                    || currentTime.compareTo(isha.toString()) == 0)
        {
            namazAlert("Namaz Timing");
        }
        else
        {
            if (prefConfig.readEvent() != null)
            {
                Gson gson = new Gson();
                String json = prefConfig.readEvent();

                Type type = new TypeToken<ArrayList<String>>() {}.getType();
                ArrayList<String> list = gson.fromJson(json, type);

                for (String row : list)
                {
                    String[] separated = row.split(",");
                    String event_name = separated[0].toString();
                    String date = separated[1].toString();
                    String time = separated[2].toString();

                    if(currentDate.compareTo(date.toString()) == 0 && currentTime.compareTo(time.toString()) == 0)
                    {
                        namazAlert(event_name);
                    }

                }
            }
        }

    }

    //=---------------Notification -----------------


    public void namazAlert(String alert_content)
    {
        Intent start = new Intent(this, Boot_Receiver.class);
        start.setAction("start");
        sendBroadcast(start);

        //------main intent-----
        Intent intent = new Intent(this, Main_Activity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //------button intent----
        Intent stop_ring = new Intent(this, Boot_Receiver.class);
        stop_ring.setAction("stop");
        PendingIntent pendingIntent_stop_ring = PendingIntent.getBroadcast(this, 0, stop_ring, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_focused);
        mBuilder.setContentTitle(" !! Alert !!");
        mBuilder.setContentText(alert_content.toString());
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.addAction(R.drawable.places_ic_clear, "Turn Off!!", pendingIntent_stop_ring);
        mBuilder.setOngoing(true);
        mBuilder.setAutoCancel(true);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(1, mBuilder.build());

    }


}