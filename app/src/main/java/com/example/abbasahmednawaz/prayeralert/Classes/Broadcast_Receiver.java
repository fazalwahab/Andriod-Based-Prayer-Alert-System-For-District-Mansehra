package com.example.abbasahmednawaz.prayeralert.Classes;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class Broadcast_Receiver extends BroadcastReceiver
{

    AudioManager audioManager;
    TelephonyManager telephonyManager;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        try
        {
            //--getting phone state---
            String newPhoneState = intent.hasExtra(TelephonyManager.EXTRA_STATE) ? intent.getStringExtra(TelephonyManager.EXTRA_STATE) : null;
            Bundle bundle = intent.getExtras();

            if (newPhoneState != null && newPhoneState.equals(TelephonyManager.EXTRA_STATE_RINGING))
            {
                //---read the incoming call number
                audioManager.setStreamMute(AudioManager.STREAM_RING,  true);
                rejectCall(context);
                String phoneNumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, "Person is in namaz.. Try later", null, null);

                Toast.makeText(context,"Incoming Call No :"+phoneNumber, Toast.LENGTH_LONG).show();
            }

            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
            {
                //---get the SMS message passed in---
                Bundle bundles = intent.getExtras();

                if (bundles != null)
                {
                    //---retrieve the SMS message received---
                    try
                    {
                        Object[] pdus = (Object[]) bundle.get("pdus");

                        for (int i = 0; i < pdus.length; i++)
                        {
                            SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);

                            String msg_no = currentMessage.getDisplayOriginatingAddress();
                            String msg_body = currentMessage.getDisplayMessageBody();

                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(msg_no, null, "Person is in namaz.. Try later", null, null);

                            Toast.makeText(context,"SenderNum: "+ msg_no + ", Message: " + msg_body, Toast.LENGTH_LONG).show();
                        }
                    }
                    catch (Exception e)
                    {
                        Log.i("------------>", "Getting Message (Failure)");
                    }
                }
            }
        }
        catch (Exception ee)
        {
            Log.i("------------>", "Broadcast Receiver (Failure)");
        }

    }

    private void rejectCall(Context context)
    {
        try
        {
                // Get the getITelephony() method
            Class<?> classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method method = classTelephony.getDeclaredMethod("getITelephony");

                //-------- Disable access check
            method.setAccessible(true);
                //-------- Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = method.invoke(telephonyManager);

                //--------  Get the endCall method from ITelephony
            Class<?> telephonyInterfaceClass = Class.forName(telephonyInterface.getClass().getName());

            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");
                //-------- Invoke endCall()
            methodEndCall.invoke(telephonyInterface);
            Toast.makeText(context,"Call Rejected", Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            Toast.makeText(context,"Call Rejected (Failed)", Toast.LENGTH_LONG).show();
        }

    }
}

