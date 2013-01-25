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

import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class MapControllerProxy
{

   private static final String TAG = "OGT.MapControllerProxy";
   private MapController mMapController;
   private org.osmdroid.views.MapView mOpenStreetMapViewControllerSource;
   private GeoPoint mPostponedSetCenterPoint = null;
   private int mPostponedSetZoom = -1;

   public MapControllerProxy()
   {
   }

   
   public void setController(Object controller)
   {
      if( controller instanceof org.osmdroid.views.MapView )
      {
         mOpenStreetMapViewControllerSource = (org.osmdroid.views.MapView) controller;
         mMapController = null;
      } else if( controller instanceof MapController )
      {
         mMapController = (MapController) controller;
         mOpenStreetMapViewControllerSource = null;
      }
   }

   public void setZoom( int i )
   {
      if( mMapController != null )
      {
         mMapController.setZoom( i );
      }
      else if( mOpenStreetMapViewControllerSource != null )
      {
         mOpenStreetMapViewControllerSource.getController().setZoom( i );
         mPostponedSetZoom = i;
      }
      else 
      {
         throw new IllegalStateException( "No working controller available" );
      }
   }

   public void animateTo( GeoPoint point )
   {
      if( point.getLatitudeE6() != 0 && point.getLongitudeE6() != 0 )
      {
         if( mMapController != null )
         {
            mMapController.animateTo( point );
         }
         else if( mOpenStreetMapViewControllerSource != null )
         {
            mOpenStreetMapViewControllerSource.getController().animateTo( new org.osmdroid.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
            mPostponedSetCenterPoint = point;
         }
         else 
         {
            throw new IllegalStateException( "No working controller available" );
         }
      }
   }

   public void setCenter( GeoPoint point )
   {
      if( point.getLatitudeE6() != 0 && point.getLongitudeE6() != 0 )
      {
         if( mMapController != null )
         {
            mMapController.setCenter( point );
         }
         else if( mOpenStreetMapViewControllerSource != null )
         {
            mOpenStreetMapViewControllerSource.getController().setCenter( new org.osmdroid.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
            mPostponedSetCenterPoint = point;
         }
      }
   }


   public boolean zoomIn()
   {
      if( mMapController != null )
      {
         return mMapController.zoomIn();
      }
      if( mOpenStreetMapViewControllerSource != null )
      {
         return mOpenStreetMapViewControllerSource.getController().zoomIn();
      }
      return false;
   }

   public boolean zoomOut()
   {
      if( mMapController != null )
      {
         return mMapController.zoomOut();
      }
      else if( mOpenStreetMapViewControllerSource != null )
      {
         return mOpenStreetMapViewControllerSource.getController().zoomOut();
      }
      return false;
   }

   public void executePostponedActions()
   {
      if( mPostponedSetCenterPoint != null )
      {
         Log.w( TAG, "mPostponedSetCenterPoint"+ mPostponedSetCenterPoint);
         setCenter( mPostponedSetCenterPoint );
         mPostponedSetCenterPoint = null;
      }
      if( mPostponedSetZoom >= 0 )
      {
         Log.w( TAG, "mPostponedSetZoom"+ mPostponedSetCenterPoint);
         setZoom( mPostponedSetZoom );
         mPostponedSetZoom = -1;
      }
   }

}
