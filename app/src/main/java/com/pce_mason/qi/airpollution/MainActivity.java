package com.pce_mason.qi.airpollution;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.button.MaterialButton;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.HeartDataManagements.HeartConnectFragment;
import com.pce_mason.qi.airpollution.HeartDataManagements.HeartDataGenerator;
import com.pce_mason.qi.airpollution.HeartDataManagements.HeartDataItem;
import com.pce_mason.qi.airpollution.KeepAliveChecker.CheckerDialogFragment;
import com.pce_mason.qi.airpollution.KeepAliveChecker.KeepAliveTimer;
import com.pce_mason.qi.airpollution.MapManagements.GpsInfo;
import com.pce_mason.qi.airpollution.RealTimeAirDataMonitoring.RealTimeDataFragment;
import com.pce_mason.qi.airpollution.SensorManagements.SensorManagementsFragment;
import com.pce_mason.qi.airpollution.UserManagements.PasswordChangeFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, CheckerDialogFragment.KeepAliveDialogListener{
    private Toolbar toolbar;

    // BackPress
    private BackPressCloseHandler backPressCloseHandler;

    //Static Variable
    public static StateNumber.STATE_SAP APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
    public static String USER_ID;
    public static Integer USER_SEQUENCE_NUMBER = 0;
    public static Integer NUMBER_OF_SIGNED_IN_COMPLETIONS = 0;
    public static Integer CONNECTION_ID = null;
    public static HeartDataGenerator HEART_GENERATOR = null;

    //UI Component
    private ProgressBar mProgressbar;
    private HttpConnectionThread mAuthTask;
    private CoordinatorLayout mFrontMain;
    public RelativeLayout searchBar;
    private TextView navTitleTxv, navWelcomtxv;
    private Button navSigninBtn;
    private NavigationView navigationView;
    private LinearLayout userIdLayout;

    //Real-Time Heart data Items
    private ConcurrentHashMap<String,HeartDataItem> mHeartDataList, mUnsuccessfulDataList, mTransferringDataList;
    private HeartDataItem PastDataStore;
    private int TimestampRecord = 0;

    //Response msg String
    private String connectionId = "cid";
    private String measurementInterval = "mti";
    private String transmissionInterval = "tti";

    //Real-Time Heart-related Data Acknowledge
    private String SuccessfulReceptionFlag = "successfulRcptFlg";
    private String ContinuityOfSuccessfulRecption = "continuityOfSuccessfulRcpt";
    private String NumberOfSuccessfulReceptions = "numOfSuccessfulRcpt";
    private String ListOfSuccessfulTimestamps = "listOfSuccessfulTs";
    private String RetransmissionRequestFlag = "retransReqFlg";
    private String ContinuityOfRetransmissionRequest = "continuityOfRetransReq";
    private String NumberOfRetransmissionRequests = "numOfRetransReq";
    private String ListOfUnsuccessfulTimestamps = "listOfUnSuccessfulTs";


    //Real-Time heart data transfer variable
    private int transmissionCount = 0;
    private int measurementCount = 0;
    private String mConnectionId = null;

    //Keep Alive Timer
    KeepAliveTimer keepAliveTimer;

    //MessageTypeString
    String MessageTypeString;

    //CheckerDialogFragment override
    @Override
    public void onDialogAliveClick(DialogFragment dialog) {
        keepAliveTimer.stopKasGraceTimer();
        keepAliveTimer.startKasTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        keepAliveTimer.stopAllTimer();
    }

    @Override
    protected void onDestroy() {
        ReleaseUserInformation();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar= (Toolbar) findViewById(R.id.toolbar);
        backPressCloseHandler = new BackPressCloseHandler(this);

        Intent intent = getIntent();
        USER_ID = intent.getStringExtra(getString(R.string.email_intent_string));
        USER_SEQUENCE_NUMBER = intent.getIntExtra(getString(R.string.USN_Intent_string),0);
        NUMBER_OF_SIGNED_IN_COMPLETIONS = intent.getIntExtra(getString(R.string.NSC_Intent_string),0);

        mHeartDataList = new ConcurrentHashMap<String, HeartDataItem>();
        mUnsuccessfulDataList= new ConcurrentHashMap<String, HeartDataItem>();
        mTransferringDataList= new ConcurrentHashMap<String, HeartDataItem>();
        PastDataStore = new HeartDataItem(null,null,null,null,null);

        keepAliveTimer = new KeepAliveTimer(MainActivity.this, getSupportFragmentManager());
        keepAliveTimer.startKasTimer();

        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        searchBar = (RelativeLayout) findViewById(R.id.search_bar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
        //if you clicked sidebar, the activate keyboard will hide.
            @Override
            public void onDrawerSlide (View drawerView, float slideOffset) {
                super.onDrawerOpened(drawerView);
                    hideKeyboard(MainActivity.this);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header = navigationView.getHeaderView(0);
        navTitleTxv = (TextView) header.findViewById(R.id.nav_title_user_id);
        navSigninBtn = (Button) header.findViewById(R.id.nav_signIn_btn);
        navWelcomtxv = (TextView) header.findViewById(R.id.nav_welcome_text);
        userIdLayout = (LinearLayout)header.findViewById(R.id.nav_title_user_id_layout);

        navSigninBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // navigation menu change
        Menu menu = navigationView.getMenu();
        MenuItem NV_passwordchange_item = menu.findItem(R.id.nav_passwordChange);
        MenuItem NV_signout_item = menu.findItem(R.id.nav_signOut);

        if(USER_SEQUENCE_NUMBER != 0)
        {
            navSigninBtn.setVisibility(View.GONE);
            navWelcomtxv.setText("Welcome");
            NV_passwordchange_item.setVisible(true);
            NV_signout_item.setVisible(true);
            userIdLayout.setVisibility(View.VISIBLE);
            navTitleTxv.setText(USER_ID);
        }
        else if(USER_SEQUENCE_NUMBER == 0)
        {
            navSigninBtn.setVisibility(View.VISIBLE);
            navWelcomtxv.setText("Hi! there");
            userIdLayout.setVisibility(View.GONE);
            NV_passwordchange_item.setVisible(false);
            NV_signout_item.setVisible(false);
        }


        locationPermissionCheck();

        mProgressbar = (ProgressBar) findViewById(R.id.main_progress);
        mFrontMain = (CoordinatorLayout) findViewById(R.id.main_front_view);
        displaySelectedFragment(R.id.nav_home); //display change main activity view -> main fragment view

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadcastReceiver, new IntentFilter("heartData"));
        //LocalBroadcastManager resister
    }

    // HeartDataGenerator에 의해서 데이터가 발생하기 시작하면(Intent로 BroadCast 되는걸 여기서 잡아준다) 이 함수에서 데이터를 잡아서
    // 데이터를 List에 추가시켜주는데 이때 ConnectionID가 발급되어 있지 않다면 DCA를 요청하게끔 되어있다.
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try
            {
                // TemporaryHeartDataItem
                HeartDataItem TempHeartData = new HeartDataItem(null,null,null,null,null);

                //Receive Broadcast Heart Data
                String heartRate = intent.getStringExtra("heartRate");
                String timeStamp = intent.getStringExtra("timeStamp");
                String latitude = intent.getStringExtra("latitude");
                String longitude = intent.getStringExtra("longitude");
                String RRInterval;
                Double RRIntervalString = 60/Double.valueOf(heartRate);
                RRInterval = RRIntervalString.toString();
                HeartDataItem heartDataItem = new HeartDataItem(timeStamp,heartRate,RRInterval,latitude,longitude);

                if(mConnectionId != null)
                {
                    if(TimestampRecord == 0) { TimestampRecord = Integer.valueOf(timeStamp); }
                    int TimestampNumericalStart = TimestampRecord%transmissionCount;
                    if(Integer.valueOf(timeStamp)%transmissionCount == (TimestampNumericalStart+mHeartDataList.size())%transmissionCount)
                    {
                        mHeartDataList.put(timeStamp,heartDataItem);
                        heartDataItem = null;
                    }
                    else
                    {
                        int Temp = Integer.valueOf(timeStamp)%transmissionCount;
                        Temp = Temp==0?transmissionCount:Temp;
                        if(Temp - ((TimestampNumericalStart+mHeartDataList.size())%transmissionCount) < 0)
                        {
                            heartDataItem = null;
                        }
                        else if(Temp - ((TimestampNumericalStart+mHeartDataList.size())%transmissionCount) > 0)
                        {
                            Log.d("D/RHD_ACK_TEST","Catch missing data and replace");
                            TempHeartData.timeStamp = String.valueOf(Integer.valueOf(timeStamp)-1);
                            TempHeartData.heartRate = TempHeartData.rrInterval = TempHeartData.latitude = TempHeartData.longitude = getString(R.string.Garbage_data_for_missing_data);
                            mHeartDataList.put(TempHeartData.timeStamp,TempHeartData);
                        }
                    }

                }
                if(mConnectionId != null) Log.d("mBroadcastReceiver",mConnectionId);
                if(mConnectionId == null) {
                    requestMessageProcess(MessageType.SAP_DCAREQ_TYPE);
                }
                if(mConnectionId != null && mHeartDataList.size() >= transmissionCount){            //DCA에서 transmissiontimeinterval을 받아와서 Count를 책정한다. 10이 기본
                    requestMessageProcess(MessageType.SAP_RHDTRN_TYPE);                             // 카운트보다 많아진다면 RHD를 시작한다.
                }
                if(heartDataItem != null && TimestampRecord != 0)
                {
                    mHeartDataList.put(timeStamp,heartDataItem);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            GpsInfo gpsInfo = new GpsInfo(MainActivity.this);
            gpsInfo.getLocation();  //gps update

            try {
                HEART_GENERATOR.setGPSData(gpsInfo.getLatitude(), gpsInfo.getLongitude());
            }catch (NullPointerException e){
                e.printStackTrace();
            }

        }
    };



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            backPressCloseHandler.onBackPressed();
            //super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.ToolbarMenu:
                if(toolbar.getTitle().toString().equals(getResources().getString(R.string.label_heart_rate)))
                {
                    if(APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE)
                    {
                        final AlertDialog dialog;
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        View mView = getLayoutInflater().inflate(R.layout.dialog_sensor_dcd,null);
                        AppCompatButton DCD_OK_Btn = (AppCompatButton)mView.findViewById(R.id.DCD_OK_btn);
                        AppCompatButton DCD_Cancel_Btn = (AppCompatButton)mView.findViewById(R.id.DCD_Cancel_btn);

                        builder.setView(mView);
                        dialog = builder.create();

                        DCD_OK_Btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestMessageProcess(MessageType.SAP_DCDNOT_TYPE);
                                dialog.dismiss();
                            }
                        });
                        DCD_Cancel_Btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "Disconnect Canceled", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        });

                        dialog.show();
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Please, Sensor Connect First", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void displaySelectedFragment(int id){
        Fragment fragment = null;
        searchBar.setVisibility(View.GONE);
        switch (id){
            case R.id.nav_home:
                toolbar.setTitle(getResources().getString(R.string.home_title));
                ToolbarMenuReset();
                fragment = MainFragment.newInstance(MainActivity.this);
                break;
            case R.id.nav_real_time_map:
                toolbar.setTitle(getResources().getString(R.string.label_real_time_map));
                ToolbarMenuReset();
                fragment = RealTimeDataFragment.newInstance(MainActivity.this);
                searchBar.setVisibility(View.VISIBLE);
                break;
            case R.id.nav_heart:
                if(USER_SEQUENCE_NUMBER != 0)
                {
                    toolbar.setTitle(getResources().getString(R.string.label_heart_rate));
                    toolbar.getMenu().getItem(0).setTitle(R.string.sensor_dcd);
                    fragment = HeartConnectFragment.newInstance(this);
                }
                break;
            case  R.id.nav_sensorManagements:
                if(USER_SEQUENCE_NUMBER != 0) {
                    toolbar.setTitle(getResources().getString(R.string.sensor_managements_title));
                    ToolbarMenuReset();
                    fragment = SensorManagementsFragment.newInstance(MainActivity.this);
                }
                break;
            case  R.id.nav_passwordChange:
                if(USER_SEQUENCE_NUMBER != 0) {
                    toolbar.setTitle(getResources().getString(R.string.password_change_title));
                    ToolbarMenuReset();
                    fragment = PasswordChangeFragment.newInstance(MainActivity.this);
                }
                break;
        }
        if(fragment != null){
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container,fragment);
            fragmentTransaction.commit();
        }
        else
        {
            Toast.makeText(this, getString(R.string.sign_in_warning), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_signOut && USER_SEQUENCE_NUMBER != 0){
            showSignOutDialog();
        }else{
            displaySelectedFragment(id);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }

    public void locationPermissionCheck() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }

    private void requestMessageProcess(int msgType){
        if (mAuthTask != null) {
            return;
        }
        hideKeyboard(MainActivity.this);
        PostMessageMaker postMessageMaker = null;
        int TemporaryRetrievalValue = 1;
        int TimerValue = 15000;
        MessageTypeString = "";
        switch (msgType){
            case MessageType.SAP_SGONOT_TYPE:
                if(APP_STATE == StateNumber.STATE_SAP.USN_INFORMED_STATE || APP_STATE == StateNumber.STATE_SAP.HALF_CID_INFORMED_STATE ||
                        APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE || APP_STATE == StateNumber.STATE_SAP.HALF_CID_RELEASED_STATE ) {
                    MessageTypeString = "SGO_NOT";
                    TimerValue = T104;
                    TemporaryRetrievalValue = R104;
                    APP_STATE = StateNumber.STATE_SAP.HALF_IDLE_STATE;
                    StateCheck(MessageTypeString);
                    postMessageMaker = new PostMessageMaker(MessageType.SAP_SGONOT_TYPE, 33, USER_SEQUENCE_NUMBER);
                    postMessageMaker.inputPayload(String.valueOf(NUMBER_OF_SIGNED_IN_COMPLETIONS));
                    Log.d("SGO_NOT_TEST","SGO NOT Message packing");
                }else{
                    Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                }
                break;

            case MessageType.SAP_DCAREQ_TYPE:
                if(APP_STATE == StateNumber.STATE_SAP.USN_INFORMED_STATE) {
                    MessageTypeString = "DCA_REQ";
                    TimerValue = T111;
                    TemporaryRetrievalValue = R111;
                    postMessageMaker = new PostMessageMaker(MessageType.SAP_DCAREQ_TYPE, 33, USER_SEQUENCE_NUMBER);
                    postMessageMaker.inputPayload(String.valueOf(NUMBER_OF_SIGNED_IN_COMPLETIONS));
                    Log.d("DCA_REQ_TEST","DCA REQ Message packing");
                }else{
                    Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                }
                break;

            case MessageType.SAP_RHDTRN_TYPE:
                if(APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE) {
                    MessageTypeString = "RHD_TRN";
                    TimerValue = T113;
                    TemporaryRetrievalValue = R113;
                    if(mUnsuccessfulDataList.size()==0 && mHeartDataList.size() == 0)
                    {
                        Toast.makeText(this, "HeartData is not exist", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<HeartDataItem> subHeartDataList = new ArrayList<HeartDataItem>();
                    subHeartDataList = fnSetTransferHeartRelatedData();
                    postMessageMaker = new PostMessageMaker(MessageType.SAP_RHDTRN_TYPE, 33, CONNECTION_ID);
                    postMessageMaker.inputPayload(subHeartDataList);
                    Log.d("RHD_TRN_TEST","RHD TRN Message packing");
                    break;
                }

            case MessageType.SAP_DCDNOT_TYPE:
                if(APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE) {
                    MessageTypeString = "DCD_NOT";
                    TimerValue = T112;
                    TemporaryRetrievalValue = R112;
                    postMessageMaker = new PostMessageMaker(MessageType.SAP_DCDNOT_TYPE, 33, CONNECTION_ID);
                    Log.d("DCD_NOT_TEST","DCD NOT Message packing");
                }
                break;

        }
        try {
            String datUrl;
            switch (msgType){
                case MessageType.SAP_RHDTRN_TYPE:
                    datUrl = getString(R.string.heart_url);
                    break;
                default:
                    datUrl = getString(R.string.air_url);
            }
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d(MessageTypeString+"_TEST",reqMsg);
            boolean RetryFlag = false;
            for(int i=0;i<TemporaryRetrievalValue;i++)
            {
                StateChange(MessageTypeString);
                StateCheck(MessageTypeString);
                mAuthTask = new HttpConnectionThread(MainActivity.this,TimerValue);
                Log.d(MessageTypeString+"_TEST",MessageTypeString+" Message Send");
                RetryFlag = messageResultProcess(mAuthTask.execute(datUrl, reqMsg).get(),MessageTypeString);
                if(RetryFlag) { break; }
            }
            if(RetryFlag == false) // Rxxx Reached
            {
                if(MessageTypeString.equals("SGO_NOT"))
                {
                    Log.d("SGO_ACK_TEST","R104 reached");
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SGO_ACK");
                }
                else if(MessageTypeString.equals("DCD_NOT"))
                {
                    Log.d("DCD_ACK_TEST","R112 reached");
                    APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                    StateCheck("DCD_ACK");
                    MainActivity.HEART_GENERATOR.stopDataGenerate();
                    CONNECTION_ID = null;
                    mConnectionId = null;
                }
                else if(MessageTypeString.equals("RHD_TRN"))
                {
                    Log.d("RHD_ACK_TEST","R113 reached");
                    MainActivity.HEART_GENERATOR.stopDataGenerate();
                    APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                    StateCheck("RHD_ACK");
                    CONNECTION_ID = null;
                    mConnectionId = null;
                    fnHeartDataClear();                                                                         // 데이터를 삭제한다.
                    //fnRestoreHeartData();                                                                     // DCA를 다시 할 수도 있으니 데이터를 보관하고 있다가 다음번 RHD에 담을 수 있게 한다.
                }
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg,String MsgType){
        boolean RetrivalCheck = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("") || responseMsg.equals("{}")){
                if(MsgType == "DCD_NOT")
                {
                    Toast.makeText(this, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
                    APP_STATE = StateNumber.STATE_SAP.CID_INFORMED_STATE;
                    StateCheck("DCD_ACK");
                }
                else
                {
                    StateCheck(MessageTypeString);
                    Toast.makeText(this, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");

                if (msgType == MessageType.SAP_SGOACK_TYPE && endpointId == USER_SEQUENCE_NUMBER && MsgType == "SGO_NOT") {
                    Log.d("SGO_ACK_TEST","SGO ACK Message Received");
                    Log.d("SGO_ACK_TEST",responseMsg);
                    RetrivalCheck = true;
                    int resultCode = jsonPayload.getInt("resultCode");
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_SGO_OK:

                            break;
                        case ResultCode.RESCODE_SAP_SGO_OTHER:
                            break;
                        case ResultCode.RESCODE_SAP_SGO_UNALLOCATED_USER_SEQUENCE_NUMBER:
                            break;
                        case ResultCode.RESCODE_SAP_SGO_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                            break;
                    }
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SGO_ACK");
                    Log.d("SGO_ACK_TEST","SGO ACK Message unpacking");
                } else if(msgType == MessageType.SAP_DCARSP_TYPE && endpointId == USER_SEQUENCE_NUMBER && MsgType == "DCA_REQ") {
                    RetrivalCheck = true;
                    Intent loginIt;
                    Log.d("DCA_RSP_TEST","DCA RSP Message Received");
                    Log.d("DCA_RSP_TEST",responseMsg);
                    int resultCode = jsonPayload.getInt("resultCode");
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_DCA_OK:
                            APP_STATE = StateNumber.STATE_SAP.CID_INFORMED_STATE;
                            StateCheck("DCA_RSP");
                            mConnectionId = jsonPayload.getString(connectionId);
                            CONNECTION_ID = Integer.parseInt(mConnectionId);
                            transmissionCount = jsonPayload.getInt(transmissionInterval);
                            measurementCount = jsonPayload.getInt(measurementInterval);
                            break;
                        case ResultCode.RESCODE_SAP_DCA_OTHER:
                            APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                            StateCheck("DCA_RSP");
                            Toast.makeText(MainActivity.this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_DCA_UNALLOCATED_USER_SEQUENCE_NUMBER:
                            APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                            StateCheck("DCA_RSP");
                            Toast.makeText(MainActivity.this, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(MainActivity.this, SignInActivity.class);
                            startActivity(loginIt);
                            finish();
                            break;
                        case ResultCode.RESCODE_SAP_DCA_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                            APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                            StateCheck("DCA_RSP");
                            Toast.makeText(MainActivity.this, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(MainActivity.this, SignInActivity.class);
                            startActivity(loginIt);
                            finish();
                            break;
                    }
                    Log.d("DCA_RSP_TEST","DCA RSP Message unpacking");
                }
                else if(msgType == MessageType.SAP_RHDACK_TYPE && endpointId == CONNECTION_ID && MsgType == "RHD_TRN")
                {

                    RetrivalCheck = true;
                    Log.d("RHD_ACK_TEST","RHD ACK Message Received");
                    Log.d("RHD_ACK_TEST",responseMsg);

                    if(jsonPayload.getInt(SuccessfulReceptionFlag) == 1)                                  // 여기서 성공적으로 송신된 Data들을 삭제해줘야 한다
                    {
                        fnDelTransferHeartRelatedData(jsonPayload.getJSONArray(ListOfSuccessfulTimestamps),jsonPayload.getInt(ContinuityOfSuccessfulRecption), jsonPayload.getInt(NumberOfSuccessfulReceptions));
                    }
                    if(jsonPayload.getInt(RetransmissionRequestFlag) == 1)
                    {
                        fnSetUnSuccessfulHeartData(jsonPayload.getJSONArray(ListOfUnsuccessfulTimestamps),jsonPayload.getInt(ContinuityOfRetransmissionRequest), jsonPayload.getInt(NumberOfRetransmissionRequests));
                    }
                    if(jsonPayload.getInt(SuccessfulReceptionFlag) == 0 && jsonPayload.getInt(RetransmissionRequestFlag) == 0)
                    {
                        Log.d("HeartDataTransfer","Heart DataTransfer have some wrong on transfer");
                    }
                    Log.d("RHD_ACK_TEST","RHD ACK Message unpacking");
                }

                else if(msgType == MessageType.SAP_DCDACK_TYPE && endpointId == CONNECTION_ID && MsgType == "DCD_NOT") {
                    RetrivalCheck = true;
                    Log.d("DCD_ACK_TEST","DCD ACK Message Received");
                    Log.d("DCD_ACK_TEST",responseMsg);
                    int resultCode = jsonPayload.getInt("resultCode");
                    APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                    MainActivity.HEART_GENERATOR.stopDataGenerate();
                    CONNECTION_ID = null;
                    mConnectionId = null;
                    switch (resultCode)
                    {
                        case ResultCode.RESCODE_SAP_DCD_OK:
                            Toast.makeText(MainActivity.this,getString(R.string.sensor_disconnect), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_DCD_OTHER:
                            Toast.makeText(MainActivity.this, "Unknown error / Sensor disconnected", Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_DCD_UNALLOCATED_CONNECTION_ID:
                            Toast.makeText(MainActivity.this, getString(R.string.unallocated_CID), Toast.LENGTH_SHORT).show();
                            APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                            break;
                    }
                    Log.d("DCD_ACK_TEST","DCD ACK Message unpacking");
                    StateCheck("DCD_ACK");
                }
                else // Other msgType
                {
                    if(CONNECTION_ID == null)
                    {
                        APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                    }
                    else
                    {
                        APP_STATE = StateNumber.STATE_SAP.CID_INFORMED_STATE;
                    }
                    receiveLog(MsgType,responseMsg);
                    Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                }
                mAuthTask = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        mAuthTask = null;
        showProgress(false);
        return RetrivalCheck;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mFrontMain.setVisibility(show ? View.GONE : View.VISIBLE);
            mFrontMain.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFrontMain.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressbar.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressbar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressbar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressbar.setVisibility(show ? View.VISIBLE : View.GONE);
            mFrontMain.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    private void showSignOutDialog(){
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_sign_out)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showProgress(true);
                        requestMessageProcess(MessageType.SAP_SGONOT_TYPE);
                        ReleaseUserInformation();
                        Toast.makeText(MainActivity.this, "Sign out complete", Toast.LENGTH_SHORT).show();
                        Intent loginIt = new Intent(MainActivity.this, SignInActivity.class);
                        startActivity(loginIt);
                        finish();
                    }
                }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        builder.show();
    }

    public static void StateCheck(String MsgType)
    {
        String statemsg="";
        switch (APP_STATE)
        {
            case IDLE_STATE:
                statemsg = "Idle state";
                break;
            case HALF_IDLE_STATE:
                statemsg = "Half Idle state";
                break;
            case CID_INFORMED_STATE:
                statemsg = "CID Informed state";
                break;
            case USN_INFORMED_STATE:
                statemsg = "USN Informed state";
                break;
            case HALF_CID_INFORMED_STATE:
                statemsg = "Half CID Informed state";
                break;
            case HALF_CID_RELEASED_STATE:
                statemsg = "Half CID Released state";
                break;
            case HALF_USN_ALLOCATE_STATE:
                statemsg = "Half USN Allocate state";
                break;
            case HALF_USN_INFORMED_STATE:
                statemsg = "Half USN Informed state";
                break;
            case USER_DUPLICATE_REQUESTED_STATE:
                statemsg = "User Duplicate requested state";
                break;
        }
        Log.d(MsgType + "_TEST",statemsg);
    }

    public void StateChange(String msgType)
    {
        switch (msgType) {
            case "SGO_NOT":
                APP_STATE = StateNumber.STATE_SAP.HALF_IDLE_STATE;
                break;
            case "DCA_REQ":
                APP_STATE = StateNumber.STATE_SAP.HALF_CID_INFORMED_STATE;
                break;
            case "DCD_NOT":
                APP_STATE = StateNumber.STATE_SAP.HALF_CID_RELEASED_STATE;
                break;
        }
    }

    public void receiveLog(String msgType,String rspMsg)
    {
        //SGO/RHD/DCD/DCA
        if(msgType == "SGO_NOT")
        {
            msgType = "SGO_ACK";
        }
        else if(msgType == "DCA_REQ")
        {
            msgType = "DCA_RSP";
        }
        else if(msgType == "DCD_NOT")
        {
            msgType = "DCD_ACK";
        }
        else if(msgType == "RHD_TRN")
        {
            msgType = "RHD_ACK";
        }
        Log.d(msgType+"_TEST",msgType+" message received");
        Log.d(msgType+"_TEST",rspMsg);
        StateCheck(msgType);
    }

    private void ToolbarMenuReset() {
        if(toolbar.getMenu().size()>0){
            toolbar.getMenu().getItem(0).setTitle(R.string.action_settings);
        }
    }

    private void ReleaseUserInformation()
    {
        NUMBER_OF_SIGNED_IN_COMPLETIONS = 0;
        USER_SEQUENCE_NUMBER = 0;
        CONNECTION_ID = null;
        mConnectionId = null;
        if(HEART_GENERATOR != null) { HEART_GENERATOR.stopDataGenerate(); }
        HEART_GENERATOR = null;
    }

    // HeartData들을 mTransferringDataList로 옮기고 전송할 데이터들을 리스트화 시켜서 반환해주는 함수
    private ArrayList<HeartDataItem> fnSetTransferHeartRelatedData()
    {
        ArrayList<HeartDataItem> TempHeartDataList = new ArrayList<HeartDataItem>();
        Iterator<String> currentDataKeys = mHeartDataList.keySet().iterator();
        Iterator<String> UnsuccessfulDataKeys = mUnsuccessfulDataList.keySet().iterator();
        List<String> SortList = new ArrayList<>();
        int DataCount = 0;

        while ( UnsuccessfulDataKeys.hasNext() )
        {
            String key = UnsuccessfulDataKeys.next();
            TempHeartDataList.add(mUnsuccessfulDataList.get(key));
            mTransferringDataList.put(mUnsuccessfulDataList.get(key).timeStamp, mUnsuccessfulDataList.remove(key));
        }

        while ( currentDataKeys.hasNext() )
        {
            String key = currentDataKeys.next();
            SortList.add(mHeartDataList.get(key).timeStamp);
        }
        Collections.sort(SortList);
        for(int i=0;i<transmissionCount;i++)
        {
            String key = SortList.get(i);
            TempHeartDataList.add(mHeartDataList.get(key));
            mTransferringDataList.put(mHeartDataList.get(key).timeStamp, mHeartDataList.remove(key));
        }

        return TempHeartDataList;
    }

    // 성공적으로 송신한 데이터들을 삭제해주는 함수
    private void fnDelTransferHeartRelatedData(JSONArray ListOfSuccessfulTs, int ContinuityOfSuccessfulReception, int NumberOfSuccessfulReceptions)
    {
        try
        {
            if(ContinuityOfSuccessfulReception == 0)                                                                            // 연속적이지 않다.
            {
                String[] Tuple = new String[NumberOfSuccessfulReceptions];
                for(int i=0;i<NumberOfSuccessfulReceptions;i++)
                {
                    Tuple[i] = ListOfSuccessfulTs.get(i).toString();
                }
                for(int i=0;i<NumberOfSuccessfulReceptions;i++)
                {
                    mTransferringDataList.remove(Tuple[i]).toString();
                }
            }
            else if(ContinuityOfSuccessfulReception == 1)                                                                       // 연속적이다.
            {
                String[] Tuple = new String[2];
                for(int i=0;i<2;i++)
                {
                    Tuple[i] = ListOfSuccessfulTs.get(i).toString();
                }
                long StartTs,EndTs;
                StartTs = Long.valueOf(Tuple[0]);
                EndTs = Long.valueOf(Tuple[1]);
                for(long i=StartTs; i<=EndTs; i++)
                {
                    mTransferringDataList.remove(String.valueOf(i)).toString();
                }
            }
            else
            {
                Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // 성공적으로 송신하지 못한 데이터들을 다시 송신하기 위해서 UnsuccessfulDataList로 옮기는 함수
    private void fnSetUnSuccessfulHeartData(JSONArray ListOfUnSuccessfulTs, int ContinuityOfRetransmissionRequest, int NumberOfRetransmissionRequests)
    {
        try
        {
            if(ContinuityOfRetransmissionRequest == 0)                                                                  // 연속적이지 않다.
            {
                String[] Tuple = new String[NumberOfRetransmissionRequests];
                for(int i=0;i<NumberOfRetransmissionRequests;i++)
                {
                    Tuple[i] = ListOfUnSuccessfulTs.get(i).toString();

                }
                for(int i=0;i<NumberOfRetransmissionRequests;i++)
                {

                    mUnsuccessfulDataList.put(Tuple[i],mTransferringDataList.remove(Tuple[i]));
                }
            }
            else if(ContinuityOfRetransmissionRequest == 1)                                                             // 연속적이다
            {
                String[] Tuple = new String[2];
                for(int i=0;i<2;i++)
                {
                    Tuple[i] = ListOfUnSuccessfulTs.get(i).toString();
                }
                long StartTs,EndTs;
                StartTs = Long.valueOf(Tuple[0]);
                EndTs = Long.valueOf(Tuple[1]);
                for(long i=StartTs; i<=EndTs; i++)
                {
                    mUnsuccessfulDataList.put(String.valueOf(i),mTransferringDataList.remove(String.valueOf(i)));
                }
            }
            else
            {
                Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void fnHeartDataClear()
    {
        mTransferringDataList.clear();
        mUnsuccessfulDataList.clear();
        mHeartDataList.clear();
    }

    private void fnRestoreHeartData()
    {
        Iterator<String> TransferringDataKeys = mTransferringDataList.keySet().iterator();

        while ( TransferringDataKeys.hasNext() )
        {
            String key = TransferringDataKeys.next();
            mUnsuccessfulDataList.put(mTransferringDataList.get(key).timeStamp, mTransferringDataList.remove(key));
        }
    }

    public class BackPressCloseHandler {
        private long backKeyPressedTime = 0;
        private Toast toast;
        private Activity activity;
        public BackPressCloseHandler(Activity context)
        {
            this.activity = context;
        }
        public void onBackPressed() {
            if (System.currentTimeMillis() > backKeyPressedTime + 2000)
            {
                backKeyPressedTime = System.currentTimeMillis();
                showGuide();
                return;
            }
            if (System.currentTimeMillis() <= backKeyPressedTime + 2000)
            {
                activity.finish();
                toast.cancel();
            }
        }

        public  void showGuide() {
            toast = Toast.makeText(activity,"Press again to Exit",Toast.LENGTH_SHORT);
            toast.show();
        }
    }

}
