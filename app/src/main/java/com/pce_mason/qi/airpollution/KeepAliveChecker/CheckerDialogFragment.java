package com.pce_mason.qi.airpollution.KeepAliveChecker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
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

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R116;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T116;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

public class CheckerDialogFragment extends DialogFragment {

    KeepAliveDialogListener mListener;
    private HttpConnectionThread mAuthTask = null;
    public CheckerDialogFragment(){}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_keep_alive_title)
                .setPositiveButton(R.string.dialog_keep_alive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestMessageProcess();
                    }
                })
                .setNegativeButton(R.string.dialog_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AlertDialog.Builder innerBuilder =  new AlertDialog.Builder(getActivity());
                        innerBuilder.setMessage(R.string.keep_alive_exit)
                                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        System.exit(1);
                                    }
                                })
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                });
                        innerBuilder.create().show();
                    }
                });
        builder.setCancelable(false);
        // Create the AlertDialog object and return it
        return builder.create();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        if (context instanceof KeepAliveDialogListener) {
            mListener = (KeepAliveDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    public interface KeepAliveDialogListener{
        void onDialogAliveClick(DialogFragment dialog);
    }

    private void requestMessageProcess() {
        if (mAuthTask != null) {
            return;
        }
        if (APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE || APP_STATE == StateNumber.STATE_SAP.USN_INFORMED_STATE) {
            int EID;
            if (MainActivity.CONNECTION_ID != null) {
                EID = MainActivity.CONNECTION_ID;
            } else {
                EID = MainActivity.USER_SEQUENCE_NUMBER;
            }
            int NSC = MainActivity.NUMBER_OF_SIGNED_IN_COMPLETIONS;
            PostMessageMaker postMessageMaker = new PostMessageMaker(MessageType.SAP_KASREQ_TYPE, 33, EID);
            postMessageMaker.inputPayload(String.valueOf(NSC));
            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("KAS_REQ_TEST", "KAS REQ Packing");
            Log.d("KAS_REQ_TEST", reqMsg);
            try {

                String airUrl = getString(R.string.air_url);
                for (int i = 0; i < R116; i++) {

                    StateCheck("KAS_REQ");
                    mAuthTask = new HttpConnectionThread(getContext().getApplicationContext(), T116);
                    Log.d("KAS_REQ_TEST", "KAS REQ Send");
                    boolean RetryFlag = messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get(),EID);
                    if (RetryFlag) {
                        break;
                    }
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getContext().getApplicationContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean messageResultProcess(String responseMsg, int EID) {
        boolean RetrivalFlag = false;
        try {
            if (responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("") || responseMsg.equals("{}")) {
                Log.d("KAS_RSP_TEST",responseMsg);
                Toast.makeText(getContext().getApplicationContext(), getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("KAS_RSP_TEST","KAS RSP Received");
                Log.d("KAS_RSP_TEST",responseMsg);
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");

                if (msgType == MessageType.SAP_KASRSP_TYPE && endpointId == EID) {
                    Log.d("KAS_RSP_TEST","KAS RSP unpacking");
                    RetrivalFlag = true;
                    Intent loginIt;
                    int resultCode = jsonPayload.getInt("resultCode");
                    switch (resultCode) {
                        case ResultCode.RESCODE_SAP_KAS_OK:
                            mListener.onDialogAliveClick(CheckerDialogFragment.this);
                            break;
                        case ResultCode.RESCODE_SAP_KAS_OTHER:
                            Toast.makeText(getContext().getApplicationContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_KAS_UNALLOCATED_USER_SEQUENCE_NUMBER:
                            Toast.makeText(getContext().getApplicationContext(), getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(getContext().getApplicationContext(), SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        case ResultCode.RESCODE_SAP_KAS_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                            Toast.makeText(getContext().getApplicationContext(), getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(getContext().getApplicationContext(), SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        default:
                            Toast.makeText(getContext().getApplicationContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
                    Toast.makeText(getContext().getApplicationContext(), getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                }

                mAuthTask = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        mAuthTask = null;
        return RetrivalFlag;
    }
}