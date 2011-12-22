/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.viewer;

import java.util.regex.Pattern;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Controller for the settings dialog
 * 
 * @version $Id: ApplicationPreferenceActivity.java 1146 2011-11-05 11:36:51Z
 *          rcgroot $
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class ApplicationPreferenceActivity extends PreferenceActivity
{

   public static final String STREAMBROADCAST_PREFERENCE = "streambroadcast_distance";
   public static final String UNITS_IMPLEMENT_WIDTH_PREFERENCE = "units_implement_width";
   public static final String CUSTOMPRECISIONDISTANCE_PREFERENCE = "customprecisiondistance";
   public static final String CUSTOMPRECISIONTIME_PREFERENCE = "customprecisiontime";
   public static final String PRECISION_PREFERENCE = "precision";
   public static final String CUSTOMUPLOAD_BACKLOG = "CUSTOMUPLOAD_BACKLOG";
   public static final String CUSTOMUPLOAD_URL = "CUSTOMUPLOAD_URL";
   
   private EditTextPreference time;
   private EditTextPreference distance;
   private EditTextPreference implentWidth;

   private EditTextPreference streambroadcast_distance;
   private EditTextPreference custumupload_backlog;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.layout.settings);

      ListPreference precision = (ListPreference) findPreference(PRECISION_PREFERENCE);
      time = (EditTextPreference) findPreference(CUSTOMPRECISIONTIME_PREFERENCE);
      distance = (EditTextPreference) findPreference(CUSTOMPRECISIONDISTANCE_PREFERENCE);
      implentWidth = (EditTextPreference) findPreference(UNITS_IMPLEMENT_WIDTH_PREFERENCE);
      streambroadcast_distance = (EditTextPreference) findPreference(STREAMBROADCAST_PREFERENCE);
      custumupload_backlog = (EditTextPreference) findPreference(CUSTOMUPLOAD_BACKLOG);

      setEnabledCustomValues(precision.getValue());
      precision.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            setEnabledCustomValues(newValue);
            return true;
         }
      });
      implentWidth.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d+([,\\.]\\d+)?";
            return Pattern.matches(fpExpr, newValue.toString());
         }
      });
      streambroadcast_distance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d+";
            boolean matches = Pattern.matches(fpExpr, newValue.toString());
            if (matches)
            {
               Editor editor = getPreferenceManager().getSharedPreferences().edit();
               double value = new UnitsI18n(ApplicationPreferenceActivity.this).conversionFromLocalToMeters(Integer.parseInt(newValue.toString()));
               editor.putFloat("streambroadcast_distance_meter", (float) value);
               editor.commit();
            }
            return matches;
         }
      });
      custumupload_backlog.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d+";
            return Pattern.matches(fpExpr, newValue.toString());
         }
      });
   }

   private void setEnabledCustomValues(Object newValue)
   {
      boolean customPresicion = Integer.toString(Constants.LOGGING_CUSTOM).equals(newValue);
      time.setEnabled(customPresicion);
      distance.setEnabled(customPresicion);
   }
}
