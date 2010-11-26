package nl.sogeti.android.gpstracker.viewer.proxy;

import java.util.List;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.util.Log;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class MapViewProxy
{
   private static final String TAG = "OGT.MapViewProxy";
   private MapView mGoogleMapView;
   private MapControllerProxy mMapControllerProxy;
   private ProjectionProxy mProjectionProxy;
   
   private OpenStreetMapView mOpenStreetMapView;
   private boolean buildinzoom;

   public MapViewProxy()
   {
      mProjectionProxy = new ProjectionProxy();
      mMapControllerProxy = new MapControllerProxy();
   }
   
   public void setMap( View newView )
   {
      if( newView instanceof MapView )
      {
         mGoogleMapView = (MapView) newView;
         mMapControllerProxy.setController( mGoogleMapView.getController() );
         mProjectionProxy.setProjection( mGoogleMapView.getProjection() );
         
         if( mOpenStreetMapView != null )
         {
            GeoPoint mapCenter = convertOSMGeoPoint( mOpenStreetMapView.getMapCenter() );
            int zoomLevel = mOpenStreetMapView.getZoomLevel();
            mMapControllerProxy.setCenter( mapCenter );
            mMapControllerProxy.setZoom( zoomLevel );
         }
         mOpenStreetMapView = null;
      }
      else if( newView instanceof OpenStreetMapView )
      {
         mOpenStreetMapView = (OpenStreetMapView) newView;
         mMapControllerProxy.setController( mOpenStreetMapView.getController() );
         mProjectionProxy.setProjection( mOpenStreetMapView.getProjection() );
         
         if( mGoogleMapView != null )
         {
            GeoPoint mapCenter = mGoogleMapView.getMapCenter();
            int zoomLevel = mGoogleMapView.getZoomLevel();
            mMapControllerProxy.setCenter( mapCenter );
            mMapControllerProxy.setZoom( zoomLevel );
         }
         mGoogleMapView = null;
      }
      setBuiltInZoomControls( buildinzoom );
   }
   
   protected View getMap()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView;
      }
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView;
      }
      return null;
   }
   
   public void postInvalidate()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.postInvalidate();
      }
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.postInvalidate();
      }
   }

   public void clearAnimation()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.clearAnimation();
      }
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.clearAnimation();
      }
   }

   public void clearOverlays()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.getOverlays().clear();
      }
      if( mOpenStreetMapView != null )
      {
         List<OpenStreetMapViewOverlay> overlays = mOpenStreetMapView.getOverlays();
         OpenStreetMapViewOverlay baseLayar = overlays.get(0);
         overlays.clear();
         overlays.add(baseLayar);
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

   /**
    * To maintain state do not alter this list, use the MapViewProxy methods instead
    *  
    * @return The list of overlays
    */
   public List<?> getOverlays()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getOverlays();
      }
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getOverlays();
      }
      return null;
   }

   public GeoPoint getMapCenter()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getMapCenter();
      }
      if( mOpenStreetMapView != null )
      {
         return convertOSMGeoPoint( mOpenStreetMapView.getMapCenter() );
      }
      return null;
   }

   public int getHeight()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getHeight();
      }      
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getHeight();
      }
      return 0;
   }

   public int getWidth()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getWidth();
      }
      else if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getWidth();
      }
      return 0;
   }

   public int getZoomLevel()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getZoomLevel();
      }
      else if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getZoomLevel();
      }
      return 0;
   }

   public int getMaxZoomLevel()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getMaxZoomLevel();
      }
      else if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getMaxZoomLevel();
      }
      return 0;
   }

   public void addOverlay( OverlayProxy overlay )
   {
      if( mGoogleMapView != null  )
      {
         mGoogleMapView.getOverlays().add( overlay.getGoogleOverlay() );
      }
      else if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.getOverlays().add( overlay.getOSMOverlay() );
      }
   }

   public void setBuiltInZoomControls( boolean b )
   {
      buildinzoom = b;
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setBuiltInZoomControls( b );
      }
      else if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.setBuiltInZoomControls( b );
      }
   }

   public void setClickable( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setClickable( b );
      }
      else if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.setClickable( b );
      }
      
   }

   static GeoPoint convertOSMGeoPoint( org.andnav.osm.util.GeoPoint point )
   {
      return new GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }
   
   static org.andnav.osm.util.GeoPoint convertMapGeoPoint( GeoPoint point )
   {
      return new org.andnav.osm.util.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }

   public boolean isSatellite()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.isSatellite();
      }
      return false;
   }

   public boolean isTraffic()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.isTraffic();
      }
      return false;
   }

   public void setTraffic( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setTraffic( b );
      }
   }

   public void setSatellite( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setSatellite( b );
      }
   }

   public void setStreetView( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setStreetView( b );
      }
   }
}
