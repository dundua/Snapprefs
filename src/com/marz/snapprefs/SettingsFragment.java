/*
 * Copyright (C) 2014  Sturmen, stammler, Ramis and P1nGu1n
 *
 * This file is part of Keepchat.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.marz.snapprefs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;


import java.io.File;

public class SettingsFragment extends PreferenceFragment {

    private static final int REQUEST_CHOOSE_DIR = 0x0B00B135;
    public static final String PREF_KEY_SAVE_LOCATION = "pref_key_save_location";

    private SharedPreferences sharedPreferences;

    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.preferences);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

       /* // Add listener to the Launcher preference
        Preference launcherPref = findPreference("pref_launcher");
        launcherPref.setOnPreferenceChangeListener(launcherChangeListener);

        // Add version to the About preference
        Preference aboutPreference = findPreference("pref_about");
        aboutPreference.setTitle(getString(R.string.pref_about_title, BuildConfig.VERSION_NAME));
*/
        // If the Save Location doesn't exist in SharedPreferences add it
        if (!sharedPreferences.contains(PREF_KEY_SAVE_LOCATION)) {
            String defaultLocation = Environment.getExternalStorageDirectory().toString() + "/snapprefs";
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(PREF_KEY_SAVE_LOCATION, defaultLocation);
            editor.apply();
        }

        // Set onClickListener for choosing the Save Location
        Preference locationChooser = findPreference(PREF_KEY_SAVE_LOCATION);
        locationChooser.setSummary(sharedPreferences.getString(PREF_KEY_SAVE_LOCATION, ""));
    }

    @Override
    public void onPause() {
        super.onPause();

        // Set preferences file permissions to be world readable
        File sharedPrefsDir = new File(getActivity().getApplicationInfo().dataDir, "shared_prefs");
        File sharedPrefsFile = new File(sharedPrefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
        if (sharedPrefsFile.exists()) {
            sharedPrefsFile.setReadable(true, false);
        }
    }

    private final Preference.OnPreferenceChangeListener launcherChangeListener = new Preference.OnPreferenceChangeListener() {

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int state = ((Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

            Activity activity = getActivity();
            ComponentName alias = new ComponentName(activity, "com.marz.snapprefs.SettingsActivity-Alias");
            PackageManager p = activity.getPackageManager();
            p.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
            return true;
        }
    };
}
