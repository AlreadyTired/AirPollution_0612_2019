package com.pce_mason.qi.airpollution.HeartDataManagements;

public class HeartDataItem {
    public String timeStamp;
    public String heartRate;
    public String rrInterval;
    public String latitude;
    public String longitude;

    public HeartDataItem(String timeStamp, String heartRate,
                         String rrInterval, String latitude, String longitude){
        this.timeStamp = timeStamp;
        this.heartRate = heartRate;
        this.rrInterval = rrInterval;
        this.latitude = latitude;
        this.longitude = longitude;

    }
}
