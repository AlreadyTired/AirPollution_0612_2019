package com.pce_mason.qi.airpollution.AppClientHeader;

public interface StateNumber {
    enum STATE_SAP{
        IDLE_STATE,
        USER_DUPLICATE_REQUESTED_STATE,
        HALF_USN_ALLOCATE_STATE,
        HALF_USN_INFORMED_STATE,
        USN_INFORMED_STATE,
        HALF_CID_INFORMED_STATE,
        CID_INFORMED_STATE,
        HALF_CID_RELEASED_STATE,
        HALF_IDLE_STATE
    }
}
