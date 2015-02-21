package com.marz.snapprefs;

/**
 * SettingsFragment.java created on 2013-12-22.
 *
 * Copyright (C) 2013 Alec McGavin <alec.mcgavin@gmail.com>
 *
 * This file is part of Snapshare.
 *
 * Snapshare is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Snapshare is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * a gazillion times. If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.marz.snapprefs.Util.CommonUtils;

/**
 * Class to hold all the regular settings
 *
 */
public class SettingsFragment_share extends PreferenceFragment implements OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, AlertDialog.OnClickListener {
    private static final int CLICKS_REQUIRED = 3;
    private int hitCounter;
    private long firstHitTimestamp;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        //addPreferencesFromResource(R.xml.regular_settings);

       // Preference launcherPref = findPreference("pref_launcher");
       // launcherPref.setOnPreferenceChangeListener(launcherChangeListener);

       // Preference aboutPreference = findPreference("pref_about");
        //aboutPreference.setTitle(getString(R.string.pref_about_title, BuildConfig.VERSION_NAME));
       // aboutPreference.setOnPreferenceClickListener(this);

        updateSummary("pref_adjustment");
        updateSummary("pref_rotation");

        if (!CommonUtils.isModuleEnabled()) {
            createXposedDialog().show();
        }
    }

    /**
     * Set the selected value as summary for a fragment
     * @param key the preference's key
     */
    private void updateSummary(String key) {
        Preference pref = findPreference(key);

        if(pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            lp.setSummary(lp.getEntry());
        }
    }

    private final Preference.OnPreferenceChangeListener launcherChangeListener = new Preference.OnPreferenceChangeListener() {

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int state = ((Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

            Activity activity = getActivity();
            ComponentName alias = new ComponentName(activity, "com.p1ngu1n.snapshare.SettingsActivity-Alias");
            PackageManager p = activity.getPackageManager();
            p.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
            return true;
        }
    };

    @Override
    public boolean onPreferenceClick(Preference preference) {
        long currentTimestamp = System.currentTimeMillis();
        if (firstHitTimestamp < (currentTimestamp - 500)) {
            hitCounter = 1;
            firstHitTimestamp = currentTimestamp;
        } else {
            hitCounter++;
        }

        if (hitCounter == CLICKS_REQUIRED) {
            hitCounter = 0;
            //Intent myIntent = new Intent(getActivity(), DeveloperSettingsActivity.class);
            //getActivity().startActivity(myIntent);
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(key);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Pretty much self-explanatory, creates a dialog saying the module is not activated.
     * @return The created dialog
     */
    private AlertDialog createXposedDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getString(R.string.app_name));
        dialogBuilder.setMessage(getString(R.string.module_not_enabled));
        dialogBuilder.setPositiveButton(getString(R.string.open_xposed_installer), this);
        dialogBuilder.setNegativeButton(getString(R.string.close), this);
        return dialogBuilder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            CommonUtils.openXposedInstaller(getActivity());
        }
    }
}
