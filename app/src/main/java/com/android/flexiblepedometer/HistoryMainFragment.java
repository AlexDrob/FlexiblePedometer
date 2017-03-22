package com.android.flexiblepedometer;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by AREG on 02.03.2017.
 */

public class HistoryMainFragment extends Fragment {

    private ViewPager viewPager;
    private TabLayout tabLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.history_main_fragment, container, false);

        // prepare Fragments
        Fragment Day_Fragment   = new HistoryFragment();
        Fragment Week_Fragment  = new HistoryFragment();
        Fragment Month_Fragment = new HistoryFragment();
        // prepare Bundles
        Bundle Day_Bundle   = new Bundle();
        Bundle Week_Bundle  = new Bundle();
        Bundle Month_Bundle = new Bundle();
        // put data to Bundles
        Day_Bundle.putString(HistoryFragment.HISTORY_SIZE, HistoryFragment.HISTORY_DAY);
        Week_Bundle.putString(HistoryFragment.HISTORY_SIZE, HistoryFragment.HISTORY_WEEK);
        Month_Bundle.putString(HistoryFragment.HISTORY_SIZE, HistoryFragment.HISTORY_MONTH);
        // put Bundles to Fragments
        Day_Fragment.setArguments(Day_Bundle);
        Week_Fragment.setArguments(Week_Bundle);
        Month_Fragment.setArguments(Month_Bundle);

        viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        HistoryPagerAdapter adapter = new HistoryPagerAdapter(getChildFragmentManager());
        adapter.addFragment(Day_Fragment,   getResources().getString(R.string.DayTitle));
        adapter.addFragment(Week_Fragment,  getResources().getString(R.string.WeekTitle));
        adapter.addFragment(Month_Fragment, getResources().getString(R.string.MonthTitle));
        viewPager.setAdapter(adapter);

        tabLayout = (TabLayout) v.findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

        return v;
    }
}
