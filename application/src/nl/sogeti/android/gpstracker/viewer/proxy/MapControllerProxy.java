package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapViewController;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class MapControllerProxy
{

   private MapController mMapController;
   private OpenStreetMapView mOpenStreetMapViewControllerSource;

   public MapControllerProxy()
   {
   }

   
   public void setController(Object controller)
   {
      if( controller instanceof OpenStreetMapViewController )
      {
         mOpenStreetMapViewControllerSource = (OpenStreetMapView) controller;
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
      if( mOpenStreetMapViewControllerSource != null )
      {
         mOpenStreetMapViewControllerSource.getController().setZoom( i );
      }
   }

   public void animateTo( GeoPoint point )
   {
      if( mMapController != null )
      {
         mMapController.animateTo( point );
      }
      if( mOpenStreetMapViewControllerSource != null )
      {
         mOpenStreetMapViewControllerSource.getController().animateTo( new org.andnav.osm.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
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
      if( mOpenStreetMapViewControllerSource != null )
      {
         return mOpenStreetMapViewControllerSource.getController().zoomOut();
      }
      return false;
   }

   public void setCenter( GeoPoint point )
   {
      if( mMapController != null )
      {
         mMapController.setCenter( point );
      }
      if( mOpenStreetMapViewControllerSource != null )
      {
         mOpenStreetMapViewControllerSource.getController().setCenter( new org.andnav.osm.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
      }
   }

}
