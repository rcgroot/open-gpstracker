/*------------------------------------------------------------------------------
 **     Ident: Innovation en Inspiration > Google Android 
 **    Author: rene
 ** Copyright: (c) Jan 22, 2009 Sogeti Nederland B.V. All Rights Reserved.
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

package nl.sogeti.android.gpstracker.util;

import java.util.Locale;

import nl.sogeti.android.gpstracker.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.TypedValue;

/**
 * Collection of methods to provide metric and imperial data
 * based on locale or overridden by configuration
 * 
 * @version $Id$
 * @author rene (c) Feb 2, 2010, Sogeti B.V.
 */
public class UnitsI18n
{
   private Context mContext;
   private double conversion_from_mps;
   private double conversion_from_meter_to_small;
   private double conversion_from_meter;
   private String speed_unit;
   private String distance_unit;
   private String distance_smallunit;
   private UnitsChangeListener mListener;
   private OnSharedPreferenceChangeListener preferenceListener = new OnSharedPreferenceChangeListener()
   {
      public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
      {
         if( key.equals( Constants.UNITS ) )
         {
            initBasedOnPreferences( sharedPreferences );
            mListener.onUnitsChange();
         }
      }
   };
   
   @SuppressWarnings("unused")
   private static final String TAG = "UnitsI18n";
   
   public UnitsI18n( Context ctx, UnitsChangeListener listener )
   {
      mContext = ctx;
      mListener =  listener ;
      initBasedOnPreferences( PreferenceManager.getDefaultSharedPreferences( mContext ) );
      PreferenceManager.getDefaultSharedPreferences( mContext ).registerOnSharedPreferenceChangeListener( preferenceListener  );
   }
   
   private void initBasedOnPreferences( SharedPreferences sharedPreferences )
   {
      int units = Integer.parseInt( sharedPreferences.getString( Constants.UNITS, Integer.toString( Constants.UNITS_DEFAULT ) ) );
      switch( units )
      {
         case( Constants.UNITS_DEFAULT ):
            setToDefault();
            break;
         case( Constants.UNITS_IMPERIAL ):
            setToImperial();
            break;
         case( Constants.UNITS_METRIC ):
            setToMetric();
            break;
         default:
            setToDefault();
            break;
      }
   }
   
   private void setToDefault()
   {
      Resources resources = mContext.getResources();
      init( resources );
   }
   private void setToMetric()
   {
      Resources resources = mContext.getResources();
      Configuration config = resources.getConfiguration();
      Locale oldLocale = config.locale;
      config.locale = new Locale("nl", "NL");
      resources.updateConfiguration( config, resources.getDisplayMetrics() );
      init( resources );
      config.locale = oldLocale;
      resources.updateConfiguration( config, resources.getDisplayMetrics() );
   }
   private void setToImperial()
   {
      Resources resources = mContext.getResources();
      Configuration config = resources.getConfiguration();
      Locale oldLocale = config.locale;
      config.locale = Locale.US;
      resources.updateConfiguration( config, resources.getDisplayMetrics() );
      init( resources );
      config.locale = oldLocale;
      resources.updateConfiguration( config, resources.getDisplayMetrics() );
   }
   
   /**
    * Based on a given Locale prefetch the units conversions and names.
    * 
    * @param resources Resources initialized with a Locale
    */
   private void init( Resources resources )
   {
      TypedValue outValue = new TypedValue();
      resources.getValue( R.raw.conversion_from_mps, outValue, false ) ;
      conversion_from_mps =  outValue.getFloat();
      
      resources.getValue( R.raw.conversion_from_meter, outValue, false ) ;
      conversion_from_meter = outValue.getFloat();
      
      resources.getValue( R.raw.conversion_from_meter_to_small, outValue, false ) ;
      conversion_from_meter_to_small = outValue.getFloat();
      
      speed_unit = resources.getString( R.string.speed_unitname );
      distance_unit = resources.getString( R.string.distance_unitname );
      distance_smallunit = resources.getString( R.string.distance_smallunitname );
   }
   
   public double conversionFromMeterAndMiliseconds( double meters, long miliseconds )
   {
      float seconds = miliseconds/1000f;
      return conversionFromMetersPerSecond( meters / seconds  );
   }
   
   public double conversionFromMetersPerSecond( double mps )
   {
      return mps * conversion_from_mps;
   }
   public double conversionFromMeter( double meters )
   {
      return meters * conversion_from_meter;
   }
   public double conversionFromMeterToSmall( double meters )
   {
      return meters * conversion_from_meter_to_small;
   }
   public String getSpeedUnit()
   {
      return speed_unit;
   }
   public String getDistanceUnit()
   {
      return distance_unit;
   }
   public String getDistanceSmallUnit()
   {
      return distance_smallunit;
   }
   
   /**
    * 
    * Interface definition for a callback to be invoked when the preference for units changed.  
    *
    * @version $Id$
    * @author rene (c) Feb 14, 2010, Sogeti B.V.
    */
   public interface UnitsChangeListener
   {
      /**
       * Called when the unit data has changed.
       */
      void onUnitsChange();
   }
}
