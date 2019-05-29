package com.pce_mason.qi.airpollution.DataManagements;

import android.graphics.Color;

public class ColorCalculator {

    public int aqiColorPicker(int value){
        if(value>300)           return Color.rgb(128,0,0);
        else if(value>200)      return Color.rgb(128,0,128);
        else if(value>150)      return Color.RED ;
        else if(value>100)      return Color.rgb(255,165,0);
        else if(value>50)       return Color.rgb(255, 204, 0);
        else                    return Color.GREEN;
    }
    public int temperatureColorPicker(float value){
        if(value>50.0 || value < -25.0)           return Color.rgb(128,0,0);
        else if(value>40.0 || value < -20.0)      return Color.rgb(128,0,128);
        else if(value>35.0 || value < -15.0)      return Color.RED ;
        else if(value>30.0 || value < -10.0)      return Color.rgb(255,165,0);
        else if(value>27.0 || value < 0.0)       return Color.rgb(255, 204, 0);
        else                    return Color.GREEN;
    }
    public int heartColorPicker(int value){
        if(value>165 || value < 48)           return Color.rgb(128,0,0);
        else if(value>160 || value < 50)      return Color.rgb(128,0,128);
        else if(value>155 || value < 52)      return Color.RED ;
        else if(value>150 || value < 54)      return Color.rgb(255,165,0);
        else if(value>145 || value < 56)       return Color.rgb(255, 204, 0);
        else                    return Color.GREEN;
    }
    public int textColorPicker(float value){
        if(value < 150) return Color.BLACK;
        else                            return Color.WHITE;
    }
}
