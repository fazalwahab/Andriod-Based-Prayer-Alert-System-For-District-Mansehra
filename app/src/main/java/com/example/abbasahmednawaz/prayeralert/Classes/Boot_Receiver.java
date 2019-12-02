package com.example.abbasahmednawaz.prayeralert.Classes;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.widget.Toast;

public class Boot_Receiver extends BroadcastReceiver
{

    @Override
    public void onReceive(final Context context, Intent intent)
    {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            Intent send = new Intent(context, Background_Service.class);
            context.startService(send);

            Toast.makeText(context,"Service Started", Toast.LENGTH_LONG).show();
        }

        if(intent.getAction().equals("start"))
        {
            context.startService(new Intent(context, Ringtone_Service.class));
            Toast.makeText(context, "Ringtone start", Toast.LENGTH_SHORT).show();
        }

        if(intent.getAction().equals("stop"))
        {
             NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
             notificationManager.cancel(1);

             Intent stop = new Intent(context, Ringtone_Service.class);
             context.stopService(stop);
        }
    }
}
