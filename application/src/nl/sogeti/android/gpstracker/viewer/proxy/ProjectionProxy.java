package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;

import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

public class ProjectionProxy
{

   private Projection mProjection;
   private OpenStreetMapView mOpenStreetMapViewProjectionSource;

   public ProjectionProxy()
   {
   }
   
   public void setProjection( Object projection )
   {
      if( projection instanceof Projection )
      {
         mProjection = (Projection) projection;
         mOpenStreetMapViewProjectionSource = null;
      } 
      else if( projection instanceof OpenStreetMapView )
      {
         mOpenStreetMapViewProjectionSource = (OpenStreetMapView) projection;
         mProjection = null;
      }
   }

   public void toPixels( GeoPoint geoPoint, Point out )
   {
      if( mProjection != null )
      {
         mProjection.toPixels( geoPoint, out );
      } 
      else if( mOpenStreetMapViewProjectionSource != null )
      {
         mOpenStreetMapViewProjectionSource.getProjection().toMapPixels( MapViewProxy.convertMapGeoPoint(geoPoint), out );
      } 
      else 
      {
         throw new IllegalStateException( "No working projection available" );
      }
   }

   public GeoPoint fromPixels( int i, int j )
   {
      GeoPoint point = null;
      if( mProjection != null )
      {
         point  = mProjection.fromPixels( i, j );
      } 
      else if( mOpenStreetMapViewProjectionSource != null )
      {
         point = MapViewProxy.convertOSMGeoPoint( mOpenStreetMapViewProjectionSource.getProjection().fromPixels( i, j ) );
      }
      else
      {
         throw new IllegalStateException( "No working projection available" );
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
      else if( mOpenStreetMapViewProjectionSource != null )
      {
         pixels = mOpenStreetMapViewProjectionSource.getProjection().metersToEquatorPixels( i ) ;
      }
      else
      {
         throw new IllegalStateException( "No working projection available" );
      }
      return pixels;
   }

}
