package com.pce_mason.qi.airpollution;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.snackbar.ContentViewCallback;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.UserManagements.ForgotPasswordActivity;
import com.pce_mason.qi.airpollution.UserManagements.SignUpActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R103;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T103;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

/**
 * A signIn screen that offers signIn via email/password.
 */
public class SignInActivity extends AppCompatActivity {
    public static LinearLayout view;
    private HttpConnectionThread mAuthTask = null;

    // BackPress
    private BackPressCloseHandler backPressCloseHandler;

    //Usr ID Saving tool
    private SharedPreferences idSharedPreferences;
    private String preferencesName = "userEmailAddress";
    private String userId = "userEmail";

    // UI references.
    private TextInputEditText mEmailView;
    private TextInputLayout mEmailLayout, mPasswordLayout;
    private EditText mPasswordView;
    private View mProgressView;
    private LinearLayout signInMainLayout, signInLayout;
    private int temporaryClientId;

    //Response Message String
    private String userSequenceNumber = "usn";
    private String numberOfSignedInComp = "nsc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        backPressCloseHandler = new BackPressCloseHandler(this);

        view = (LinearLayout)findViewById(R.id.signInMainLayout);

        signInMainLayout = (LinearLayout) findViewById(R.id.signInMainLayout);
        signInMainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard(SignInActivity.this);
                return true;
            }
        });
        signInLayout = (LinearLayout) findViewById(R.id.email_signIn_form);
        signInLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard(SignInActivity.this);
                return true;
            }
        });
        // Set up the signIn form.
        mEmailView = (TextInputEditText) findViewById(R.id.SignInEmail);
        mEmailLayout = (TextInputLayout)findViewById(R.id.SignInEmailLayout);
        mPasswordLayout = (TextInputLayout)findViewById(R.id.SignInPasswordLayout);

        mPasswordView = (EditText) findViewById(R.id.SignInPassword);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptSignIn();
                    return true;
                }
                return false;
            }
        });


        mEmailView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(mEmailView);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(mEmailView);
                }
            }
        });
        mPasswordView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    setFocusedEdtBackground(mPasswordView);
                }
                if(!hasFocus)
                {
                    setEnableEdtBackground(mPasswordView);
                }
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.SignInAttemptBtn);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignIn();
            }
        });

        TextView RealtimeDataView = (TextView) findViewById(R.id.real_time_data_view);
        RealtimeDataView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.USER_SEQUENCE_NUMBER = 0;
                 Intent mainIt = new Intent(SignInActivity.this, MainActivity.class);
                 startActivity(mainIt);
            }
        });

        final TextView mSignUpButton = (TextView) findViewById(R.id.sign_up);
        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signUpIt = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(signUpIt);
            }
        });

        final TextView mForgetPasswordButton = (TextView) findViewById(R.id.forgot_password);
        mForgetPasswordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent forgotIt = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
                startActivity(forgotIt);
            }
        });

        //sharedPreferences data get and set
        setUserId();

        mProgressView = findViewById(R.id.signIn_progress);

    }

    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    private int temporaryClientIdGenerator(){
        Random random = new Random();
        return random.nextInt(DefaultValue.MAXIMUM_ENDPOINT_ID_SIZE);
    }

    private void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //E mail Validation Check
    private boolean isEmailValid(String email) {
        return Pattern.matches(DefaultValue.VALID_EMAIL_ADDRESS, email);
    }
    //Password Validation Check
    private boolean isPasswordValid(String password) {
        return Pattern.matches(DefaultValue.VALID_PASSWORD, password);
    }

    /**
     * Attempts to sign in or register the account specified by the signIn form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual signIn attempt is made.
     */
    private void attemptSignIn() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailLayout.setErrorEnabled(false);
        mPasswordLayout.setErrorEnabled(false);
        setEnableEdtBackground(mEmailView);
        setEnableEdtBackground(mPasswordView);

        // Store values at the time of the signIn attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;
        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordLayout.setErrorTextAppearance(R.style.errorcolor);
            mPasswordLayout.setError(getString(R.string.error_field_required));
            setErrorEdtBackground(mPasswordView);
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordLayout.setErrorTextAppearance(R.style.errorcolor);
            mPasswordLayout.setError(getString(R.string.error_invalid_password));
            setErrorEdtBackground(mPasswordView);
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailLayout.setErrorTextAppearance(R.style.errorcolor);
            mEmailLayout.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailLayout.setErrorTextAppearance(R.style.errorcolor);
            mEmailLayout.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt signIn and focus the first
            // form field with an error.
            focusView.requestFocus();
            setErrorEdtBackground((EditText)focusView);
        } else {
            showProgress(true);
            Log.d("SGI_REQ_TEST","Input Format Verified");
            requestMessageProcess();
        }
    }
    private void requestMessageProcess(){
        if(APP_STATE == StateNumber.STATE_SAP.IDLE_STATE){

            String email = mEmailView.getText().toString();
            String password = mPasswordView.getText().toString();

            hideKeyboard(SignInActivity.this);
            temporaryClientId = temporaryClientIdGenerator();
            PostMessageMaker postMessageMaker = new PostMessageMaker(MessageType.SAP_SGIREQ_TYPE,33,temporaryClientId);
            postMessageMaker.inputPayload(email,password);
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("SGI_REQ_TEST","SGI REQ Packing");
            Log.d("SGI_REQ_TEST",reqMsg);
            try {

                String airUrl = getString(R.string.air_url);
                for(int i=0;i<R103;i++)
                {
                    APP_STATE = StateNumber.STATE_SAP.HALF_USN_INFORMED_STATE;
                    StateCheck("SGI_REQ");
                    mAuthTask = new HttpConnectionThread(SignInActivity.this,T103);
                    Log.d("SGI_REQ_TEST","SGI REQ Send");
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

    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalFlag = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("{}") || responseMsg.equals("")){
                Log.d("SGI_RSP_TEST",responseMsg);
                APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                StateCheck("SGI_REQ");
                Toast.makeText(this, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            }else {
                Log.d("SGI_RSP_TEST","SGI RSP Received");
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");

                if (msgType == MessageType.SAP_SGIRSP_TYPE && endpointId == temporaryClientId) {
                    Log.d("SGI_RSP_TEST",responseMsg);
                    Log.d("SGI_RSP_TEST","SGI RSP unpacking");
                    RetrivalFlag = true;
                    int resultCode = jsonPayload.getInt("resultCode");
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_SGI_OK:
                            APP_STATE = StateNumber.STATE_SAP.USN_INFORMED_STATE;
                            StateCheck("SGI_RSP");
                            Intent mainIt = new Intent(SignInActivity.this, MainActivity.class);
                            mainIt.putExtra(getString(R.string.email_intent_string),mEmailView.getText().toString());
                            mainIt.putExtra(getString(R.string.USN_Intent_string),jsonPayload.getInt(userSequenceNumber));
                            mainIt.putExtra(getString(R.string.NSC_Intent_string),jsonPayload.getInt(numberOfSignedInComp));
                            saveUserId();
                            startActivity(mainIt);
                            finish();
                            break;
                        case ResultCode.RESCODE_SAP_SGI_OTHER:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGI_RSP");
                            Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_SGI_CONFLICT_OF_TEMPORARY_CLIENT_ID:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGI_RSP");
                            requestMessageProcess();
                            break;
                        case ResultCode.RESCODE_SAP_SGI_NOT_EXIST_USER_ID:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGI_RSP");
                            mEmailLayout.setError(getString(R.string.error_invalid_email));
                            mEmailView.requestFocus();
                            break;
                        case ResultCode.RESCODE_SAP_SGI_INCORRECT_CURRENT_USER_PASSWORD:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("SGI_RSP");
                            mPasswordLayout.setError(getString(R.string.error_incorrect_password));
                            mPasswordView.requestFocus();
                            break;
                    }
                } else {
                    Log.d("SGI_RSP_TEST",responseMsg);
                    Toast.makeText(this, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                    APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                    StateCheck("SGI_RSP");
                }
                mAuthTask = null;
                showProgress(false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        mAuthTask = null;
        showProgress(false);
        return RetrivalFlag;
    }
    private void saveUserId(){
        String usrEmail = mEmailView.getText().toString();
        SharedPreferences.Editor editor = idSharedPreferences.edit();
        editor.putString(userId,usrEmail);
        editor.commit();
    }
    private void setUserId(){
        idSharedPreferences = getSharedPreferences(preferencesName,MODE_PRIVATE);
        String previousId = idSharedPreferences.getString(userId,"");
        mEmailView.setText(previousId);
    }

    /**
     * Shows the progress UI and hides the signIn form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            signInMainLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            signInMainLayout.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    signInMainLayout.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show`
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            signInMainLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;

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