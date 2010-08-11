package nl.sogeti.android.gpstracker.viewer.proxy;

import java.util.List;

import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapViewProxy
{
   private MapView mGoogleMapView;
   private MapControllerProxy mMapControllerProxy;
   private ProjectionProxy mProjectionProxy;
   
   public MapViewProxy( View view )
   {
      mProjectionProxy = new ProjectionProxy();
      mMapControllerProxy = new MapControllerProxy();
      setMap( view );
   }
   
   public void setMap( View newView )
   {
      if( newView instanceof MapView )
      {
         mGoogleMapView = (MapView) newView;
         mMapControllerProxy.setController( mGoogleMapView.getController() );
         mProjectionProxy.setProjection( mGoogleMapView.getProjection() );
      }
   }
   
   protected View getMap()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView;
      }
      return null;
   }
   
   public void postInvalidate()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.postInvalidate();
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
      if( mGoogleMapView != null )
      {
         mGoogleMapView.clearAnimation();
      }
   }

   public int getHeight()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getHeight();
      }
      return 0;
   }

   public GeoPoint getMapCenter()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getMapCenter();
      }
      return null;
   }

   public int getWidth()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getWidth();
      }
      return 0;
   }

   public int getZoomLevel()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getZoomLevel();
      }
      return 0;
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

   public void setBuiltInZoomControls( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setBuiltInZoomControls( b );
      }
   }

   public void setClickable( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setClickable( b );
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

   public void setTraffic( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setTraffic( b );
      }
   }

   public int getMaxZoomLevel()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getMaxZoomLevel();
      }
      return 0;
   }

   /**
    * To maintain state do not alter this list, use the MapViewProxy methods instead
    *  
    * @return
    */
   public List<?> getOverlays()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getOverlays();
      }
      return null;
   }

   public void clearOverlays()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.getOverlays().clear();
      }
   }

   public void addOverlay( Object overlay )
   {
      if( mGoogleMapView != null && overlay instanceof Overlay )
      {
         mGoogleMapView.getOverlays().add( (Overlay) overlay );
      }
   }
}
