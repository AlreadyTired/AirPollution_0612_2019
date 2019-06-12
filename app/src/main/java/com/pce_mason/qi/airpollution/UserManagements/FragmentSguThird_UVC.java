package com.pce_mason.qi.airpollution.UserManagements;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
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

import com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer;
import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.R;
import com.pce_mason.qi.airpollution.SignInActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R102;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T102;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class FragmentSguThird_UVC extends Fragment implements SignUpActivity.onKeyBackPressedListener{

    private int AUTHENTICATION_CODE_LENGTH = 20;
    private View view;
    // UI Connect
    private CountDownTimer validationTimer;
    private TextView VerificationCodeView, EmailView, verificationTimer;
    private String Email, verificationCode;
    private EditText AuthenticationCodeEdt;
    private TextInputLayout AuthticationCodeLayout;
    private Button ContinueBtn;

    private HttpConnectionThread mAuthTask;
    private int temporaryClientId;
    private long remindTime;

    public FragmentSguThird_UVC() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Email = getArguments().getString("Email");
            verificationCode = getArguments().getString("VerificationCode");
            temporaryClientId = Integer.valueOf(getArguments().getString("TemporaryClientID"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sgu_third, container, false);

        AuthenticationCodeEdt = (EditText)view.findViewById(R.id.SignUpAuthenticationCode);
        AuthticationCodeLayout = (TextInputLayout)view.findViewById(R.id.SignUpAuthenticationCodeLayout);
        ContinueBtn = (Button)view.findViewById(R.id.sgu_Thirdpage_continue_btn);
        VerificationCodeView = (TextView)view.findViewById(R.id.verificationCodeView);
        EmailView = (TextView)view.findViewById(R.id.sgu_thirdpage_emailtext);
        VerificationCodeView.setText(getString(R.string.verificationi_number) + " " + verificationCode);
        EmailView.setText(getString(R.string.uvc_email_set) + " " + Email);
        verificationTimer = (TextView) view.findViewById(R.id.verificationTimer);

        AuthenticationCodeEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(AuthenticationCodeEdt);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(AuthenticationCodeEdt);
                }
            }
        });

        ContinueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // UVC Request
                attemptVerificationCode();
            }
        });

        // Timer
        validationTimer = new CountDownTimer(CustomTimer.T251, 1000) {
            @Override
            public void onTick(long l) {
                remindTime = l;
                updateTimer();
            }

            @Override
            public void onFinish() {

            }
        }.start();

        ((SignUpActivity)getActivity()).SGUStatusBarChange(3);
        return view;
    }

    public static FragmentSguThird_UVC getInstance(String Email, String VerificationCode, String TCI)
    {
        FragmentSguThird_UVC mFragment = new FragmentSguThird_UVC();
        Bundle args = new Bundle();
        args.putString("Email",Email);
        args.putString("VerificationCode",VerificationCode);
        args.putString("TemporaryClientID",TCI);
        mFragment.setArguments(args);
        return mFragment;
    }

    private boolean isAuthenticationCodeValid(String authenticationCode) {
        return authenticationCode.length() == AUTHENTICATION_CODE_LENGTH;
    }

    private void attemptVerificationCode() {
        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        AuthticationCodeLayout.setError(null);
        setEnableEdtBackground(AuthenticationCodeEdt);

        // Store values at the time of the login attempt.
        String authentication = AuthenticationCodeEdt.getText().toString();

        boolean cancel = false;
        View focusView = null;

        hideKeyboard();
        // Check for a valid email address.
        if (TextUtils.isEmpty(authentication)) {
            AuthticationCodeLayout.setErrorTextAppearance(R.style.errorcolor);
            AuthticationCodeLayout.setError(getString(R.string.Authentication_code_blank_error));
            setErrorEdtBackground(AuthenticationCodeEdt);
            focusView = AuthenticationCodeEdt;
            cancel = true;
        } else if (!isAuthenticationCodeValid(authentication)) {
            AuthticationCodeLayout.setError(getString(R.string.error_authentication_code_length));
            focusView = AuthenticationCodeEdt;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            setErrorEdtBackground((EditText)focusView);
        } else {
            Log.d("SGU_UVC_REQ_TEST","UVC Input Verified");
            requestMessageProcess();
        }
    }

    private void requestMessageProcess(){
        if(APP_STATE == StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE) {
            String authentication = AuthenticationCodeEdt.getText().toString();

            hideKeyboard();
            PostMessageMaker postMessageMaker = new PostMessageMaker(MessageType.SAP_UVCREQ_TYPE, 33, temporaryClientId);
            postMessageMaker.inputPayload(verificationCode, authentication);
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("SGU_UVC_REQ_TEST","UVC_REQ Message Packing");
            Log.d("SGU_UVC_REQ_TEST",reqMsg);
            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0;i<R102;i++)
                {
                    APP_STATE = StateNumber.STATE_SAP.HALF_USN_ALLOCATE_STATE;
                    Log.d("UVC_REQ_TEST","State : Haf USN allocated state");
                    mAuthTask = new HttpConnectionThread(getContext(),T102);
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
            Toast.makeText(getContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
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
                            Snackbar.make(SignInActivity.view,"You have successfully signed up",3000).show();
                            validationTimer.cancel();
                            getActivity().finish();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_OTHER:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            Toast.makeText(getContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_DUPLICATE_OF_USER_ID:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            Toast.makeText(getContext(), getString(R.string.email_duplicate), Toast.LENGTH_SHORT).show();
                            onDestroy();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_NOT_EXIT_A_TEMPORARY_CLIENT_IDDUPLICATE_OF_USER_ID:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            Toast.makeText(getContext(), getString(R.string.not_exist_TCI), Toast.LENGTH_SHORT).show();
                            onDestroy();
                            break;
                        case ResultCode.RESCODE_SAP_UVC_INCORRECT_AUTHENTICATION_CODE:
                            APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                            StateCheck("UVC_RSP");
                            AuthticationCodeLayout.setErrorTextAppearance(R.style.errorcolor);
                            AuthticationCodeLayout.setError(getString(R.string.error_incorrect_authentication));
                            setErrorEdtBackground(AuthenticationCodeEdt);
                            AuthenticationCodeEdt.requestFocus();
                            break;
                    }
                } else {
                    Log.d("UVC_RSP_TEST",responseMsg);
                    Toast.makeText(getContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
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

    @Override
    public void onBackKey()
    {
        SignUpActivity SguActivity = (SignUpActivity)getActivity();
        SguActivity.setOnKeyBackPressedListener(null);
        SguActivity.onBackPressed();
    }

    @Override
    public void onAttach(Context context)
    {
        Log.d(this.getClass().getSimpleName(), "onAttach()");
        super.onAttach(context);
        ((SignUpActivity)context).setOnKeyBackPressedListener(this);
    }

    public void updateTimer(){
        int minutes = (int) remindTime / 60000;
        int seconds = (int) (remindTime % 60000) / 1000;
        String timeLeftText;

        timeLeftText = "" +minutes;
        timeLeftText += ":";
        if (seconds < 10)
        {
            timeLeftText += "0";
            validationTimer.cancel();
            Toast.makeText(getContext(), getString(R.string.verification_code_timeout), Toast.LENGTH_SHORT).show();
            onBackKey();
            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
        }
        timeLeftText += seconds;

        verificationTimer.setText(timeLeftText);
    }

    @Override
    public void onDetach()
    {
        try
        {
            validationTimer.cancel();
        }catch (Exception e){
            validationTimer = null;
        }
        super.onDetach();
    }

    public void setEnableEdtBackground(EditText Edt)
    {
        Edt.setBackgroundResource(R.drawable.edittext_border_enable);
        Edt.setTextColor(Color.parseColor(getString(R.string.Normal_Color)));
    }

    public void setFocusedEdtBackground(EditText Edt)
    {
        Edt.setBackgroundResource(R.drawable.edittext_border_focused);
    }

    public void setErrorEdtBackground(EditText Edt)
    {
        Edt.setBackgroundResource(R.drawable.edittext_border_error);
        Edt.setTextColor(Color.parseColor(getString(R.string.Error_Color)));
    }
}
