/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Nov 4, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.activity;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.fragment.GoogleMapFragment;
import nl.sogeti.android.gpstracker.fragment.MapQuestFragment;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.viewer.map.LoggerMapHelper;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) Nov 4, 2012, Sogeti B.V.
 */
public class LoggerMapActivity extends Activity
{
   private static final String TAG = "LoggerMapActivity";

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_map);

      if (savedInstanceState == null)
      {
         Fragment mapFragment = null;
         int provider = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.MAPPROVIDER, "" + Constants.GOOGLE)).intValue();
         switch (provider)
         {
            case Constants.GOOGLE:
               mapFragment = new GoogleMapFragment();
               mapFragment.setArguments(getIntent().getExtras());
               break;
            case Constants.MAPQUEST:
               mapFragment = new MapQuestFragment();
               mapFragment.setArguments(getIntent().getExtras());
               break;
            default:
               mapFragment = new GoogleMapFragment();
               mapFragment.setArguments(getIntent().getExtras());
               Log.e(TAG, "Fault in value " + provider + " as MapProvider, defaulting to Google Maps.");
               break;
         }
         getFragmentManager().beginTransaction().add(R.id.activity_logmapview, mapFragment).commit();
      }
   }
   
   public void updateMapProvider()
   {
      int provider = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.MAPPROVIDER, "" + Constants.GOOGLE)).intValue();
      Fragment mapFragment = null;
      switch (provider)
      {
         case Constants.GOOGLE:
            mapFragment = new GoogleMapFragment();
            mapFragment.setArguments(getIntent().getExtras());
            break;
         case Constants.MAPQUEST:
            mapFragment = new MapQuestFragment();
            mapFragment.setArguments(getIntent().getExtras());
            break;
         default:
            mapFragment = new GoogleMapFragment();
            mapFragment.setArguments(getIntent().getExtras());
            Log.e(TAG, "Fault in value " + provider + " as MapProvider, defaulting to Google Maps.");
            break;
      }
      getFragmentManager().beginTransaction().replace(R.id.activity_logmapview, mapFragment).commit();
   }
}
