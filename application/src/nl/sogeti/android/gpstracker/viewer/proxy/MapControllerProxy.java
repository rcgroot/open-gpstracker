package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.OpenStreetMapViewController;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class MapControllerProxy
{

   private MapController mMapController;
   private OpenStreetMapViewController mOpenStreetMapViewController;

   public MapControllerProxy()
   {
   }

   
   public void setController(Object controller)
   {
      if( controller instanceof OpenStreetMapViewController )
      {
         mOpenStreetMapViewController = (OpenStreetMapViewController) controller;
         mMapController = null;
      } else if( controller instanceof MapController )
      {
         mMapController = (MapController) controller;
         mOpenStreetMapViewController = null;
      }
   }

   public void setZoom( int i )
   {
      if( mMapController != null )
      {
         mMapController.setZoom( i );
      }
      if( mOpenStreetMapViewController != null )
      {
         mOpenStreetMapViewController.setZoom( i );
      }
   }

   public void animateTo( GeoPoint point )
   {
      if( mMapController != null )
      {
         mMapController.animateTo( point );
      }
      if( mOpenStreetMapViewController != null )
      {
         mOpenStreetMapViewController.animateTo( new org.andnav.osm.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
      }
   }

   public boolean zoomIn()
   {
      if( mMapController != null )
      {
         return mMapController.zoomIn();
      }
      if( mOpenStreetMapViewController != null )
      {
         return mOpenStreetMapViewController.zoomIn();
      }
      return false;
   }

   public boolean zoomOut()
   {
      if( mMapController != null )
      {
         return mMapController.zoomOut();
      }
      if( mOpenStreetMapViewController != null )
      {
         return mOpenStreetMapViewController.zoomOut();
      }
      return false;
   }

   public void setCenter( GeoPoint point )
   {
      if( mMapController != null )
      {
         mMapController.setCenter( point );
      }
      if( mOpenStreetMapViewController != null )
      {
         mOpenStreetMapViewController.setCenter( new org.andnav.osm.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
      }
   }

}
