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

import java.util.List;
import java.util.Vector;

import nl.sogeti.android.gpstracker.util.Constants;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import android.util.Log;
import android.view.View;

public class MapViewProxy
{
   private static final String TAG = "OGT.MapViewProxy";
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
   
   public void setMap( View newView )
   {
      if( newView instanceof org.osmdroid.views.MapView )
      {
         mOpenStreetMapView = (org.osmdroid.views.MapView) newView;
         mMapControllerProxy.setController( mOpenStreetMapView );
         mProjectionProxy.setProjection( mOpenStreetMapView );
      }
      else 
      {
         Log.e( TAG, "Unusable map provided: "+ newView);
         throw new IllegalStateException( "Unusable map provided" );
      }
      setBuiltInZoomControls( buildinzoom );
      
      // Add the local overlay to any newly referenced map
      for( OverlayProxy proxy : getOverlays() )
      {
         this.addToMapsOverlays(proxy);
      }
   }
   
   protected View getMap()
   {
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView;
      }
      else 
      {
         return null;
      }
   }
   
   public void postInvalidate()
   {
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.postInvalidate();
      }
   }
   
   public void invalidate()
   {
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.invalidate();
      }
   }

   public void clearAnimation()
   {
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.clearAnimation();
      }
   }

   public MapControllerProxy getController()
   {
      return mMapControllerProxy;
   }

   public ProjectionProxy getProjection()
   {
      return mProjectionProxy;
   }

   public IGeoPoint getMapCenter()
   {
      if( mOpenStreetMapView != null )
      {
         return  mOpenStreetMapView.getMapCenter();
      }
      return null;
   }

   public int getHeight()
   {
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getHeight();
      }
      return 0;
   }

   public int getWidth()
   {
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getWidth();
      }
      return 0;
   }

   public int getZoomLevel()
   {
      int zoomlevel = -1;
      if( mOpenStreetMapView != null )
      {
         zoomlevel = mOpenStreetMapView.getZoomLevel();
      }
      return zoomlevel;
   }

   public int getMaxZoomLevel()
   {
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getMaxZoomLevel();
      }
      return 0;
   }

   public void addOverlay( OverlayProxy overlay )
   {
      mOverlayProxies.add(overlay);
      addToMapsOverlays(overlay);
   }

   private void addToMapsOverlays(OverlayProxy overlay)
   {
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.getOverlays().add( overlay.getOSMOverlay() );
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

   public void clearOverlays()
   {
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.getOverlays().clear();
      }
      for( OverlayProxy proxy : mOverlayProxies )
      {
         proxy.closeResources();
      }
      mOverlayProxies.clear();
   }

   public void setBuiltInZoomControls( boolean b )
   {
      buildinzoom = b;
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.setBuiltInZoomControls( b );
      }
   }

   public void setClickable( boolean b )
   {
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.setClickable( b );
      }
      
   }
   
   public void setOSMType(int renderer )
   {
      if( mOpenStreetMapView != null )
      {
         switch( renderer )
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
}
