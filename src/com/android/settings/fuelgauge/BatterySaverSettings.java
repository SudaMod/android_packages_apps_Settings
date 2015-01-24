/*
 * Copyright (C) 2015 The New One Android
 * Copyright (C) 2015 The SudaMod Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.Global;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class BatterySaverSettings extends SettingsPreferenceFragment 
             implements OnPreferenceChangeListener {

    private static final String TAG = "BatterySaverSettings";

    private static final String KEY_POWER_SAVE_SETTING = "power_save_setting";

    private ListPreference mPowerSaveSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.battery_saver_settings);

        ContentResolver resolver = getActivity().getContentResolver();

        mPowerSaveSettings = (ListPreference) findPreference(KEY_POWER_SAVE_SETTING);
        int PowerSaveSettings = Settings.System.getInt(
                resolver, Settings.System.POWER_SAVE_SETTINGS, 0);
        mPowerSaveSettings.setValue(String.valueOf(PowerSaveSettings));
        mPowerSaveSettings.setSummary(mPowerSaveSettings.getEntry());
        mPowerSaveSettings.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mPowerSaveSettings) {
            int PowerSaveSettings = Integer.valueOf((String) newValue);
            int index = mPowerSaveSettings.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.POWER_SAVE_SETTINGS, PowerSaveSettings);
            mPowerSaveSettings.setSummary(mPowerSaveSettings.getEntries()[index]);
            Settings.Global.putInt(resolver,
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
            return true;
        }
        return false;
    }

}

