package com.pce_mason.qi.airpollution.SensorManagements;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.MainActivity;
import com.pce_mason.qi.airpollution.R;
import com.pce_mason.qi.airpollution.SignInActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R107;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R108;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R109;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R110;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T107;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T108;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T109;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T110;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class SensorManagementsFragment extends Fragment {

    //Sensor Managements Default Value
    private String RESULT_CODE ="resultCode";
    private String LIST_NAME = "listEncodings";
    private String ASE_CODE = "aseCode";
    private int TUPLE_LENGTH = 9;

    //Floating Action Button
    FloatingActionMenu FAM_sensor;
    com.github.clans.fab.FloatingActionButton Fab_srg, Fab_sas;

    // MessagTypeString
    String MessageTypeString;

    private Context context;
    private HttpConnectionThread mAuthTask = null;
    private List<SensorItem> mSensorItems = new ArrayList<>();

    public SensorManagementsFragment() {
    }

    public static SensorManagementsFragment newInstance(Context context) {
        SensorManagementsFragment fragment = new SensorManagementsFragment();
        fragment.context = context;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_managements, container, false);

        final Context context = view.getContext();
        requestMessageProcess(MessageType.SAP_SLVREQ_TYPE);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new MySensorRecyclerViewAdapter(mSensorItems,context, SensorManagementsFragment.this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        ItemTouchHelper.SimpleCallback simpleCallback = new RecyclerItemTouchHelper(0,ItemTouchHelper.LEFT);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        FAM_sensor = (FloatingActionMenu)view.findViewById(R.id.fam_sensor_menu);
        Fab_sas = (com.github.clans.fab.FloatingActionButton)view.findViewById(R.id.fab_sas);
        Fab_srg = (com.github.clans.fab.FloatingActionButton)view.findViewById(R.id.fab_srg);

        FAM_sensor.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FAM_sensor.isOpened()) {
                    FAM_sensor.close(true);
                } else {
                    FAM_sensor.open(true);
                }
            }
        });

        Fab_sas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSensorAssociationDialog();
                FAM_sensor.close(true);
            }
        });

        Fab_srg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSensorRegistrationDialog();
                FAM_sensor.close(true);
            }
        });

        return view;
    }

    //Show SAS Dialog
    private void showSensorAssociationDialog(){

        final EditText sensorAssociationEdt;
        final Spinner sensorAssociationSpinner;

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.sensor_association_title));
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mView = inflater.inflate(R.layout.dialog_sensor_add, null);

        sensorAssociationEdt = mView.findViewById(R.id.sensorAssociation);
        sensorAssociationSpinner = mView.findViewById(R.id.sensorAssociationSpinner);

        builder.setView(mView)
                // Add action buttons
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }

                });
        final AlertDialog mAssociationDialog = builder.create();

        mAssociationDialog.show();

        //after dialog show()
        mAssociationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean cancel = false;
                View focusView = null;

                String wifiMacAddress = sensorAssociationEdt.getText().toString();
                int sensorMobility = sensorAssociationSpinner.getSelectedItemPosition();

                if (TextUtils.isEmpty(wifiMacAddress)) {
                    sensorAssociationEdt.setError(getString(R.string.error_field_required));
                    sensorAssociationEdt.requestFocus();
                } else if (!isMacAddressValid(wifiMacAddress)) {
                    sensorAssociationEdt.setError(getString(R.string.error_invalid_mac_address));
                    sensorAssociationEdt.requestFocus();
                }else{
                    Log.d("SAS_REQ_TEST","SAS Input Format Verified");
                    requestMessageProcess(MessageType.SAP_SASREQ_TYPE,wifiMacAddress,String.valueOf(sensorMobility));
                    mAssociationDialog.dismiss();
                }
            }
        });
    }

    //Show SRG Dialog
    private void showSensorRegistrationDialog(){

        final EditText sensorregistrationMACedt;
        final EditText sensorregistrationCellularedt;

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.sensor_registration_title));
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mView = inflater.inflate(R.layout.dialog_sensor_registration, null);

        sensorregistrationMACedt = mView.findViewById(R.id.SRGMACAddress);
        sensorregistrationCellularedt = mView.findViewById(R.id.SRGCellularAddress);

        builder.setView(mView)
                // Add action buttons
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }

                });
        final AlertDialog mSensorRegistrationDialog = builder.create();

        mSensorRegistrationDialog.show();

        //after dialog show()
        mSensorRegistrationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean cancel = false;
                View focusView = null;

                String wifiMacAddress = sensorregistrationMACedt.getText().toString();
                String celluarMacAddress = sensorregistrationCellularedt.getText().toString();


                if (TextUtils.isEmpty(wifiMacAddress)) {
                    sensorregistrationMACedt.setError(getString(R.string.error_field_required));
                    sensorregistrationMACedt.requestFocus();
                } else if (!isMacAddressValid(wifiMacAddress)) {
                    sensorregistrationMACedt.setError(getString(R.string.error_invalid_mac_address));
                    sensorregistrationMACedt.requestFocus();
                }else if (TextUtils.isEmpty(celluarMacAddress)) {
                    sensorregistrationCellularedt.setError(getString(R.string.error_field_required));
                    sensorregistrationCellularedt.requestFocus();
                } else if (!isMacAddressValid(celluarMacAddress)) {
                    sensorregistrationCellularedt.setError(getString(R.string.error_invalid_mac_address));
                    sensorregistrationCellularedt.requestFocus();
                }else{
                    Log.d("SRG_REQ_TEST","SRG Input Format Verified");
                    requestMessageProcess(MessageType.SAP_SRGREQ_TYPE,wifiMacAddress,celluarMacAddress);
                    mSensorRegistrationDialog.dismiss();
                }
            }
        });
    }


    //Repeat MacAddress Validation Check
    private boolean isMacAddressValid(String macAddress) {
        return Pattern.matches(DefaultValue.VALID_MAC_ADDRESS, macAddress);
    }

    //SLV, SAS, SDD Request Message Processor   (the return boolean for sdd that is in adapter)
    protected boolean requestMessageProcess(int requestMessageType, String... params){
        if(APP_STATE == StateNumber.STATE_SAP.USN_INFORMED_STATE || APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE)
        {
            PostMessageMaker postMessageMaker;
            int USN = MainActivity.USER_SEQUENCE_NUMBER;
            int NSC = MainActivity.NUMBER_OF_SIGNED_IN_COMPLETIONS;
            int TemporaryRetrievalValue=1;
            int TimerValue = 15000;
            MessageTypeString = "";

            switch(requestMessageType){
                case MessageType.SAP_SASREQ_TYPE:
                    TemporaryRetrievalValue = R108;
                    TimerValue = T108;
                    MessageTypeString="SAS_REQ";
                    StateCheck(MessageTypeString);
                    postMessageMaker = new PostMessageMaker(requestMessageType, 33, USN);
                    postMessageMaker.inputPayload(String.valueOf(NSC), params[0],params[1]);
                    break;
                case MessageType.SAP_SDDREQ_TYPE:
                    MessageTypeString = "SDD_REQ";
                    TemporaryRetrievalValue=R109;
                    TimerValue = T109;
                    StateCheck(MessageTypeString);
                    postMessageMaker = new PostMessageMaker(requestMessageType, 33, USN);
                    postMessageMaker.inputPayload(String.valueOf(NSC), params[0],params[1]);
                    break;
                default:    //SLVREQ TYPE
                    MessageTypeString = "SLV_REQ";
                    TemporaryRetrievalValue=R110;
                    TimerValue = T110;
                    StateCheck(MessageTypeString);
                    postMessageMaker = new PostMessageMaker(requestMessageType,33,USN);
                    postMessageMaker.inputPayload(String.valueOf(NSC));
                    break;
                case MessageType.SAP_SRGREQ_TYPE:
                    MessageTypeString = "SRG_REQ";
                    TemporaryRetrievalValue=R107;
                    TimerValue = T107;
                    StateCheck(MessageTypeString);
                    postMessageMaker = new PostMessageMaker(requestMessageType, 33, USN);
                    postMessageMaker.inputPayload(String.valueOf(NSC), params[0],params[1]);
                    break;
            }

            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d(MessageTypeString+"_TEST",MessageTypeString+" Message Packing");
            Log.d(MessageTypeString+"_TEST",reqMsg);
            boolean RetryFlag=false;

            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0;i<TemporaryRetrievalValue;i++)
                {
                    mAuthTask = new HttpConnectionThread(context,TimerValue);
                    Log.d(MessageTypeString+"_TEST",MessageTypeString+" Message Send");
                    RetryFlag = messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get());
                    if(RetryFlag){ break;}
                }
                return RetryFlag;
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
        boolean RetrievalCheckFlag = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("") || responseMsg.equals("{}")){
                Log.d(MessageTypeString+"_TEST","Response : " + responseMsg);
                Toast.makeText(context, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            }else {
                RetrievalCheckFlag = true;
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");
                int USN = MainActivity.USER_SEQUENCE_NUMBER;
                if (msgType == MessageType.SAP_SLVRSP_TYPE && endpointId == USN && MessageTypeString=="SLV_REQ") {
                    Log.d("SLV_RSP_TEST","SLV RSP Message Received");
                    Log.d("SLV_RSP_TEST",responseMsg);
                    slvMessageProcessor(jsonPayload);
                } else if(msgType == MessageType.SAP_SASRSP_TYPE && endpointId == USN && MessageTypeString=="SAS_REQ"){
                    Log.d("SAS_RSP_TEST","SAS RSP Mes'sage Received");
                    Log.d("SAS_RSP_TEST",responseMsg);
                    sasMessageProcessor(jsonPayload);
                } else if(msgType == MessageType.SAP_SDDRSP_TYPE && endpointId == USN && MessageTypeString=="SDD_REQ"){
                    Log.d("SDD_RSP_TEST","SDD RSP Message Received");
                    Log.d("SDD_RSP_TEST",responseMsg);
                    return sddMessageProcessor(jsonPayload);
                } else if(msgType == MessageType.SAP_SRGRSP_TYPE && endpointId == USN && MessageTypeString=="SRG_REQ") {
                    Log.d("SRG_RSP_TEST", "SRG RSP Message Received");
                    Log.d("SRG_RSP_TEST", responseMsg);
                    srgMessageProcessor(jsonPayload);
                }
                else
                {
                    Log.d(MessageTypeString+"_TEST",MessageTypeString+" Message Received");
                    Log.d(MessageTypeString+"_TEST",responseMsg);
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
        return RetrievalCheckFlag;
    }

    //SRG Message Processor
    public void srgMessageProcessor(JSONObject jsonPayload){
        try{
            Log.d("SRG_RSP_TEST","SRG RSP Message Unpacked");
            int resultCode = jsonPayload.getInt(RESULT_CODE);
            Intent loginIt;
            switch (resultCode){
                case ResultCode.RESCODE_SAP_SRG_OK:
                    Toast.makeText(context,getString(R.string.sensor_registration_success),Toast.LENGTH_SHORT).show();
                    requestMessageProcess(MessageType.SAP_SLVREQ_TYPE);
                    break;
                case ResultCode.RESCODE_SAP_SRG_OTHER:
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    break;
                case ResultCode.RESCODE_SAP_SRG_UNALLOCATED_USER_SEQUENCE_NUMBER:
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SRG_RSP");
                    Toast.makeText(context, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    break;
                case ResultCode.RESCODE_SAP_SRG_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                    Toast.makeText(context, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SRG_RSP");
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    break;
                default:
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //SAS Message Processor
    public void sasMessageProcessor(JSONObject jsonPayload){
        try{
            Log.d("SAS_RSP_TEST","SAS RSP Message Unpacked");
            int resultCode = jsonPayload.getInt(RESULT_CODE);
            Intent loginIt;
            switch (resultCode){
                case ResultCode.RESCODE_SAP_SAS_OK:
                    Toast.makeText(context,getString(R.string.sensor_association_success),Toast.LENGTH_SHORT).show();
                    requestMessageProcess(MessageType.SAP_SLVREQ_TYPE);
                    break;
                case ResultCode.RESCODE_SAP_SAS_OTHER:
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    break;
                case ResultCode.RESCODE_SAP_SAS_UNALLOCATED_USER_SEQUENCE_NUMBER:
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SAS_RSP");
                    Toast.makeText(context, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    break;
                case ResultCode.RESCODE_SAP_SAS_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SAS_RSP");
                    Toast.makeText(context, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    break;
                case ResultCode.RESCODE_SAP_SAS_NOT_EXIST_WIFI_MAC_ADDRESS:
                    Toast.makeText(context, getString(R.string.not_exist_mac_address), Toast.LENGTH_SHORT).show();
                    break;
                case ResultCode.RESCODE_SAP_SAS_THE_REQUESTED_WIFI_MAC_ADDRESS_WAS_ALREADY_ASSOCIATED_WITH_USN:
                    Toast.makeText(context, "This MAC address is already associated", Toast.LENGTH_SHORT).show();
                    break;
                case ResultCode.RESCODE_SAP_SAS_THE_REQUESTED_WIFI_MAC_ADDRESS_WAS_ALREADY_ASSOCIATED_WITH_OTHER:
                    Toast.makeText(context, "This MAC address is already associated with other user", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //SDD Message Processor
    public Boolean sddMessageProcessor(JSONObject jsonPayload){
        try{
            Intent loginIt;
            Log.d("SDD_RSP_TEST","SDD RSP Message Unpacked");
            int resultCode = jsonPayload.getInt(RESULT_CODE);
            switch (resultCode){
                case ResultCode.RESCODE_SAP_SDD_OK:
                    Toast.makeText(context,getString(R.string.sensor_dissociation_success),Toast.LENGTH_SHORT).show();
                    return true;
                case ResultCode.RESCODE_SAP_SDD_OTHER:
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    return false;
                case ResultCode.RESCODE_SAP_DCD_UNALLOCATED_CONNECTION_ID:
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SDD_RSP");
                    Toast.makeText(context, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    return false;
                case ResultCode.RESCODE_SAP_SDD_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SDD_RSP");
                    Toast.makeText(context, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    return false;
                case ResultCode.RESCODE_SAP_SDD_NOT_EXIST_WIFI_MAC_ADDRESS:
                    Toast.makeText(context, getString(R.string.not_exist_mac_address), Toast.LENGTH_SHORT).show();
                    return false;
                case ResultCode.RESCODE_SAP_SDD_THE_REQUESTED_USER_SEQUENCE_NUMBER_AND_WIFI_MAC_ADDRESS_ARE_NOT_ASSOCIATED:
                    Toast.makeText(context, "Requested WiFi MAC address is not associated", Toast.LENGTH_SHORT).show();
                    return false;
                default:
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    //SLV Message Processor
    public void slvMessageProcessor(JSONObject jsonPayload){
        try{
            mSensorItems.clear();
            Log.d("SLV_RSP_TEST","SLV RSP Message Unpacked");
            int resultCode = jsonPayload.getInt(RESULT_CODE);
            Intent loginIt;
            switch (resultCode){
                case ResultCode.RESCODE_SAP_SLV_OK:
                    if(jsonPayload.getString("selectedSensorInformationList")!=null)
                    {
                        listTupleParser(jsonPayload.getString("selectedSensorInformationList").replace("\"",""));
                    }
                    else
                    {
                        showSensorAssociationDialog();
                    }
//                    if(jsonPayload.getInt(ASE_CODE) == 0){
//                        showSensorAssociationDialog();
//                    }
//                    else{
//                        listTupleParser(jsonPayload.getString(LIST_NAME).replace("\"",""));
//                    }
                    break;
                case ResultCode.RESCODE_SAP_SLV_OTHER:
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    break;
                case ResultCode.RESCODE_SAP_SLV_UNALLOCATED_USER_SEQUENCE_NUMBER:
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    Toast.makeText(context, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                    StateCheck("SLV_RSP");
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    break;
                case ResultCode.RESCODE_SAP_SLV_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SLV_RSP");
                    Toast.makeText(context, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                    loginIt = new Intent(context, SignInActivity.class);
                    startActivity(loginIt);
                    getActivity().finish();
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //SLV List Item Parser
    public void listTupleParser(String listData){
        String[] listTuple = listData.split("],\\[");

        listTuple[0] = listTuple[0].replace("[[","");
        listTuple[listTuple.length-1] = listTuple[listTuple.length-1].replace("]]","");

        for(int i=0; i < listTuple.length; i++){
            SensorItem sensorTuple = null;
            String[] tuple = listTuple[i].split(",",-1);
            if (tuple.length == TUPLE_LENGTH){
                String wifiMacAddress = tuple[0];
                String cellularMacAddress = tuple[1];
                String sensorRegistrationDate = tuple[2];
                String sensorActivationFlag = tuple[3];
                String sensorStatus = tuple[4];
                String sensorMobility = tuple[5];
                String nation = tuple[6];
                String state = tuple[7];
                String city = tuple[8];
                sensorTuple = new SensorItem(wifiMacAddress, cellularMacAddress, sensorRegistrationDate,
                        sensorActivationFlag, sensorStatus, sensorMobility, nation, state, city);
            }
            try {
                mSensorItems.add(sensorTuple);
            }catch (Exception e){
                e.printStackTrace();
            };
        }

    }

}
