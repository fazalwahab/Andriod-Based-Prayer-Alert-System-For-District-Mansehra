package com.example.abbasahmednawaz.prayeralert.Activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.abbasahmednawaz.prayeralert.R;

public class Splash_Activity extends AppCompatActivity
{
    private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_);

        //--------------------------

        handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                Intent intent=new Intent(getApplicationContext(), Main_Activity.class);
                startActivity(intent);
                finish();
            }
        },4000);

    }
}
