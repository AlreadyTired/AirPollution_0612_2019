package com.pce_mason.qi.airpollution.HeartDataManagements;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.pce_mason.qi.airpollution.AppClientHeader.DefaultValue;
import com.pce_mason.qi.airpollution.AppClientHeader.MessageType;
import com.pce_mason.qi.airpollution.AppClientHeader.ResultCode;
import com.pce_mason.qi.airpollution.AppClientHeader.StateNumber;
import com.pce_mason.qi.airpollution.DataManagements.ColorCalculator;
import com.pce_mason.qi.airpollution.DataManagements.HttpConnectionThread;
import com.pce_mason.qi.airpollution.DataManagements.PostMessageMaker;
import com.pce_mason.qi.airpollution.HeartDataManagements.ChartValueFormatter.MyAxisValueFormatter;
import com.pce_mason.qi.airpollution.MainActivity;
import com.pce_mason.qi.airpollution.R;
import com.pce_mason.qi.airpollution.SignInActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import devs.mulham.horizontalcalendar.HorizontalCalendar;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;

import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.R115;
import static com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer.T115;
import static com.pce_mason.qi.airpollution.MainActivity.APP_STATE;
import static com.pce_mason.qi.airpollution.MainActivity.StateCheck;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HeartDataViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HeartDataViewFragment extends Fragment {
    ColorCalculator colorCalculator;
    String maxHeart,minHeart;
    boolean maxWorking = true;
    TextView heartRateTxt, minMaxHeartTxt,minMaxHeartLabel, historyChartTxt;
    LineChart heartLineChart;
    BarChart historyHeartBarChart;
    int heartChartDataCount = 0;
    Context context;
    int historyDataRange = 3600 * 24 * 14;

    private HttpConnectionThread mAuthTask = null;
    public List<HeartDataItem> mRealTimeItems = new ArrayList<HeartDataItem>();
    public List<HistoryChartItem> mHistoryChartItems = new ArrayList<HistoryChartItem>();
    private HistoryHeartDataArranger historyHeartDataArranger;

    private final String RESULT_CODE ="resultCode";
    private final String LIST_NAME = "historicalHeartQualityDataListEncodings";
    public HeartDataViewFragment() {
        // Required empty public constructor
    }

    public static HeartDataViewFragment newInstance(Context context) {
        HeartDataViewFragment fragment = new HeartDataViewFragment();
        fragment.context = context;
        return fragment;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String heartRate = intent.getStringExtra("heartRate");
            String timeStamp = intent.getStringExtra("timeStamp");
            addEntry(Integer.parseInt(heartRate));

            maxHeart = MainActivity.HEART_GENERATOR.getMaxHeartRate();
            minHeart = MainActivity.HEART_GENERATOR.getMinHeartRate();

            heartRateTxt.setText(heartRate);
            if(maxWorking){
                minMaxHeartTxt.setText(maxHeart);
            }else{
                minMaxHeartTxt.setText(minHeart);
            }
            minMaxHeartTxt.setTextColor(colorCalculator.heartColorPicker(Integer.parseInt(heartRate)));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, new IntentFilter("heartData"));
        colorCalculator = new ColorCalculator();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_heart_data_view, container, false);

        heartLineChart = (LineChart) view.findViewById(R.id.heart_line_chart);
        chartInitialize();
        historyHeartBarChart = (BarChart) view.findViewById(R.id.history_heart_bar);
        barChartInitialize();

        historyChartTxt = (TextView) view.findViewById(R.id.history_heart_txt);
        heartRateTxt = (TextView) view.findViewById(R.id.real_time_heart_txt);
        minMaxHeartLabel = (TextView) view.findViewById(R.id.min_max_label);
        minMaxHeartTxt = (TextView) view.findViewById(R.id.real_time_heart_min_max_txt);
        minMaxHeartTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(maxWorking){
                    minMaxHeartLabel.setText(getString(R.string.heart_min));
                    maxWorking = false;
                }else {
                    minMaxHeartLabel.setText(getString(R.string.heart_max));
                    maxWorking = true;
                }
            }
        });
        /* starts before 1 month from now */
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.MONTH, -1);
        /* ends after 1 month from now */
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 1);
        HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder(view, R.id.history_calendarView)
                .range(startDate, endDate)
                .datesNumberOnScreen(5)
                .build();
        horizontalCalendar.setCalendarListener(new HorizontalCalendarListener() {
            @Override
            public void onDateSelected(Calendar selectedDate, int position) {
                //do something
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

                Date date = selectedDate.getTime();
                String selectedTimeStampDate = dateFormat.format(date);
                for (int i=0; i<mHistoryChartItems.size(); i++){
                    if((mHistoryChartItems.get(i).date).equals(selectedTimeStampDate)){
                        historyChartTxt.setVisibility(View.GONE);
                        historyHeartBarChart.setVisibility(View.VISIBLE);
                        barChartStarter(selectedDataMaker(i));
                        break;
                    }else{
                        historyHeartBarChart.setVisibility(View.GONE);
                        historyChartTxt.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        requestMessageProcess();
        return view;
    }

    /////////////////////   Data communication   /////////////////////
    protected boolean requestMessageProcess(){
        if (mAuthTask != null) {
            return false;
        }
        if(APP_STATE == StateNumber.STATE_SAP.CID_INFORMED_STATE || APP_STATE == StateNumber.STATE_SAP.USN_INFORMED_STATE)
        {
            PostMessageMaker postMessageMaker;
            int USN = MainActivity.USER_SEQUENCE_NUMBER;
            int NSC = MainActivity.NUMBER_OF_SIGNED_IN_COMPLETIONS;

            Long tsLong = System.currentTimeMillis()/1000;
            String endTimestamp = tsLong.toString();
            String startTimestamp = String.valueOf(tsLong - historyDataRange);
            postMessageMaker = new PostMessageMaker(MessageType.SAP_HHVREQ_TYPE, 33, USN);
            postMessageMaker.inputPayload(String.valueOf(NSC), startTimestamp, endTimestamp, "Q30","Q99","Q16552");

            String reqMsg = postMessageMaker.makeRequestMessage();
            Log.d("HHV_REQ_TEST","HHV REQ Packing");
            Log.d("HHV_REQ_TEST",reqMsg);
            try {
                String airUrl = getString(R.string.air_url);
                for(int i=0;i<R115;i++)
                {
                    StateCheck("HHV_REQ");
                    mAuthTask = new HttpConnectionThread(getActivity(),T115);
                    Log.d("HHV_REQ_TEST","HHV REQ Send");
                    boolean RetryFlag = messageResultProcess(mAuthTask.execute(airUrl, reqMsg).get());
                    if(RetryFlag) {break;}
                }

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //Response message parsing and processing
    private boolean messageResultProcess(String responseMsg){
        boolean RetrivalFlag = false;
        try {
            if(responseMsg.equals(DefaultValue.CONNECTION_FAIL) || responseMsg.equals("") || responseMsg.equals("{}")){
                Log.d("HHV_RSP_TEST",responseMsg);
                Toast.makeText(context, getString(R.string.error_server_not_working), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("HHV_RSP_TEST","HHV RSP Received");
                Log.d("HHV_RSP_TEST",responseMsg);
                JSONObject jsonResponse = new JSONObject(responseMsg);
                JSONObject jsonHeader = new JSONObject(jsonResponse.getString("header"));
                JSONObject jsonPayload = new JSONObject(jsonResponse.getString("payload"));

                int msgType = jsonHeader.getInt("msgType");
                int msgLen = jsonHeader.getInt("msgLen");
                int endpointId = jsonHeader.getInt("endpointId");
                int USN = MainActivity.USER_SEQUENCE_NUMBER;

                if (msgType == MessageType.SAP_HHVRSP_TYPE && endpointId == USN ) {
                    Log.d("HHV_RSP_TEST","HHV RSP unpacking");
                    RetrivalFlag = true;
                    Intent loginIt;
                    int resultCode = jsonPayload.getInt(RESULT_CODE);
                    switch (resultCode){
                        case ResultCode.RESCODE_SAP_HHV_OK:
                            listTupleParser(new JSONArray(jsonPayload.getString(LIST_NAME)));
                            break;
                        case ResultCode.RESCODE_SAP_HHV_OTHER:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                        case ResultCode.RESCODE_SAP_HHV_UNALLOCATED_USER_SEQUENCE_NUMBER:
                            Toast.makeText(context, getString(R.string.unallocated_USN), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(context, SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        case ResultCode.RESCODE_SAP_HHV_INCORRECT_NUMBER_OF_SIGNED_IN_COMPLETIONS:
                            Toast.makeText(context, getString(R.string.error_NSC), Toast.LENGTH_SHORT).show();
                            loginIt = new Intent(context, SignInActivity.class);
                            startActivity(loginIt);
                            getActivity().finish();
                            break;
                        default:
                            Toast.makeText(context, getString(R.string.error_result_other), Toast.LENGTH_SHORT).show();
                            break;
                    }
                } else {
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
        return RetrivalFlag;
    }
    public void listTupleParser(JSONArray listData){

        for(int i=0; i < listData.length(); i++){
            HeartDataItem heartDataItem = null;
            String[] tuple = new String[0];
            try {
                tuple = listData.get(i).toString().split(",");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(true){
                String timestamp = tuple[0];
                String latitude = tuple[1];
                String longitude = tuple[2];
                String heartRate = tuple[3];
                String rrInterval = tuple[4];
                heartDataItem = new HeartDataItem(timestamp, heartRate, rrInterval, latitude, longitude);
            }
            try {
                mRealTimeItems.add(heartDataItem);
            } catch (Exception e){
                e.printStackTrace();
            };
        }
        if(mRealTimeItems.size() != 0) {
            historyHeartDataArranger = new HistoryHeartDataArranger(mRealTimeItems);
            mHistoryChartItems = historyHeartDataArranger.getChartItem();
        }
    }
    /////////////////////   Real-Time Chart   /////////////////////
    private void chartInitialize(){
        heartLineChart.setData(new LineData());
        heartLineChart.getDescription().setEnabled(false);
        heartLineChart.setDrawGridBackground(false);
        heartLineChart.setBackgroundColor(Color.WHITE);
        heartLineChart.setViewPortOffsets(10, 0, 10, 0);
        heartLineChart.setDragEnabled(true);
        heartLineChart.setScaleEnabled(true);
        heartLineChart.setTouchEnabled(true);
        heartLineChart.setPinchZoom(false);

        Legend l = heartLineChart.getLegend();
        l.setEnabled(false);

        heartLineChart.getAxisLeft().setEnabled(false);
        heartLineChart.getAxisLeft().setSpaceTop(40);
        heartLineChart.getAxisLeft().setSpaceBottom(40);
        heartLineChart.getAxisRight().setEnabled(false);
        heartLineChart.getXAxis().setEnabled(false);

        heartLineChart.invalidate();
    }

    private void addEntry(int heartRate) {
        if (heartChartDataCount > 99) removeLastEntry();
        LineData data = heartLineChart.getData();

        ILineDataSet set = data.getDataSetByIndex(0);
        // set.addEntry(...); // can be called as well

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        // choose a random dataSet
        int randomDataSetIndex = (int) (Math.random() * data.getDataSetCount());
        int yValue = heartRate;

        data.addEntry(new Entry(data.getDataSetByIndex(randomDataSetIndex).getEntryCount(), yValue), randomDataSetIndex);
        data.notifyDataChanged();

        // let the chart know it's data has changed
        heartLineChart.notifyDataSetChanged();

        heartLineChart.setVisibleXRangeMaximum(6);
        //mChart.setVisibleYRangeMaximum(15, AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
        heartLineChart.moveViewTo(data.getEntryCount() - 7, 50f, YAxis.AxisDependency.LEFT);
        heartChartDataCount++;
    }
    private void removeLastEntry() {

        LineData data = heartLineChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);

            if (set != null) {

                Entry e = set.getEntryForXValue(set.getEntryCount() - 1, Float.NaN);

                data.removeEntry(e, 0);
                // or remove by index
                // mData.removeEntryByXValue(xIndex, dataSetIndex);
                data.notifyDataChanged();
                heartLineChart.notifyDataSetChanged();
                heartLineChart.invalidate();
            }
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "DataSet 1");
        set.setLineWidth(3f);
        set.setCircleRadius(5.5f);
        set.setColor(Color.rgb(240, 99, 99));
        set.setCircleColor(Color.rgb(240, 99, 99));
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(10f);

        return set;
    }


    /////////////////////   History Bar Chart   /////////////////////
    private BarData selectedDataMaker(int index){
        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();
        int hourIndex = 0;
        for(int i = 0; i< 24; i++){
            if (i == mHistoryChartItems.get(index).heartHour.get(hourIndex)){
                entries.add(new BarEntry(i,mHistoryChartItems.get(index).heartAvg.get(hourIndex)));
            }else{
                entries.add(new BarEntry(i,0));
            }

        }
        BarDataSet barData = new BarDataSet(entries,"HeartRate Average");
        barData.setColors(ColorTemplate.MATERIAL_COLORS);
        barData.setHighLightAlpha(255);
        BarData chartData = new BarData(barData);
        return chartData;
    }
    private void barChartInitialize() {
        historyHeartBarChart.getDescription().setEnabled(false);
        historyHeartBarChart.setDrawGridBackground(false);
        historyHeartBarChart.setDrawBarShadow(false);

        MyMarkerView mv = new MyMarkerView(getActivity(), R.layout.chart_custom_marker_view);
        mv.setChartView(historyHeartBarChart); // For bounds control
        historyHeartBarChart.setMarker(mv);

        IAxisValueFormatter custom = new MyAxisValueFormatter();

        YAxis leftAxis = historyHeartBarChart.getAxisLeft();
        leftAxis.setLabelCount(8, false);
        leftAxis.setValueFormatter(custom);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)


    }
    private void barChartStarter(BarData chartData){
        historyHeartBarChart.setData(chartData);
        historyHeartBarChart.invalidate();
    }

}
