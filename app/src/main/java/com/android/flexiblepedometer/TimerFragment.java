package com.android.flexiblepedometer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by AREG on 02.03.2017.
 */

public class TimerFragment extends Fragment {

    private TextView mTimerSteps;
    private TextView mTimerSpeed;
    private TextView mTimerDistance;
    private TextView mTimerTotalTime;
    private Button mTimerButton;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private float mStepWidth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.timer_fragment, container, false);

        // get step width
        mStepWidth = PedometerService.getStepWidth();

        mTimerSpeed = (TextView) v.findViewById(R.id.total_speed);
        mTimerSteps = (TextView) v.findViewById(R.id.total_steps);
        mTimerDistance = (TextView) v.findViewById(R.id.total_distance);
        mTimerTotalTime = (TextView) v.findViewById(R.id.total_time);

        mTimerButton = (Button) v.findViewById(R.id.start_pause_stop);

        int steps = PedometerService.getTimerSteps();
        int offsetTime = PedometerService.getTimerTimeOffset();
        int distance = Math.round(steps * mStepWidth);

        mTimerSteps.setText(HomeFragment.IntToString(steps));
        mTimerDistance.setText(HomeFragment.IntToString(distance) + getContext().getResources().
                getString(R.string.History_Meters_Abbreviation));
        mTimerSpeed.setText("0 " + getContext().getResources().getString(R.string.History_Speed_Abbreviation));

        InitButtonLogic(offsetTime);

        mTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PedometerService.isTimerEnable() == false) { // turn on
                    PedometerService.setTimerEnable(true);
                    InitButtonLogic(PedometerService.getTimerTimeOffset());
                    PedometerService.setTimerTimeStart((int)(System.currentTimeMillis() / 1000));
                } else { // turn off
                    PedometerService.setTimerEnable(false);
                    // change offset time
                    int time = (int)(System.currentTimeMillis() / 1000);
                    time -= PedometerService.getTimerTimeStart();
                    PedometerService.setTimerTimeOffset(PedometerService.getTimerTimeOffset() + time);
                    InitButtonLogic(PedometerService.getTimerTimeOffset());
                }
            }
        });

        mTimerButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (PedometerService.isTimerEnable() == false) {
                    PedometerService.setTimerSteps(0);
                    PedometerService.setTimerTimeOffset(0);
                    InitButtonLogic(PedometerService.getTimerTimeOffset());
                    mTimerSpeed.setText("0.0 " + getContext().getResources().
                            getString(R.string.History_Speed_Abbreviation));
                    mTimerSteps.setText("0");
                    mTimerDistance.setText("0" + getContext().getResources().
                            getString(R.string.History_Meters_Abbreviation));
                    UpdateTime();
                }
                return true;
            }
        });

        UpdateTime();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer = new Timer(); // Create timer
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (PedometerService.isTimerEnable() == true) { // if timer is turn on
                    Handler refresh = new Handler(Looper.getMainLooper());
                    refresh.post(new Runnable() {
                        public void run()
                        {
                            UpdateTime();
                        }
                    });
                }
            }
        };
        mTimer.schedule(mTimerTask, 10, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop timer
        mTimer.cancel();
        mTimer.purge();
    }

    // init button
    private void InitButtonLogic(int offsetTime) {
        if (PedometerService.isTimerEnable() == false) {
            if (offsetTime == 0) {
                mTimerButton.setText(R.string.START_TIMER);
            } else {
                mTimerButton.setText(R.string.START_RESET_TIMER);
            }
            mTimerButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.start, 0, 0, 0);
        } else {
            mTimerButton.setText(R.string.STOP_TIMER);
            mTimerButton.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.stop, 0, 0, 0);
        }
    }

    // update time on screen
    private void UpdateTime() {
        int offsetTime = PedometerService.getTimerTimeOffset();
        int startTime = PedometerService.getTimerTimeStart();
        int steps = PedometerService.getTimerSteps();
        int distance = Math.round(steps * mStepWidth);
        int speed = 0;
        int total_time = 0;
        if (PedometerService.isTimerEnable() == true) { // if timer is turn on
            total_time = (int)(System.currentTimeMillis() / 1000) - startTime;
        }
        total_time += offsetTime;
        mTimerTotalTime.setText(HomeFragment.TimeToString(total_time));
        // update speed
        if (total_time != 0) {
            speed = (int)(((float)distance / (float)total_time) * 360f);
            mTimerSpeed.setText(String.valueOf(((float)speed / 100f)) + " " +
                    getContext().getResources().getString(R.string.History_Speed_Abbreviation));
        }
    }
}
