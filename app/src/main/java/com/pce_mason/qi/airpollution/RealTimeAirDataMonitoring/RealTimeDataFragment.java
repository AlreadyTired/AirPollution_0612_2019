package com.pce_mason.qi.airpollution.RealTimeAirDataMonitoring;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.ColorCalculator;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.MainActivity;
import com.pce_mason.qi.airpollution.MapManagements.GpsInfo;
import com.pce_mason.qi.airpollution.MapManagements.MarkerRenderer;
import com.pce_mason.qi.airpollution.R;
import com.pce_mason.qi.airpollution.SignInActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R114;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T114;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class RealTimeDataFragment extends Fragment implements OnMapReadyCallback {
    private ClusterManager<RealTimeDataItem> mClusterManager;
    GoogleMap mMap;
    GpsInfo gpsInfo;
    Context context;

    Timer refreshTimer = null;
    private int refreshTime = 10 * 1000;
    private Handler mHandler;

    private ColorCalculator colorCalculator;

    private BottomSheetBehavior bottomSheetBehavior;
    private LinearLayout bottomSheet;
    private TextView wifiDetailTxv, coDetailTxv, o3DetailTxv, no2DetailTxv, so2DetailTxv, pm25DetailTxv, tempDetailTxv;
    private CircularProgressBar coDetailProgress, o3DetailProgress, no2DetailProgress, so2DetailProgress, pm25DetailProgress, tempDetailProgress;

    private float mMapZoomLevel = 14;

    private HttpConnectionThread mAuthTask = null;
    private final String RESULT_CODE ="resultCode";
    private final String LIST_NAME = "realtimeAirQualityDataList";
    private final int TUPLE_LENGTH = 11;
    private final int PROGRESS_STROKE = 15;
    float peekHeightDp;
    public List<RealTimeDataItem> mRealTimeItems = new ArrayList<RealTimeDataItem>();
    public RealTimeDataFragment() {
        // Required empty public constructor
    }

    public static RealTimeDataFragment newInstance(Context context) {
        RealTimeDataFragment fragment = new RealTimeDataFragment();
        fragment.context = context;
        fragment.gpsInfo = new GpsInfo(context);
        fragment.peekHeightDp = context.getResources().getDimension(R.dimen.real_time_detail_wifi);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        colorCalculator = new ColorCalculator();
        initRefreshHandler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDataRefresher();
    }

    static View view; // 프래그먼트의 뷰 인스턴스
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMap.clear();
        if(view!=null){
            ViewGroup parent = (ViewGroup)view.getParent();
            if(parent!=null){
                parent.removeView(view);
            }
        }
        stopDataRefresher();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        try {
            view = inflater.inflate(R.layout.fragment_real_time_data, container, false);
        }catch (InflateException e){

        }
        startDataRefresher();

//        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.realTimeAirDataList);
//        recyclerView.setLayoutManager(new LinearLayoutManager(context));
//
//        recyclerView.setAdapter(new RealTimeDataListAdapter(mRealTimeItems, context));

        bottomSheet = (LinearLayout) view.findViewById(R.id.real_time_air_bottom_view);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, peekHeightDp, getResources().getDisplayMetrics()));
        bottomSheet.setVisibility(View.GONE);

        wifiDetailTxv = (TextView) view.findViewById(R.id.real_time_air_wifi_detail);
        wifiDetailTxv.setHeight((int) (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, peekHeightDp, getResources().getDisplayMetrics()));
        coDetailTxv = (TextView) view.findViewById(R.id.co_detail_txv);
        o3DetailTxv = (TextView) view.findViewById(R.id.o3_detail_txv);
        no2DetailTxv = (TextView) view.findViewById(R.id.no2_detail_txv);
        so2DetailTxv = (TextView) view.findViewById(R.id.so2_detail_txv);
        pm25DetailTxv = (TextView) view.findViewById(R.id.pm25_detail_txv);
        tempDetailTxv = (TextView) view.findViewById(R.id.temperature_detail_txv);

        coDetailProgress = (CircularProgressBar) view.findViewById(R.id.co_detail_progress);
        o3DetailProgress = (CircularProgressBar) view.findViewById(R.id.o3_detail_progress);
        no2DetailProgress = (CircularProgressBar) view.findViewById(R.id.no2_detail_progress);
        so2DetailProgress = (CircularProgressBar) view.findViewById(R.id.so2_detail_progress);
        pm25DetailProgress = (CircularProgressBar) view.findViewById(R.id.pm25_detail_progress);
        tempDetailProgress = (CircularProgressBar) view.findViewById(R.id.temperature_detail_progress);

        initProgress(coDetailProgress,500,0);
        initProgress(o3DetailProgress,500,0);
        initProgress(no2DetailProgress,500,0);
        initProgress(so2DetailProgress,500,0);
        initProgress(pm25DetailProgress,500,0);
        initProgress(tempDetailProgress,100,-30);



