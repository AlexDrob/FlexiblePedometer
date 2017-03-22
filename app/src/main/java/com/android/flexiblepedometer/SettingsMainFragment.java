package com.android.flexiblepedometer;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by AREG on 03.03.2017.
 */

public class SettingsMainFragment extends Fragment {

    private PreferenceFragment mPreferenceFragment;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mPreferenceFragment = new SettingsFragment();
        getActivity().getFragmentManager().beginTransaction()
               .replace(R.id.content_frame, mPreferenceFragment)
                .commit();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onPause() {
        mPreferenceFragment.getPreferenceScreen().removeAll();
        super.onPause();
    }
}
