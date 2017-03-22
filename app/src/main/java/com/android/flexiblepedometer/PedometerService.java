package com.android.flexiblepedometer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by AREG on 28.02.2017.
 */

public class PedometerService extends Service
        implements PedometerAccelerometer.PedometerAccelerometerListener{

    public final static String ACCELEROMETER_KEY = "accelerometer_key";
    public final static int ACCELEROMETER_OFF = 0;
    public final static int ACCELEROMETER_ON = 1;

    private final String POWER_TAG = "MY_POWER_TAG";
    private static final int PEDOMETER_NOTIFICATION_ID = 1385;

    private PedometerAccelerometer mPedometerAccelerometer;

    private static boolean sListenAccelerometer;

    private long mLastSpeakTime;

    // preference settings
    private static float sStepWidth;

    // variables for pedometer
    private static int sSteps;
    private static long sTime;
    private static int sMeters;

    // variables for timer
    private static boolean sTimerEnable;
    private static int sTimerSteps;
    private static int sTimerTimeStart;
    private static int sTimerTimeOffset;

    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmIntent;

    private static Notification mNotification;
    private static Notification.Builder mBuilder;

    private PowerManager.WakeLock mWakeLock;
    @Override
    public void onCreate() {
        super.onCreate();
        sTime = 0;
        sSteps = 0;
        sMeters = 0;
        sTimerEnable = false;
        sTimerSteps = 0;
        sTimerTimeStart = 0;
        sTimerTimeOffset = 0;
        mLastSpeakTime = System.currentTimeMillis();
        sListenAccelerometer = false;
        mPedometerAccelerometer = new PedometerAccelerometer(PedometerService.this);
        mPedometerAccelerometer.PedometerAccelerometerListenerSet(this);

        // Schedule every hour
        mAlarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, EveryHourHandler.class);
        mAlarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + MillisecondsBeforeNextHour(),
                AlarmManager.INTERVAL_HOUR, mAlarmIntent);

        // not turn off CPU if accelerometer listener is not null
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, POWER_TAG);

        // get step width
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sStepWidth = sharedPref.getInt(getApplicationContext().
                getResources().getString(R.string.StepWidthPreference), 20);
        sStepWidth = sStepWidth / 100;
        float sensit = sharedPref.getFloat(getApplicationContext().
                getResources().getString(R.string.SensitivityPreference), 5f);
        PedometerAccelerometer.setSensitivity(sensit);

        // update today's time and steps from SharedPreferences
        Date date = new Date();
        int[] steps_time = PedometerSharedPreferences.ReadStepsAndTime(this, String.valueOf(date.getDate())
                + String.valueOf(date.getMonth()) + String.valueOf(date.getYear() + 1900));
        Log.d("PedometerService", "steps_time: " + Arrays.toString(steps_time));
        sTime = steps_time[24] * 1000; // seconds to miliseconds
        for (int i = 0; i < 24; i++) {
            if (steps_time[i] > sSteps) {
                sSteps = steps_time[i];
            }
        }
        Log.d("PedometerService", "sTime: " + String.valueOf(sTime));
        Log.d("PedometerService", "sSteps: " + String.valueOf(sSteps));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            int val = intent.getIntExtra(ACCELEROMETER_KEY, 2);
            if (val == ACCELEROMETER_OFF) {
                mWakeLock.release();
                sListenAccelerometer = false;
                mPedometerAccelerometer.StopListen();
            }
            if (val == ACCELEROMETER_ON) {
                mWakeLock.acquire();
                sListenAccelerometer = true;
                mPedometerAccelerometer.StartListen();
            }
        }

        if (mBuilder == null) {
            mBuilder = new Notification.Builder(this);
        }
        getMyActivityNotification(this);
        startForeground(PEDOMETER_NOTIFICATION_ID, mNotification);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("PedometerService", "onDestroy called");
        // Remove all our notifications
        NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(PEDOMETER_NOTIFICATION_ID);
        // Stop listen data from accelerometer
        mPedometerAccelerometer.StopListen();
        // If the alarm has been set, cancel it.
        if (mAlarmManager!= null) {
            mAlarmManager.cancel(mAlarmIntent);
        }
        if (sSteps != 0) {
            Log.d("PedometerService", "saved sSteps");
            PedometerSharedPreferences.WriteCurrentStepsAndTime(this); // store current steps and time
        }

        if ((mWakeLock != null) && (mWakeLock.isHeld())) {
            mWakeLock.release();
        }
    }

    // Listener, calls when step has been done
    public void StepHasBeenDone() {
        Log.d("StepHasBeenDone", "StepHasBeenDone()");
        // increase steps
        sSteps++;
        int meters = Math.round(sSteps * sStepWidth);
        // check time
        long now = System.currentTimeMillis();
        long delta = now - mLastSpeakTime;
        mLastSpeakTime = now;
        if (delta / 1000 <= 5) { // less than 5 seconds
            sTime += delta;
        }

        // timer increase steps
        if (sTimerEnable == true) {
            sTimerSteps++;
        }

        if (sMeters != meters) {
            sMeters = meters;
            UpdateNotification(this);
        }
    }

    // return current pedometer state, True - pedometer is on, False - pedometer is off
    public static boolean isListenAccelerometer() {
        return sListenAccelerometer;
    }

    // return steps with
    public static float getStepWidth() {
        return sStepWidth;
    }

    // set steps with
    public static void setStepWidth(float stepWidth, Context context) {
        sStepWidth = stepWidth;
        Log.d("123", "sStepWidth: " + String.valueOf(sStepWidth));
        UpdateNotification(context);
    }

    // return the number of steps today
    public static int getSteps() {
        return sSteps;
    }

    // return the today's total time
    public static int getTotalTime() {
        Long result = sTime/(long)1000;
        return result.intValue();
    }

    // reset steps (if it is new day)
    public static void resetSteps(Context context) {
        sSteps = 0;
        UpdateNotification(context);
    }

    // reset time (if it is new day)
    public static void resetTime() {
        sTime = 0;
    }

    // -----------------------------------------------
    // ------------- START TIMER SECTION -------------
    // -----------------------------------------------
    public static boolean isTimerEnable() {
        return sTimerEnable;
    }

    public static void setTimerEnable(boolean timerEnable) {
        sTimerEnable = timerEnable;
    }

    public static int getTimerSteps() {
        return sTimerSteps;
    }

    public static void setTimerSteps(int timerSteps) {
        sTimerSteps = timerSteps;
    }

    public static int getTimerTimeOffset() {
        return sTimerTimeOffset;
    }

    public static void setTimerTimeOffset(int timerTimeOffset) {
        sTimerTimeOffset = timerTimeOffset;
    }

    public static int getTimerTimeStart() {
        return sTimerTimeStart;
    }

    public static void setTimerTimeStart(int timerTimeStart) {
        sTimerTimeStart = timerTimeStart;
    }
    // -----------------------------------------------
    // -------------- END TIMER SECTION --------------
    // -----------------------------------------------

    // prepare notification
    private static void getMyActivityNotification(Context context) {
        // Convert steps to meters
        int meters = Math.round(sSteps * sStepWidth);
        // prepare data
        String text = HomeFragment.IntToString(meters);
        // create intent for updating service view
        Intent notificationIntent = new Intent(context, PedometerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0); // PendingIntent.FLAG_UPDATE_CURRENT

        // Set current distance in meters
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.pedometer_service);
        remoteViews.setTextViewText(R.id.distance, text + "m");
        // Set image view (on or off pedometer)
        if (sListenAccelerometer == false) {
            remoteViews.setImageViewResource(R.id.state, R.mipmap.red_foot);
        } else {
            remoteViews.setImageViewResource(R.id.state, R.mipmap.green_foot);
        }

        // set receiver for ImageView
        Intent buttonStateIntent = new Intent(context, NotificationStateButtonHandler.class);
        buttonStateIntent.putExtra("action", "close");

        PendingIntent buttonClosePendingIntent = pendingIntent.getBroadcast(context, 0, buttonStateIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.state, buttonClosePendingIntent);

        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);

        if (mNotification == null) {
            mNotification = mBuilder.build();
        }
        mNotification.contentView = remoteViews;
    }

    // update notification
    private static void UpdateNotification(Context context) {
        getMyActivityNotification(context);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(PEDOMETER_NOTIFICATION_ID, mNotification);
    }

    // return milliseconds before next hour
    private int MillisecondsBeforeNextHour() {
        Date date=new Date();
        return ((60 - date.getMinutes()) * 60 * 1000);
    }
}
