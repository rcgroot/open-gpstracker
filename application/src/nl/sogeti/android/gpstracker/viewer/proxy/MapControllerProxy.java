package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapViewController;

import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class MapControllerProxy
{

   private static final String TAG = "OGT.MapControllerProxy";
   private MapController mMapController;
   private OpenStreetMapView mOpenStreetMapViewControllerSource;
   private GeoPoint mPostponedSetCenterPoint = null;
   private int mPostponedSetZoom = -1;

   public MapControllerProxy()
   {
   }

   
   public void setController(Object controller)
   {
      if( controller instanceof OpenStreetMapView )
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
            mOpenStreetMapViewControllerSource.getController().animateTo( new org.andnav.osm.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
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
            mOpenStreetMapViewControllerSource.getController().setCenter( new org.andnav.osm.util.GeoPoint( point.getLatitudeE6(), point.getLongitudeE6() ) );
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
