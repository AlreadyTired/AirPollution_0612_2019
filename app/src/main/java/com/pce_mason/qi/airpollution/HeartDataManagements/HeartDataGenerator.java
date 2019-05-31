package com.pce_mason.qi.airpollution.HeartDataManagements;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.currentTimeMillis;

public class HeartDataGenerator {
    private Context context;
    private Timer generateTimer = null;
    private int generateInterval = 999;

    private int highestValue = 170;
    private int lowestValue = 53;
    private int hearRate = 70;
    private int randomIncrease,randomVector;
    private int minHeartRate = 70;
    private int maxHeartRate = 70;

    private double latitude, longitude;

    public HeartDataGenerator(Context context, double latitude, double longitude){
        this.context = context;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    private String dataUpdater(){
        randomIncrease = new Random().nextInt(6); // 0 ~ 5
        randomVector = new Random().nextInt(3) - 1; // -1, 0, 1
        hearRate = hearRate + (randomVector * randomIncrease);
        if (hearRate > highestValue){
            hearRate -= 4;
        }else if(hearRate < lowestValue){
            hearRate += 4;
        }
        if(hearRate > maxHeartRate) maxHeartRate = hearRate;
        if(hearRate < minHeartRate) minHeartRate = hearRate;
        return String.valueOf(hearRate);
    }
    private void generateDataBroadcaster(){
        Long CurrentTimeStamp = System.currentTimeMillis()/1000;
        String timeStamp = CurrentTimeStamp.toString();
        Intent intent = new Intent("heartData");
        intent.putExtra("heartRate",dataUpdater());
        intent.putExtra("timeStamp",timeStamp);
        intent.putExtra("latitude",String.valueOf(latitude));
        intent.putExtra("longitude",String.valueOf(longitude));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    private TimerTask dataGenerateTimerTaskMaker() {
        TimerTask dataTimerTask = new TimerTask() {
            @Override
            public void run() {
                generateDataBroadcaster();
            }
        };

        return dataTimerTask;
    }
    public boolean getGenerateState(){
        if (generateTimer == null){
            return true;
        }else {
            return false;
        }
    }
    public void startDataGenerate(){
        generateTimer = new Timer();
        generateTimer.schedule(dataGenerateTimerTaskMaker(), 0, generateInterval);
    }

    public void stopDataGenerate(){
        if (generateTimer != null) {
            generateTimer.cancel();
            generateTimer = null;
            Toast.makeText(context, "Sensor disconnected", Toast.LENGTH_SHORT).show();
        }
    }

    public void setGPSData(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getMaxHeartRate(){
        return String.valueOf(maxHeartRate);
    }

    public String getMinHeartRate(){
        return String.valueOf(minHeartRate);
    }
}
