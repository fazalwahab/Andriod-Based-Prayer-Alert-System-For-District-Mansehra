package com.example.abbasahmednawaz.prayeralert.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class Permission_Requests_Class
{
    private Context context;
    public static int SMS_PERMISSION = 001, READ_PHONE_STATE_PERMISSION = 002, CALL_PHONE_PERMISSION = 003 ;

    public Permission_Requests_Class(Context context)
    {
        this.context = context;
    }
    public void SMS_Permission()
    {
        if( (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) )
        {
            ActivityCompat.requestPermissions((Activity) context, new String[] { Manifest.permission.SEND_SMS }, SMS_PERMISSION);
        }
    }

    public void READ_PHONE_STATE_Permission()
    {
        if( (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) )
        {
            ActivityCompat.requestPermissions((Activity) context, new String[] { Manifest.permission.READ_PHONE_STATE }, READ_PHONE_STATE_PERMISSION);
        }
    }

    public void CALL_PHONE_Permission()
    {
        if( (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) )
        {
            ActivityCompat.requestPermissions((Activity) context, new String[] { Manifest.permission.CALL_PHONE }, CALL_PHONE_PERMISSION);
        }
    }

}
