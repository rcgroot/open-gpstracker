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

import android.os.Build;
import android.util.Log;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.util.List;
import java.util.Vector;

import nl.sogeti.android.gpstracker.util.Constants;

public class MapViewProxy
{
   private static final String TAG = "OGT.MapViewProxy";
   private MapView mGoogleMapView;
   private MapControllerProxy mMapControllerProxy;
   private ProjectionProxy mProjectionProxy;

   private org.osmdroid.views.MapView mOpenStreetMapView;
   private boolean buildinzoom;
   private List<OverlayProxy> mOverlayProxies;

   public MapViewProxy()
   {
      mProjectionProxy = new ProjectionProxy();
      mMapControllerProxy = new MapControllerProxy();
      mOverlayProxies = new Vector<OverlayProxy>();
   }

   static org.osmdroid.util.GeoPoint convertMapGeoPoint(GeoPoint point)
   {
      return new org.osmdroid.util.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
   }

   protected View getMap()
   {
      if (mGoogleMapView != null)
      {
         return mGoogleMapView;
      }
      else if (mOpenStreetMapView != null)
      {
         return mOpenStreetMapView;
      }
      else
      {
         return null;
      }
   }

   public void setMap(View newView)
   {
      if (newView instanceof MapView)
      {
         mGoogleMapView = (MapView) newView;
         mMapControllerProxy.setController(mGoogleMapView.getController());
         mProjectionProxy.setProjection(mGoogleMapView.getProjection());

         if (mOpenStreetMapView != null)
         {
            GeoPoint mapCenter = convertOSMGeoPoint(mOpenStreetMapView.getMapCenter());
            int zoomLevel = mOpenStreetMapView.getZoomLevel();
            mMapControllerProxy.setCenter(mapCenter);
            mMapControllerProxy.setZoom(zoomLevel);
         }
         mOpenStreetMapView = null;
      }
      else if (newView instanceof org.osmdroid.views.MapView)
      {
         mOpenStreetMapView = (org.osmdroid.views.MapView) newView;
         if (Build.VERSION.SDK_INT > 11)
         {
            mOpenStreetMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
         }
         mMapControllerProxy.setController(mOpenStreetMapView);
         mProjectionProxy.setProjection(mOpenStreetMapView);

         if (mGoogleMapView != null)
         {
            GeoPoint mapCenter = mGoogleMapView.getMapCenter();
            int zoomLevel = mGoogleMapView.getZoomLevel();
            mMapControllerProxy.setCenter(mapCenter);
            mMapControllerProxy.setZoom(zoomLevel);
         }
         mGoogleMapView = null;
      }
      else
      {
         Log.e(TAG, "Unusable map provided: " + newView);
         throw new IllegalStateException("Unusable map provided");
      }
      setBuiltInZoomControls(buildinzoom);

      // Add the local overlay to any newly referenced map
      for (OverlayProxy proxy : getOverlays())
      {
         this.addToMapsOverlays(proxy);
      }
   }

   static GeoPoint convertOSMGeoPoint(IGeoPoint point)
   {
      return new GeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
   }

   public void setBuiltInZoomControls(boolean b)
   {
      buildinzoom = b;
      if (mGoogleMapView != null)
      {
         mGoogleMapView.setBuiltInZoomControls(b);
      }
      else if (mOpenStreetMapView != null)
      {
         mOpenStreetMapView.setBuiltInZoomControls(b);
      }
   }

   /**
    * To maintain state do not alter this list, use the MapViewProxy methods instead
    *
    * @return The list of overlays
    */
   public List<OverlayProxy> getOverlays()
   {
      return mOverlayProxies;
   }

