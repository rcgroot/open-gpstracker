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
      else if( projection instanceof OpenStreetMapViewProjection )
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
      else if( mOpenStreetMapViewProjection != null )
      {
         mOpenStreetMapViewProjection.toMapPixels( MapViewProxy.convertMapGeoPoint(geoPoint), out );
      }

   }

   public GeoPoint fromPixels( int i, int j )
   {
      GeoPoint point = null;
      if( mProjection != null )
      {
         point  = mProjection.fromPixels( i, j );
      } 
      if( mOpenStreetMapViewProjection != null )
      {
         point = MapViewProxy.convertOSMGeoPoint( mOpenStreetMapViewProjection.fromPixels( i, j ) );
      }
      return point;
   }

   public float metersToEquatorPixels( float i )
   {
      float pixels = 0f;
      if( mProjection != null )
      {
         pixels  = mProjection.metersToEquatorPixels( i );
      } 
      if( mOpenStreetMapViewProjection != null )
      {
         pixels = mOpenStreetMapViewProjection.metersToEquatorPixels( i ) ;
      }
      return pixels;
   }

}
