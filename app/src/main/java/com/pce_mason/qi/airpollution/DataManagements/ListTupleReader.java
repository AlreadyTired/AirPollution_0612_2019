package com.pce_mason.qi.airpollution.DataManagements;


import org.json.JSONException;
import org.json.JSONObject;

public class ListTupleReader {


    //이전 버전의 서버에 맞춰서 만들어진 클래스 현재 사용 하지 않음


    public void listTupleParser(JSONObject listObject){

        JSONObject aa = new JSONObject();
        String data = "[512314,14123,56235]";
        String data1 = "[512314,14123,56235],[22,33,44],[555,666,777]";
        try {
            String listData = listObject.getString("");
            String[] listTuple = listData.split("],\\[");

            listTuple[0] = listTuple[0].startsWith("[") ? listTuple[0].substring(1) : listTuple[0];
            listTuple[listTuple.length-1] = listTuple[listTuple.length-1].replace("]","");

            for(int i=0; i < listTuple.length; i++){
                String[] tuple = listTuple[i].split(",");

                String wifiMac = tuple[0];
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
//    public void listTupleParser(JSONObject listObject){
//
//        try {
//            String listData = listObject.getString(TUPLE_NAME);
//            String[] listTuple = listData.split("],\\[");
//
//            listTuple[0] = listTuple[0].startsWith("[[") ? listTuple[0].substring(1) : listTuple[0];
//            listTuple[listTuple.length-1] = listTuple[listTuple.length-1].replace("]]","");
//
//            for(int i=0; i < listTuple.length; i++){
//                SensorItem sensorTuple = null;
//                String[] tuple = listTuple[i].split(",");
//                if (tuple.length == TUPLE_LENGTH){
//                    String wifiMacAddress = tuple[0];
//                    String cellularMacAddress = tuple[1];
//                    String sensorRegistrationDate = tuple[2];
//                    String sensorActivationFlag = tuple[3];
//                    String sensorStatus = tuple[4];
//                    String sensorMobility = tuple[5];
//                    String nation = tuple[6];
//                    String state = tuple[7];
//                    String city = tuple[8];
//                    String userID = tuple[9];
//                    sensorTuple = new SensorItem(wifiMacAddress, cellularMacAddress, sensorRegistrationDate,
//                            sensorActivationFlag, sensorStatus, sensorMobility, nation, state, city, userID);
//                }
//                try {
//                    mSensorItems.add(sensorTuple);
//                }catch (NullPointerException e){};
//            }
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//    }


}
