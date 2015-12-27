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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.util.regex.Pattern;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.service.util.ExternalConstants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;

public class SettingsFragment extends PreferenceFragmentCompat {

    private EditTextPreference time;
    private EditTextPreference distance;
    private EditTextPreference streambroadcast_distance;
    private EditTextPreference custumupload_backlog;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);


        ListPreference precision = (ListPreference) findPreference(Helper.PRECISION);
        time = (EditTextPreference) findPreference(Helper.CUSTOM_PRECISION_TIME);
        distance = (EditTextPreference) findPreference(Helper.CUSTOM_DISTANCE_TIME);
        streambroadcast_distance = (EditTextPreference) findPreference(Helper.BROADCAST_STREAM_DISTANCE);
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
                boolean matches = Pattern.matches(fpExpr, newValue.toString());
                if (matches) {
                    SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
                    double value = new UnitsI18n(getActivity()).conversionFromLocalToMeters(Integer
                            .parseInt(newValue.toString()));
                    editor.putFloat(Helper.BROADCAST_STREAM_DISTANCE_METER, (float) value);
                    editor.commit();
                }
                return matches;
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