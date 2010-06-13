package nl.sogeti.android.gpstracker.viewer.proxy;

import java.util.List;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapViewProxy
{
   private MapView mMapView;
   private MapControllerProxy mMapControllerProxy;
   private ProjectionProxy mProjectionProxy;
   
   private OpenStreetMapView mOpenStreetMapView;

   public MapViewProxy( View view )
   {
      mProjectionProxy = new ProjectionProxy();
      mMapControllerProxy = new MapControllerProxy();
      setMap( view );
   }
   
   public void setMap( View view )
   {
      if( view instanceof MapView )
      {
         mMapView = (MapView) view;
         mMapControllerProxy.setController( mMapView.getController() );
         mProjectionProxy.setProjection( mMapView.getProjection() );
         
         if( mOpenStreetMapView != null )
         {
            GeoPoint mapCenter = new GeoPoint( mOpenStreetMapView.getMapCenterLatitudeE6(), mOpenStreetMapView.getMapCenterLongitudeE6() );
            int zoomLevel = mOpenStreetMapView.getZoomLevel();
            mMapControllerProxy.setCenter( mapCenter );
            mMapControllerProxy.setZoom( zoomLevel );
         }
         mOpenStreetMapView = null;
      }
      else if( view instanceof OpenStreetMapView )
      {
         mOpenStreetMapView = (OpenStreetMapView) view;
         mMapControllerProxy.setController( mOpenStreetMapView.getController() );
         mProjectionProxy.setProjection( mOpenStreetMapView.getProjection() );
         
         if( mMapView != null )
         {
            GeoPoint mapCenter = mMapView.getMapCenter();
            int zoomLevel = mMapView.getZoomLevel();
            mMapControllerProxy.setCenter( mapCenter );
            mMapControllerProxy.setZoom( zoomLevel );
         }

         mMapView = null;
      }
   }
   
   public void postInvalidate()
   {
      if( mMapView != null )
      {
         mMapView.postInvalidate();
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

   public void clearAnimation()
   {
      if( mMapView != null )
      {
         mMapView.clearAnimation();
      }
   }

   public int getHeight()
   {
      if( mMapView != null )
      {
         return mMapView.getHeight();
      }
      return 0;
   }

   public GeoPoint getMapCenter()
   {
      if( mMapView != null )
      {
         return mMapView.getMapCenter();
      }
      return null;
   }

   public int getWidth()
   {
      if( mMapView != null )
      {
         return mMapView.getWidth();
      }
      return 0;
   }

   public int getZoomLevel()
   {
      if( mMapView != null )
      {
         return mMapView.getZoomLevel();
      }
      return 0;
   }

   public boolean isSatellite()
   {
      if( mMapView != null )
      {
         return mMapView.isSatellite();
      }
      return false;
   }

   public boolean isTraffic()
   {
      if( mMapView != null )
      {
         return mMapView.isTraffic();
      }
      return false;
   }

   public void setBuiltInZoomControls( boolean b )
   {
      if( mMapView != null )
      {
         mMapView.setBuiltInZoomControls( b );
      }
   }

   public void setClickable( boolean b )
   {
      if( mMapView != null )
      {
         mMapView.setClickable( b );
      }
   }

   public void setSatellite( boolean b )
   {
      if( mMapView != null )
      {
         mMapView.setSatellite( b );
      }
   }

   public void setStreetView( boolean b )
   {
      if( mMapView != null )
      {
         mMapView.setStreetView( b );
      }
   }

   public void setTraffic( boolean b )
   {
      if( mMapView != null )
      {
         mMapView.setTraffic( b );
      }
   }

   public int getMaxZoomLevel()
   {
      if( mMapView != null )
      {
         return mMapView.getMaxZoomLevel();
      }
      return 0;
   }

   protected MapView getGoogleMapView()
   {
      return mMapView;
   }
   
   protected OpenStreetMapView getOsmMapView()
   {
      return null;
   }

   /**
    * To maintain state do not alter this list, use the MapViewProxy methods instead
    *  
    * @return
    */
   public List<?> getOverlays()
   {
      if( mMapView != null )
      {
         return mMapView.getOverlays();
      }
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getOverlays();
      }
      return null;
   }

   public void clearOverlays()
   {
      if( mMapView != null )
      {
         mMapView.getOverlays().clear();
      }
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.getOverlays().clear();
      }
   }

   public void addOverlay( Object overlay )
   {
      if( mMapView != null && overlay instanceof Overlay )
      {
         mMapView.getOverlays().add( (Overlay) overlay );
      }
      else if( mOpenStreetMapView != null && overlay instanceof OpenStreetMapViewOverlay )
      {
         mOpenStreetMapView.getOverlays().add( (OpenStreetMapViewOverlay) overlay );
      }
      else if( overlay instanceof OverlayProxy )
      {
         if( mOpenStreetMapView != null )
         {
            mOpenStreetMapView.getOverlays().add( ((OverlayProxy) overlay).getOsmOverlay() );
         }
         if( mMapView != null )
         {
            mMapView.getOverlays().add( ((OverlayProxy) overlay).getGoogleOverlay() );
         }
      }
   }
}
