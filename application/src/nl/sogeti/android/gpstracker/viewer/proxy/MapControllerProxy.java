package nl.sogeti.android.gpstracker.viewer.proxy;


import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

public class MapControllerProxy
{

   private MapController mMapController;

   public MapControllerProxy()
   {
   }

   
   public void setController(Object controller)
   {
      if( controller instanceof MapController )
      {
         mMapController = (MapController) controller;
      }
   }

   public void setZoom( int i )
   {
      if( mMapController != null )
      {
         mMapController.setZoom( i );
      }
   }

   public void animateTo( GeoPoint point )
   {
      if( mMapController != null )
      {
         mMapController.animateTo( point );
      }
   }

   public boolean zoomIn()
   {
      if( mMapController != null )
      {
         return mMapController.zoomIn();
      }
      return false;
   }

   public boolean zoomOut()
   {
      if( mMapController != null )
      {
         return mMapController.zoomOut();
      }
      return false;
   }

   public void setCenter( GeoPoint point )
   {
      if( mMapController != null )
      {
         mMapController.setCenter( point );
      }
   }

}
