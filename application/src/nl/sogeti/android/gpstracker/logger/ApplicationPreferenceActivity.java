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
package nl.sogeti.android.gpstracker.logger;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Controller for the settings dialog
 *
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class ApplicationPreferenceActivity extends PreferenceActivity
{

   private EditTextPreference time;
   private EditTextPreference distance;

   @Override
   protected void onCreate( Bundle savedInstanceState ) 
   {
       super.onCreate( savedInstanceState );

       addPreferencesFromResource( R.layout.settings );
       
       ListPreference precision = (ListPreference)findPreference("precision");
       time = (EditTextPreference)findPreference("customprecisiontime");
       distance = (EditTextPreference)findPreference("customprecisiondistance");
       setEnabledCustomValues(precision.getValue());
       
       precision.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener()
       {
         
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {  
            setEnabledCustomValues(newValue);
            return true;
         }
       });
   }
   
   private void setEnabledCustomValues(Object newValue)
   {
      boolean customPresicion = Integer.toString( Constants.LOGGING_CUSTOM ).equals(newValue);
      time.setEnabled(customPresicion);
      distance.setEnabled(customPresicion);
   }

}
