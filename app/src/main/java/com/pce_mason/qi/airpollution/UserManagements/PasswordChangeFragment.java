package com.pce_mason.qi.airpollution.UserManagements;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import com.pce_mason.qi.airpollution.MainActivity;
import com.pce_mason.qi.airpollution.R;
import com.pce_mason.qi.airpollution.SignInActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R105;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T105;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;


public class PasswordChangeFragment extends Fragment {
     // UI references.
    private Context context;
    private EditText currentPasswordChange, newPasswordChange, repeatNewPasswordChange;
    private LinearLayout mainLayoutPasswordChange;

    private HttpConnectionThread mAuthTask = null;

    //  Response Message String
    private String numberOfSignedInComp = "nsc";

    public PasswordChangeFragment() {
        // Required empty public constructor
    }

    public static PasswordChangeFragment newInstance(Context context) {
        PasswordChangeFragment fragment = new PasswordChangeFragment();
        fragment.context =context;
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
        //Resource match
        View layout = inflater.inflate(R.layout.fragment_password_change, container, false);
        currentPasswordChange = (EditText) layout.findViewById(R.id.passwordCurreuntPassword);
        newPasswordChange = (EditText) layout.findViewById(R.id.passwordNewPassword);
        repeatNewPasswordChange = (EditText) layout.findViewById(R.id.passwordRepeatNewPassword);
        mainLayoutPasswordChange = (LinearLayout) layout.findViewById(R.id.passwordChangeMainLayout);
        mainLayoutPasswordChange.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard();
                return true;
            }
        });

        Button passwordChangeButton= (Button) layout.findViewById(R.id.passwordButton);
        passwordChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptPasswordChange();
            }
        });

        return layout;
    }


    //Password Validation Check
    private boolean isPasswordValid(String password) {
        return Pattern.matches(DefaultValue.VALID_PASSWORD, password);
    }
    //Repeat Password Validation Check
    private boolean isPasswordsEqual(String password, String repeatPassword) {
        return password.equals(repeatPassword);
    }

    private void attemptPasswordChange() {
//        if (mAuthTask != null) {
//            return;
//        }
        // Reset errors.
        currentPasswordChange.setError(null);
        newPasswordChange.setError(null);
        repeatNewPasswordChange.setError(null);

        // Store values at the time of the login attempt.
        String currentPassword = currentPasswordChange.getText().toString();
        String newPassword = newPasswordChange.getText().toString();
        String repeatNewPassword = repeatNewPasswordChange.getText().toString();

        boolean cancel = false;
        View focusView = null;
        hideKeyboard();

        // Check for a valid current password
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordChange.setError(getString(R.string.error_field_required));
            focusView = currentPasswordChange;
            cancel = true;
        } else if (!isPasswordValid(currentPassword)) {
            currentPasswordChange.setError(getString(R.string.error_invalid_password));
            focusView = currentPasswordChange;
            cancel = true;
        }
        // Check for a valid new password
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordChange.setError(getString(R.string.error_field_required));
            focusView = newPasswordChange;
            cancel = true;
        } else if (!isPasswordsEqual(newPassword, repeatNewPassword)) {
            newPasswordChange.setError(getString(R.string.error_invalid_new_password));
            focusView = newPasswordChange;
            cancel = true;
        }
        // Check for a valid repeat new password
        if (TextUtils.isEmpty(repeatNewPassword)) {
            repeatNewPasswordChange.setError(getString(R.string.error_field_required));
            focusView = repeatNewPasswordChange;
            cancel = true;
        } else if (!isPasswordsEqual(newPassword, repeatNewPassword)) {
            repeatNewPasswordChange.setError(getString(R.string.error_invalid_repeat_password));
            focusView = repeatNewPasswordChange;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            Log.d("UPC_REQ_TEST","Input Format Verified");
            requestMessageProcess();
        }
    }
    private void requestMessageProcess(){
        if(APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE || APP_STATE == StateNumber.STATE_SAP.USN_INFORMED_STATE)
        {
            StateCheck("UPC_REQ");
            String currentPassword = currentPasswordChange.getText().toString();
            String newPassword = newPasswordChange.getText().toString();

            hideKeyboard();
            int USN = MainActivity.USER_SEQUENCE_NUMBER;
            int NSC = MainActivity.NUMBER_OF_SIGNED_IN_COMPLETIONS;
            PostMessageMaker postMessageMaker = new PostMessageMaker(MessageType.SAP_UPCREQ_TYPE,33,USN);
            postMessageMaker.inputPayload(String.valueOf(NSC), currentPassword, newPassword);
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("UPC_REQ_TEST","UPC REQ Message Packing");
            Log.d("UPC_REQ_TEST",reqMsg);
            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0; i<R105;i++)
                {
                    StateCheck("UPC_REQ");
                    mAuthTask = new HttpConnectionThread(context,T105);
                    Log.d("UPC_REQ_TEST","UPC REQ Message Send");
                    boolean RetryFlag = messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get());
                    if(RetryFlag) { break; }
                }

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else
        {
            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
        }
    }
    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalCheck = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL)){
                Toast.makeText(context, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            }else {
                Log.d("UPC_RSP_TEST","UPC RSP Message Received");
                Log.d("UPC_RSP_TEST",responseMsg);
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");
                int USN = MainActivity.USER_SEQUENCE_NUMBER;
                if (msgType == MessageType.SAP_UPCRSP_TYPE && endpointId == USN) {
                    RetrivalCheck = true;
                    Log.d("UPC_RSP_TEST","UPC RSP Message unpacking");
                    int resultCode = jsonPayload.getInt("resultCode");
                    Intent loginIt;
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_UPC_OK:
                            MainActivity.NUMBER_OF_SIGNED_IN_COMPLETIONS = jsonPayload.getInt(numberOfSignedInComp);
                            Toast.makeText(context, getString(R.string.password_change_success), Toast.LENGTH_SHORT).show();
                            currentPasswordChange.setText("");
                            repeatNewPasswordChange.setText("");
                            newPasswordChange.setText("");
                            break;
                        case ResultCode.RESCODE_SAP_UPC_OTHER:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_UPC_UNALLOCATED_USER_SEQUENCE_NUMBER:
                            Toast.makeText(context, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("UPC_RSP");
                            loginIt = new Intent(context, SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        case ResultCode.RESCODE_SAP_UPC_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                            APP_STATE = StateNumber.STATE_SAP.IDLE_STATE;
                            StateCheck("UPC_RSP");
                            Toast.makeText(context, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(context, SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        case ResultCode.RESCODE_SAP_UPC_INCORRECT_CURRENT_USER_PASSWORD:
                            currentPasswordChange.setError(getString(R.string.error_incorrect_password));
                            currentPasswordChange.requestFocus();
                            break;
                    }
                } else {
                    Log.d("UPC_RSP_TEST",responseMsg);
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
        return RetrivalCheck;
    }
    private void hideKeyboard() {
        View view = getActivity().findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
