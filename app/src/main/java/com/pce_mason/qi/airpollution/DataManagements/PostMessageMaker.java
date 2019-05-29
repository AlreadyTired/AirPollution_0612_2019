package com.pce_mason.qi.airpollution.DataManagements;

import android.content.Intent;
import android.util.Log;

import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.HeartDataManagements.HeartDataItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class PostMessageMaker {
    private int mMsgType;
    private int mMsgLength;
    private int mEndpointId;
    private JSONObject mHeader;
    private JSONObject mPayload;
    private JSONObject mReqMsg;

    //Message Payload Syntax
    final String message_header = "header";
    final String message_payload = "payload";
    final String message_type = "msgType";
    final String length = "msgLen";
    final String endpoint_id = "endpointId";
    final String user_id = "userId";
    final String user_password = "userPw";
    final String user_current_password = "curPw";
    final String user_new_password = "newPw";
    final String user_first_name = "userFn";
    final String user_last_name = "userLn";
    final String user_birthdate = "bdt";
    final String user_gender = "gender";
    final String verification_code = "vc";
    final String authentication_code = "ac";
    final String number_of_signed_in_completions = "nsc";
    final String wifi_mac_address = "wmac";
    final String cellular_mac_address = "cmac";
    final String sensor_mobility = "mobf";
    final String deregistration_reason_code = "drgcd";
    final String timestamp = "timestamp";
    final String heart_rate = "heartRate";
    final String rr_interval = "rrInterval";
    final String instantaneous_mobility = "instantaneousMobilty";
    final String latitude = "lat";
    final String longitude = "lon";
    final String nation_code = "nat";
    final String state = "state";
    final String city = "city";
    final String province_list = "provinceListEncodings";
    final String keyword_list = "keywordSearchListEncodings";
    final String nation_tier_tuple = "commonNatTierTuple";
    final String state_tier_tuple = "commonStateTierTuple";
    final String city_tier_tuple = "commonCityTierTuple";
    final String map_zoom_level = "mapZoomLevel";
    final String heart_data_list = "heartRelatedDataListEncodings";
    final String data_tuple_length = "dataTupleLen";
    final String heart_data_tupe = "heartRelatedDataTuples";

    final String start_timestamp = "sTs";
    final String end_timestamp = "eTs";



    public PostMessageMaker(int msgType, int msgLength, int endpointId){
        mMsgType = msgType;
        mMsgLength = msgLength;
        mEndpointId = endpointId;
        mHeader = new JSONObject();
        mPayload = new JSONObject();
        mReqMsg = new JSONObject();
        inputHeader();
    }
    private void inputHeader(){
        try {
            mHeader.put(message_type, mMsgType);
            mHeader.put(length, mMsgLength);
            mHeader.put(endpoint_id, mEndpointId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void inputPayload(List<HeartDataItem> mHeartDataList ){
        try{
            JSONArray heartDataList = new JSONArray();
            for(int i=0 ; i < mHeartDataList.size(); i++){
                JSONArray heartTuple = new JSONArray();
                int timeStamp = Integer.parseInt(mHeartDataList.get(i).timeStamp);
                String latlng = mHeartDataList.get(i).latitude +","+ mHeartDataList.get(i).longitude;
                String natCode = "Q30";
                String staCode = "Q99";
                String cityCode = "Q16552";
                int heartRate = Integer.parseInt(mHeartDataList.get(i).heartRate);
                double rrInterval = Double.valueOf(mHeartDataList.get(i).rrInterval);
                heartTuple.put(timeStamp);
                heartTuple.put(latlng);
                heartTuple.put(natCode);
                heartTuple.put(staCode);
                heartTuple.put(cityCode);
                heartTuple.put(heartRate);
                heartTuple.put(rrInterval);
                heartDataList.put(heartTuple);
            }

            JSONObject heartDataObject= new JSONObject();
            heartDataObject.put(data_tuple_length,mHeartDataList.size());
            heartDataObject.put(heart_data_tupe,heartDataList);

            mPayload.put(heart_data_list,heartDataObject);

        }catch (JSONException e) {
            e.printStackTrace();
        }


    }
    public void inputPayload(String... params){
        switch (mMsgType){
            case MessageType.SAP_SGUREQ_TYPE:
                try {
                    mPayload.put(user_birthdate, params[0]);
                    mPayload.put(user_gender, params[1]);
                    mPayload.put(user_id, params[2]);
                    mPayload.put(user_password, params[3]);
                    mPayload.put(user_first_name, params[4]);
                    mPayload.put(user_last_name, params[5]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_UVCREQ_TYPE:
                try {
                    mPayload.put(verification_code, params[0]);
                    mPayload.put(authentication_code, params[1]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_SGIREQ_TYPE:
                try {
                    mPayload.put(user_id, params[0]);
                    mPayload.put(user_password, params[1]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_SGONOT_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_UPCREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                    mPayload.put(user_current_password, params[1]);
                    mPayload.put(user_new_password, params[2]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_FPUREQ_TYPE:
                try {
                    mPayload.put(user_birthdate, params[0]);
                    mPayload.put(user_id, params[1]);
                    mPayload.put(user_first_name, params[2]);
                    mPayload.put(user_last_name, params[3]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_SRGREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                    mPayload.put(wifi_mac_address, params[1]);
                    mPayload.put(cellular_mac_address, params[2]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_SASREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                    mPayload.put(wifi_mac_address, params[1]);
                    mPayload.put(sensor_mobility, params[2]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_SDDREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                    mPayload.put(wifi_mac_address, params[1]);
                    mPayload.put(deregistration_reason_code, params[2]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_SLVREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_DCAREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_DCDNOT_TYPE:
                mPayload = null;
                break;
            case MessageType.SAP_RHDTRN_TYPE:
                try {
                    mPayload.put(timestamp, params[0]);
                    mPayload.put(heart_rate, params[1]);
                    mPayload.put(rr_interval, params[2]);
                    mPayload.put(instantaneous_mobility, params[2]);
                    if (instantaneous_mobility.contains("1")) {
                        mPayload.put(latitude, params[3]);
                        mPayload.put(longitude, params[4]);
                        mPayload.put(nation_code, params[5]);
                        mPayload.put(state, params[6]);
                        mPayload.put(city, params[7]);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_RAVREQ_TYPE:
                try {
                    JSONObject provinceListJsonObject = new JSONObject();
                    JSONObject keywordSearchListJsonObject = new JSONObject();

                    String city_1 = "Q16552";
                    String city_2 = "Q65";

                    JSONArray cityTierJsonArray = new JSONArray();
                    cityTierJsonArray.put(city_1);
                    cityTierJsonArray.put(city_2);

                    JSONObject stateTierJsonObject = new JSONObject();
                    stateTierJsonObject.put(state,"Q99");
                    stateTierJsonObject.put(city_tier_tuple, cityTierJsonArray);

                    JSONObject natTierJsonObject = new JSONObject();
                    natTierJsonObject.put(nation_code,"Q30");

                    JSONArray stateTierJsonArray = new JSONArray();
                    stateTierJsonArray.put(stateTierJsonObject);
                    natTierJsonObject.put(state_tier_tuple, stateTierJsonArray);

                    provinceListJsonObject.put(latitude, params[1]);
                    provinceListJsonObject.put(longitude, params[2]);
                    provinceListJsonObject.put(nation_tier_tuple, natTierJsonObject);

                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                    mPayload.put(province_list,provinceListJsonObject);
                    mPayload.put(keyword_list,keywordSearchListJsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case MessageType.SAP_HHVREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                    mPayload.put(start_timestamp, params[1]);
                    mPayload.put(end_timestamp, params[2]);
                    mPayload.put(nation_code, params[3]);
                    mPayload.put(state, params[4]);
                    mPayload.put(city, params[5]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case MessageType.SAP_KASREQ_TYPE:
                try {
                    mPayload.put(number_of_signed_in_completions, Integer.parseInt(params[0]));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    public String makeRequestMessage(){
        try {
            mReqMsg.put(message_header, mHeader);
            mReqMsg.put(message_payload, mPayload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mReqMsg.toString();
    }


}
