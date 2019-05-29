package com.pce_mason.qi.airpollution.UserManagements;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.lang.annotation.Retention;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R101;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T101;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class SignUpActivity extends AppCompatActivity {

    // UI references.
    private EditText birthDateSignUp,lastNameSignUp, firstNameSignUp, passwordSighUp, passwordRepeatSignUp;
    private AutoCompleteTextView  emailSignUp;
    private Spinner genderSignUp;
    private LinearLayout signUpMainLayout;
    private Button completeSignUpButton;
    Toolbar toolbar;

    private String gender;
    private HttpConnectionThread mAuthTask = null;
    private int temporaryClientId;

//  Response Message String
    private String verificationCde = "vc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Hide Keyboard
//        signUpMainLayout = (LinearLayout) findViewById(R.id.signUpMainLayout);
//        signUpMainLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                hideKeyboard(SignUpActivity.this);
//                return true;
//            }
//        });

        //Input User Information
        emailSignUp = (AutoCompleteTextView) findViewById(R.id.signUpEmail);
        passwordSighUp = (EditText) findViewById(R.id.signUpPassword);
        passwordRepeatSignUp = (EditText) findViewById(R.id.signUpRepeatPassword);
        firstNameSignUp = (EditText) findViewById(R.id.signUpFirstName);
        lastNameSignUp = (EditText) findViewById(R.id.signUpLastName);
        birthDateSignUp = (EditText) findViewById(R.id.signUpBirth);
        birthDateSignUp.setText(getCurrentDate());
        birthDateSignUp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focusOn) {
                if (focusOn){
                    showDatePickerDialog(view);
                    hideKeyboard(SignUpActivity.this);
                }
            }
        });
        //Spinner for gender
        genderSignUp = (Spinner) findViewById(R.id.signUpGender);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        genderSignUp.setAdapter(adapter);
        genderSignUp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                gender = String.valueOf(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        completeSignUpButton = (Button) findViewById(R.id.signUpComplete);
        completeSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignUp();
            }
        });

    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = DatePickerFragment.newInstance(1);
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
    protected String getCurrentDate(){
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        return formatter.format(today);
    }
    private String getTimestamp(){
        String birth = String.valueOf(birthDateSignUp.getText());
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

    //E mail Validation Check
    private boolean isEmailValid(String email) {
        return Pattern.matches(DefaultValue.VALID_EMAIL_ADDRESS, email);
    }
    //Password Validation Check
    private boolean isPasswordValid(String password) {
        return Pattern.matches(DefaultValue.VALID_PASSWORD, password);
    }
    //Repeat Password Validation Check
    private boolean isPasswordsEqual(String password, String repeatPassword) {
        return password.equals(repeatPassword);
    }

    private void attemptSignUp() {
        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        emailSignUp.setError(null);
        passwordSighUp.setError(null);
        passwordRepeatSignUp.setError(null);
        firstNameSignUp.setError(null);
        lastNameSignUp.setError(null);
        firstNameSignUp.setError(null);
        birthDateSignUp.setError(null);

        // Store values at the time of the login attempt.
        String email = emailSignUp.getText().toString();
        String password = passwordSighUp.getText().toString();
        String repeatPassword = passwordRepeatSignUp.getText().toString();
        String firstName = firstNameSignUp.getText().toString();
        String lastName = lastNameSignUp.getText().toString();
        String birthDate = birthDateSignUp.getText().toString();

        boolean cancel = false;
        View focusView = null;
        hideKeyboard(SignUpActivity.this);
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailSignUp.setError(getString(R.string.error_field_required));
            focusView = emailSignUp;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailSignUp.setError(getString(R.string.error_invalid_email));
            focusView = emailSignUp;
            cancel = true;
        }
        // Check for a valid password
        if (TextUtils.isEmpty(password)) {
            passwordSighUp.setError(getString(R.string.error_field_required));
            focusView = passwordSighUp;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            passwordSighUp.setError(getString(R.string.error_invalid_password));
            focusView = passwordSighUp;
            cancel = true;
        }
        // Check for a valid repeat password
        if (TextUtils.isEmpty(repeatPassword)) {
            passwordRepeatSignUp.setError(getString(R.string.error_field_required));
            focusView = passwordRepeatSignUp;
            cancel = true;
        } else if (!isPasswordsEqual(password, repeatPassword)) {
            passwordRepeatSignUp.setError(getString(R.string.error_invalid_repeat_password));
            focusView = passwordRepeatSignUp;
            cancel = true;
        }
        // Check for a valid first name
        if (TextUtils.isEmpty(firstName)) {
            firstNameSignUp.setError(getString(R.string.error_field_required));
            focusView = firstNameSignUp;
            cancel = true;
        }
        // Check for a valid last name
        if (TextUtils.isEmpty(lastName)) {
            lastNameSignUp.setError(getString(R.string.error_field_required));
            focusView = lastNameSignUp;
            cancel = true;
        }
        // Check for a valid birthDate
        if (TextUtils.isEmpty(birthDate)) {
            birthDateSignUp.setError(getString(R.string.error_field_required));
            focusView = birthDateSignUp;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            Log.d("SGU_REQ_TEST","SGU Input Format Verified");
            requestMessageProcess();
        }
    }

    private void requestMessageProcess(){
        if(APP_STATE == StateNumber.STATE_SAP.IDLE_STATE) {
            String email = emailSignUp.getText().toString();
            String password = passwordSighUp.getText().toString();
            String firstName = firstNameSignUp.getText().toString();
            String lastName = lastNameSignUp.getText().toString();

            hideKeyboard(SignUpActivity.this);
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
                    mAuthTask = new HttpConnectionThread(SignUpActivity.this,T101);
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
            Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
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
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalCheckFlag = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL)){
                APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                StateCheck("SGU_RSP");
                Toast.makeText(this, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
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
                            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                            VerificationCodeConfirmFragment fragment =
                                    VerificationCodeConfirmFragment.newInstance(SignUpActivity.this, verificationCode, temporaryClientId);
                            fragmentTransaction.replace(R.id.signUpMainLayout1, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                            blockAllComponents();
                            break;
                        case ResultCode.RESCODE_SAP_SGU_OTHER:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGU_RSP");
                            Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_SGU_CONFLICT_OF_TEMPORARY_CLIENT_ID:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGU_RSP");
                            requestMessageProcess();
                            break;
                        case ResultCode.RESCODE_SAP_SGU_DUPLICATE_OF_USER_ID:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGU_RSP");
                            emailSignUp.setError(getString(R.string.error_invalid_email));
                            emailSignUp.requestFocus();
                            break;
                    }
                } else {
                    Log.d("SGU_RSP_TEST",responseMsg);
                    Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SGU_RSP");
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
        return RetrivalCheckFlag;
    }

    private void blockAllComponents(){
        completeSignUpButton.setVisibility(View.GONE);
        birthDateSignUp.setEnabled(false);
        lastNameSignUp.setEnabled(false);
        firstNameSignUp.setEnabled(false);
        passwordSighUp.setEnabled(false);
        passwordRepeatSignUp.setEnabled(false);
        emailSignUp.setEnabled(false);
        genderSignUp.setEnabled(false);
    }
    public void initAllComponents(){
        completeSignUpButton.setVisibility(View.VISIBLE);
        birthDateSignUp.setEnabled(true);
        lastNameSignUp.setEnabled(true);
        firstNameSignUp.setEnabled(true);
        passwordSighUp.setEnabled(true);
        passwordRepeatSignUp.setEnabled(true);
        emailSignUp.setEnabled(true);
        genderSignUp.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        initAllComponents();
    }
}

