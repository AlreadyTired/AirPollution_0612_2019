package com.pce_mason.qi.airpollution.RealTimeAirDataMonitoring;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.card.MaterialCardView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
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

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    //Marker
    RealTimeDataItem Past_selected_Item = null;
    Marker Past_selected_marker = null;

    //Bottom Sheet UI
    private TextView BottomAqiTxt, BottomTimeTxt, BottomCityTxt, BottomStateTxt, BottomNationTxt, BottomTexttxt, BottomTempTxt;
    Geocoder geocoder;
    List<Address> address;
    private ImageView BottomPin;

    //AutoComplete
    AutoCompleteTextView autoCompleteTextView;
    AutoCompleteAdapter adapter;
    PlacesClient placesClient;
    String apiKey = "AIzaSyDZPZjZ2VxDSI3N-JT970CuZVG3_ppm35c";
    ImageButton SearchBarClearBtn;

    //Floating Action Button
    FloatingActionMenu materialDesignFAM;
    FloatingActionButton Fab_NO2, Fab_O3, Fab_SO2, Fab_CO, Fab_PM25, Fab_AQI, CurrentLocationFAB;

    //Navigation bar Btn
    ImageButton MapHamBtn;

    Timer refreshTimer = null;
    private int refreshTime = 10 * 1000;
    private Handler mHandler;

    private ColorCalculator colorCalculator;

    private BottomSheetBehavior bottomSheetBehavior;
    private MaterialCardView bottomSheet;
    private TextView coDetailTxv, o3DetailTxv, no2DetailTxv, so2DetailTxv, pm25DetailTxv;
    private CircularProgressBar coDetailProgress, o3DetailProgress, no2DetailProgress, so2DetailProgress, pm25DetailProgress;

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

        geocoder = new Geocoder(getContext());
        bottomSheet = (MaterialCardView) view.findViewById(R.id.real_time_air_bottom_view);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics()));
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (materialDesignFAM.isOpened()) {
                    materialDesignFAM.close(true);
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });

        BottomAqiTxt = (TextView) view.findViewById(R.id.bottom_sheet_AQI_display);
        BottomTimeTxt = (TextView) view.findViewById(R.id.bottom_sheet_time_display);
        BottomCityTxt = (TextView) view.findViewById(R.id.bottom_sheet_city_display);
        BottomStateTxt = (TextView) view.findViewById(R.id.bottom_sheet_state_display);
        BottomNationTxt = (TextView) view.findViewById(R.id.bottom_sheet_nation_display);
        BottomTexttxt = (TextView) view.findViewById(R.id.bottom_sheet_text_display);
        BottomPin = (ImageView) view.findViewById(R.id.bottom_sheet_pin);

        coDetailTxv = (TextView) view.findViewById(R.id.co_detail_txv);
        o3DetailTxv = (TextView) view.findViewById(R.id.o3_detail_txv);
        no2DetailTxv = (TextView) view.findViewById(R.id.no2_detail_txv);
        so2DetailTxv = (TextView) view.findViewById(R.id.so2_detail_txv);
        pm25DetailTxv = (TextView) view.findViewById(R.id.pm25_detail_txv);
        BottomTempTxt = (TextView) view.findViewById(R.id.bottom_sheet_temp_display);

        coDetailProgress = (CircularProgressBar) view.findViewById(R.id.co_detail_progress);
        o3DetailProgress = (CircularProgressBar) view.findViewById(R.id.o3_detail_progress);
        no2DetailProgress = (CircularProgressBar) view.findViewById(R.id.no2_detail_progress);
        so2DetailProgress = (CircularProgressBar) view.findViewById(R.id.so2_detail_progress);
        pm25DetailProgress = (CircularProgressBar) view.findViewById(R.id.pm25_detail_progress);

        initProgress(coDetailProgress,500,0);
        initProgress(o3DetailProgress,500,0);
        initProgress(no2DetailProgress,500,0);
        initProgress(so2DetailProgress,500,0);
        initProgress(pm25DetailProgress,500,0);

        //Floating Action Button
        materialDesignFAM = (FloatingActionMenu) view.findViewById(R.id.material_design_android_floating_action_menu);
        Fab_NO2 = (FloatingActionButton) view.findViewById(R.id.fab_NO2);
        Fab_O3 = (FloatingActionButton) view.findViewById(R.id.fab_O3);
        Fab_SO2 = (FloatingActionButton) view.findViewById(R.id.fab_SO2);
        Fab_CO = (FloatingActionButton) view.findViewById(R.id.fab_CO);
        Fab_PM25 = (FloatingActionButton) view.findViewById(R.id.fab_PM25);
        Fab_AQI = (FloatingActionButton) view.findViewById(R.id.fab_AQI);
        CurrentLocationFAB = (FloatingActionButton) view.findViewById(R.id.current_location);

        materialDesignFAM.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (materialDesignFAM.isOpened()) {
                    materialDesignFAM.close(true);
                } else {
                    materialDesignFAM.open(true);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }

            }
        });
        Fab_NO2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu first item clicked
                Toast.makeText(getContext().getApplicationContext(), "FAB1", Toast.LENGTH_SHORT).show();
            }
        });
        Fab_O3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu second item clicked
                Toast.makeText(getContext().getApplicationContext(), "FAB2", Toast.LENGTH_SHORT).show();
            }
        });
        Fab_SO2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu third item clicked
                Toast.makeText(getContext().getApplicationContext(), "FAB3", Toast.LENGTH_SHORT).show();
            }
        });
        Fab_CO.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu third item clicked
                Toast.makeText(getContext().getApplicationContext(), "FAB4", Toast.LENGTH_SHORT).show();
            }
        });
        Fab_PM25.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu third item clicked
                Toast.makeText(getContext().getApplicationContext(), "FAB5", Toast.LENGTH_SHORT).show();
            }
        });
        Fab_AQI.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO something when floating action menu third item clicked
                Toast.makeText(getContext().getApplicationContext(), "FAB6", Toast.LENGTH_SHORT).show();
            }
        });

        CurrentLocationFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double Lat = gpsInfo.getLatitude();
                double lon = gpsInfo.getLongitude();
                LatLng latLng = new LatLng(Lat, lon);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
            }
        });

        //This code is to open the navigation bar using the other hamburger button instead of an app bar hamburger button
        final DrawerLayout mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        MapHamBtn = (ImageButton) view.findViewById(R.id.Navigation_Hamburg_Btn);
        MapHamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });

