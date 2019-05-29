package com.pce_mason.qi.airpollution.HeartDataManagements;

import android.util.Log;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class HistoryHeartDataArranger {

    public List<HeartDataItem> mRealTimeItems = new ArrayList<HeartDataItem>();
    private List<HistoryChartItem> mChartItems = new ArrayList<HistoryChartItem>();

    public HistoryHeartDataArranger(List<HeartDataItem> mRealTimeItems){
        this.mRealTimeItems = mRealTimeItems;
        TimeArranger();
    }

    private void TimeArranger(){
        SimpleDateFormat date = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat hour = new SimpleDateFormat("hh");

        String firstTs = mRealTimeItems.get(0).timeStamp;
        Date firstDate = new Date(Integer.parseInt(firstTs));
        String lastTimeStampDate = date.format(firstDate);
        String lastTimeStampHour = hour.format(firstDate);
        int sumCount=0;
        int heartRateSum = 0;
        HistoryChartItem historyChartItem = new HistoryChartItem(lastTimeStampDate);

        for(int i=0; i < mRealTimeItems.size(); i++) {
            String timestamp = mRealTimeItems.get(i).timeStamp;
            Date tsDate = new Date(Long.parseLong(timestamp)* 1000L);
            String dateString = date.format(tsDate);
            String hourString = hour.format(tsDate);

            if(lastTimeStampDate.equals(dateString)){
                if(lastTimeStampHour.equals(hourString)){
                    heartRateSum = heartRateSum + Integer.parseInt(mRealTimeItems.get(i).heartRate);
                    sumCount++;
                }else{
                    heartRateSum = heartRateSum + Integer.parseInt(mRealTimeItems.get(i).heartRate);
                    sumCount++;
                    historyChartItem.insertListData(Integer.parseInt(hour.format(tsDate)),(int)(heartRateSum/sumCount));
                    lastTimeStampHour = hour.format(tsDate);
                    heartRateSum = 0;
                    sumCount = 0;
                }
            }
            else{
                mChartItems.add(historyChartItem);
                lastTimeStampDate = date.format(tsDate);
                historyChartItem = new HistoryChartItem(lastTimeStampDate);
            }
            if((i+1) == mRealTimeItems.size()){
                historyChartItem.insertListData(Integer.parseInt(hour.format(tsDate)),(int)(heartRateSum/sumCount));
                mChartItems.add(historyChartItem);
            }

        }
    }
    public List<HistoryChartItem> getChartItem(){
        return mChartItems;
    }
}
