/*
 * Copyright 2013 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rootbox.rootboxota.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.rootbox.rootboxota.DirectoryChooserDialog;
import com.rootbox.rootboxota.IOUtils;
import com.rootbox.rootboxota.R;
import com.rootbox.rootboxota.Utils;
import com.rootbox.rootboxota.helpers.RecoveryHelper;
import com.rootbox.rootboxota.helpers.RecoveryHelper.RecoveryInfo;
import com.rootbox.rootboxota.helpers.SettingsHelper;

public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private SettingsHelper mSettingsHelper;
    private RecoveryHelper mRecoveryHelper;
    private CheckBoxPreference mExpertMode;
    private ListPreference mGappsSource;
    private CheckBoxPreference mStableOnly;
    private ListPreference mCheckTimeRom;
    private ListPreference mCheckTimeGapps;
    private Preference mDownloadPath;
    private CheckBoxPreference mDownloadFinished;
    private PreferenceCategory mRecoveryCategory;
    private Preference mRecovery;
    private Preference mInternalSdcard;
    private Preference mExternalSdcard;
    private MultiSelectListPreference mOptions;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {

        mSettingsHelper = new SettingsHelper(this);
        mRecoveryHelper = new RecoveryHelper(this);

        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.layout.activity_settings);

        mExpertMode = (CheckBoxPreference) findPreference(SettingsHelper.PROPERTY_EXPERT);
        mStableOnly = (CheckBoxPreference) findPreference(SettingsHelper.PROPERTY_STABLE_ONLY);
        mGappsSource = (ListPreference) findPreference(SettingsHelper.PROPERTY_GAPPS_SOURCE);
        mCheckTimeRom = (ListPreference) findPreference(SettingsHelper.PROPERTY_CHECK_TIME_ROM);
        mCheckTimeGapps = (ListPreference) findPreference(SettingsHelper.PROPERTY_CHECK_TIME_GAPPS);
        mDownloadPath = findPreference(SettingsHelper.PROPERTY_DOWNLOAD_PATH);
        mDownloadFinished = (CheckBoxPreference) findPreference(SettingsHelper.PROPERTY_DOWNLOAD_FINISHED);
        mRecovery = findPreference(SettingsHelper.PROPERTY_RECOVERY);
        mInternalSdcard = findPreference(SettingsHelper.PROPERTY_INTERNAL_STORAGE);
        mExternalSdcard = findPreference(SettingsHelper.PROPERTY_EXTERNAL_STORAGE);
        mRecoveryCategory = (PreferenceCategory) findPreference(SettingsHelper.PROPERTY_SETTINGS_RECOVERY);
        mOptions = (MultiSelectListPreference) findPreference(SettingsHelper.PROPERTY_SHOW_OPTIONS);

        if (!IOUtils.hasSecondarySdCard()) {
            mRecoveryCategory.removePreference(mExternalSdcard);
        }

        mExpertMode.setDefaultValue(mSettingsHelper.getExpertMode());
        mGappsSource.setValue(String.valueOf(mSettingsHelper.getGappsSource()));
        mStableOnly.setChecked(mSettingsHelper.getStableOnly());
        mCheckTimeRom.setValue(String.valueOf(mSettingsHelper.getCheckTimeRom()));
        mCheckTimeGapps.setValue(String.valueOf(mSettingsHelper.getCheckTimeGapps()));
        mDownloadFinished.setChecked(mSettingsHelper.getDownloadFinished());
        mOptions.setDefaultValue(mSettingsHelper.getShowOptions());

        updateSummaries();
        addOrRemovePreferences();

        try {
            findPreference(SettingsHelper.PROPERTY_VERSION).setSummary(getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            findPreference(SettingsHelper.PROPERTY_VERSION).setSummary("Unknown...");
        }

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        if (SettingsHelper.PROPERTY_DOWNLOAD_PATH.equals(key)) {
            selectDownloadPath();
        } else if (SettingsHelper.PROPERTY_RECOVERY.equals(key)) {
            mRecoveryHelper.selectRecovery();
        } else if (SettingsHelper.PROPERTY_INTERNAL_STORAGE.equals(key)) {
            mRecoveryHelper.selectSdcard(true);
        } else if (SettingsHelper.PROPERTY_EXTERNAL_STORAGE.equals(key)) {
            mRecoveryHelper.selectSdcard(false);
        } else if (SettingsHelper.PROPERTY_VERSION.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://rootbox.ca"));
            startActivity(intent);
        } else if (SettingsHelper.PROPERTY_PARANOID.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/ParanoidAndroid"));
            startActivity(intent);
        } else if (SettingsHelper.PROPERTY_ROOTBOX.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/Root-Box"));
            startActivity(intent);
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsHelper.PROPERTY_EXPERT.equals(key)) {
            addOrRemovePreferences();
        } else if (SettingsHelper.PROPERTY_CHECK_TIME_ROM.equals(key) || SettingsHelper.PROPERTY_STABLE_ONLY.equals(key)) {
            Utils.setAlarm(this, mSettingsHelper.getCheckTimeRom(), false, true);
        } else if (SettingsHelper.PROPERTY_CHECK_TIME_GAPPS.equals(key) || SettingsHelper.PROPERTY_GAPPS_SOURCE.equals(key)) {
            Utils.setAlarm(this, mSettingsHelper.getCheckTimeGapps(), false, false);
        }

        updateSummaries();
    }

    private void updateSummaries() {
        mDownloadPath.setSummary(mSettingsHelper.getDownloadPath());
        RecoveryInfo info = mRecoveryHelper.getRecovery();
        boolean expert = mSettingsHelper.getExpertMode();
        boolean stableonly = mSettingsHelper.getStableOnly();
        boolean downloadfinished = mSettingsHelper.getDownloadFinished();
        mRecovery.setSummary(getResources().getText(R.string.settings_selectrecovery_summary)
                + "\n- Currently set: " + info.getName());
        mInternalSdcard.setSummary(getResources().getText(R.string.settings_internalsdcard_summary)
                + "\n- Currently set: " + mSettingsHelper.getInternalStorage());
        mExternalSdcard.setSummary(getResources().getText(R.string.settings_externalsdcard_summary)
                + "\n- Currently set: " + mSettingsHelper.getExternalStorage());
        mExpertMode.setSummary(expert ? getResources().getText(R.string.settings_expertmode_summary_on)
                : getResources().getText(R.string.settings_expertmode_summary_off));
        mStableOnly.setSummary(stableonly ? getResources().getText(R.string.settings_stable_only_summary_on)
                : getResources().getText(R.string.settings_stable_only_summary_off));
        mDownloadFinished.setSummary(downloadfinished ? getResources().getText(R.string.settings_download_finished_summary_on)
                : getResources().getText(R.string.settings_download_finished_summary_off));
    }

    @SuppressWarnings("deprecation")
    private void addOrRemovePreferences() {
        boolean expert = mSettingsHelper.getExpertMode();
        if (expert) {
            getPreferenceScreen().addPreference(mRecoveryCategory);
        } else {
            getPreferenceScreen().removePreference(mRecoveryCategory);
        }
    }

    private void selectDownloadPath() {
        new DirectoryChooserDialog(this, new DirectoryChooserDialog.DirectoryChooserListener() {

            @Override
            public void onDirectoryChosen(String chosenDir) {
                mSettingsHelper.setDownloadPath(chosenDir);
                updateSummaries();
            }
        }).chooseDirectory();
    }
}