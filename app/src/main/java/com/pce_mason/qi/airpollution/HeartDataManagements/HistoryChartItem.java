package com.pce_mason.qi.airpollution.HeartDataManagements;

import java.util.ArrayList;
import java.util.List;

public class HistoryChartItem {
    public String date;
    public List<Integer> heartHour;
    public List<Integer> heartAvg;

    public HistoryChartItem(String date){
        this.date = date;
        this.heartHour = new ArrayList<>();
        this.heartAvg = new ArrayList<>();
    }
    public void insertListData(int timeData, int heartData){
        heartHour.add(timeData);
        heartAvg.add(heartData);
    }

}