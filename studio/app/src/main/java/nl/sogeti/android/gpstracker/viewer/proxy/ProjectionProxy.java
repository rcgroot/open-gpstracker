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

import android.graphics.Point;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

public class ProjectionProxy
{

   private static final String TAG = "OGT.ProjectionProxy";

   private Projection mProjection;
   private org.osmdroid.views.MapView mOpenStreetMapViewProjectionSource;

   public ProjectionProxy()
   {
   }

   public void setProjection(Object projection)
   {
      if (projection instanceof Projection)
      {
         mProjection = (Projection) projection;
         mOpenStreetMapViewProjectionSource = null;
      }
      else if (projection instanceof org.osmdroid.views.MapView)
      {
         mOpenStreetMapViewProjectionSource = (org.osmdroid.views.MapView) projection;
         mProjection = null;
      }
   }

   public void toPixels(GeoPoint geoPoint, Point out)
   {
      if (mProjection != null)
      {
         try
         {
            mProjection.toPixels(geoPoint, out);
         }
         catch (NullPointerException e)
         {
            Log.w(TAG, "Problem using the Google map projection");
         }
      }
      else if (mOpenStreetMapViewProjectionSource != null)
      {
         org.osmdroid.views.Projection projection = mOpenStreetMapViewProjectionSource.getProjection();
         if (projection != null)
         {
            org.osmdroid.util.GeoPoint osmGeopoint = MapViewProxy.convertMapGeoPoint(geoPoint);
            projection.toPixels(osmGeopoint, out);
         }
      }
      else
      {
         throw new IllegalStateException("No working projection available");
      }
   }

   public GeoPoint fromPixels(int i, int j)
   {
      GeoPoint point = null;
      if (mProjection != null)
      {
         point = mProjection.fromPixels(i, j);
      }
      else if (mOpenStreetMapViewProjectionSource != null)
      {
         point = MapViewProxy.convertOSMGeoPoint(mOpenStreetMapViewProjectionSource.getProjection().fromPixels(i, j));
      }
      else
      {
         throw new IllegalStateException("No working projection available");
      }
      return point;
   }

   public float metersToEquatorPixels(float i)
   {
      float pixels = 0f;
      if (mProjection != null)
      {
         pixels = mProjection.metersToEquatorPixels(i);
      }
      else if (mOpenStreetMapViewProjectionSource != null)
      {
         pixels = mOpenStreetMapViewProjectionSource.getProjection().metersToEquatorPixels(i);
      }
      else
      {
         throw new IllegalStateException("No working projection available");
      }
      return pixels;
   }

}
