package com.pce_mason.qi.airpollution.RealTimeAirDataMonitoring;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pce_mason.qi.airpollution.R;

import java.util.List;

public class RealTimeDataListAdapter extends RecyclerView.Adapter<RealTimeDataListAdapter.ViewHolder>{
    private final List<RealTimeDataItem> mValues;
    Context context;

    public RealTimeDataListAdapter(List<RealTimeDataItem> items, Context context) {
        mValues = items;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item_real_time_air_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mTxt.setText(mValues.get(position).wifiMacAddress);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mTxt;
//
        public ViewHolder(View view) {
            super(view);
           mTxt = (TextView) view.findViewById(R.id.bottomTxt);
        }

    }


}
