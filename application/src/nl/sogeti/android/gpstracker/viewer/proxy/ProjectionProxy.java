package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;

import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

public class ProjectionProxy
{

   private Projection mProjection;
   private OpenStreetMapViewProjection mOpenStreetMapViewProjection;

   public ProjectionProxy()
   {
      }
   
   public void setProjection( Object projection )
   {
      if( projection instanceof Projection )
      {
         mProjection = (Projection) projection;
         mOpenStreetMapViewProjection = null;
      }
      if( projection instanceof OpenStreetMapViewProjection )
      {
         mOpenStreetMapViewProjection = (OpenStreetMapViewProjection) projection;
         mProjection = null;
      }

   }

   public void toPixels( GeoPoint geoPoint, Point out )
   {
      if( mProjection != null )
      {
         mProjection.toPixels( geoPoint, out );
      }
   }

}
