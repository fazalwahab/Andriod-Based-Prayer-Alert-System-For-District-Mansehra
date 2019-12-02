package com.example.abbasahmednawaz.prayeralert.Classes;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.abbasahmednawaz.prayeralert.R;

public class PrefConfig
{
    private SharedPreferences sharedPreferences;
    private Context context;

    public PrefConfig(Context context)
    {
        this.context = context;
        sharedPreferences =context.getSharedPreferences(context.getString(R.string.pref_file), Context.MODE_PRIVATE);
    }

    public void writeCorrdinates(String corrdinates)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.pref_corrdinates), corrdinates);
        editor.commit();
    }

    public String readCorrdinates()
    {
        return sharedPreferences.getString(context.getString(R.string.pref_corrdinates), null);
    }

    public void writeListInfo(String listInfo)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.pref_corrdinates), listInfo);
        editor.commit();
    }

    public String readListInfo()
    {
        return sharedPreferences.getString(context.getString(R.string.pref_list_info), null);
    }

    public void writeEvent(String event)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.pref_event), event);
        editor.commit();
    }

    public String readEvent()
    {
        return sharedPreferences.getString(context.getString(R.string.pref_event), null);
    }
}

