package com.android.flexiblepedometer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by AREG on 02.03.2017.
 */

public class HistoryFragment extends Fragment {

    public static final String HISTORY_SIZE  = "HistorySize";
    public static final String HISTORY_DAY   = "HistoryDay";
    public static final String HISTORY_WEEK  = "HistoryWeek";
    public static final String HISTORY_MONTH = "HistoryMonth";

    private final int PLUS = 1;
    private final int MINUS = -1;

    private ImageButton mLeftButton;
    private ImageButton mRightButton;

    private TextView mDate;
    private TextView mTime;
    private TextView mSpeed;
    private TextView mSteps;
    private TextView mMeters;

    private String mSize;

    private XAxis mxAxis;
    private BarChart mBarChart;
    private ArrayList<BarEntry> mBarEntryArrayList;

    private Calendar mCalendar;
    private SimpleDateFormat mSimpleDateFormat;

    private float mStepWidth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.history_fragment, container, false);

        mBarChart = (BarChart) v.findViewById(R.id.whole_schedule);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mSize = bundle.getString(HISTORY_SIZE);
            Log.d("HistoryFragment", "mSize: " + mSize);
        }

        // get step width
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mStepWidth = sharedPref.getInt(getString(R.string.StepWidthPreference), 70) / 100f;

        mDate   = (TextView) v.findViewById(R.id.date);
        mMeters = (TextView) v.findViewById(R.id.history_meters);
        mSteps  = (TextView) v.findViewById(R.id.history_steps);
        mTime   = (TextView) v.findViewById(R.id.history_time);
        mSpeed  = (TextView) v.findViewById(R.id.history_speed);

        mLeftButton  = (ImageButton) v.findViewById(R.id.left_arrow);
        mRightButton = (ImageButton) v.findViewById(R.id.right_arrow);

        mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeCurrentDateView(MINUS);
            }
        });

        mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeCurrentDateView(PLUS);
            }
        });

        mCalendar = Calendar.getInstance();
        mCalendar.setFirstDayOfWeek(Calendar.MONDAY);
        mCalendar.setTime(new Date());

        if (mSize.equals(HISTORY_DAY)) {
            mSimpleDateFormat = new SimpleDateFormat("dd MMM");
            DayHistoryUpdate();
        } else if (mSize.equals(HISTORY_WEEK)) {
            mSimpleDateFormat = new SimpleDateFormat("dd MMM");
            WeekHistoryUpdate();
        } else if (mSize.equals(HISTORY_MONTH)) {
            mSimpleDateFormat = new SimpleDateFormat("MMM yyyy");
            MonthHistoryUpdate();
        }

        return v;
    }

    // update day history
    private void DayHistoryUpdate() {
        mBarEntryArrayList = new ArrayList<>();

        int data[] = PedometerSharedPreferences.ReadStepsAndTime(getContext(), dateString());

        int previous_value = data[0];
        for (int i = 0; i < 24; i++) {
            if ((data[i] != 0) && (i != 0)) {
                int temp = data[i];
                data[i] -= previous_value;
                previous_value = temp;
            }
        }

        int steps = CalculateDayDistance(data);
        int time = data[24];

        for (int i = 0; i < 24; i++) {
            mBarEntryArrayList.add(new BarEntry(i, data[i] * mStepWidth));
        }

        BarSettings();

        mxAxis.setAxisMaximum(24);
        mxAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, AxisBase axisBase) {
                if ((v % 2) == 0) {
                    return String.valueOf((int)v);
                }
                return "";
            }
        });

        mBarChart.invalidate();

        UpdateScreenValues(steps, time);
        mDate.setText(mSimpleDateFormat.format(mCalendar.getTime()));
    }

    // update week history
    private void WeekHistoryUpdate() {
        int data[], steps, time;
        int[] weekDays = new int[7];
        final String[] weeks = new String[] {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun", ""};

        steps = time = 0;
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            mCalendar.set(Calendar.DAY_OF_WEEK, i);
            data = PedometerSharedPreferences.ReadStepsAndTime(getContext(), dateString());
            int previous_value = data[0];
            for (int j = 0; j < 24; j++) {
                if ((data[j] != 0) && (j != 0)) {
                    int temp = data[j];
                    data[j] -= previous_value;
                    previous_value = temp;
                }
            }
            weekDays[i - 1] = CalculateDayDistance(data);
            steps += weekDays[i - 1];
            time += data[24];
        }

        mBarEntryArrayList = new ArrayList<>();
        for (int i = 0; i < weekDays.length; i++) {
            mBarEntryArrayList.add(new BarEntry(i, weekDays[i] * mStepWidth));
        }

        BarSettings();

        mxAxis.setAxisMaximum(8);
        mxAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, AxisBase axisBase) {
                return weeks[(int)v];
            }
        });

        mBarChart.invalidate();

        String date = "";
        mCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        date += mSimpleDateFormat.format(mCalendar.getTime()) + " - ";
        mCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        date += mSimpleDateFormat.format(mCalendar.getTime());
        mDate.setText(date);

        UpdateScreenValues(steps, time);
    }

    // update month history
    private void MonthHistoryUpdate() {
        int data[], steps, time;
        int[] monthDays = new int[mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)];

        steps = time = 0;
        for (int i = 1; i <= mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
            mCalendar.set(Calendar.DAY_OF_MONTH, i);
            data = PedometerSharedPreferences.ReadStepsAndTime(getContext(), dateString());
            int previous_value = data[0];
            for (int j = 0; j < 24; j++) {
                if ((data[j] != 0) && (j != 0)) {
                    int temp = data[j];
                    data[j] -= previous_value;
                    previous_value = temp;
                }
            }
            monthDays[i - 1] = CalculateDayDistance(data);
            steps += monthDays[i - 1];
            time += data[24];
        }

        mBarEntryArrayList = new ArrayList<>();
        for (int i = 0; i < monthDays.length; i++) {
            mBarEntryArrayList.add(new BarEntry(i + 1, monthDays[i] * mStepWidth));
        }

        BarSettings();

        mxAxis.setAxisMaximum(mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
        mxAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, AxisBase axisBase) {
                if ((v % 2) == 0) {
                    return String.valueOf((int)v);
                }
                return "";
            }
        });

        mBarChart.invalidate();

        UpdateScreenValues(steps, time);
        mDate.setText(mSimpleDateFormat.format(mCalendar.getTime()));
    }

    // common settings for all bars
    private void BarSettings() {
        BarDataSet mBarDataSet = new BarDataSet(mBarEntryArrayList, "Projects");
        mBarDataSet.setDrawValues(false);
        BarData BARDATA = new BarData(mBarDataSet);
        BARDATA.setHighlightEnabled(false);
        mBarChart.setData(BARDATA);

        final Description description = new Description();
        description.setText("");
        mBarChart.setDescription(description); // disable description text
        mBarChart.getLegend().setEnabled(false); // disable legend
        mBarChart.setDoubleTapToZoomEnabled(false);

        mxAxis = mBarChart.getXAxis();
        mxAxis.setDrawGridLines(true);
        mxAxis.setAxisMinimum(0);
        mxAxis.setDrawGridLines(false);
        mxAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        mBarChart.getAxisLeft().setAxisMinimum(0);

        YAxis rightAxis = mBarChart.getAxisRight();
        rightAxis.setDrawLabels(false);
        rightAxis.setDrawGridLines(false);
    }

    // image button has been pressed
    private void ChangeCurrentDateView(int delta) {
        if (mSize.equals(HISTORY_DAY)) {
            mCalendar.add(Calendar.DATE, delta);
            DayHistoryUpdate();
        } else if (mSize.equals(HISTORY_WEEK)) {
            mCalendar.add(Calendar.WEEK_OF_YEAR, delta);
            WeekHistoryUpdate();
        } else if (mSize.equals(HISTORY_MONTH)) {
            mCalendar.add(Calendar.MONTH, delta);
            MonthHistoryUpdate();
        }
    }

    // calculate of array elements (only from 0 to 23)
    private int CalculateDayDistance(int[] steps) {
        int sum = 0;
        for (int i = 0; i < 24; i++) {
            sum += steps[i];
        }
        return sum;
    }

    // update steps, meters, time and speed on the screen
    private void UpdateScreenValues(int steps, int time) {
        int speed = 0;
        int meters = (int)(steps * mStepWidth);
        if (time != 0) {
            speed = (int)(((float)meters / (float)time) * 360f);
        }
        mSteps.setText(getContext().getResources().getString(R.string.History_Steps) + " " +
                HomeFragment.IntToString(steps));
        mTime.setText(getContext().getResources().getString(R.string.History_Time) + " " +
                HomeFragment.TimeToString(time));
        mSpeed.setText(getContext().getResources().getString(R.string.History_Speed) + " " +
                String.valueOf(((float)speed / 100f)) + " " + getContext().getResources().
                getString(R.string.History_Speed_Abbreviation));
        mMeters.setText(getContext().getResources().getString(R.string.History_Meters) + " " +
                HomeFragment.IntToString(meters) + " " + getContext().getResources().
                getString(R.string.History_Meters_Abbreviation));
    }

    // return date string
    private String dateString() {
        // key format day - month - year
        String currentDate = String.valueOf(mCalendar.get(Calendar.DATE));
        currentDate += String.valueOf(mCalendar.get(Calendar.MONTH));
        currentDate += String.valueOf(mCalendar.get(Calendar.YEAR));
        return currentDate;
    }
}