//        MapView map = (MapView) view.findViewById(R.id.fragmentMap);
//        map.getMapAsync(this);
        final MapFragment mapFragment = (MapFragment) getActivity().getFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        mapFragment.getMapAsync(this);
        initAutocompletePlaceInput();
        return view;
    }

    private void initRefreshHandler(){
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                requestMessageProcess();
            }
        };
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

    private void initProgress(CircularProgressBar pb, int max, int min) {
        pb.setProgress(0);
        pb.setMin(min);
        pb.setMax(max);
        pb.setStrokeWidth(PROGRESS_STROKE);
        pb.setMax(max);
//        pb.setScaleY(2.5f);
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

    protected boolean requestMessageProcess(){
        if (APP_STATE == StateNumber.STATE_SAP.HALF_CID_INFORMED_STATE || APP_STATE == StateNumber.STATE_SAP.HALF_CID_RELEASED_STATE)
        {
            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
            return false;
        }
        else
        {
            PostMessageMaker postMessageMaker;
            int USN = MainActivity.USER_SEQUENCE_NUMBER;
            int NSC = MainActivity.NUMBER_OF_SIGNED_IN_COMPLETIONS;
            double lat = gpsInfo.getLatitude();
            double lon = gpsInfo.getLongitude();

            postMessageMaker = new PostMessageMaker(MessageType.SAP_RAVREQ_TYPE, 33, USN);
            postMessageMaker.inputPayload(String.valueOf(NSC), String.valueOf(lat), String.valueOf(lon));

            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("RAV_REQ_TEST","RAV REQ Packing");
            Log.d("RAV_REQ_TEST",reqMsg);
            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0;i<R114;i++)
                {
                    StateCheck("RAV_REQ");
                    mAuthTask = new HttpConnectionThread(context,T114);
                    Log.d("RAV_REQ_TEST","RAV REQ Message Send");
                    boolean RetryFlag = messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get());
                    if(RetryFlag) { break;}
                }

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalFlag = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("") || responseMsg.equals("{}")){
                Toast.makeText(context, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("RAV_RSP_TEST","RAV RSP Received");
                Log.d("RAV_RSP_TEST",responseMsg);
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");
                int USN = MainActivity.USER_SEQUENCE_NUMBER;
                if (msgType == MessageType.SAP_RAVRSP_TYPE && endpointId == USN) {
                    Log.d("RAV_RSP_TEST","RAV RSP unpacking");
                    RetrivalFlag = true;
                    Intent loginIt;
                    int resultCode = jsonPayload.getInt(RESULT_CODE);
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_RAV_OK:
                            listTupleParser(new JSONArray(jsonPayload.getString(LIST_NAME)));
                            break;
                        case ResultCode.RESCODE_SAP_RAV_OTHER:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_RAV_UNALLOCATED_USER_SEQUENCE_NUMBER:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("RAV_RSP");
                            Toast.makeText(context, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(context, SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        case ResultCode.RESCODE_SAP_RAV_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("RAV_RSP");
                            Toast.makeText(context, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(context, SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        default:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                }

                mAuthTask = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        mAuthTask = null;
        return RetrivalFlag;
    }
    //RAV List Item Parser

    public void listTupleParser(JSONArray listData){
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
            }
            try {
                mRealTimeItems.add(airDataTuple);
            } catch (Exception e){
                e.printStackTrace();
            };
        }
        setUpCluster();
    }
    /////////////////////   Progressbar   /////////////////////

    private void progressChanger(CircularProgressBar pb, int value){
        int lastProgress = pb.getProgress();
        pb.setColor(colorCalculator.aqiColorPicker(value));
        pb.setProgress(value);
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress",lastProgress, value);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }
    private void progressChanger(CircularProgressBar pb, float value){
        int lastProgress = pb.getProgress();
        pb.setColor(colorCalculator.temperatureColorPicker(value));
        pb.setProgress((int)value);
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress",lastProgress, (int)value);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }


//////////////////Map Function////////////////
    @Override
    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap;
        setUpCluster();
//        MarkerOptions markerOptions = new MarkerOptions().position(QI).title("Calit2").snippet("Qualcomm Institute");
//        googleMap.addMarker(markerOptions);
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(QI));
//        googleMap.animateCamera(CameraUpdateFactory.zoomTo(13));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gpsInfo.getLatitude(), gpsInfo.getLongitude()), mMapZoomLevel));
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_retro));
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                for(int i=0; i<mRealTimeItems.size(); i++){
                    if(mRealTimeItems.get(i).wifiMacAddress.equals(marker.getTitle())){
                        setBottomSheetItem(i);
                        bottomSheet.setVisibility(View.VISIBLE);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
                return false;
            }
        });
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                mMapZoomLevel = googleMap.getCameraPosition().zoom;
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Log.d("CAMERA_MOVE","MOVE");
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Log.d("CAMERA_MOVE","Click");
            }
        });

    }
    private void setBottomSheetItem(int itemIndex){
        SimpleDateFormat sensingTimeFormat = new SimpleDateFormat("MM-dd-yyyy hh:mm");
        sensingTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT-7"));
        Date sensingDate = new Date(Long.parseLong(mRealTimeItems.get(itemIndex).timestamp));
        String wifiTime = mRealTimeItems.get(itemIndex).wifiMacAddress + " (" +
                sensingTimeFormat.format(sensingDate) +")";

        wifiDetailTxv.setText(wifiTime);
        coDetailTxv.setText(mRealTimeItems.get(itemIndex).coAqi);
        o3DetailTxv.setText(mRealTimeItems.get(itemIndex).o3Aqi);
        no2DetailTxv.setText(mRealTimeItems.get(itemIndex).no2Aqi);
        so2DetailTxv.setText(mRealTimeItems.get(itemIndex).so2Aqi);
        pm25DetailTxv.setText(mRealTimeItems.get(itemIndex).pm25);
        tempDetailTxv.setText(mRealTimeItems.get(itemIndex).temperature);
        progressChanger(coDetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).coAqi));
        progressChanger(o3DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).o3Aqi));
        progressChanger(no2DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).no2Aqi));
        progressChanger(so2DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).so2Aqi));
        progressChanger(pm25DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).pm25));
        progressChanger(tempDetailProgress,Float.parseFloat(mRealTimeItems.get(itemIndex).temperature));

        wifiDetailTxv.setBackgroundColor(mRealTimeItems.get(itemIndex).mMarkerColor);
        wifiDetailTxv.setTextColor(colorCalculator.textColorPicker(mRealTimeItems.get(itemIndex).mHighestValue));


    }
    private void setUpCluster() {
        // Position the map.
        mMap.clear();

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<RealTimeDataItem>(context, mMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mClusterManager.setRenderer(new MarkerRenderer(context,mMap,mClusterManager,getLayoutInflater()));
        mMap.setOnCameraIdleListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        for(int i=0; i<mRealTimeItems.size(); i++){
            mClusterManager.addItem(mRealTimeItems.get(i));
        }

    }

    private void initAutocompletePlaceInput(){
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getActivity().getFragmentManager().findFragmentById(R.id.search_input);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                String locationName = String.valueOf(place.getName());
                Log.i("Search_result", "Place: " + locationName);
                LatLng location = place.getLatLng();

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, mMapZoomLevel));
                requestMessageProcess();
                markerInitialize(location, locationName);

                LatLngBounds bounds= mMap.getProjection().getVisibleRegion().latLngBounds;
                LatLng north = bounds.northeast;
                LatLng south = bounds.southwest;
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("Search_result", "An error occurred: " + status);
            }
        });
    }
    public void markerInitialize(LatLng location, String locationName){
        mMap.addMarker(new MarkerOptions().position(location).title(locationName));
    }
}
