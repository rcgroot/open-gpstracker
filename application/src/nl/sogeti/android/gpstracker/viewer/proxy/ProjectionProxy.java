package nl.sogeti.android.gpstracker.viewer.proxy;

import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

public class ProjectionProxy
{

   private Projection mProjection;
   private org.osmdroid.views.MapView mOpenStreetMapViewProjectionSource;

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
      else if( projection instanceof org.osmdroid.views.MapView )
      {
         mOpenStreetMapViewProjectionSource = (org.osmdroid.views.MapView) projection;
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
         org.osmdroid.views.MapView.Projection projection = mOpenStreetMapViewProjectionSource.getProjection();
         if( projection != null )
         {
            projection.toMapPixels( MapViewProxy.convertMapGeoPoint(geoPoint), out );
         }
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
