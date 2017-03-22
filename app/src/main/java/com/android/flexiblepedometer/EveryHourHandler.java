package com.android.flexiblepedometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

/**
 * Created by AREG on 03.03.2017.
 */

public class EveryHourHandler extends BroadcastReceiver {

    private static int NOTIFY_ID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        PedometerSharedPreferences.WriteCurrentStepsAndTime(context);
    }
}
