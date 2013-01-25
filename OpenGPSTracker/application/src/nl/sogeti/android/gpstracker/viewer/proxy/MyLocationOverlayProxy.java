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
package nl.sogeti.android.gpstracker.viewer.proxy;

import nl.sogeti.android.gpstracker.viewer.FixedMyLocationOverlay;

import android.content.Context;
import android.location.Location;
import android.view.View;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;


public class MyLocationOverlayProxy implements OverlayProxy
{
   @SuppressWarnings("unused")
   private static final String TAG = "OGT.MyLocationOverlayProxy";

   private MyLocationOverlay googleLocationOverlay;
   private org.osmdroid.views.overlay.MyLocationOverlay osmLocationOverlay;
   private Context mContext;

   private MapViewProxy mMapViewProxy;
   
   public MyLocationOverlayProxy(Context ctx, MapViewProxy view)
   {
      mContext = ctx;
      mMapViewProxy = view;
   }

   public void enableMyLocation()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.enableMyLocation();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.enableMyLocation();
      }
   }

   public void disableMyLocation()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.disableMyLocation();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.disableMyLocation();
         osmLocationOverlay.onLocationChanged( new Location( "STUB" ) );
         mMapViewProxy.invalidate();
      }
   }

   public void enableCompass()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.enableCompass();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.enableCompass();
      }
   }

   public void disableCompass()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.disableCompass();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.disableCompass();
         mMapViewProxy.invalidate();
      }
   }

   @Override
   public Overlay getGoogleOverlay()
   {
      View mapview = mMapViewProxy.getMap();
      googleLocationOverlay = new FixedMyLocationOverlay( mContext, (MapView) mapview );
      return googleLocationOverlay;
   }

   @Override
   public org.osmdroid.views.overlay.Overlay getOSMOverlay()
   {
      View mapview = mMapViewProxy.getMap();
      osmLocationOverlay = new org.osmdroid.views.overlay.MyLocationOverlay( mContext, (org.osmdroid.views.MapView) mapview );
      return osmLocationOverlay;
   }

   @Override
   public void closeResources()
   {
      disableCompass();
      disableMyLocation();
   }

}
