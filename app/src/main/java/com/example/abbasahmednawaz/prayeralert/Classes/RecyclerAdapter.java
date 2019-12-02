package com.example.abbasahmednawaz.prayeralert.Classes;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.abbasahmednawaz.prayeralert.Activities.Main_Activity;
import com.example.abbasahmednawaz.prayeralert.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter <RecyclerAdapter.RecyclerViewHolder>
{
    private ArrayList<String> arrayList;

    public RecyclerAdapter(ArrayList<String> arrayList)
    {
        this.arrayList = arrayList;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position)
    {
        String[] separated=arrayList.get(position).split("!");
        String[] againSeparated = separated[1].split(",");
        String location_ID = againSeparated[0].toString();
        String date_time = againSeparated[1].toString();

        holder.txt_location_ID.setText(location_ID);
        holder.txt_date_time.setText(date_time);
    }

    @Override
    public int getItemCount()
    {
        return arrayList.size();
    }


    @Override
    public int getItemViewType(int position)
    {
        return super.getItemViewType(position);
    }

    @Override
    public long getItemId(int position)
    {
        return super.getItemId(position);
    }

    public void removeItem(int position)
    {
        this.arrayList.remove(position);
        // Anytime data changes, notify the recyclerview to update
        notifyItemRemoved(position);
    }


    //-----nested recycleviewholder class

    public class RecyclerViewHolder extends RecyclerView.ViewHolder
    {
        private TextView txt_location_ID, txt_date_time;
        RelativeLayout deleteItem;

        public RecyclerViewHolder(View itemview)
        {
            super(itemview);

            txt_location_ID = (TextView) itemview.findViewById(R.id.txt_location_ID);
            txt_date_time = (TextView) itemview.findViewById(R.id.txt_date_time);

            deleteItem = (RelativeLayout) itemView.findViewById(R.id.delete_item);

            deleteItem.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                        Gson gson = new Gson();
                        String json = Main_Activity.prefConfig.readCorrdinates();

                        Type type = new TypeToken<ArrayList<String>>() {}.getType();
                        ArrayList<String> list = gson.fromJson(json, type);

                        list.remove(getAdapterPosition());

                        String update = gson.toJson(list);
                        Main_Activity.prefConfig.writeCorrdinates(update);

                    removeItem(getAdapterPosition());
                    // Any other actions for deleting
                }
            });
        }
    }

}
