package com.paulscott56.favouriteplaces;

/**
 * Created by paul on 2017/11/04.
 */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;


/**
 * Created by paul on 2016/06/22.
 */

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    private List<LocationDetail> locationList;

    public PlacesAdapter(List<LocationDetail> locationList) {
        this.locationList = locationList;
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    @Override
    public void onBindViewHolder(PlaceViewHolder placeViewHolder, int i) {
        LocationDetail ci = locationList.get(i);
        placeViewHolder.vPlace.setText(ci.UTTERANCE_PREFIX + " " + ci.place);
    }

    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_layout, viewGroup, false);

        return new PlaceViewHolder(itemView);
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {

        protected TextView vPlace;

        public PlaceViewHolder(View v) {
            super(v);
            vPlace = (TextView) v.findViewById(R.id.place);
        }
    }
}