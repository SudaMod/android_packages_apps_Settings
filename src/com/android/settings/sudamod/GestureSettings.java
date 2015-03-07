/*
 * Copyright (C) 2015 The SudaMod project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.android.settings.sudamod;

import android.content.ContentResolver;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.CmHardwareManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.SearchIndexableResource;
import android.util.Log;

import static android.hardware.CmHardwareManager.FEATURE_TAP_TO_WAKE;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

public class GestureSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "GestureSettings";

    private static final String CATEGORY_GESTURES = "category_gestures";
    private static final String CATEGORY_DIRECT_CONTROL = "direct_control";

    private static final String KEY_GESTURES = "device_specific_gesture_settings";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_TAP_TO_WAKE = "double_tap_wake_gesture";
    private static final String DIRECT_CALL_FOR_DIALER = "direct_call_for_dialer";
    private static final String DIRECT_CALL_FOR_MMS = "direct_call_for_mms";

    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mTapToWake;
    private SwitchPreference mDirectCallForDialer;
    private SwitchPreference mDirectCallForMms;

    private CmHardwareManager mCmHardwareManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        mCmHardwareManager = (CmHardwareManager) activity.getSystemService(Context.CMHW_SERVICE);

        addPreferencesFromResource(R.xml.gesture_settings);

        // update or remove gesture activity
        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_GESTURES);

        final PreferenceCategory category_gesture = (PreferenceCategory) findPreference(CATEGORY_GESTURES);
        final PreferenceCategory category_direct_control = (PreferenceCategory) findPreference(CATEGORY_DIRECT_CONTROL);

        mTapToWake = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);

        if (!mCmHardwareManager.isSupported(FEATURE_TAP_TO_WAKE)) {
            category_gesture.removePreference(mTapToWake);
            mTapToWake = null;
        }

        mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            category_direct_control.removePreference(mLiftToWakePreference);
        }

        mDirectCallForDialer = (SwitchPreference) findPreference(DIRECT_CALL_FOR_DIALER);
        mDirectCallForDialer.setOnPreferenceChangeListener(this);
        mDirectCallForMms = (SwitchPreference) findPreference(DIRECT_CALL_FOR_MMS);
        mDirectCallForMms.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (preference == mTapToWake) {
            return  mCmHardwareManager.set(FEATURE_TAP_TO_WAKE, mTapToWake.isChecked());
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        } else if (preference == mDirectCallForDialer) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), DIRECT_CALL_FOR_DIALER, value ? 1 : 0);
        } else if (preference == mDirectCallForMms) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), DIRECT_CALL_FOR_MMS, value ? 1 : 0);
        }
        return true;
    }
    private void updateState() {

        ContentResolver resolver = getActivity().getContentResolver();

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(resolver, WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        // Update tap-to-wake if it is available.
        if (mTapToWake != null) {
            mTapToWake.setChecked(mCmHardwareManager.get(FEATURE_TAP_TO_WAKE));
        }

        mDirectCallForDialer.setChecked((Settings.System.getInt(resolver,
                Settings.System.DIRECT_CALL_FOR_DIALER, 0) == 1));

        mDirectCallForMms.setChecked((Settings.System.getInt(resolver,
                Settings.System.DIRECT_CALL_FOR_MMS, 0) == 1));

    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    public static void restore(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            CmHardwareManager cmHardwareManager =
                (CmHardwareManager) ctx.getSystemService(Context.CMHW_SERVICE);
            if (cmHardwareManager.isSupported(FEATURE_TAP_TO_WAKE)) {
            final boolean enabled = prefs.getBoolean(KEY_TAP_TO_WAKE,
               cmHardwareManager.get(FEATURE_TAP_TO_WAKE));

            if (!cmHardwareManager.set(FEATURE_TAP_TO_WAKE, enabled)) {
                Log.e(TAG, "Failed to restore tap-to-wake settings.");
            } else {
                Log.d(TAG, "Tap-to-wake settings restored.");
            }
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                private boolean mHasTapToWake;

            CmHardwareManager cmHardwareManager =
                (CmHardwareManager) context.getSystemService(Context.CMHW_SERVICE);

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.gesture_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();

                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!cmHardwareManager.isSupported(FEATURE_TAP_TO_WAKE)) {
                        result.add(KEY_TAP_TO_WAKE);
                    }

                    return result;
                }
        };
}
