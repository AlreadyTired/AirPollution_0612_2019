package com.pce_mason.qi.airpollution.SensorManagements;

import android.util.Log;

public class SensorItem {
    public String wifiMacAddress;
    public String cellularMacAddress;
    public String sensorRegistrationDate;
    public String sensorActivationFlag;
    public String sensorStatus;
    public String sensorMobility;
    public String nation;
    public String state;
    public String city;

    public SensorItem(String wifiMacAddress, String cellularMacAddress, String sensorRegistrationDate,
                      String sensorActivationFlag, String sensorStatus, String sensorMobility,
                      String nation, String state, String city){
        this.wifiMacAddress = wifiMacAddress;
        this.cellularMacAddress = cellularMacAddress;
        this.sensorRegistrationDate = String.valueOf(Long.valueOf(sensorRegistrationDate)*(long)1000);
        Log.d("timestampTest",this.sensorRegistrationDate);
        this.sensorActivationFlag = sensorActivationFlag;
        this.sensorStatus = sensorStatus;
        this.sensorMobility = sensorMobility;
        this.nation = nation;
        this.state = state;
        this.city = city;
    }
}
