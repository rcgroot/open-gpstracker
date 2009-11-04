/*------------------------------------------------------------------------------
 **     Ident: Innovation en Inspiration > Google Android 
 **    Author: rene
 ** Copyright: (c) Jan 22, 2009 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.viewer;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * Creates an overlay that can draw a single segment of connected waypoints
 * 
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class TrackingOverlay extends Overlay
{

   public static final int MIDDLE_SEGMENT = 0;
   public static final int FIRST_SEGMENT = 1;
   public static final int LAST_SEGMENT = 2;
   public static final String TAG = TrackingOverlay.class.getName();

   public static final int DRAW_GREEN = 0;
   public static final int DRAW_RED = 1;
   public static final int DRAW_MEASURED = 2;
   public static final int DRAW_CALCULATED = 3;
   public static final int DRAW_DOTS = 4;
   public static final String TRACKCOLORING = "trackcoloring";
   private int trackColoringMethod = DRAW_CALCULATED;

   private ContentResolver mResolver;
   private Context mContext;
   private Projection mProjection;
   
   private int mPlacement = TrackingOverlay.MIDDLE_SEGMENT;
   private Uri mSegmentUri;
   private double mAvgSpeed;
   private GeoPoint mTopLeft;
   private GeoPoint mBottumRight;
 
   private Path mPath;
   private Canvas mCanvas;
   private Shader mShader;
   
   private GeoPoint mStartPoint;
   private GeoPoint mEndPoint;
   private int mCalculatedPoints;
   private Point mPrevScreenPoint;
   private Point mScreenPoint;
   private int stepSize = 1;
   private int step = 0;
   private MapView mMapView;
   
   TrackingOverlay( Context cxt, ContentResolver resolver, Uri segmentUri, int color, double avgSpeed, MapView mapView )
   {
      super();
      this.mContext = cxt;
      this.mMapView = mapView;
      this.trackColoringMethod = color;
      this.mAvgSpeed = avgSpeed;
      this.mPath = new Path();
      this.mResolver = resolver;
      this.mSegmentUri = segmentUri;
   }  

   @Override
   public void draw( Canvas canvas, MapView mapView, boolean shadow )
   {
      switch (trackColoringMethod)
      {
         case ( DRAW_CALCULATED ):
         case ( DRAW_MEASURED ):
         case ( DRAW_RED ):
         case ( DRAW_GREEN ):
            drawPath( canvas, mapView, shadow );
            break;
         case( DRAW_DOTS ):
            drawDots( canvas, mapView, shadow );
            break;
      }
   }
   
   /**
    * 
    * @param canvas
    * @param mapView
    * @param shadow
    * 
    * @see TrackingOverlay#draw(Canvas, MapView, boolean)
    */
   private void drawDots( Canvas canvas, MapView mapView, boolean shadow )
   {  
      this.mCanvas = canvas;
      this.mScreenPoint = new Point();
      this.mPrevScreenPoint = new Point();
      mProjection = mapView.getProjection();
      mTopLeft = mProjection.fromPixels( 0, 0 );
      mBottumRight = mProjection.fromPixels( this.mCanvas.getWidth(), this.mCanvas.getHeight() );
      
      transformSegmentToCanvasDots();

      drawStartStopCircles();
      
      super.draw( this.mCanvas, mapView, shadow );
      this.mCanvas = null;
   }
   /**
    * 
    * @param canvas
    * @param mapView
    * @param shadow
    * 
    * @see TrackingOverlay#draw(Canvas, MapView, boolean)
    */
   public void drawPath( Canvas canvas, MapView mapView, boolean shadow )
   {
      this.mCanvas = canvas;
      this.mScreenPoint = new Point();
      this.mPrevScreenPoint = new Point();
      mProjection = mapView.getProjection();
      mTopLeft = mProjection.fromPixels( 0, 0 );
      mBottumRight = mProjection.fromPixels( this.mCanvas.getWidth(), this.mCanvas.getHeight() );

      this.mPath.rewind();
      this.mShader = null;
      transformSegmentToPath();

      // Just the rendering bits left to do
      Paint routePaint = new Paint();
      routePaint.setPathEffect( new CornerPathEffect( 10 ) );
//      Log.d( TAG, "Drawing color is "+trackColoringMethod );
      switch (trackColoringMethod)
      {
         case ( DRAW_CALCULATED ):
         case ( DRAW_MEASURED ):
            routePaint.setShader( this.mShader );
            break;
         case ( DRAW_RED ):
            routePaint.setColor( Color.RED );
            break;
         case ( DRAW_GREEN ):
         default:
            routePaint.setColor( Color.GREEN );
            break;
      }
      routePaint.setStyle( Paint.Style.STROKE );
      routePaint.setStrokeWidth( 8 );
      routePaint.setAntiAlias( true );

      this.mCanvas.drawPath( this.mPath, routePaint );

      drawStartStopCircles();
      
      super.draw( this.mCanvas, mapView, shadow );
      this.mCanvas = null;
   }

   private void drawStartStopCircles()
   {
      Bitmap bitmap;
      if( (this.mPlacement == FIRST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT) && this.mStartPoint != null)
      {
         Point out = new Point();
         mProjection.toPixels( this.mStartPoint, out ) ;
         mCalculatedPoints++;
         bitmap = BitmapFactory.decodeResource( this.mContext.getResources(), R.drawable.stip2 );
         this.mCanvas.drawBitmap( bitmap, out.x - 8, out.y - 8, new Paint() );
      }
      if( (this.mPlacement == LAST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT) && this.mEndPoint != null )
      {
         Point out = new Point();
         mProjection.toPixels( this.mEndPoint, out ) ;
         mCalculatedPoints++;
         bitmap = BitmapFactory.decodeResource( this.mContext.getResources(), R.drawable.stip );
         this.mCanvas.drawBitmap( bitmap, out.x - 5, out.y - 5, new Paint() );

      }
   }

   /**
    * Set the mPlace to the specified value.
    * 
    * @see TrackingOverlay.FIRST
    * @see TrackingOverlay.MIDDLE
    * @see TrackingOverlay.LAST
    * @param place The placement of this segment in the line.
    */
   public void setPlacement( int place )
   {
      this.mPlacement += place;
   }

   private void transformSegmentToCanvasDots()
   {     
      Cursor trackCursor = null;
      GeoPoint geoPoint;
      mCalculatedPoints = 0;
      setStepSize();
      step = 0;
      
      try
      {
         trackCursor = this.mResolver.query( 
               this.mSegmentUri,
               new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.ACCURACY }
               , null, null, null );
         if( trackCursor.moveToFirst() )
         {
            // Start point of the segments, possible a dot
            this.mStartPoint = extractGeoPoint( trackCursor );
            moveToGeoPoint( this.mStartPoint );
            
            Paint radiusPaint = new Paint();
            radiusPaint.setColor( Color.YELLOW );
            radiusPaint.setAlpha( 100 );
            
            do
            {
               geoPoint = extractGeoPoint( trackCursor );
               Point out = new Point();
               mProjection.toPixels( geoPoint, out ) ;
               mCalculatedPoints++;
               Bitmap bitmap = BitmapFactory.decodeResource( this.mContext.getResources(), R.drawable.stip2 );
               this.mCanvas.drawBitmap( bitmap, out.x - 8, out.y - 8, new Paint() );
               float radius = mProjection.metersToEquatorPixels( trackCursor.getFloat( 2 ) );
               if( radius > 8f )
               {
                  this.mCanvas.drawCircle( out.x, out.y, radius, radiusPaint );
               }
               this.mPrevScreenPoint.x = this.mScreenPoint.x;
               this.mPrevScreenPoint.y = this.mScreenPoint.y;
            }
            while( moveToNextWayPoint( trackCursor ) );
            
            // End point of the segments, possible a dot
            this.mEndPoint = extractGeoPoint( trackCursor );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
      
//      Log.d( TAG, "transformSegmentToPath stop: points "+mCalculatedPoints );
   }

   /**
    * Convert the cursor from the GPSTracking provider into Points on the Path
    * 
    * @see Cursor Cursor used as input
    * @see Point Point used as transformation target
    * @see Path Path used as drawable line
    */
   private void transformSegmentToPath()
   {
      Cursor trackCursor = null;
      Location location = null;
      Location prevLocation = null;
      GeoPoint geoPoint;
      mCalculatedPoints = 0;
      setStepSize();
      step = 0;
      
      try
      {
         switch (trackColoringMethod)
         {
            case ( DRAW_CALCULATED ):
            case ( DRAW_MEASURED ):
               trackCursor = this.mResolver.query( this.mSegmentUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.SPEED, Waypoints.TIME }, null, null, null );
               break;
            case ( DRAW_GREEN ):   
            case ( DRAW_RED ):
            default:
               trackCursor = this.mResolver.query( this.mSegmentUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE }, null, null, null );
               break;
         }
         if( trackCursor.moveToFirst() )
         {
            // Start point of the segments, possible a dot
            this.mStartPoint = extractGeoPoint( trackCursor );
            moveToGeoPoint( this.mStartPoint );
            
            do
            {
               geoPoint = extractGeoPoint( trackCursor );
               switch (trackColoringMethod)
               {
                  case DRAW_RED:
                     lineToGeoPoint( geoPoint, -1d );
                     break;
                  case DRAW_MEASURED:
                     lineToGeoPoint( geoPoint, trackCursor.getDouble( 2 ) );
                     break;
                  case DRAW_CALCULATED:
                     double speed = -1d;
                     location = new Location( this.getClass().getName() );
                     location.setLatitude( trackCursor.getDouble( 0 ) );
                     location.setLongitude( trackCursor.getDouble( 1 ) );
                     location.setTime( trackCursor.getLong( 3 ) );
                     float distance = 0;
                     if( prevLocation != null )
                     {
                        distance = prevLocation.distanceTo( location );
                     }
                     else
                     {
                        prevLocation = location; 
                     }
                     float seconds = ( ( location.getTime() - prevLocation.getTime() ) / 1000f );
                     speed = distance / seconds;
                     prevLocation = location; 
                     lineToGeoPoint( geoPoint, speed );
                     break;
                  default:
                     lineToGeoPoint( geoPoint, -1d );
                     break;
               }
            }
            while( moveToNextWayPoint( trackCursor ) );
            
            // End point of the segments, possible a dot
            this.mEndPoint = extractGeoPoint( trackCursor );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
      
//      Log.d( TAG, "transformSegmentToPath stop: points "+mCalculatedPoints );
   }
   
   private void moveToGeoPoint( GeoPoint geoPoint )
   {
      this.mProjection.toPixels( geoPoint, this.mScreenPoint );
      mCalculatedPoints++;
      this.mPath.moveTo( this.mScreenPoint.x, this.mScreenPoint.y );
   }  

   private void lineToGeoPoint( GeoPoint geoPoint, double speed )
   {
      this.mProjection.toPixels( geoPoint, this.mScreenPoint );     
      mCalculatedPoints++;
      this.mPath.lineTo( this.mScreenPoint.x, this.mScreenPoint.y );

      if( speed > 0  )
      {
         int greenfactor = (int) Math.min( ( 127 * speed ) / mAvgSpeed, 255 );
         int redfactor = 255 - greenfactor;
         int currentColor = Color.rgb( redfactor, greenfactor, 0 );
         Shader s = new LinearGradient( this.mPrevScreenPoint.x, this.mPrevScreenPoint.y, extendPoint( this.mPrevScreenPoint.x, this.mScreenPoint.x ), extendPoint( this.mPrevScreenPoint.y, this.mScreenPoint.y ),
               new int[] { Color.TRANSPARENT, currentColor, Color.TRANSPARENT }, new float[] { 0, 0.5f, 1 }, TileMode.CLAMP );
         //Log.d( TAG, "Created shader for speed " + speed + " with greenfactor " + greenfactor );
         if( this.mShader != null )
         {
            this.mShader = new ComposeShader( this.mShader, s, Mode.XOR );
         }
         else
         {
            this.mShader = s;
         }
      }
      
      this.mPrevScreenPoint.x = this.mScreenPoint.x;
      this.mPrevScreenPoint.y = this.mScreenPoint.y;
   }
   
   private float extendPoint( int x1, int x2 )
   {
      int diff = x2 - x1;
      int next = x2 + diff;
      return next;
   }

   private boolean moveToNextWayPoint( Cursor trackCursor )
   {
      if( trackCursor.isLast() )
      {
         return false;
      }
      
      GeoPoint lastPoint = extractGeoPoint( trackCursor );
      boolean onScreen = isOnScreen( lastPoint );
      
      GeoPoint evalPoint;
      if( onScreen )
      {
         while( trackCursor.moveToNext() )
         {
            step++;
            if( trackCursor.isLast() )
            {
//               Log.d(TAG, "last on screen "+trackCursor.getPosition() );
               return true;
            }
            
            evalPoint = extractGeoPoint( trackCursor );
            if( !isOnScreen( evalPoint ) )
            {
//               Log.d(TAG, "first out screen "+trackCursor.getPosition() );
               return true;
            }
            if( isGoodDrawable() )
            {
               return true; 
            }
         }        
         trackCursor.moveToLast();
         return false;
      }
      else
      {
         while( trackCursor.moveToNext() )
         {
            step++;
            if( trackCursor.isLast() )
            {
//               Log.d(TAG, "last off screen "+trackCursor.getPosition() );
               return true;
            }
            
            evalPoint = extractGeoPoint( trackCursor );
            if( isOnScreen( evalPoint ) )
            {
               moveToGeoPoint( lastPoint );   
//               Log.d(TAG, "first in screen "+trackCursor.getPosition() );
               return true;
            }
            lastPoint = evalPoint;
         }
         trackCursor.moveToLast();
         return false;
      }
   }
   
   private GeoPoint extractGeoPoint( Cursor trackCursor )
   {
      int microLatitude = (int) ( trackCursor.getDouble( 0 ) * 1E6d );
      int microLongitude = (int) ( trackCursor.getDouble( 1 ) * 1E6d );
      return new GeoPoint( microLatitude, microLongitude );
   }

   private boolean isGoodDrawable()
   {
      return step % stepSize == 0;
   }
   
   private void setStepSize()
   {
      int zoomLevel = mMapView.getZoomLevel();
      int maxZoomLevel = mMapView.getMaxZoomLevel();
      if( mMapView != null && zoomLevel >= maxZoomLevel-1 )
      {
         stepSize = 1;
      } 
      else 
      {
         stepSize = (maxZoomLevel - zoomLevel)*2;
      }
//      Log.d( TAG, "Setting stepSize "+stepSize+" on a zoom of "+zoomLevel+"/"+maxZoomLevel );
   }
   
   private boolean isOnScreen( GeoPoint eval )
   {
      boolean under = this.mTopLeft.getLatitudeE6() > eval.getLatitudeE6();
      boolean above = this.mBottumRight.getLatitudeE6() < eval.getLatitudeE6();
      boolean right = this.mTopLeft.getLongitudeE6() < eval.getLongitudeE6();
      boolean left = this.mBottumRight.getLongitudeE6() > eval.getLongitudeE6();
      return under && above && right && left;
   }

   public void setTrackColoringMethod( int coloring )
   {
      this.trackColoringMethod = coloring;
   }
}
