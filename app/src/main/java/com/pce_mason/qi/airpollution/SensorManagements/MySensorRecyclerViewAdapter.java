package com.pce_mason.qi.airpollution.SensorManagements;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MySensorRecyclerViewAdapter extends RecyclerView.Adapter<MySensorRecyclerViewAdapter.ViewHolder> {

    private final List<SensorItem> mValues;
    Context context;
    private int sensorStateFlag;
    SensorManagementsFragment sensorManagementsFragment;
    boolean FragmentCheck;

    public MySensorRecyclerViewAdapter(List<SensorItem> items, Context context, SensorManagementsFragment sensorManagementsFragment) {
        mValues = items;
        this.context = context;
        this.sensorManagementsFragment = sensorManagementsFragment;
        FragmentCheck = true;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_sensor, parent, false);
        return new ViewHolder(view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public final ImageView mStatusIcon;
        public final TextView mAddressWifi, mCellularMac, mStatusActivation, mRegistrationDate, mSensorMobility;
        public final RelativeLayout foregroundView, backgroundView;
        public final FrameLayout itemLayout;
        public final ImageButton extendViewButton;
        public final LinearLayout extendViewLayout;
        public SensorItem mItem;
        public boolean extendFlag = false;

        public ViewHolder(View view) {
            super(view);

            mStatusIcon = (ImageView) view.findViewById(R.id.item_status_icon);
            mAddressWifi = (TextView) view.findViewById(R.id.item_address_wifi);
            mCellularMac = (TextView) view.findViewById(R.id.extend_view_cellular_mac);
            mStatusActivation = (TextView) view.findViewById(R.id.extend_view_status_activation);
            mRegistrationDate = (TextView) view.findViewById(R.id.extend_view_registration);
            mSensorMobility = (TextView) view.findViewById(R.id.extend_view_mobility);

//            mContentView = (TextView) view.findViewById(R.id.content);
            foregroundView = (RelativeLayout) view.findViewById(R.id.view_foreground);
            backgroundView = (RelativeLayout) view.findViewById(R.id.view_background);
            itemLayout = (FrameLayout) view.findViewById(R.id.item_layout);
            extendViewLayout = (LinearLayout) view.findViewById(R.id.extend_view_layout);
            extendViewButton = (ImageButton) view.findViewById(R.id.extend_view_btn);
        }
        public void hideExtendView(){
            extendViewLayout.setVisibility(View.GONE);
            extendViewButton.startAnimation(AnimationUtils.loadAnimation(context,R.anim.rotate_recover_button));
            extendFlag = false;
        }
        public void showExtendView(){
            extendViewButton.startAnimation(AnimationUtils.loadAnimation(context,R.anim.rotate_button));
            extendViewLayout.setVisibility(View.VISIBLE);
            extendFlag = true;
        }
        public void sensorDeleteAlert(){
            final int position = getAdapterPosition();
            final String delWifiMac = mValues.get(position).wifiMacAddress;
            final String delCellularMac = mValues.get(position).cellularMacAddress;
            final String delRegistrationDate = mValues.get(position).sensorRegistrationDate;
            final String delActivationFlag = mValues.get(position).sensorActivationFlag;
            final String delSensorStatus = mValues.get(position).sensorStatus;
            final String delSensorMobility = mValues.get(position).sensorMobility;
            final String delNation = mValues.get(position).nation;
            final String delState = mValues.get(position).state;
            final String delCity = mValues.get(position).city;
            mValues.remove(position);
            notifyItemRemoved(position);
            View mView;

            mView = sensorManagementsFragment.getLayoutInflater()
                    .inflate(R.layout.dialog_sensor_del,null);


            final Spinner sensorDissociationSpinner;
            sensorDissociationSpinner = mView.findViewById(R.id.sensorDissociationSpinner);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.dialog_alert_dialog)
                    .setView(mView)
                    .setPositiveButton(R.string.dialog_discard, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            boolean result;
                            result = sensorManagementsFragment.requestMessageProcess(MessageType.SAP_SDDREQ_TYPE,
                                        delWifiMac, String.valueOf(sensorDissociationSpinner.getSelectedItemPosition()));
                            if(!result) {
                                mValues.add(position, new SensorItem(delWifiMac, delCellularMac, delRegistrationDate,
                                        delActivationFlag, delSensorStatus, delSensorMobility, delNation, delState, delCity));
                                notifyItemInserted(position);
                            }//for status return
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            mValues.add(position,new SensorItem(delWifiMac, delCellularMac, delRegistrationDate,
                                    delActivationFlag, delSensorStatus, delSensorMobility, delNation, delState, delCity));
                            notifyItemInserted(position);
                        }
                    });
            builder.show();
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = mValues.get(position);
        String itemName = addressStringMaker(holder.mItem.wifiMacAddress,
                holder.mItem.nation, holder.mItem.state, holder.mItem.city);
        holder.mAddressWifi.setText(itemName);

        String itemStatus = statusStringMaker(holder.mItem.sensorStatus,
                holder.mItem.sensorActivationFlag);
        holder.mStatusActivation.setText(itemStatus);

        holder.mStatusIcon.setImageResource(selectMainListIcon(sensorStateFlag));
        holder.mCellularMac.setText(holder.mItem.cellularMacAddress);

        String itemDate = timestampToDataString(holder.mItem.sensorRegistrationDate);
        holder.mRegistrationDate.setText(itemDate);

        holder.mSensorMobility.setText(sensorMobilityStringMaker(holder.mItem.sensorMobility));

        holder.extendViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!holder.extendFlag)  holder.showExtendView();
                else    holder.hideExtendView();
            }
        });
    }
    private String addressStringMaker(String macAddress, String nation, String state, String city){
        return (macAddress + " - " + city + ", " + state +", " + nation);
    }
    private String statusStringMaker(String sensorStatus, String activationFlag){
        String makeResult;
        //Activation Flag Maker
        String activationString = null;
        switch (activationFlag){
            case "0":   //registered sensor
                activationString = "Not Association";
                sensorStateFlag = 0;
                break;
            case "1":   // associated
                activationString= "Not Operating";
                sensorStateFlag = 1;
                break;
            case "2":   // operating
                activationString = "Operating";
                sensorStateFlag = 2;
                break;
            case "3":   // deregistered
                activationString = "Deregistered";
                sensorStateFlag = 3;
                break;
        }

        //Activation Sensor Status Maker
        String errorSensor = "";
        String[] STATUS_STRING = {"Temperature","CO","O3","NO2","SO2","PM2.5","PM10","GPS"};
        String[] sensorState = sensorStatus.split("");

        for(int i=1; i<sensorState.length; i++){
            if(sensorState[i].equals("0")) errorSensor = errorSensor + STATUS_STRING[i-1] + " Sensor, ";
        }

        if(errorSensor == ""){
            makeResult = "(" + activationString + ") " + DefaultValue.NORMAL_STATUS;
        }
        else{
            sensorStateFlag = 0;
            makeResult = "(" + activationString + ") " + errorSensor + DefaultValue.ABNORMAL_STATUS;
        }
        return makeResult;
    }
    private int selectMainListIcon(int sensorStateFlag){
        switch (sensorStateFlag){
            case 0:
                return R.drawable.ic_error_outline_red;
            case 1:
                return R.drawable.ic_not_operating_yellow;
            case 2:
                return R.drawable.ic_normal_status_green;
            case 3:
                return R.drawable.ic_not_operating_light_gray;
            default:
                return 0;
        }
    }
    private String timestampToDataString(String registrationTime){
        if(registrationTime.equals(null) || registrationTime.equals("")){registrationTime = "1549952037";}
        registrationTime = "1549952037";
        Date registrationDate = new Date(Long.parseLong(registrationTime));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(registrationDate) ;
    }

    private String sensorMobilityStringMaker(String sensorMobility){
        switch (sensorMobility){
            case "0":
                return "Stationary Sensor";
            default:
                return "Potable Sensor";
        }
    }
}