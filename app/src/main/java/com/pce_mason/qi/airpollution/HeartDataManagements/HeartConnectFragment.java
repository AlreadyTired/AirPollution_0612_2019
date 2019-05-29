package com.pce_mason.qi.airpollution.HeartDataManagements;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.pce_mason.qi.airpollution.MainActivity;
import com.pce_mason.qi.airpollution.MapManagements.GpsInfo;
import com.pce_mason.qi.airpollution.R;
import com.google.android.gms.plus.PlusOneButton;

/**
 * A fragment with a Google +1 button.
 * Use the {@link HeartConnectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HeartConnectFragment extends Fragment {
    private static final int PLUS_ONE_REQUEST_CODE = 0;
    private Button connectDeviceButton;
    GpsInfo gpsInfo;
    Context context;
    public HeartConnectFragment() {
        // Required empty public constructor
    }

    public static HeartConnectFragment newInstance(Context context) {
        HeartConnectFragment fragment = new HeartConnectFragment();
        fragment.context = context;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gpsInfo = new GpsInfo(getActivity());
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, new IntentFilter("heartData"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_heart_connect, container, false);

        connectDeviceButton = (Button) view.findViewById(R.id.heart_connect_device);
        connectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(MainActivity.HEART_GENERATOR == null){
                    MainActivity.HEART_GENERATOR = new HeartDataGenerator(getContext(),gpsInfo.getLatitude(),gpsInfo.getLongitude());
                }
                if (MainActivity.HEART_GENERATOR.getGenerateState()) {
                    MainActivity.HEART_GENERATOR.startDataGenerate();
                }else{
                    MainActivity.HEART_GENERATOR.stopDataGenerate();
                }
                fragmentChange();
            }
        });
        return view;
    }

    public void fragmentChange(){
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        HeartDataViewFragment fragment = HeartDataViewFragment.newInstance(context);
        fragmentTransaction.replace(R.id.fragment_container,fragment);
        fragmentTransaction.commit();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fragmentChange();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }
}
