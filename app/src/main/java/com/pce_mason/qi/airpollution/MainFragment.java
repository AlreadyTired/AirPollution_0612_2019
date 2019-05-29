package com.pce_mason.qi.airpollution;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.DataManagements.ColorCalculator;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.HeartDataManagements.HeartDataGenerator;
import com.pce_mason.qi.airpollution.MapManagements.GpsInfo;
import com.pce_mason.qi.airpollution.RealTimeAirDataMonitoring.RealTimeDataItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class MainFragment extends Fragment implements OnMapReadyCallback{
    private Context context;
    private final int AQI_MAX = 500;
    private final int PM_MAX = 500;
    private TextView coValueText, so2ValueText, o3ValueText, no2ValueText, pmValueText;
    private TextView temperatureTxv, temperatureUnitTxv;
    private TextView heartRateTxv;
    private ProgressBar coProgress, so2Progress, o3Progress, no2Progress, pmProgress;
    private Spinner sensorSelectSpinner;

    private ColorCalculator colorCalculator;
    private MapView map;
    private GpsInfo gpsInfo;

    private HttpConnectionThread mAuthTask = null;
    public List<RealTimeDataItem> mRealTimeItems = new ArrayList<RealTimeDataItem>();
    public ArrayList<String> mSpinnerItems = new ArrayList<>();
    private int mMapZoomLevel = 14;
    private final String RESULT_CODE ="resultCode";
    private final String LIST_NAME = "realtimeAirQualityDataList";
//    private final int TUPLE_LENGTH = 11;

    Timer refreshTimer = null;
    private int refreshTime = 10 * 1000;

    private Handler mHandler;

    Button realTimeSensorManagementsButton, realTimeHeartButton;
    View realTimeAirView, realTimeTempView, realTimeHeartView;
    View realTimeAirViewNone, realTimeTempViewNone, realTimeHeartViewNone;;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("heartRate");
            if(realTimeHeartView.getVisibility() == View.GONE) {
                realTimeHeartView.setVisibility(View.VISIBLE);
                realTimeHeartViewNone.setVisibility(View.GONE);
            }
            heartRateTxv.setText(data);
        }
    };

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance(Context context) {
        MainFragment fragment = new MainFragment();
        fragment.context = context;
        fragment.gpsInfo = new GpsInfo(context);
        if (!fragment.gpsInfo.isGetLocation()) {
            fragment.gpsInfo.showSettingsAlert();     // GPS setting Alert
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        colorCalculator = new ColorCalculator();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, new IntentFilter("heartData"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        realTimeAirView = (View)view.findViewById(R.id.real_time_air_view);
        realTimeAirViewNone = (View)view.findViewById(R.id.real_time_air_view_none);
        realTimeTempView = (View)view.findViewById(R.id.real_time_temperature_view);
        realTimeTempViewNone = (View)view.findViewById(R.id.real_time_temperature_view_none);
        realTimeHeartView = (View)view.findViewById(R.id.real_time_heart_view);
        realTimeHeartViewNone = (View)view.findViewById(R.id.real_time_heart_view_none);

        coValueText = (TextView) view.findViewById(R.id.co_value);
        so2ValueText = (TextView) view.findViewById(R.id.so2_value);
        o3ValueText = (TextView) view.findViewById(R.id.o3_value);
        no2ValueText = (TextView) view.findViewById(R.id.no2_value);
        pmValueText = (TextView) view.findViewById(R.id.pm_value);
        temperatureTxv = (TextView) view.findViewById(R.id.real_time_temp_txt);
        heartRateTxv = (TextView) view.findViewById(R.id.real_time_heart_txt);
        temperatureUnitTxv = (TextView) view.findViewById(R.id.real_time_temp_unit);

        coProgress = (ProgressBar) view.findViewById(R.id.co_progress);
        so2Progress = (ProgressBar) view.findViewById(R.id.so2_progress);
        o3Progress = (ProgressBar) view.findViewById(R.id.o3_progress);
        no2Progress = (ProgressBar) view.findViewById(R.id.no2_progress);
        pmProgress = (ProgressBar) view.findViewById(R.id.pm_progress);
        sensorSelectSpinner = (Spinner) view.findViewById(R.id.real_time_dashboard_spinner);

        initProgress(coProgress,AQI_MAX);
        initProgress(so2Progress,AQI_MAX);
        initProgress(o3Progress,AQI_MAX);
        initProgress(no2Progress,AQI_MAX);
        initProgress(pmProgress,PM_MAX);

        requestMessageProcess();
        initRefreshHandler();
        startDataRefresher();

        map = (MapView) view.findViewById(R.id.mapView);
        map.setClickable(false);
        map.getMapAsync(this);

        sensorSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                setDashboardView(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        temperatureTxv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (temperatureUnitTxv.getText().equals("C")){
                    temperatureUnitTxv.setText("F");
                    float celsius = Float.parseFloat(temperatureTxv.getText().toString());
                    float fahrenheit = celsius*9/5 +32;
                    temperatureTxv.setText(String.format("%.1f", fahrenheit));
                }else {
                    temperatureUnitTxv.setText("C");
                    float fahrenheit = Float.parseFloat(temperatureTxv.getText().toString());
                    float celsius = ((fahrenheit-32)*5/9);
                    temperatureTxv.setText(String.format("%.1f", celsius));
                }
            }
        });

//      --------------------
//        DashBoard Button click listener
//      --------------------
        realTimeSensorManagementsButton = (Button) view.findViewById(R.id.real_time_sensor_managements);
        realTimeSensorManagementsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestMessageProcess();
            }
        });
        realTimeHeartButton = (Button) view.findViewById(R.id.real_time_heart_btn);
        realTimeHeartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(MainActivity.USER_SEQUENCE_NUMBER == 0)
                {
                    Toast.makeText(getContext().getApplicationContext(), getString(R.string.sign_in_warning), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if(MainActivity.HEART_GENERATOR == null) {
                        MainActivity.HEART_GENERATOR = new HeartDataGenerator(context, gpsInfo.getLatitude(), gpsInfo.getLongitude());
                    }
                    if (MainActivity.HEART_GENERATOR.getGenerateState()) {
                        MainActivity.HEART_GENERATOR.startDataGenerate();
                    }else{
                        MainActivity.HEART_GENERATOR.stopDataGenerate();
                    }
                }
            }
        });

        return view;
    }

    //Fill the Dashboard
    private void setDashboardView(int index){

        progressChanger(coProgress,Integer.parseInt(mRealTimeItems.get(index).coAqi));
        progressChanger(so2Progress, Integer.parseInt(mRealTimeItems.get(index).so2Aqi));
        progressChanger(o3Progress, Integer.parseInt(mRealTimeItems.get(index).o3Aqi));
        progressChanger(no2Progress, Integer.parseInt(mRealTimeItems.get(index).no2Aqi));
        progressChanger(pmProgress, Integer.parseInt(mRealTimeItems.get(index).pm25));

        coValueText.setText(mRealTimeItems.get(index).coAqi);
        so2ValueText.setText(mRealTimeItems.get(index).so2Aqi);
        o3ValueText.setText(mRealTimeItems.get(index).o3Aqi);
        no2ValueText.setText(mRealTimeItems.get(index).no2Aqi);
        pmValueText.setText(mRealTimeItems.get(index).pm25);

        temperatureTxv.setText(String.format("%.1f", Float.parseFloat(mRealTimeItems.get(index).temperature)));
        temperatureTxv.setTextColor(colorCalculator.temperatureColorPicker(Float.parseFloat(mRealTimeItems.get(index).temperature)));

    }
    private void initRefreshHandler(){
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                requestMessageProcess();
                gpsInfo.getLocation();
                Log.d("mHandler","handler working");
            }
        };
    }
    public void startDataRefresher(){
        refreshTimer = new Timer();
        refreshTimer.schedule(dataRefresher(), 0, refreshTime);
    }

    public void stopDataRefresher(){
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
}
    private TimerTask dataRefresher() {
        TimerTask dataTimerTask = new TimerTask() {
            @Override
            public void run() {
                Message msg = mHandler.obtainMessage();
                mHandler.sendMessage(msg);
            }
        };

        return dataTimerTask;
    }

    /////////////////////   Data communication   /////////////////////
    protected boolean requestMessageProcess(){
        if (mAuthTask != null) {
            return false;
        }
        PostMessageMaker postMessageMaker;
        Integer USN = MainActivity.USER_SEQUENCE_NUMBER;
        Integer NSC = MainActivity.NUMBER_OF_SIGNED_IN_COMPLETIONS;
        double lat = gpsInfo.getLatitude();
        double lon = gpsInfo.getLongitude();

        postMessageMaker = new PostMessageMaker(MessageType.SAP_RAVREQ_TYPE, 33, USN);
        postMessageMaker.inputPayload(String.valueOf(NSC), String.valueOf(lat), String.valueOf(lon), String.valueOf(mMapZoomLevel));

        String reqMsg = postMessageMaker.makeRequestMessage();
        Log.d("The Request is",reqMsg);
        mAuthTask = new HttpConnectionThread(context);
        try {
            String airUrl = getString(R.string.air_url);
            messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mRealTimeItems.size() != 0){
            realTimeAirView.setVisibility(View.VISIBLE);
            realTimeAirViewNone.setVisibility(View.GONE);
            realTimeTempView.setVisibility(View.VISIBLE);
            realTimeTempViewNone.setVisibility(View.GONE);

        }
        return false;
    }

    //Response message parsing and processing
    private void messageResultProcess(String responseMsg){
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL)){
                Toast.makeText(context, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            } else {
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");
                int USN = MainActivity.USER_SEQUENCE_NUMBER;
                if (msgType == MessageType.SAP_RAVRSP_TYPE && endpointId == USN) {
                    int resultCode = jsonPayload.getInt(RESULT_CODE);
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_RAV_OK:
                            listTupleParser(new JSONArray(jsonPayload.getString(LIST_NAME)));
                            break;
                        case ResultCode.RESCODE_SAP_RAV_OTHER:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_RAV_UNALLOCATED_USER_SEQUENCE_NUMBER:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            getActivity().finish();
                            break;
                        case ResultCode.RESCODE_SAP_RAV_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            Intent loginIt = new Intent(context, SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        default:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                }

                mAuthTask = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        mAuthTask = null;
    }
    //RAV List Item Parser
    public void listTupleParser(JSONArray listData){
        mSpinnerItems.clear();
        mRealTimeItems.clear();
        for(int i=0; i < listData.length(); i++){
            RealTimeDataItem airDataTuple = null;
            String[] tuple = new String[0];
            try {
                tuple = listData.get(i).toString().split(",");
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            if (tuple.length == TUPLE_LENGTH){
//                String wifiMacAddress = tuple[0];
//                String timestamp = tuple[1];
//                String temperature = tuple[2];
//                String coAqi = tuple[3];
//                String o3Aqi = tuple[4];
//                String no2Aqi = tuple[5];
//                String so2Aqi = tuple[6];
//                String pm25Aqi = tuple[7];
//                String pm10Aqi = tuple[8];
//                Double latitude = Double.valueOf(tuple[9]);
//                Double longitude = Double.valueOf(tuple[10]);
//                airDataTuple = new RealTimeDataItem(wifiMacAddress, timestamp, temperature, coAqi,
//                        o3Aqi, no2Aqi, so2Aqi, pm25Aqi, pm10Aqi, latitude, longitude);
//                String spinnerString = wifiMacAddress + " (" + latitude + ", " + longitude +")";
//                mSpinnerItems.add(spinnerString);
//            }
            if(true){
                String wifiMacAddress = tuple[0];
                String timestamp = tuple[1];
                String temperature = tuple[4];
                String coAqi = tuple[11];
                String o3Aqi = tuple[12];
                String no2Aqi = tuple[13];
                String so2Aqi = tuple[14];
                String pm25Aqi = tuple[15];
                String pm10Aqi = tuple[16];
                Double latitude = Double.valueOf(tuple[2]);
                Double longitude = Double.valueOf(tuple[3]);
                airDataTuple = new RealTimeDataItem(wifiMacAddress, timestamp, temperature, coAqi,
                        o3Aqi, no2Aqi, so2Aqi, pm25Aqi, pm10Aqi, latitude, longitude);
                String spinnerString = wifiMacAddress + " (" + latitude + ", " + longitude +")";
                mSpinnerItems.add(spinnerString);

            }
            try {
                mRealTimeItems.add(airDataTuple);
            } catch (Exception e){
                e.printStackTrace();
            };
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter(context, R.layout.support_simple_spinner_dropdown_item, mSpinnerItems);
        sensorSelectSpinner.setAdapter(spinnerAdapter);

    }
    // Data communication

    /////////////////////   Progressbar   /////////////////////
    private void initProgress(ProgressBar pb, int max) {
        pb.setMax(max);
        progressChanger(pb,0);
        pb.setScaleY(3.0f);
    }

    private void progressChanger(ProgressBar pb, int value){
        int lastProgress = pb.getProgress();
        pb.getProgressDrawable().setColorFilter(colorCalculator.aqiColorPicker(value), PorterDuff.Mode.SRC_IN);
        pb.setProgress(value);
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress",lastProgress, value);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }
    //Progressbar


    /////////////////////   Map Ready   /////////////////////
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng currentLocation = new LatLng(gpsInfo.getLatitude(),gpsInfo.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(currentLocation).title("Current Location").snippet("Qualcomm Institute");
        googleMap.addMarker(markerOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(mMapZoomLevel));
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_retro));
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
    }
    //  Map Ready


    @Override
    public void onStart() {
        super.onStart();
        map.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        map.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        map.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        map.onLowMemory();
        stopDataRefresher();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //액티비티가 처음 생성될 때 실행되는 함수
        if(map != null)
        {
            map.onCreate(savedInstanceState);
        }
    }


}
