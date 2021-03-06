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

package com.rootbox.rootboxota.updater;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.rootbox.rootboxota.R;
import com.rootbox.rootboxota.Utils;
import com.rootbox.rootboxota.helpers.SettingsHelper;
import com.rootbox.rootboxota.http.URLStringReader;

public class RomUpdater extends Updater {
    private Context mContext;

    private static SettingsHelper mSettingsHelper;

    private boolean mScanning = false;
    private boolean mFromAlarm;

    public RomUpdater(Context context, boolean fromAlarm) {
        super(context);
        mContext = context;
        mFromAlarm = fromAlarm;
    }

    @Override
    public void check() {
        if (mFromAlarm) {
            if (mSettingsHelper == null) {
                mSettingsHelper = new SettingsHelper(getContext());
            }
            if (mSettingsHelper.getCheckTimeRom() < 0) {
                return;
            }
        }
        mScanning = true;
        fireStartChecking();
        new URLStringReader(this).execute(String.format(mSettingsHelper.getRomVersion(), new Object[] {
                getDevice(),
                getVersion() }));
    }

    @Override
    public long getVersion() {
        String version = Utils.getProp(PROPERTY_VERSION_ROOTBOX);
        if (version == null || "".equals(version)) {
            // check for old version
            version = Utils.getProp(PROPERTY_VERSION_ROOTBOX_OLD);
        }
        String stripped = version.replaceAll("\\D+", "");
        return Long.parseLong(stripped);
    }

    @Override
    public boolean isScanning() {
        return mScanning;
    }

    @Override
    public boolean isRom() {
        return true;
    }

    private String getDevice() {
        String device = Utils.getProp(PROPERTY_DEVICE_ROOTBOX);
        if (device == null || "".equals(device)) {
            // check for default device name
            device = Utils.getProp(PROPERTY_DEVICE);
        }
        return device;
    }

    @Override
    public void onReadEnd(String buffer) {
        try {
            mScanning = false;
            PackageInfo[] lastRoms = null;
            setLastUpdates(null);
            String error = null;
            List<PackageInfo> list = new ArrayList<PackageInfo>();
            if (buffer != null && !buffer.isEmpty()) {
                JSONObject updateInfo = new JSONObject(buffer);
                error = updateInfo.optString("error");
                if (error == null || error.isEmpty()) {
                    JSONArray updates = updateInfo.getJSONArray("updates");
                    for (int i = updates.length() - 1; i >= 0; i--) {
                        JSONObject update = updates.getJSONObject(i);
                        list.add(new UpdatePackage(getDevice(), update.getString("name"), update
                                .getLong("version"), update.getString("size"), update
                                .getString("url"), update.getString("md5"), false));
                    }
                }
            }
            lastRoms = list.toArray(new PackageInfo[list.size()]);
            if (list.size() > 0) {
                if (mFromAlarm) {
                    Utils.showNotification(getContext(), lastRoms, ROM_NOTIFICATION_ID,
                            R.string.new_rom_found_title);
                }
            } else {
                if (error != null && !error.isEmpty()) {
                    versionError(error);
                } else {
                    if (!mFromAlarm) {
                        Utils.showToastOnUiThread(getContext(), R.string.check_rom_updates_no_new);
                    }
                }
            }
            setLastUpdates(lastRoms);
            fireCheckCompleted(lastRoms);
        } catch (Exception ex) {
            mScanning = false;
            ex.printStackTrace();
            versionError(null);
        }
    }

    @Override
    public void onReadError(Exception ex) {
        mScanning = false;
        versionError(null);
    }

    private void versionError(String error) {
        if (!mFromAlarm) {
            if (error != null) {
                Utils.showToastOnUiThread(getContext(),
                        getContext().getResources().getString(R.string.check_rom_updates_error)
                                + ": " + error);
            } else {
                Utils.showToastOnUiThread(getContext(), R.string.check_rom_updates_error);
            }
        }
        fireCheckCompleted(null);
    }
}
