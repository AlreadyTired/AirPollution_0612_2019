package com.pce_mason.qi.airpollution.AppClientHeader;

import android.graphics.Color;

import java.util.regex.Pattern;

public interface DefaultValue {
    String NULL_VALUE = "";
    int ANONYMOUS_USER_SEQUENCENUMBER = 0x00000000;
    int EMPTY_VALUE = 0;
    int RES_SUCCESS = 1;
    int RES_FAILED = 0;
    int EXIST_SENSORS = 0;
    int NOT_EXIST_SENSORS = 1;
    int EXTENDED_DATA_SIZE = 255;
    int HEART_RELATED_DATA_LIST_TYPE = 1;
    int EXTENDED_HEART_RELATED_DATA_LIST_TYPE = 2;
    int SENSOR_INFORMATION_LIST_TYPE = 1;
    int EXTENDED_SENSOR_INFORMATION_LIST_TYPE = 2;
    int TIMESTAMP_SIZE = 4;
    int MAXIMUM_ENDPOINT_ID_SIZE = 16777216;
    int MAC_LENGTH = 12;

    String CONNECTION_FAIL = "FAIL";
    String VALID_EMAIL_ADDRESS ="^[_a-zA-Z0-9-\\.]+@[\\.a-zA-Z0-9-]+\\.[a-zA-Z]+$";
    String VALID_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])(?=.*[!@.#$%^&*?_~]).{6,16}$";
    String NORMAL_STATUS = "All Air Quality Sensor is working well";
    String ABNORMAL_STATUS = " is(are) not working";
    String VALID_MAC_ADDRESS = "[a-fA-F0-9]{12}";

    int COLOR_GOOD = Color.argb(255, 33, 224, 81);
    int COLOR_MODERATE = Color.argb(255, 235, 232, 41);
    int COLOR_SENS_UNHEALTHY = Color.argb(255, 241, 90, 40);
    int COLOR_UNHEALTHY = Color.argb(255, 236, 45, 45);
    int COLOR_VERY_UNHEALTHY = Color.argb(255, 176, 46, 224);
    int COLOR_HAZARDOUS = Color.argb(255, 107, 13, 46);

}
