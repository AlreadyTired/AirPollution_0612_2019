package com.pce_mason.qi.airpollution.RealTimeAirDataMonitoring;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RealTimeDataItem implements ClusterItem {

    public String wifiMacAddress;
    public String timestamp;
    public String temperature;
    public String coAqi;
    public String o3Aqi;
    public String no2Aqi;
    public String so2Aqi;
    public String pm25;
    public String pm10;
    public double latitude;
    public double longitude;

    protected final int mMarkerColor;
    protected final int mHighestValue;

    private LatLng mPosition;
    private String mTitle;
    private String mSnippet;

    public RealTimeDataItem(String wifiMacAddress, String timestamp, String temperature,
                            String coAqi, String o3Aqi, String no2Aqi, String so2Aqi,
                            String pm25, String pm10, double latitude, double longitude) {
        this.wifiMacAddress = wifiMacAddress;
        this.timestamp = String.valueOf(Long.valueOf(timestamp)*(long)1000);
        this.temperature = temperature;
        this.coAqi = coAqi;
        this.o3Aqi = o3Aqi;
        this.no2Aqi = no2Aqi;
        this.so2Aqi = so2Aqi;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.latitude = latitude;
        this.longitude = longitude;

        mPosition = new LatLng(latitude,longitude);
        mHighestValue = getHighestValue();
        mMarkerColor = getMarkerImageColor(mHighestValue);
    }
    private int getHighestValue(){
        List<Integer> values = Arrays.asList(Integer.parseInt(coAqi),Integer.parseInt(o3Aqi),
                Integer.parseInt(no2Aqi), Integer.parseInt(so2Aqi), Integer.parseInt(pm25),
                Integer.parseInt(pm10));
        return Collections.max(values);
    }
    private int getMarkerImageColor(int aqiValue){
        if (aqiValue > 300)         return DefaultValue.COLOR_HAZARDOUS;
        else if (aqiValue > 200)    return DefaultValue.COLOR_VERY_UNHEALTHY;
        else if (aqiValue > 150)    return DefaultValue.COLOR_UNHEALTHY;
        else if (aqiValue > 100)    return DefaultValue.COLOR_SENS_UNHEALTHY;
        else if (aqiValue > 50)     return DefaultValue.COLOR_MODERATE;
        else                        return DefaultValue.COLOR_GOOD;

    }

    public int getMarkerColor(){return mMarkerColor;}

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() { return mTitle; }

    @Override
    public String getSnippet() { return mSnippet; }

    /**
     * Set the title of the marker
     * @param title string to be set as title
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    /**
     * Set the description of the marker
     * @param snippet string to be set as snippet
     */
    public void setSnippet(String snippet) {
        mSnippet = snippet;
    }
}
