package com.pce_mason.qi.airpollution.UserManagements;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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

import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R101;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T101;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class FragmentSguSecond extends Fragment implements SignUpActivity.onKeyBackPressedListener{

    private static String Email = null;
    private static String mPassword = null;
    private static String mConfirmPassword = null;

    //UI Connect
    private EditText EmailEdt,PasswordEdt,ConfirmPasswordEdt;
    private TextInputLayout EmailLayout,PasswordLayout,ConfromPasswordLayout;
    private Button ContinueBtn;
    private TextView UserNameTextView;

    private String FirstName;
    private String LastName;
    private String BirthDate;
    private String gender;

    private HttpConnectionThread mAuthTask = null;
    private int temporaryClientId;
    private String verificationCde = "vc";

    public FragmentSguSecond() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            FirstName = getArguments().getString("FirstName");
            LastName = getArguments().getString("LastName");
            BirthDate = getArguments().getString("BirthDate");
            gender = getArguments().getString("Gender");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sgu_second, container, false);

        EmailEdt = (EditText)view.findViewById(R.id.SignUpEmail);
        PasswordEdt = (EditText)view.findViewById(R.id.SignUpPassword);
        ConfirmPasswordEdt = (EditText)view.findViewById(R.id.SignUpConfirmPassword);
        EmailLayout = (TextInputLayout)view.findViewById(R.id.SignUpEmailLayout);
        PasswordLayout = (TextInputLayout)view.findViewById(R.id.SignUpPasswordLayout);
        ConfromPasswordLayout = (TextInputLayout)view.findViewById(R.id.SignUpConfirmPasswordLayout);
        UserNameTextView = (TextView)view.findViewById(R.id.sgu_secondpage_name);

        UserNameTextView.setText("Hi, " + FirstName + " " + LastName);

        ContinueBtn = (Button)view.findViewById(R.id.sgu_secondpage_continue_btn);
        ContinueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSignUp();
            }
        });

        if (Email != null) {
            EmailEdt.setText(Email);
            PasswordEdt.setText(mPassword);
            ConfirmPasswordEdt.setText(mConfirmPassword);
        }

        EmailEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(EmailEdt);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(EmailEdt);
                }
            }
        });

        PasswordEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(PasswordEdt);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(PasswordEdt);
                }
            }
        });

        ConfirmPasswordEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(ConfirmPasswordEdt);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(ConfirmPasswordEdt);
                }
            }
        });

        ((SignUpActivity)getActivity()).SGUStatusBarChange(2);
        return view;
    }

    public static FragmentSguSecond getInstance(String FirstName, String LastName, String Birthdate, String gender)
    {
        FragmentSguSecond mFragment = new FragmentSguSecond();
        Bundle args = new Bundle();
        args.putString("FirstName",FirstName);
        args.putString("LastName",LastName);
        args.putString("BirthDate",Birthdate);
        args.putString("Gender",gender);
        mFragment.setArguments(args);
        return mFragment;
    }

    public void attemptSignUp()
    {
        if(mAuthTask != null)
        {
            return;
        }

        // Reset erroes
        EmailLayout.setError(null);
        PasswordLayout.setError(null);
        ConfromPasswordLayout.setError(null);
        setEnableEdtBackground(EmailEdt);
        setEnableEdtBackground(PasswordEdt);
        setEnableEdtBackground(ConfirmPasswordEdt);

        String Password = PasswordEdt.getText().toString();
        String ConfirmPassword = ConfirmPasswordEdt.getText().toString();
        Email = EmailEdt.getText().toString();

        boolean cancel = false;
        View focusView = null;
        hideKeyboard(getActivity());
        // Check for a valid email address.
        if (TextUtils.isEmpty(Email)) {
            EmailLayout.setErrorTextAppearance(R.style.errorcolor);
            EmailLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(EmailEdt);
            focusView = EmailEdt;
            cancel = true;
        } else if (!isEmailValid(Email)) {
            EmailLayout.setErrorTextAppearance(R.style.errorcolor);
            EmailLayout.setError(getString(R.string.error_invalid_email));
            setErrorEdtBackground(EmailEdt);
            focusView = EmailEdt;
            cancel = true;
        }
        // Check for a valid password
        if (TextUtils.isEmpty(Password)) {
            PasswordLayout.setErrorTextAppearance(R.style.errorcolor);
            PasswordLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(PasswordEdt);
            focusView = PasswordEdt;
            cancel = true;
        } else if (!isPasswordValid(Password)) {
            PasswordLayout.setErrorTextAppearance(R.style.errorcolor);
            PasswordLayout.setError(getString(R.string.error_invalid_password));
            setErrorEdtBackground(PasswordEdt);
            focusView = PasswordEdt;
            cancel = true;
        }
        // Check for a valid repeat password
        if (TextUtils.isEmpty(ConfirmPassword)) {
            ConfromPasswordLayout.setErrorTextAppearance(R.style.errorcolor);
            ConfromPasswordLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(ConfirmPasswordEdt);
            focusView = ConfirmPasswordEdt;
            cancel = true;
        } else if (!isPasswordsEqual(Password, ConfirmPassword)) {
            ConfromPasswordLayout.setErrorTextAppearance(R.style.errorcolor);
            ConfromPasswordLayout.setError(getString(R.string.error_invalid_repeat_password));
            setErrorEdtBackground(ConfirmPasswordEdt);
            focusView = ConfirmPasswordEdt;
            cancel = true;
        }

        if(cancel)
        {
            focusView.requestFocus();
            setErrorEdtBackground((EditText)focusView);
        }
        else
        {
            Log.d("SGU_REQ_TEST","SGU Input Format Verified");
            requestMessageProcess();
        }
    }

    private void requestMessageProcess(){
        if(mAuthTask != null)
        {
            return;
        }
        if(APP_STATE == StateNumber.STATE_SAP.IDLE_STATE) {
            String email = EmailEdt.getText().toString();
            String password = PasswordEdt.getText().toString();
            String firstName = FirstName;
            String lastName = LastName;

            hideKeyboard(getActivity());
            temporaryClientId = temporaryClientIdGenerator();
            PostMessageMaker postMessageMaker = new PostMessageMaker(MessageType.SAP_SGUREQ_TYPE, 33, temporaryClientId);
            postMessageMaker.inputPayload(getTimestamp(), gender, email, password, firstName, lastName);
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("SGU_REQ_TEST","SGU Message Packing");
            Log.d("SGU_REQ_TEST",reqMsg);

            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0;i<R101;i++)
                {
                    APP_STATE = StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE;
                    StateCheck("SGU_REQ");
                    mAuthTask = new HttpConnectionThread(getContext().getApplicationContext(),T101);
                    Log.d("SGU_REQ_TEST","SGU Message Send");
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

    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalCheckFlag = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL)){
                APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                StateCheck("SGU_RSP");
                Toast.makeText(getContext(), getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            }
            else {
                Log.d("SGU_RSP_TEST","SGU_RSP Message Received");
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");

                if (msgType == MessageType.SAP_SGURSP_TYPE && endpointId == temporaryClientId) {
                    Log.d("SGU_RSP_TEST",responseMsg);
                    int resultCode = jsonPayload.getInt("resultCode");
                    RetrivalCheckFlag = true;
                    Log.d("SGU_RSP_TEST","SGU_RSP Message Unpacked");
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_SGU_OK:
                            String verificationCode = null;
                            verificationCode = jsonPayload.getString(verificationCde);
                            ((SignUpActivity)getActivity()).replaceFragment(3, FragmentSguThird_UVC.getInstance(Email,verificationCode,String.valueOf(temporaryClientId)));
                            break;
                        case ResultCode.RESCODE_SAP_SGU_OTHER:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGU_RSP");
                            Toast.makeText(getContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_SGU_CONFLICT_OF_TEMPORARY_CLIENT_ID:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGU_RSP");
                            mAuthTask = null;
                            requestMessageProcess();
                            break;
                        case ResultCode.RESCODE_SAP_SGU_DUPLICATE_OF_USER_ID:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGU_RSP");
                            EmailLayout.setErrorTextAppearance(R.style.errorcolor);
                            EmailLayout.setError(getString(R.string.error_invalid_email));
                            setErrorEdtBackground(EmailEdt);
                            EmailEdt.requestFocus();
                            break;
                    }
                } else {
                    Log.d("SGU_RSP_TEST",responseMsg);
                    Toast.makeText(getContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SGU_RSP");
                }
            }
            mAuthTask = null;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        mAuthTask = null;
        return RetrivalCheckFlag;
    }

    private void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private int temporaryClientIdGenerator(){
        Random random = new Random();
        return random.nextInt(DefaultValue.MAXIMUM_ENDPOINT_ID_SIZE);
    }

    // Email Validation Check
    private boolean isEmailValid(String email) {
        return Pattern.matches(DefaultValue.VALID_EMAIL_ADDRESS, email);
    }
    // Password Validation Check
    private boolean isPasswordValid(String password) {
        return Pattern.matches(DefaultValue.VALID_PASSWORD, password);
    }
    // Repeat Password Validation Check
    private boolean isPasswordsEqual(String password, String repeatPassword) {
        return password.equals(repeatPassword);
    }

    private String getTimestamp(){
        String birth = BirthDate;
        try{
            String[] pieces = birth.split("/");
            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.set(Integer.parseInt(pieces[2]),Integer.parseInt(pieces[0]),Integer.parseInt(pieces[1]),0,0,0);
            int birthTimestamp = (int)((birthCalendar.getTimeInMillis()-25200000)/1000);
            return String.valueOf(birthTimestamp);
        }catch (Exception e ){
            return DefaultValue.NULL_VALUE;
        }
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

    @Override
    public void onPause()
    {
        Email = EmailEdt.getText().toString();
        mPassword = PasswordEdt.getText().toString();
        mConfirmPassword = ConfirmPasswordEdt.getText().toString();
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        Email = null;
        mPassword = null;
        mConfirmPassword = null;
        super.onDestroy();
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
