package com.example.abbasahmednawaz.prayeralert.Classes;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class Ringtone_Service extends Service
{
    Ringtone ringtone;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

            try
            {
                Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
                ringtone.play();
            }
            catch (Exception e)
            {
                Toast.makeText(getApplicationContext(), "Ringtone (Failure)", Toast.LENGTH_SHORT).show();
            }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        ringtone.stop();

    }
}
