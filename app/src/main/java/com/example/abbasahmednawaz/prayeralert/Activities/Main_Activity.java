package com.example.abbasahmednawaz.prayeralert.Activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.azan.PrayerTimes;
import com.azan.TimeCalculator;
import com.azan.types.PrayersType;
import com.example.abbasahmednawaz.prayeralert.Classes.Background_Service;
import com.example.abbasahmednawaz.prayeralert.Classes.Boot_Receiver;
import com.example.abbasahmednawaz.prayeralert.Classes.Broadcast_Receiver;
import com.example.abbasahmednawaz.prayeralert.Classes.PrefConfig;
import com.example.abbasahmednawaz.prayeralert.Classes.Ringtone_Service;
import com.example.abbasahmednawaz.prayeralert.R;
import com.example.abbasahmednawaz.prayeralert.Utils.Permission_Requests_Class;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.azan.types.AngleCalculationType.KARACHI;

public class Main_Activity extends AppCompatActivity implements View.OnClickListener
{
    public static PrefConfig prefConfig;
    private Dialog dialog;
    private Button btn_submit, btn_cancel;
    private EditText txt_name, txt_date, txt_time;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //-----------
        prefConfig = new PrefConfig(getApplicationContext());
        //--------------------------------- button inslizaition

        findViewById(R.id.btn_locate_mosques).setOnClickListener(this);
        findViewById(R.id.btn_mosque_list).setOnClickListener(this);
        findViewById(R.id.btn_set_events).setOnClickListener(this);
        findViewById(R.id.btn_events_list).setOnClickListener(this);


        //---------------initally disable broadcast receiver for calls and messages---

        PackageManager packageManager = this.getPackageManager();
        ComponentName componentName = new ComponentName(this, Broadcast_Receiver.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        //-----------------dialog box-----------

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.event_dialogbox);
        dialog.setTitle("Event Reminder");

        btn_submit = (Button) dialog.findViewById(R.id.btn_submit);
        btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel);

        txt_name = (EditText) dialog.findViewById(R.id.txt_name);
        txt_date = (EditText) dialog.findViewById(R.id.txt_date);
        txt_time = (EditText) dialog.findViewById(R.id.txt_time);

        //---------------starting Geofence and namaz Alert service----

        Intent send = new Intent(getApplicationContext(), Background_Service.class);
        startService(send);

        //---------------------dialog event buttons-------------------
        btn_submit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ArrayList<String> events = new ArrayList();

                String combine = txt_name.getText().toString()+","+txt_date.getText().toString()+","+txt_time.getText().toString();
                events.add(combine);

                //--------------------------
                Gson newgson = new Gson();
                String newjson_list;

                if( Main_Activity.prefConfig.readEvent() != null)
                {
                    Gson oldgson = new Gson();
                    String oldjson_list = Main_Activity.prefConfig.readEvent();

                    Type type_list = new TypeToken<ArrayList<String>>() {}.getType();
                    ArrayList<String> existedList = oldgson.fromJson(oldjson_list, type_list);

                    existedList.addAll(events);

                    newjson_list = newgson.toJson(existedList);

                   // Toast.makeText(getApplicationContext(), String.valueOf("Latest"+existedList.size()), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    newjson_list = newgson.toJson(events);
                }
                //-------------
                Main_Activity.prefConfig.writeEvent(newjson_list);
                txt_name.setText(null);
                dialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_locate_mosques:
                startActivity(new Intent(this, Geofence_Activity.class));
                //  getJsonData();
                break;

            case R.id.btn_mosque_list:
                startActivity(new Intent(this, Mosque_List_Activity.class));
                break;

            case R.id.btn_set_events:
                dialog.show();
               // stopService(new Intent(getApplicationContext(), Background_Service.class));
                break;

            case R.id.btn_events_list:
                startActivity(new Intent(this, Events_List_Activity.class));
                break;
        }
    }

    //-----------------optional------

    public String getJsonAssest()
    {
        String jsonString = null;
        try
        {
            InputStream inputStream = getAssets().open("November.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            jsonString = new String(buffer, "UTF-8");
        }
        catch (IOException ex)
        {
            Toast.makeText(getApplicationContext(), "JSON Assest (Failure)", Toast.LENGTH_SHORT).show();
        }

        return jsonString;
    }

    public void getJsonData()
    {
        String jsonString = getJsonAssest();

        try
        {
            Calendar calendar = Calendar.getInstance();
            //--------------
            JSONObject mainObj = new JSONObject(jsonString);
            JSONObject dateObj  = mainObj.getJSONObject(String.valueOf(calendar.DAY_OF_MONTH));

            System.out.println("fajir  : "+dateObj.getString("fajr"));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

    }
}



