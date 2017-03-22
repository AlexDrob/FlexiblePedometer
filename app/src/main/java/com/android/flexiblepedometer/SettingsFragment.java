package com.android.flexiblepedometer;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Created by AREG on 03.03.2017.
 */

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_fragment);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getResources().getString(R.string.SensitivityPreference))) {
            PedometerAccelerometer.setSensitivity(sharedPreferences.getFloat(key, 5f));
            //Log.d("SettingsFragment", key + " " + String.valueOf(sharedPreferences.getFloat(key, 5f)));
        } else if (key.equals(getResources().getString(R.string.StepWidthPreference))) {
            PedometerService.setStepWidth((float)sharedPreferences.getInt(key, 70) / 100f, getActivity());
        }
    }
}
