package com.pce_mason.qi.airpollution.UserManagements;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R106;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T106;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout FirstNameLayout, LastNameLayout, BirthdateLayout, EmailLayout;
    private EditText firstNameForgot, lastNameForgot, birthDateForgot, emailForgot;
    private CoordinatorLayout forgotMainLayout;
    private int temporaryClientId;

    private HttpConnectionThread mAuthTask = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        forgotMainLayout = (CoordinatorLayout) findViewById(R.id.forgotMainLayout);
        forgotMainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard(ForgotPasswordActivity.this);
                return true;
            }
        });
        //Input User Information
        EmailLayout = (TextInputLayout)findViewById(R.id.ForgotPasswordEmailLayout);
        FirstNameLayout = (TextInputLayout)findViewById(R.id.ForgotPasswordFirstnameLayout);
        LastNameLayout = (TextInputLayout)findViewById(R.id.ForgotPasswordLastnameLayout);
        BirthdateLayout = (TextInputLayout)findViewById(R.id.ForgotPasswordBirthLayout);
        emailForgot = (EditText) findViewById(R.id.forgotEmail);
        firstNameForgot = (EditText) findViewById(R.id.forgotFirstName);
        lastNameForgot = (EditText) findViewById(R.id.forgotLastName);
        birthDateForgot = (EditText) findViewById(R.id.forgotBirth);
        birthDateForgot.setText(getCurrentDate());

        emailForgot.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(emailForgot);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(emailForgot);
                }
            }
        });

        firstNameForgot.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(firstNameForgot);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(firstNameForgot);
                }
            }
        });

        lastNameForgot.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(lastNameForgot);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(lastNameForgot);
                }
            }
        });

        birthDateForgot.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus){
                    setFocusedEdtBackground(birthDateForgot);
                    showDatePickerDialog(view);
                    hideKeyboard(ForgotPasswordActivity.this);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(birthDateForgot);
                }
            }
        });

        Button resetPassword = (Button) findViewById(R.id.forgotResetPassword);
        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptForgotPassword();
            }
        });
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
    protected String getCurrentDate(){
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        return formatter.format(today);
    }
    private String getTimestamp(){
        String birth = String.valueOf(birthDateForgot.getText());
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

    private int temporaryClientIdGenerator(){
        Random random = new Random();
        return random.nextInt(DefaultValue.MAXIMUM_ENDPOINT_ID_SIZE);
    }
    private boolean isEmailValid(String email) {
        return Pattern.matches(DefaultValue.VALID_EMAIL_ADDRESS, email);
    }

    private void attemptForgotPassword() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        EmailLayout.setError(null);
        FirstNameLayout.setError(null);
        LastNameLayout.setError(null);
        BirthdateLayout.setError(null);
        setEnableEdtBackground(emailForgot);
        setEnableEdtBackground(firstNameForgot);
        setEnableEdtBackground(lastNameForgot);
        setEnableEdtBackground(birthDateForgot);


        // Store values at the time of the login attempt.
        String email = emailForgot.getText().toString();
        String firstName = firstNameForgot.getText().toString();
        String lastName = lastNameForgot.getText().toString();
        String birthDate = birthDateForgot.getText().toString();

        boolean cancel = false;
        View focusView = null;
        hideKeyboard(ForgotPasswordActivity.this);
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            EmailLayout.setErrorTextAppearance(R.style.errorcolor);
            EmailLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(emailForgot);
            focusView = emailForgot;
            cancel = true;
        } else if (!isEmailValid(email)) {
            EmailLayout.setErrorTextAppearance(R.style.errorcolor);
            EmailLayout.setError(getString(R.string.error_invalid_email));
            setErrorEdtBackground(emailForgot);
            focusView = emailForgot;
            cancel = true;
        }
        // Check for a valid first name
        if (TextUtils.isEmpty(firstName)) {
            FirstNameLayout.setErrorTextAppearance(R.style.errorcolor);
            FirstNameLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(firstNameForgot);
            focusView = firstNameForgot;
            cancel = true;
        }
        // Check for a valid last name
        if (TextUtils.isEmpty(lastName)) {
            LastNameLayout.setErrorTextAppearance(R.style.errorcolor);
            LastNameLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(lastNameForgot);
            focusView = lastNameForgot;
            cancel = true;
        }
        // Check for a valid birthDate
        if (TextUtils.isEmpty(birthDate)) {
            BirthdateLayout.setErrorTextAppearance(R.style.errorcolor);
            BirthdateLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(birthDateForgot);
            focusView = birthDateForgot;
            cancel = true;
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            setErrorEdtBackground((EditText)focusView);
        } else {
            Log.d("FPU_REQ_TEST","Input Format Verified");
            requestMessageProcess();
        }

    }
    private void requestMessageProcess(){

        if(APP_STATE == StateNumber.STATE_SAP.IDLE_STATE || APP_STATE == StateNumber.STATE_SAP.USER_DUPLICATE_REQUESTED_STATE ||
                APP_STATE == StateNumber.STATE_SAP.HALF_USN_ALLOCATE_STATE || APP_STATE == StateNumber.STATE_SAP.HALF_USN_INFORMED_STATE) {
            String email = emailForgot.getText().toString();
            String firstName = firstNameForgot.getText().toString();
            String lastName = lastNameForgot.getText().toString();
            String birthDate = birthDateForgot.getText().toString();

            hideKeyboard(ForgotPasswordActivity.this);
            temporaryClientId = temporaryClientIdGenerator();
            PostMessageMaker postMessageMaker = new PostMessageMaker(MessageType.SAP_FPUREQ_TYPE, 33, temporaryClientId);
            postMessageMaker.inputPayload(getTimestamp(), email, firstName, lastName);
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("FPU_REQ_TEST","FPU REQ Message Packing");
            Log.d("FPU_REQ_TEST",reqMsg);
            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0;i<R106;i++)
                {
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("FPU_REQ");
                    mAuthTask = new HttpConnectionThread(ForgotPasswordActivity.this,T106);
                    Log.d("FPU_REQ_TEST","FPU REQ Message Send");
                    boolean RetryFlag = messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get());
                    if(RetryFlag) { break;}
                }

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
        }
    }
    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalCheck = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("") || responseMsg.equals("{}")){
                Log.d("FPU_RSP_TEST",responseMsg);
                APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                StateCheck("FPU_RSP");
                Toast.makeText(this, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            }else {
                Log.d("FPU_RSP_TEST","FPU RSP Message Received");
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");

                if (msgType == MessageType.SAP_FPURSP_TYPE && endpointId == temporaryClientId) {
                    Log.d("FPU_RSP_TEST",responseMsg);
                    Log.d("FPU_RSP_TEST","FPU RSP Message unpacking");
                    RetrivalCheck = true;
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    int resultCode = jsonPayload.getInt("resultCode");
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_FPU_OK:
                            showEmailSendDialog();
                            break;
                        case ResultCode.RESCODE_SAP_FPU_OTHER:
                            Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_FPU_CONFLICT_OF_TEMPORARY_CLIENT_ID:
                            requestMessageProcess();
                            break;
                        case ResultCode.RESCODE_SAP_FPU_INCORRECT_USER_INFORMATION:
                            FirstNameLayout.setErrorTextAppearance(R.style.errorcolor);
                            FirstNameLayout.setError(getString(R.string.error_incorrect_user_info));
                            firstNameForgot.requestFocus();
                            LastNameLayout.setErrorTextAppearance(R.style.errorcolor);
                            LastNameLayout.setError(getString(R.string.error_incorrect_user_info));
                            BirthdateLayout.setErrorTextAppearance(R.style.errorcolor);
                            BirthdateLayout.setError(getString(R.string.error_incorrect_user_info));
                            setErrorEdtBackground(firstNameForgot);
                            setErrorEdtBackground(lastNameForgot);
                            setErrorEdtBackground(birthDateForgot);
                            break;
                        case ResultCode.RESCODE_SAP_FPU_NOT_EXIST_USER_ID:
                            EmailLayout.setErrorTextAppearance(R.style.errorcolor);
                            EmailLayout.setError(getString(R.string.not_exist_email));
                            setErrorEdtBackground(emailForgot);
                            emailForgot.requestFocus();
                            break;
                    }
                } else {
                    Log.d("FPU_RSP_TEST",responseMsg);
                    Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("FPU_RSP");
                }
                mAuthTask = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        mAuthTask = null;
        return RetrivalCheck;
    }

    private void showEmailSendDialog(){
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_check_email)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        finish();
                    }
                });
        // Create the AlertDialog object and return it
        builder.show();
    }
    private void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