//        MapView map = (MapView) view.findViewById(R.id.fragmentMap);
//        map.getMapAsync(this);
        final MapFragment mapFragment = (MapFragment) getActivity().getFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        mapFragment.getMapAsync(this);
//        initAutocompletePlaceInput();

        //AutoCompleteTextview
        // Setup Places Client
        if (!Places.isInitialized()) {
            Places.initialize(getContext().getApplicationContext(), apiKey);
        }

        placesClient = Places.createClient(getContext());
        initAutoCompleteTextView();
        SearchBarClearBtn = (ImageButton)view.findViewById(R.id.EditTextClear);
        SearchBarClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoCompleteTextView.setText("");
            }
        });

        return view;
    }

    private void initAutoCompleteTextView() {

        autoCompleteTextView = view.findViewById(R.id.auto);
        autoCompleteTextView.setThreshold(1);
        autoCompleteTextView.setOnItemClickListener(autocompleteClickListener);
        adapter = new AutoCompleteAdapter(getContext(), placesClient);
        autoCompleteTextView.setAdapter(adapter);
    }

    private AdapterView.OnItemClickListener autocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            try {
                final AutocompletePrediction item = adapter.getItem(i);
                String placeID = null;
                if (item != null) {
                    placeID = item.getPlaceId();
                }

//                To specify which data types to return, pass an array of Place.Fields in your FetchPlaceRequest
//                Use only those fields which are required.

                List<com.google.android.libraries.places.api.model.Place.Field> placeFields = Arrays.asList(com.google.android.libraries.places.api.model.Place.Field.ID, com.google.android.libraries.places.api.model.Place.Field.NAME, com.google.android.libraries.places.api.model.Place.Field.ADDRESS
                        , com.google.android.libraries.places.api.model.Place.Field.LAT_LNG);

                FetchPlaceRequest request = null;
                if (placeID != null) {
                    request = FetchPlaceRequest.builder(placeID, placeFields)
                            .build();
                }

                if (request != null) {
                    placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onSuccess(FetchPlaceResponse task) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(task.getPlace().getLatLng()));
                            mMap.addMarker(new MarkerOptions().position(task.getPlace().getLatLng()));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

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

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Log.d("CAMERA_MOVE","Click");
            }
        });
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                mMapZoomLevel = googleMap.getCameraPosition().zoom;
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Log.d("CAMERA_MOVE","MOVE");
            }
        });
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gpsInfo.getLatitude(), gpsInfo.getLongitude()), mMapZoomLevel));
//        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_custom));
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                try {
                    if(Past_selected_marker != null)
                    {
                        Past_selected_marker.setIcon(bitmapDescriptorFromVector(context,R.drawable.marker_circle_50dp, Past_selected_Item.getMarkerColor(),1f, Past_selected_Item.mHighestValue, false));
                    }
                    for(int i=0; i<mRealTimeItems.size(); i++){
                        if (mRealTimeItems.get(i).wifiMacAddress.equals(marker.getTitle())) {
                            setBottomSheetItem(i);
                            bottomSheet.setVisibility(View.VISIBLE);
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            bottomSheetBehavior.setPeekHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, peekHeightDp, getResources().getDisplayMetrics()));
                            marker.setIcon(bitmapDescriptorFromVector(context,R.drawable.marker_circle_50dp, mRealTimeItems.get(i).getMarkerColor(),1f, mRealTimeItems.get(i).mHighestValue, true));
                            Past_selected_Item = mRealTimeItems.get(i);
                            Past_selected_marker = marker;
                        }
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                return false;
            }
        });


    }
    private void setBottomSheetItem(int itemIndex)
    {
        String City= "",State="",Nation="N/A";
        String currentLocationAddress = "Can't find location address";
        try{
            address = geocoder.getFromLocation(mRealTimeItems.get(itemIndex).latitude,mRealTimeItems.get(itemIndex).longitude,1);
            Log.d("TEST", address.toString());
            if(address != null && address.size() > 0)
            {
                currentLocationAddress = address.get(0).getAddressLine(0);
                Log.d("TEST",currentLocationAddress);

                String[] parsing = currentLocationAddress.split(",");
                Log.d("TEST",String.valueOf(parsing.length));
                if(parsing.length == 0)
                {
                    Nation = currentLocationAddress;
                }
                else
                {
                    for(int i=parsing.length-1;i>=0;i--)
                    {
                        if(i == parsing.length-1) { Nation = parsing[i]; }
                        else if( i == parsing.length-2) { State =" " + parsing[i].split(" ")[1]; }
                        else if( i == parsing.length-3) { City = parsing[i]; }
                    }
                }
            }
        }catch (IOException e)
        {
            e.printStackTrace();
        }
        BottomTempTxt.setText(mRealTimeItems.get(itemIndex).temperature + "°C");
        BottomTimeTxt.setText(setBottomSheetTime(itemIndex));
        BottomAqiTxt.setText("AQI\n" + mRealTimeItems.get(itemIndex).mHighestValue);
        BottomTexttxt.setText(mRealTimeItems.get(itemIndex).mExpression);
        BottomCityTxt.setText(City);
        BottomStateTxt.setText(State);
        BottomNationTxt.setText(Nation);
        BottomPin.setImageResource(mRealTimeItems.get(itemIndex).getPinColorID(mRealTimeItems.get(itemIndex).mHighestValue));
        coDetailTxv.setText(mRealTimeItems.get(itemIndex).coAqi);
        o3DetailTxv.setText(mRealTimeItems.get(itemIndex).o3Aqi);
        no2DetailTxv.setText(mRealTimeItems.get(itemIndex).no2Aqi);
        so2DetailTxv.setText(mRealTimeItems.get(itemIndex).so2Aqi);
        pm25DetailTxv.setText(mRealTimeItems.get(itemIndex).pm25);
        progressChanger(coDetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).coAqi));
        progressChanger(o3DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).o3Aqi));
        progressChanger(no2DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).no2Aqi));
        progressChanger(so2DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).so2Aqi));
        progressChanger(pm25DetailProgress,Integer.parseInt(mRealTimeItems.get(itemIndex).pm25));
    }

    //For current Time & Data Time compare and then set Time
    private String setBottomSheetTime(int itemIndex)
    {
        SimpleDateFormat sensingTimeFormat = new SimpleDateFormat("MM-dd-yyyy");
        sensingTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT-7"));
        Date sensingDate = new Date(Long.parseLong(mRealTimeItems.get(itemIndex).timestamp));
        String compareString = sensingTimeFormat.format(sensingDate);
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat currentDate = new SimpleDateFormat("MM-dd-yyyy");
        currentDate.setTimeZone(java.util.TimeZone.getTimeZone("GMT-7"));
        String currentTime = currentDate.format(date);
        String BottomSheetDate;
        if(currentTime.equals(compareString))
        {
            BottomSheetDate = "Today\n";
        }
        else
        {
            BottomSheetDate = compareString+"\n";
        }
        SimpleDateFormat Tsdf = new SimpleDateFormat("hh:mm aa");
        Tsdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-7"));
        BottomSheetDate = BottomSheetDate + Tsdf.format(sensingDate);
        return BottomSheetDate;
    }

    private void setUpCluster() {
        // Position the map.
        mMap.clear();
        Past_selected_marker = null;

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

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId, int markerColor, float zoomRate, int AqiValue, boolean BorderChecker) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        int boundsWidth = (int)(vectorDrawable.getIntrinsicHeight() *zoomRate);
        int boundsHeight = (int)(vectorDrawable.getIntrinsicHeight() *zoomRate);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boundsWidth,boundsHeight,Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        vectorDrawable.setBounds(0,0,boundsWidth,boundsHeight);
        vectorDrawable.setTint(markerColor);
        vectorDrawable.draw(canvas);

        String text = String.valueOf(AqiValue);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text,bitmap.getWidth()/2,bitmap.getHeight()/2+10,paint);

        if(BorderChecker)
        {
            Paint borderpaint = new Paint();
            borderpaint.setARGB(255,255,255,255);
            borderpaint.setAntiAlias(true);
            borderpaint.setStyle(Paint.Style.STROKE);
            borderpaint.setStrokeWidth(5);
            canvas.drawCircle(boundsWidth/2,boundsHeight/2,38,borderpaint);
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
