package com.example.abbasahmednawaz.prayeralert.Activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.abbasahmednawaz.prayeralert.Classes.PrefConfig;
import com.example.abbasahmednawaz.prayeralert.Classes.RecyclerAdapter;
import com.example.abbasahmednawaz.prayeralert.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Mosque_List_Activity extends AppCompatActivity
{
    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;
    public PrefConfig prefConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mosque__list_);

        //------------
        prefConfig = new PrefConfig(getApplicationContext());
        //----------------------------------------------------------------------------------------

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview_mosques_list);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        //------------------getting info array-----
        showData();
        //-------------
    }

    public void showData()
    {
        if( prefConfig.readCorrdinates() != null )
        {
            Gson gson = new Gson();
            String json = prefConfig.readCorrdinates();

            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            ArrayList<String> list = gson.fromJson(json, type);

            recyclerAdapter = new RecyclerAdapter(list);
            recyclerView.setAdapter(recyclerAdapter);
        }
    }
}