   private void addToMapsOverlays(OverlayProxy overlay)
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.getOverlays().add(overlay.getGoogleOverlay());
      }
      else if (mOpenStreetMapView != null)
      {
         mOpenStreetMapView.getOverlays().add(overlay.getOSMOverlay());
      }
   }

   public void postInvalidate()
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.postInvalidate();
      }
      if (mOpenStreetMapView != null)
      {
         mOpenStreetMapView.postInvalidate();
      }
   }

   public void invalidate()
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.invalidate();
      }
      if (mOpenStreetMapView != null)
      {
         mOpenStreetMapView.invalidate();
      }
   }

   public void clearAnimation()
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.clearAnimation();
      }
      if (mOpenStreetMapView != null)
      {
         mOpenStreetMapView.clearAnimation();
      }
   }

   public ProjectionProxy getProjection()
   {
      return mProjectionProxy;
   }

   public GeoPoint getMapCenter()
   {
      if (mGoogleMapView != null)
      {
         return mGoogleMapView.getMapCenter();
      }
      if (mOpenStreetMapView != null)
      {
         return convertOSMGeoPoint(mOpenStreetMapView.getMapCenter());
      }
      return null;
   }

   public int getHeight()
   {
      if (mGoogleMapView != null)
      {
         return mGoogleMapView.getHeight();
      }
      if (mOpenStreetMapView != null)
      {
         return mOpenStreetMapView.getHeight();
      }
      return 0;
   }

   public int getWidth()
   {
      if (mGoogleMapView != null)
      {
         return mGoogleMapView.getWidth();
      }
      else if (mOpenStreetMapView != null)
      {
         return mOpenStreetMapView.getWidth();
      }
      return 0;
   }

   public int getZoomLevel()
   {
      int zoomlevel = -1;
      if (mGoogleMapView != null)
      {
         zoomlevel = mGoogleMapView.getZoomLevel();
      }
      else if (mOpenStreetMapView != null)
      {
         zoomlevel = mOpenStreetMapView.getZoomLevel();
      }
      return zoomlevel;
   }

   public int getMaxZoomLevel()
   {
      if (mGoogleMapView != null)
      {
         return mGoogleMapView.getMaxZoomLevel();
      }
      else if (mOpenStreetMapView != null)
      {
         return mOpenStreetMapView.getMaxZoomLevel();
      }
      return 0;
   }

   public void addOverlay(OverlayProxy overlay)
   {
      mOverlayProxies.add(overlay);
      addToMapsOverlays(overlay);
   }

   public void clearOverlays()
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.getOverlays().clear();
      }
      if (mOpenStreetMapView != null)
      {
         mOpenStreetMapView.getOverlays().clear();
      }
      for (OverlayProxy proxy : mOverlayProxies)
      {
         proxy.closeResources();
      }
      mOverlayProxies.clear();
   }

   public void setClickable(boolean b)
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.setClickable(b);
      }
      else if (mOpenStreetMapView != null)
      {
         mOpenStreetMapView.setClickable(b);
      }

   }

   public boolean isSatellite()
   {
      if (mGoogleMapView != null)
      {
         return mGoogleMapView.isSatellite();
      }
      return false;
   }

   public void setSatellite(boolean b)
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.setSatellite(b);
      }
   }

   public boolean isTraffic()
   {
      if (mGoogleMapView != null)
      {
         return mGoogleMapView.isTraffic();
      }
      return false;
   }

   public void setTraffic(boolean b)
   {
      if (mGoogleMapView != null)
      {
         mGoogleMapView.setTraffic(b);
      }
   }

   public void setOSMType(int renderer)
   {
      if (mOpenStreetMapView != null)
      {
         switch (renderer)
         {
            case Constants.OSM_CLOUDMADE:
               mOpenStreetMapView.setTileSource(TileSourceFactory.CLOUDMADESTANDARDTILES);
               break;
            case Constants.OSM_MAKNIK:
               mOpenStreetMapView.setTileSource(TileSourceFactory.MAPNIK);
               break;
            case Constants.OSM_CYCLE:
               mOpenStreetMapView.setTileSource(TileSourceFactory.CYCLEMAP);
               break;
            default:
               break;
         }

      }
   }

   public void executePostponedActions()
   {
      getController().executePostponedActions();
   }

   public MapControllerProxy getController()
   {
      return mMapControllerProxy;
   }
}
