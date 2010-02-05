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

import nl.sogeti.android.gpstracker.R;
import android.content.Context;
import android.util.TypedValue;

/**
 * Collection of methods to provide metric and imperial data
 * based on locale or overriden by configuration
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
   
   public static final String UNIT_PREF = "UNITS";
   
   public UnitsI18n(Context ctx)
   {
      mContext = ctx;
      TypedValue outValue = new TypedValue();
      mContext.getResources().getValue( R.raw.conversion_from_mps, outValue, false ) ;
      conversion_from_mps =  outValue.getFloat();
      
      mContext.getResources().getValue( R.raw.conversion_from_meter, outValue, false ) ;
      conversion_from_meter = outValue.getFloat();
      
      mContext.getResources().getValue( R.raw.conversion_from_meter_to_small, outValue, false ) ;
      conversion_from_meter_to_small = outValue.getFloat();
      
      speed_unit = mContext.getResources().getString( R.string.speed_unitname );
      distance_unit = mContext.getResources().getString( R.string.distance_unitname );
      distance_smallunit = mContext.getResources().getString( R.string.distance_smallunitname );
   }
   
   public double conversionFromMeterAndMiliseconds( double meters, long miliseconds )
   {
      float hours = miliseconds/3600000f;
      return conversionFromMetersPerSecond( meters / hours  );
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
}
