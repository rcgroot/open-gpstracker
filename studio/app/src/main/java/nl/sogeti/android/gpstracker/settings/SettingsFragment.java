/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) 2015 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced
 ** Distributed Software Engineering |  or transmitted in any form or by any
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the
 ** 4131 NJ Vianen                   |  purpose, without the express written
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.settings;

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.util.regex.Pattern;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.integration.ExternalConstants;

public class SettingsFragment extends PreferenceFragmentCompat {

    private EditTextPreference time;
    private EditTextPreference distance;
    private EditTextPreference streambroadcast_distance;
    private EditTextPreference streambroadcast_time;
    private EditTextPreference custumupload_backlog;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);


        ListPreference precision = (ListPreference) findPreference(Helper.PRECISION_PREFERENCE);
        time = (EditTextPreference) findPreference(Helper.CUSTOMPRECISIONTIME_PREFERENCE);
        distance = (EditTextPreference) findPreference(Helper.CUSTOMPRECISIONDISTANCE_PREFERENCE);
        streambroadcast_distance = (EditTextPreference) findPreference(Helper.BROADCAST_STREAM_DISTANCE_METER);
        streambroadcast_time = (EditTextPreference) findPreference(Helper.BROADCAST_STREAM_TIME);
        custumupload_backlog = (EditTextPreference) findPreference(Helper.CUSTOM_UPLOAD_BACKLOG);

        setEnabledCustomValues(precision.getValue());
        precision.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setEnabledCustomValues(newValue);
                return true;
            }
        });
        streambroadcast_distance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String fpExpr = "\\d{1,5}";
                return Pattern.matches(fpExpr, newValue.toString());
            }
        });
        streambroadcast_time.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String fpExpr = "\\d{1,21}";
                return Pattern.matches(fpExpr, newValue.toString());
            }
        });
        custumupload_backlog.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String fpExpr = "\\d{1,3}";
                return Pattern.matches(fpExpr, newValue.toString());
            }
        });
    }

    private void setEnabledCustomValues(Object newValue) {
        boolean customPrecision = Integer.toString(ExternalConstants.LOGGING_CUSTOM).equals(newValue);
        time.setEnabled(customPrecision);
        distance.setEnabled(customPrecision);
    }
}