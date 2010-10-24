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
   private double mConversion_from_mps_to_speed;
   private double mConversion_from_meter_to_distance;
   private double mConversion_from_meter_to_height;
   private String mSpeed_unit;
   private String mDistance_unit;
   private String mHeight_unit;
   private UnitsChangeListener mListener;
   private OnSharedPreferenceChangeListener mPreferenceListener = new OnSharedPreferenceChangeListener()
   {
      public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
      {
         if( key.equals( Constants.UNITS ) )
         {
            initBasedOnPreferences( sharedPreferences );
            if( mListener != null )
            {
               mListener.onUnitsChange();
            }
         }
      }
   };
   
   @SuppressWarnings("unused")
   private static final String TAG = "OGT.UnitsI18n";
   
   public UnitsI18n( Context ctx, UnitsChangeListener listener )
   {
      mContext = ctx;
      mListener =  listener ;
      initBasedOnPreferences( PreferenceManager.getDefaultSharedPreferences( mContext ) );
      PreferenceManager.getDefaultSharedPreferences( mContext ).registerOnSharedPreferenceChangeListener( mPreferenceListener  );
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
         case( Constants.UNITS_NAUTIC ):
            setToMetric();
            overrideWithNautic( mContext.getResources() );
            break;
         case( Constants.UNITS_METRICPACE ):
            setToMetric();
            overrideWithPace( mContext.getResources() );
            break;
         case( Constants.UNITS_IMPERIALPACE ):
            setToImperial();
            overrideWithPaceImperial( mContext.getResources() );
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
      config.locale = new Locale("");
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
      mConversion_from_mps_to_speed =  outValue.getFloat();
      resources.getValue( R.raw.conversion_from_meter, outValue, false ) ;
      mConversion_from_meter_to_distance = outValue.getFloat();
      resources.getValue( R.raw.conversion_from_meter_to_height, outValue, false ) ;
      mConversion_from_meter_to_height = outValue.getFloat();
      
      mSpeed_unit    = resources.getString( R.string.speed_unitname );
      mDistance_unit = resources.getString( R.string.distance_unitname );
      mHeight_unit   = resources.getString( R.string.distance_smallunitname );
   }

   private void overrideWithNautic( Resources resources )
   {
      TypedValue outValue = new TypedValue();
      resources.getValue( R.raw.conversion_from_mps_to_knot, outValue, false ) ;
      mConversion_from_mps_to_speed =  outValue.getFloat();
      resources.getValue( R.raw.conversion_from_meter_to_nauticmile, outValue, false ) ;
      mConversion_from_meter_to_distance = outValue.getFloat();
      
      mSpeed_unit    = resources.getString( R.string.knot_unitname );
      mDistance_unit = resources.getString( R.string.nautic_unitname );
   }
   
   private void overrideWithPace( Resources resources )
   {
      TypedValue outValue = new TypedValue();
      resources.getValue( R.raw.conversion_from_mps_to_pace, outValue, false ) ;
      mConversion_from_mps_to_speed =  outValue.getFloat();
      
      mSpeed_unit    = resources.getString( R.string.pace_unitname );
   }
   
   private void overrideWithPaceImperial( Resources resources )
   {
      TypedValue outValue = new TypedValue();
      resources.getValue( R.raw.conversion_from_mps_to_pace, outValue, false ) ;
      mConversion_from_mps_to_speed =  outValue.getFloat();
      
      mSpeed_unit    = resources.getString( R.string.pace_unitname_imperial );
   }
   
   public double conversionFromMeterAndMiliseconds( double meters, long miliseconds )
   {
      float seconds = miliseconds/1000f;
      return conversionFromMetersPerSecond( meters / seconds  );
   }
   
   public double conversionFromMetersPerSecond( double mps )
   {
      return mps * mConversion_from_mps_to_speed;
   }
   public double conversionFromMeter( double meters )
   {
      double value = meters * mConversion_from_meter_to_distance;
//      Log.d( TAG, String.format( "Converting %f.4 meters to a value of %f.4", meters, value ) );
      return value;
   }
   public double conversionFromMeterToHeight( double meters )
   {
      return meters * mConversion_from_meter_to_height;
   }
   public String getSpeedUnit()
   {
      return mSpeed_unit;
   }
   public String getDistanceUnit()
   {
      return mDistance_unit;
   }
   public String getHeightUnit()
   {
      return mHeight_unit;
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
