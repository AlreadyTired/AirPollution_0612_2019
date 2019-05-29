package com.pce_mason.qi.airpollution.UserManagements;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R102;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T102;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VerificationCodeConfirmFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VerificationCodeConfirmFragment extends Fragment {
    private CountDownTimer validationTimer;
    private TextView verificationCode, verificationTimer;
    private EditText authenticationCode;
    private Button verifyButton;

    private long remindTime;
    private String verifyCode;
    private Context context;
    private HttpConnectionThread mAuthTask;
    private int temporaryClientId;

    private int AUTHENTICATION_CODE_LENGTH = 20;

    public VerificationCodeConfirmFragment() {
        // Required empty public constructor
    }

    public static VerificationCodeConfirmFragment newInstance(Context context, String verifyCode, int tci) {
        VerificationCodeConfirmFragment fragment = new VerificationCodeConfirmFragment();
        fragment.context = context;
        fragment.verifyCode = verifyCode;
        fragment.temporaryClientId = tci;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_verification_code_confirm, container, false);

        verificationCode = (TextView) view.findViewById(R.id.verificationCode);
        verificationCode.setText(verifyCode);
        verificationTimer = (TextView) view.findViewById(R.id.verificationTimer);
        authenticationCode = (EditText) view.findViewById(R.id.authenticationCode);
        verifyButton = (Button) view.findViewById(R.id.verificationButton);
        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptVerificationCode();
            }
        });

        validationTimer = new CountDownTimer(CustomTimer.T251, 1000) {
            @Override
            public void onTick(long l) {
                remindTime = l;
                updateTimer();
            }
            @Override
            public void onFinish() {
                Toast.makeText(context, getString(R.string.verification_code_timeout), Toast.LENGTH_SHORT).show();
                onDestroy();
            }
        }.start();

        return view;
    }

    public void updateTimer(){
        int minutes = (int) remindTime / 60000;
        int seconds = (int) remindTime % 60000 / 1000;
        String timeLeftText;

        timeLeftText = "" +minutes;
        timeLeftText += ":";
        if (seconds < 10)
        {
            timeLeftText += "0";
            validationTimer.onFinish();
        }
        timeLeftText += seconds;

        verificationTimer.setText(timeLeftText);
    }

    //Repeat Password Validation Check
    private boolean isAuthenticationCodeValid(String authenticationCode) {
        return authenticationCode.length() == AUTHENTICATION_CODE_LENGTH;
    }

    private void attemptVerificationCode() {
        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        authenticationCode.setError(null);

        // Store values at the time of the login attempt.
        String authentication = authenticationCode.getText().toString();

        boolean cancel = false;
        View focusView = null;

        hideKeyboard();
        // Check for a valid email address.
        if (TextUtils.isEmpty(authentication)) {
            authenticationCode.setError(getString(R.string.error_field_required));
            focusView = authenticationCode;
            cancel = true;
        } else if (!isAuthenticationCodeValid(authentication)) {
            authenticationCode.setError(getString(R.string.error_authentication_code_length));
            focusView = authenticationCode;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            Log.d("SGU_UVC_REQ_TEST","UVC Input Verified");
            requestMessageProcess();
        }
    }

    private void requestMessageProcess(){
        if(APP_STATE == StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE) {
            String authentication = authenticationCode.getText().toString();

            hideKeyboard();
            PostMessageMaker postMessageMaker = new PostMessageMaker(MessageType.SAP_UVCREQ_TYPE, 33, temporaryClientId);
            postMessageMaker.inputPayload(verifyCode, authentication);
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("SGU_UVC_REQ_TEST","UVC_REQ Message Packing");
            Log.d("SGU_UVC_REQ_TEST",reqMsg);
            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0;i<R102;i++)
                {
                    APP_STATE = StateNumber.STATE_SAP.HALF_USN_ALLOCATE_STATE;
                    Log.d("UVC_REQ_TEST","State : Haf USN allocated state");
                    mAuthTask = new HttpConnectionThread(context,T102);
                    Log.d("SGU_UVC_REQ_TEST","UVC Message Send");
                    boolean RetryFlag = messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get());
                    if(RetryFlag) { break; }
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        View view = getActivity().findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalChecking = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("{}")){
                APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                StateCheck("UVC_RSP");
                Toast.makeText(context, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            }
            else {
                Log.d("SGU_UVC_RSP_TEST","UVC Rsp Received");
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");

                if (msgType == MessageType.SAP_UVCRSP_TYPE && endpointId == temporaryClientId) {
                    Log.d("UVC_RSP_TEST",responseMsg);
                    Log.d("SGU_UVC_RSP_TEST","UVC Rsp Unpacking");
                    int resultCode = jsonPayload.getInt("resultCode");
                    RetrivalChecking = true;
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_UVC_OK:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("UVC_RSP");
                            showCompletionDialog();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_OTHER:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_DUPLICATE_OF_USER_ID:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            Toast.makeText(context, getString(R.string.email_duplicate), Toast.LENGTH_SHORT).show();
                            onDestroy();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_NOT_EXIT_A_TEMPORARY_CLIENT_IDDUPLICATE_OF_USER_ID:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            Toast.makeText(context, getString(R.string.not_exist_TCI), Toast.LENGTH_SHORT).show();
                            onDestroy();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_INCORRECT_AUTHENTICATION_CODE:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            authenticationCode.setError(getString(R.string.error_incorrect_authentication));
                            authenticationCode.requestFocus();
                            break;
                    }
                } else {
                    Log.d("UVC_RSP_TEST",responseMsg);
                    Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("UVC_RSP");
//                mPasswordView.setError(getString(R.string.error_incorrect_password));
//                mPasswordView.requestFocus();
                }
            }
            mAuthTask = null;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        mAuthTask = null;
        return RetrivalChecking;
    }
    private void showCompletionDialog(){
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.dialog_congratulation)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        getActivity().finish();
                    }
                });
        // Create the AlertDialog object and return it
        builder.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(validationTimer != null){
            validationTimer.cancel();
            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
    }
}
