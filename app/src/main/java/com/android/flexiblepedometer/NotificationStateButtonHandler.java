package com.android.flexiblepedometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by AREG on 03.03.2017.
 */

public class NotificationStateButtonHandler extends BroadcastReceiver {

    private Intent mPedometerService;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Change pedometer state (on to off, or off to on)
        if (PedometerService.isListenAccelerometer() == false) {
            SendIntentToService(context, PedometerService.ACCELEROMETER_ON);
        } else {
            SendIntentToService(context, PedometerService.ACCELEROMETER_OFF);
        }
    }

    // Create new intent and send it to service for transmitting in service new value of
    // accelerometer listener (on or off)
    private void SendIntentToService(Context context, int lister_state) {
        mPedometerService = new Intent(context, PedometerService.class);
        mPedometerService.putExtra(PedometerService.ACCELEROMETER_KEY, lister_state);
        context.startService(mPedometerService);
    }
}
