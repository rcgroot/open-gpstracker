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

import java.util.List;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.viewer.proxy.MapViewProxy;
import nl.sogeti.android.gpstracker.viewer.proxy.OverlayProxy;
import nl.sogeti.android.gpstracker.viewer.proxy.ProjectionProxy;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Creates an overlay that can draw a single segment of connected waypoints
 * 
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class SegmentOverlay extends Overlay implements OverlayProxy
{
   public static final int MIDDLE_SEGMENT = 0;
   public static final int FIRST_SEGMENT = 1;
   public static final int LAST_SEGMENT = 2;
   public static final int DRAW_GREEN = 0;
   public static final int DRAW_RED = 1;
   public static final int DRAW_MEASURED = 2;
   public static final int DRAW_CALCULATED = 3;
   public static final int DRAW_DOTS = 4;
   private static final String TAG = "OGT.SegmentOverlay";
   private static final float MINIMUM_PX_DISTANCE = 15;
   private static final int CALCULATE_TRACK = 11;
   private static final int CALCULATE_MEDIA = 12;
   
   private int mTrackColoringMethod = DRAW_CALCULATED;

   private ContentResolver mResolver;
   private LoggerMap mLoggerMap;
   private ProjectionProxy mProjection;

   private int mPlacement = SegmentOverlay.MIDDLE_SEGMENT;
   private Uri mWaypointsUri;
   private Uri mMediaUri;
   private double mAvgSpeed;
   private GeoPoint mTopLeft;
   private GeoPoint mBottumRight;

   private Vector<DotVO> mDotPath;
   private Vector<DotVO> mDotPathCalculation;
   private Path mPath;
   private Path mPathCalculation;
   private Shader mShader;
   private Vector<MediaVO> mMediaPath;
   private Vector<MediaVO> mMediaPathCalculation;

   private GeoPoint mStartPoint;
   private GeoPoint mEndPoint;
   private int mCalculatedPoints;
   private Point mPrevDrawnScreenPoint;
   private Point mScreenPointBackup;
   private Point mScreenPoint;
   private int mStepSize = -1;
   private MapViewProxy mMapView;
   private Location mLocation;
   private Location mPrevLocation;
   private Cursor mWaypointsCursor;
   private Cursor mMediaCursor;
   private Uri mSegmentUri;
   private int mWaypointCount;
   private int mWidth;
   private int mHeight;
   //   private Canvas mDebugCanvas;
   private GeoPoint mPrevGeoPoint;
   private int mCurrentColor;
   private Paint dotpaint;
   private Paint radiusPaint;
   private Paint routePaint;
   private Paint defaultPaint;
   private boolean mRequeryFlag;
   private Handler mHandler;

   private final ContentObserver mTrackSegmentsObserver = new ContentObserver( new Handler() )
      {

         @Override
         public void onChange( boolean selfUpdate )
         {
            if( !selfUpdate )
            {
               mRequeryFlag = true;
            }
            else
            {
               Log.w( TAG, "mTrackSegmentsObserver skipping change on " + mSegmentUri );
            }
         }
      };
      
   private final Runnable mediaCalculator = new Runnable()
   {
      public void run()
      {
         SegmentOverlay.this.calculateMediaAsync();
      }
   };
   
   private final Runnable mTrackCalculator = new Runnable()
   {
      public void run()
      {
         SegmentOverlay.this.calculateTrackAsync();
      }
   };
   /**
    * Constructor: create a new TrackingOverlay.
    * 
    * @param loggermap
    * @param segmentUri
    * @param color
    * @param avgSpeed
    * @param mMapView
    */
   public SegmentOverlay(LoggerMap loggermap, Uri segmentUri, int color, double avgSpeed, MapViewProxy mapView, Handler handler)
   {
      super();
      mHandler = handler;
      Log.d( TAG, "Got calculation handler with thread: "+mHandler.getLooper().getThread() );
      mLoggerMap = loggermap;
      mMapView = mapView;
      mTrackColoringMethod = color;
      mAvgSpeed = avgSpeed;
      mResolver = mLoggerMap.getContentResolver();
      mSegmentUri = segmentUri;
      mMediaUri = Uri.withAppendedPath( mSegmentUri, "media" );
      mWaypointsUri = Uri.withAppendedPath( mSegmentUri, "waypoints" );
      mCurrentColor = Color.rgb( 255, 0, 0 );
      mProjection = mapView.getProjection();
      
      mResolver.registerContentObserver( mWaypointsUri, false, mTrackSegmentsObserver );

      dotpaint = new Paint();
      radiusPaint = new Paint();
      radiusPaint.setColor( Color.YELLOW );
      radiusPaint.setAlpha( 100 );
      routePaint = new Paint();
      routePaint.setStyle( Paint.Style.STROKE );
      routePaint.setStrokeWidth( 6 );
      routePaint.setAntiAlias( true );
      routePaint.setPathEffect( new CornerPathEffect( 10 ) );
      defaultPaint = new Paint();
      mScreenPoint = new Point();
      mScreenPointBackup = new Point();
      mPrevDrawnScreenPoint = new Point();
      
      mDotPath = new Vector<DotVO>();
      mDotPathCalculation = new Vector<DotVO>();
      mPath = new Path();
      mPathCalculation = new Path();
      mMediaPath = new Vector<MediaVO>();
      mMediaPathCalculation = new Vector<MediaVO>();
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#finalize()
    */
   @Override
   protected void finalize() throws Throwable
   {
      try
      {
         closeResources();
      }
      finally
      {
         super.finalize();
      }

   }

   public void closeResources()
   {
      if( mWaypointsCursor != null )
      {
         mWaypointsCursor.close();
      }
      if( mMediaCursor != null )
      {
         mMediaCursor.close();
      }
      mResolver.unregisterContentObserver( this.mTrackSegmentsObserver );
   }
   
   @Override
   public void draw( Canvas canvas, MapView mapView, boolean shadow )
   {
      super.draw( canvas, mapView, shadow );
      if( !shadow )
      {
         mProjection.setProjection( mapView.getProjection() ); 
         draw( canvas );
      }
   }
      
   /** 
    * Private draw method called by both the draw from Google Overlay and the OSM Overlay  
    * 
    * @param canvas
    */
   private void draw( Canvas canvas )
   {
      mWidth = canvas.getWidth();
      mHeight = canvas.getHeight();
      
      calculateTrack(); // Screen changed, need to adjust the path to match the screen
      
      switch( mTrackColoringMethod )
      {
         case ( DRAW_CALCULATED ):
         case ( DRAW_MEASURED ):
         case ( DRAW_RED ):
         case ( DRAW_GREEN ):
            drawPath( canvas );
            break;
         case ( DRAW_DOTS ):
            drawDots( canvas );
            break;
      }
      drawStartStopCircles( canvas );
      calculateMedia();
      drawMedia( canvas );
   }
   
   public synchronized void calculateTrack()
   {
      Log.d( TAG, "Message calculateTrack     "+ mWaypointsUri );
      mHandler.removeCallbacks(mTrackCalculator);
      mHandler.post(mTrackCalculator);
   }
   

   /**
    * Either the Path or the Dots are calculated based on he current track coloring method
    *
    */
   private synchronized void calculateTrackAsync()
   {
      Log.d( TAG, "calculateTrackAsync()      "+ mWaypointsUri );
      GeoPoint oldTopLeft = mTopLeft;
      GeoPoint oldBottumRight = mBottumRight;
      mTopLeft = mProjection.fromPixels( 0, 0 );
      mBottumRight = mProjection.fromPixels( mWidth, mHeight );

      if( oldTopLeft == null || oldBottumRight == null 
              || mTopLeft.getLatitudeE6() / 100 != oldTopLeft.getLatitudeE6() / 100 
              || mTopLeft.getLongitudeE6() / 100 != oldTopLeft.getLongitudeE6() / 100
              || mBottumRight.getLatitudeE6() / 100 != oldBottumRight.getLatitudeE6() / 100 
              || mBottumRight.getLongitudeE6() / 100 != oldBottumRight.getLongitudeE6() / 100 )
      {
         calculateStepSize();
        
         mScreenPoint.x = -1;
         mScreenPoint.y = -1;
         this.mPrevDrawnScreenPoint.x = -1;
         this.mPrevDrawnScreenPoint.y = -1;
         
         switch( mTrackColoringMethod )
         {
            case ( DRAW_CALCULATED ):
            case ( DRAW_MEASURED ):
            case ( DRAW_RED ):
            case ( DRAW_GREEN ):
               calculatePath();
               synchronized (mPath) // Switch the fresh path with the old Path object
               {
                  Path oldPath = mPath;
                  mPath = mPathCalculation;
                  mPathCalculation = oldPath;
               }
               break;
            case ( DRAW_DOTS ):
               calculateDots();
               synchronized (mDotPath) // Switch the fresh path with the old Path object
               {
                  Vector<DotVO> oldDotPath = mDotPath;
                  mDotPath = mDotPathCalculation;
                  mDotPathCalculation = oldDotPath;
               }
               break;
         }
         mLoggerMap.onDateOverlayChanged();
      }
      Log.d( TAG, "Done calculateTrackAsync() "+ mWaypointsUri );
   }

   /**
    * Calculated the new contents of segment in the mDotPathCalculation
    */
   private void calculatePath()
   {
      mDotPathCalculation.clear();
      this.mPathCalculation.rewind();
      
      this.mShader = null;
   
      GeoPoint geoPoint;
      mCalculatedPoints = 0;
      this.mPrevLocation = null;
      int moves = 0;
   
      if( mWaypointsCursor == null )
      {
         mWaypointsCursor = this.mResolver.query( this.mWaypointsUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.SPEED, Waypoints.TIME }, null, null, null );
      }
      if( mRequeryFlag )
      {
         mWaypointsCursor.requery();
      }
      if( mProjection != null && mWaypointsCursor.moveToFirst() )
      {
         // Start point of the segments, possible a dot
         this.mStartPoint = extractGeoPoint();
         mPrevGeoPoint = mStartPoint;
         this.mLocation = new Location( this.getClass().getName() );
         this.mLocation.setLatitude( mWaypointsCursor.getDouble( 0 ) );
         this.mLocation.setLongitude( mWaypointsCursor.getDouble( 1 ) );
         this.mLocation.setTime( mWaypointsCursor.getLong( 3 ) );
   
         moveToGeoPoint( this.mStartPoint );
   
         do
         {
            geoPoint = extractGeoPoint();
            double speed = -1d;
            switch( mTrackColoringMethod )
            {
               case DRAW_GREEN:
               case DRAW_RED:
                  lineToGeoPoint( geoPoint, speed );
                  break;
               case DRAW_MEASURED:
                  lineToGeoPoint( geoPoint, mWaypointsCursor.getDouble( 2 ) );
                  break;
               case DRAW_CALCULATED:
                  this.mPrevLocation = this.mLocation;
                  this.mLocation = new Location( this.getClass().getName() );
                  this.mLocation.setLatitude( mWaypointsCursor.getDouble( 0 ) );
                  this.mLocation.setLongitude( mWaypointsCursor.getDouble( 1 ) );
                  this.mLocation.setTime( mWaypointsCursor.getLong( 3 ) );
                  speed = calculateSpeedBetweenLocations( this.mPrevLocation, this.mLocation );
                  lineToGeoPoint( geoPoint, speed );
                  break;
               default:
                  lineToGeoPoint( geoPoint, speed );
                  break;
            }
            moves++;
         }
         while( moveToNextWayPoint() );
   
         this.mEndPoint = extractGeoPoint(); // End point of the segments, possible a dot
   
      }
      //      Log.d( TAG, "transformSegmentToPath stop: points "+mCalculatedPoints+" from "+moves+" moves" );
   }

   /**
    * @param canvas
    * @param mapView
    * @param shadow
    * @see SegmentOverlay#draw(Canvas, MapView, boolean)
    */
   private void calculateDots()
   {
      mPathCalculation.reset();
      mDotPathCalculation.clear();
      
      mCalculatedPoints = 0;

      if( mWaypointsCursor == null )
      {
         mWaypointsCursor = this.mResolver.query( this.mWaypointsUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.SPEED, Waypoints.TIME }, null, null, null );
      }
      if( mRequeryFlag )
      {
         mWaypointsCursor.requery();
      }
      if( mProjection != null && mWaypointsCursor.moveToFirst() )
      {
         GeoPoint geoPoint;

         mStartPoint = extractGeoPoint();
         mPrevGeoPoint = mStartPoint;

         do
         {
            geoPoint = extractGeoPoint();
            setScreenPoint( geoPoint );

            float distance = (float) distanceInPoints( this.mPrevDrawnScreenPoint, this.mScreenPoint );
            if( distance > MINIMUM_PX_DISTANCE )
            {
               DotVO dotVO = new DotVO();
               dotVO.x = this.mScreenPoint.x;
               dotVO.y = this.mScreenPoint.y;
               dotVO.radius = mProjection.metersToEquatorPixels( mWaypointsCursor.getFloat( 2 ) );
               mDotPathCalculation.add( dotVO );

               this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
               this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
            }
         }
         while( moveToNextWayPoint() );

         this.mEndPoint = extractGeoPoint();
         DotVO pointVO = new DotVO();
         pointVO.x = this.mScreenPoint.x;
         pointVO.y = this.mScreenPoint.y;
         pointVO.radius = mProjection.metersToEquatorPixels( mWaypointsCursor.getFloat( 2 ) );
         mDotPathCalculation.add( pointVO );
      }
   }

   public void calculateMedia()
   {
      mHandler.removeCallbacks(mediaCalculator);
      mHandler.post(mediaCalculator);
   }
   
   public void calculateMediaAsync()
   {
      mMediaPathCalculation.clear();
      if (mMediaCursor == null)
      {
         mMediaCursor = this.mResolver.query(this.mMediaUri, new String[] { Media.WAYPOINT, Media.URI }, null, null, null);
      }
      else
      {
         mMediaCursor.requery();
      }
      if (mProjection != null && mMediaCursor.moveToFirst())
      {
         do
         {
            MediaVO mediaVO = new MediaVO();
            mediaVO.waypointId = mMediaCursor.getLong(0);
            mediaVO.uri = Uri.parse(mMediaCursor.getString(1));

            Uri mediaWaypoint = ContentUris.withAppendedId(mWaypointsUri, mediaVO.waypointId);
            Cursor waypointCursor = null;
            try
            {
               waypointCursor = this.mResolver.query(mediaWaypoint, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE }, null, null, null);
               if (waypointCursor != null && waypointCursor.moveToFirst())
               {
                  int microLatitude = (int) (waypointCursor.getDouble(0) * 1E6d);
                  int microLongitude = (int) (waypointCursor.getDouble(1) * 1E6d);
                  mediaVO.geopoint = new GeoPoint(microLatitude, microLongitude);
               }
            }
            finally
            {
               if (waypointCursor != null)
               {
                  waypointCursor.close();
               }
            }
            mMediaPathCalculation.add(mediaVO);
         }
         while (mMediaCursor.moveToNext());

         mLoggerMap.onDateOverlayChanged();
      }
      synchronized (mMediaPath) // Switch the fresh path with the old Path object
      {
         Vector<MediaVO> oldmMediaPath = mMediaPath;
         mMediaPath = mMediaPathCalculation;
         mMediaPathCalculation = oldmMediaPath;
      }
      //      Log.d( TAG, "Calculated a media path for "+this.mMediaUri+" of size "+mMediaPath.size() );
   }

   /**
    * @param canvas
    * 
    * @see SegmentOverlay#draw(Canvas, MapView, boolean)
    */
   private void drawPath( Canvas canvas )
   {
      switch( mTrackColoringMethod )
      {
         case ( DRAW_CALCULATED ):
         case ( DRAW_MEASURED ):
            routePaint.setShader( this.mShader );
            break;
         case ( DRAW_RED ):
            routePaint.setShader( null );
            routePaint.setColor( Color.RED );
            break;
         case ( DRAW_GREEN ):
            routePaint.setShader( null );
            routePaint.setColor( Color.GREEN );
            break;
         default:
            routePaint.setShader( null );
            routePaint.setColor( Color.YELLOW );
            break;
      }
      synchronized ( mPath )
      {
         canvas.drawPath( mPath, routePaint );         
      }
   }

   private void drawDots( Canvas canvas )
   {
      synchronized ( mDotPath )
      {         
         for( DotVO dotVO : mDotPath )
         {
            Bitmap bitmap = BitmapFactory.decodeResource( this.mLoggerMap.getResources(), R.drawable.stip2 );
            canvas.drawBitmap( bitmap, dotVO.x - 8, dotVO.y - 8, dotpaint );
            if( dotVO.radius > 8f )
            {
               canvas.drawCircle( dotVO.x, dotVO.y, dotVO.radius, radiusPaint );
            }
         }
      }
   }

   private void drawMedia( Canvas canvas )
   {
      synchronized( mMediaPath )
      {
         GeoPoint lastPoint = null;
         int wiggle = 0;
         for( MediaVO mediaVO : mMediaPath )
         {
            if( isOnScreen( mediaVO.geopoint ) )
            {
               setScreenPoint( mediaVO.geopoint );
               int drawable = getResourceForMedia( mediaVO.uri );
               if( mediaVO.geopoint.equals( lastPoint ) )
               {
                  wiggle += 4;
               }
               else
               {
                  wiggle = 0;
               }
               Bitmap bitmap = BitmapFactory.decodeResource( this.mLoggerMap.getResources(), drawable );
               mediaVO.w = bitmap.getWidth();
               mediaVO.h = bitmap.getHeight();
               int left = ( mediaVO.w * 3 ) / 7 + wiggle;
               int up = ( mediaVO.h * 6 ) / 7 - wiggle;
               mediaVO.x = mScreenPoint.x - left;
               mediaVO.y = mScreenPoint.y - up;
               canvas.drawBitmap( bitmap, mediaVO.x, mediaVO.y, defaultPaint );
               lastPoint = mediaVO.geopoint;
            }
         }
      }
   }

   private static int getResourceForMedia( Uri uri )
   {
      int drawable = 0;
      if( uri.getScheme().equals( "file" ) )
      {
         if( uri.getLastPathSegment().endsWith( "3gp" ) )
         {
            drawable = R.drawable.media_film;
         }
         else if( uri.getLastPathSegment().endsWith( "jpg" ) )
         {
            drawable = R.drawable.media_camera;
         }
         else if( uri.getLastPathSegment().endsWith( "txt" ) )
         {
            drawable = R.drawable.media_notepad;
         }
      }
      else if( uri.getScheme().equals( "content" ) )
      {
         if( uri.getAuthority().equals( GPStracking.AUTHORITY + ".string" ) )
         {
            drawable = R.drawable.media_mark;
         }
         else if( uri.getAuthority().equals( "media" ) )
         {
            drawable = R.drawable.media_speech;
         }
      }
      return drawable;
   }

   private void drawStartStopCircles( Canvas canvas )
   {
      Bitmap bitmap;
      if( ( this.mPlacement == FIRST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT ) && this.mStartPoint != null )
      {
         setScreenPoint( this.mStartPoint );
         bitmap = BitmapFactory.decodeResource( this.mLoggerMap.getResources(), R.drawable.stip2 );
         canvas.drawBitmap( bitmap, mScreenPoint.x - 8, mScreenPoint.y - 8, defaultPaint );
      }
      if( ( this.mPlacement == LAST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT ) && this.mEndPoint != null )
      {
         setScreenPoint( this.mEndPoint );
         bitmap = BitmapFactory.decodeResource( this.mLoggerMap.getResources(), R.drawable.stip );
         canvas.drawBitmap( bitmap, mScreenPoint.x - 5, mScreenPoint.y - 5, defaultPaint );
      }
   }

   /**
    * Set the mPlace to the specified value.
    * 
    * @see SegmentOverlay.FIRST
    * @see SegmentOverlay.MIDDLE
    * @see SegmentOverlay.LAST
    * @param place The placement of this segment in the line.
    */
   public void addPlacement( int place )
   {
      this.mPlacement += place;
   }

   public boolean isLast()
   {
      return ( mPlacement >= LAST_SEGMENT );
   }

   public long getSegmentId()
   {
      return Long.parseLong( mSegmentUri.getLastPathSegment() );
   }

   /**
    * Set the beginnging to the next contour of the line to the give GeoPoint
    * 
    * @param geoPoint
    */
   private void moveToGeoPoint( GeoPoint geoPoint )
   {
      setScreenPoint( geoPoint );

      if( this.mPathCalculation != null )
      {
         this.mPathCalculation.moveTo( this.mScreenPoint.x, this.mScreenPoint.y );
         this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
         this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
      }
   }

   private void lineToGeoPoint( GeoPoint geoPoint, double speed )
   {
      setScreenPoint( geoPoint );

      //      Log.d( TAG, "Draw line to " + geoPoint+" with speed "+speed );

      if( speed > 0 )
      {
         int greenfactor = (int) Math.min( ( 127 * speed ) / mAvgSpeed, 255 );
         int redfactor = 255 - greenfactor;
         mCurrentColor = Color.rgb( redfactor, greenfactor, 0 );
      }
      else
      {
         int greenfactor = Color.green( mCurrentColor );
         int redfactor = Color.red( mCurrentColor );
         mCurrentColor = Color.argb( 128, redfactor, greenfactor, 0 );
      }

      float distance = (float) distanceInPoints( this.mPrevDrawnScreenPoint, this.mScreenPoint );
      if( distance > MINIMUM_PX_DISTANCE )
      {
         //         Log.d( TAG, "Circle between " + mPrevDrawnScreenPoint+" and "+mScreenPoint );
         int x_circle = ( this.mPrevDrawnScreenPoint.x + this.mScreenPoint.x ) / 2;
         int y_circle = ( this.mPrevDrawnScreenPoint.y + this.mScreenPoint.y ) / 2;
         float radius_factor = 0.4f;
         Shader lastShader = new RadialGradient( x_circle, y_circle, distance, new int[] { mCurrentColor, mCurrentColor, Color.TRANSPARENT }, new float[] { 0, radius_factor, 0.6f }, TileMode.CLAMP );
         //            Paint debug = new Paint();
         //            debug.setStyle( Paint.Style.FILL_AND_STROKE );
         //            this.mDebugCanvas.drawCircle(
         //                  x_circle,
         //                  y_circle, 
         //                  distance*radius_factor/2, 
         //                  debug );
         //            this.mDebugCanvas.drawCircle(
         //                  x_circle,
         //                  y_circle, 
         //                  distance*radius_factor, 
         //                  debug );
         //            if( distance > 100 )
         //            {
         //               Log.d( TAG, "Created shader for speed " + speed + " on " + x_circle + "," + y_circle );
         //            }
         if( this.mShader != null )
         {
            this.mShader = new ComposeShader( this.mShader, lastShader, Mode.DST_OVER );
         }
         else
         {
            this.mShader = lastShader;
         }
         this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
         this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
      }

      this.mPathCalculation.lineTo( this.mScreenPoint.x, this.mScreenPoint.y );
   }

   /**
    * Use to update location/point state when calculating the line
    * 
    * @param geoPoint
    */
   private void setScreenPoint( GeoPoint geoPoint )
   {
      mScreenPointBackup.x = this.mScreenPoint.x;
      mScreenPointBackup.y = this.mScreenPoint.x;
      
      this.mProjection.toPixels( geoPoint, this.mScreenPoint );
      mCalculatedPoints++;
   }

   /**
    * Move to a next waypoint, for on screen this are the points with mStepSize % position == 0 to avoid jittering in the rendering or the points on the either side of the screen edge.
    * 
    * @return if a next waypoint is pointed to with the mWaypointsCursor
    */
   private boolean moveToNextWayPoint()
   {
      boolean cursorReady = true;
      boolean onscreen = isOnScreen( extractGeoPoint() );
      if( mWaypointsCursor.isLast() ) // End of the line, cant move onward
      {
         cursorReady = false;
      }
      else if( onscreen ) // Are on screen
      {
         cursorReady = moveOnScreenWaypoint();
      }
      else
      // Are off screen => accelerate
      {
         int acceleratedStepsize = mStepSize * ( mWaypointCount / 1000 + 6 );
         cursorReady = moveOffscreenWaypoint( acceleratedStepsize );
      }
      return cursorReady;
   }

   /**
    * Move the cursor to the next waypoint modulo of the step size or less if the screen edge is reached
    * 
    * @param trackCursor
    * @return
    */
   private boolean moveOnScreenWaypoint()
   {
      int nextPosition = mStepSize * ( mWaypointsCursor.getPosition() / mStepSize ) + mStepSize;
      if( mWaypointsCursor.moveToPosition( nextPosition ) )
      {
         if( isOnScreen( extractGeoPoint() ) ) // Remained on screen
         {
            return true; // Cursor is pointing to somewhere
         }
         else
         {
            mWaypointsCursor.move( -1 * mStepSize ); // Step back
            boolean nowOnScreen = true; // onto the screen
            while( nowOnScreen ) // while on the screen 
            {
               mWaypointsCursor.moveToNext(); // inch forward to the edge
               nowOnScreen = isOnScreen( extractGeoPoint() );
            }
            return true; // with a cursor point to somewhere
         }
      }
      else
      {
         return mWaypointsCursor.moveToLast(); // No full step can be taken, move to last
      }
   }

   /**
    * Previous path GeoPoint was off screen and the next one will be to or the first on screen when the path reaches the projection.
    * 
    * @return
    */
   private boolean moveOffscreenWaypoint( int flexStepsize )
   {
      while( mWaypointsCursor.move( flexStepsize ) )
      {
         if( mWaypointsCursor.isLast() )
         {
            return true;
         }
         GeoPoint evalPoint = extractGeoPoint();
         //         Log.d( TAG, String.format( "Evaluate point number %d ", mWaypointsCursor.getPosition() ) );
         if( possibleScreenPass( mPrevGeoPoint, evalPoint ) )
         {
            mPrevGeoPoint = evalPoint;
            if( flexStepsize == 1 ) // Just stumbled over a border
            {
               return true;
            }
            else
            {
               mWaypointsCursor.move( -1 * flexStepsize ); // Take 1 step back
               return moveOffscreenWaypoint( flexStepsize / 2 ); // Continue at halve accelerated speed
            }
         }
         else
         {
            moveToGeoPoint( evalPoint );
            mPrevGeoPoint = evalPoint;
         }

      }
      return mWaypointsCursor.moveToLast();
   }

   /**
    * If a segment contains more then 500 waypoints and is zoomed out more then twice then some waypoints will not be used to render the line, this speeding things along.
    */
   private void calculateStepSize()
   {
      Log.d( TAG, "calculateStepSize()        "+ mWaypointsUri );
      Cursor waypointsCursor = null;
      if( mRequeryFlag || mStepSize < 1 || mWaypointCount == 0 )
      {
         try
         {
            waypointsCursor = this.mResolver.query( this.mWaypointsUri, new String[] { Waypoints._ID }, null, null, null );
            mWaypointCount = waypointsCursor.getCount();
         }
         finally
         {
            waypointsCursor.close();
         }
      }
      if( mWaypointCount < 250 )
      {
         mStepSize = 1;
      }
      else
      {
         int zoomLevel = mMapView.getZoomLevel();
         int maxZoomLevel = mMapView.getMaxZoomLevel();
         if( zoomLevel >= maxZoomLevel - 2 )
         {
            mStepSize = 1;
         }
         else
         {
            mStepSize = maxZoomLevel - zoomLevel;
         }
      }
      Log.d( TAG, "Done calculateStepSize()   "+ mWaypointsUri );
   }

   /**
    * Is a given GeoPoint in the current projection of the map.
    * 
    * @param eval
    * @return
    */
   protected boolean isOnScreen( GeoPoint eval )
   {
      boolean onscreen = false;
      if( eval != null && mTopLeft != null && mBottumRight != null )
      {
         onscreen = mTopLeft.getLatitudeE6() > eval.getLatitudeE6();
         onscreen = onscreen && mBottumRight.getLatitudeE6() < eval.getLatitudeE6();
         onscreen = onscreen && mTopLeft.getLongitudeE6() < eval.getLongitudeE6();
         onscreen = onscreen && mBottumRight.getLongitudeE6() > eval.getLongitudeE6();
      }
      return onscreen;
   }

   /**
    * Calculates in which segment opposited to the projecting a geo point resides
    * 
    * @param p1
    * @return
    */
   private int toSegment( GeoPoint p1 )
   {
      //      Log.d( TAG, String.format( "Comparing %s to points TL %s and BR %s", p1, mTopLeft, mBottumRight )); 
      int nr;
      if( p1.getLongitudeE6() < mTopLeft.getLongitudeE6() ) // left
      {
         nr = 1;
      }
      else if( p1.getLongitudeE6() > mBottumRight.getLongitudeE6() ) // right
      {
         nr = 3;
      }
      else
      // middle
      {
         nr = 2;
      }

      if( p1.getLatitudeE6() > mTopLeft.getLatitudeE6() ) // top
      {
         nr = nr + 0;
      }
      else if( p1.getLatitudeE6() < mBottumRight.getLatitudeE6() ) // bottom
      {
         nr = nr + 6;
      }
      else
      // middle
      {
         nr = nr + 3;
      }
      return nr;
   }

   private boolean possibleScreenPass( GeoPoint fromGeo, GeoPoint toGeo )
   {
      boolean safe = true;
      if( fromGeo != null && toGeo != null )
      {
         int from = toSegment( fromGeo );
         int to = toSegment( toGeo );

         switch( from )
         {
            case 1:
               safe = to == 1 || to == 2 || to == 3 || to == 4 || to == 7;
               break;
            case 2:
               safe = to == 1 || to == 2 || to == 3;
               break;
            case 3:
               safe = to == 1 || to == 2 || to == 3 || to == 6 || to == 9;
               break;
            case 4:
               safe = to == 1 || to == 4 || to == 7;
               break;
            case 5:
               safe = false;
               break;
            case 6:
               safe = to == 3 || to == 6 || to == 9;
               break;
            case 7:
               safe = to == 1 || to == 4 || to == 7 || to == 8 || to == 9;
               break;
            case 8:
               safe = to == 7 || to == 8 || to == 9;
               break;
            case 9:
               safe = to == 3 || to == 6 || to == 7 || to == 8 || to == 9;
               break;
            default:
               safe = false;
               break;
         }
         //            Log.d( TAG, String.format( "From %d to %d is safe: %s", from, to, safe ) );
      }
      return !safe;
   }

   public void setTrackColoringMethod( int coloring, double avgspeed )
   {
      this.mTrackColoringMethod = coloring;
      this.mAvgSpeed = avgspeed;
   }

   /**
    * For the current waypoint cursor returns the GeoPoint
    * 
    * @return
    */
   private GeoPoint extractGeoPoint()
   {
      int microLatitude = (int) ( mWaypointsCursor.getDouble( 0 ) * 1E6d );
      int microLongitude = (int) ( mWaypointsCursor.getDouble( 1 ) * 1E6d );
      return new GeoPoint( microLatitude, microLongitude );
   }

   /**
    * @param startLocation
    * @param endLocation
    * @return speed in m/s between 2 locations
    */
   private static double calculateSpeedBetweenLocations( Location startLocation, Location endLocation )
   {
      double speed = -1d;
      if( startLocation != null && endLocation != null )
      {
         float distance = startLocation.distanceTo( endLocation );
         float seconds = ( endLocation.getTime() - startLocation.getTime() ) / 1000f;
         speed = distance / seconds;
         //         Log.d( TAG, "Found a speed of "+speed+ " over a distance of "+ distance+" in a time of "+seconds);
      }
      if( speed > 0 )
      {
         return speed;
      }
      else
      {
         return -1d;
      }
   }

   public static int extendPoint( int x1, int x2 )
   {
      int diff = x2 - x1;
      int next = x2 + diff;
      return next;
   }

   private static double distanceInPoints( Point start, Point end )
   {
      int x = Math.abs( end.x - start.x );
      int y = Math.abs( end.y - start.y );
      return (double) Math.sqrt( x * x + y * y );
   }

   private boolean handleMediaTapList( List<Uri> tappedUri )
   {
      if( tappedUri.size() == 1 )
      {
         return handleMedia( mLoggerMap, tappedUri.get( 0 ) );
      }
      else
      {
         BaseAdapter adapter = new MediaAdapter( mLoggerMap, tappedUri );
         mLoggerMap.showDialog( adapter );
         return true;
      }
   }

   public static boolean handleMedia( Context ctx, Uri mediaUri )
   {
      if( mediaUri.getScheme().equals( "file" ) )
      {
         Intent intent = new Intent( android.content.Intent.ACTION_VIEW );
         if( mediaUri.getLastPathSegment().endsWith( "3gp" ) )
         {
            intent.setDataAndType( mediaUri, "video/3gpp" );
            ctx.startActivity( intent );
            return true;
         }
         else if( mediaUri.getLastPathSegment().endsWith( "jpg" ) )
         {
            //<scheme>://<authority><absolute path>
            Uri.Builder builder = new Uri.Builder();
            mediaUri = builder.scheme( mediaUri.getScheme() ).authority( mediaUri.getAuthority() ).path( mediaUri.getPath() ).build();
            intent.setDataAndType( mediaUri, "image/jpeg" );
            ctx.startActivity( intent );
            return true;
         }
         else if( mediaUri.getLastPathSegment().endsWith( "txt" ) )
         {
            intent.setDataAndType( mediaUri, "text/plain" );
            ctx.startActivity( intent );
            return true;
         }
      }
      else if( mediaUri.getScheme().equals( "content" ) )
      {
         if( mediaUri.getAuthority().equals( GPStracking.AUTHORITY + ".string" ) )
         {
            String text = mediaUri.getLastPathSegment();
            Toast toast = Toast.makeText( ctx, text, Toast.LENGTH_LONG );
            toast.show();
            return true;
         }
         else if( mediaUri.getAuthority().equals( "media" ) )
         {
            ctx.startActivity( new Intent( Intent.ACTION_VIEW, mediaUri ) );
            return true;
         }
      }
      return false;
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.Overlay#onTap(com.google.android.maps.GeoPoint, com.google.android.maps.MapView)
    */
   @Override
   public boolean onTap( GeoPoint tappedGeoPoint, MapView mapView )
   {
      return commonOnTap(tappedGeoPoint) ;
   }

   private boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      List<Uri> tappedUri = new Vector<Uri>();

      Point tappedPoint = new Point();
      for( MediaVO media : mMediaPath )
      {
         mProjection.toPixels( tappedGeoPoint, tappedPoint );

         if( media.x < tappedPoint.x && tappedPoint.x < media.x + media.w && media.y < tappedPoint.y && tappedPoint.y < media.y + media.h )
         {
            //Log.d( TAG, String.format( "Tapped at a (x,y) (%d,%d)", tappedPoint.x, tappedPoint.y ) );
            tappedUri.add( media.uri );
         }
      }
      if( tappedUri.size() > 0 )
      {
         return handleMediaTapList( tappedUri );
      }
      else
      {
         return false;
      }
   }

   private static class MediaVO
   {
      public Uri uri;
      public GeoPoint geopoint;
      public int x;
      public int y;
      public int w;
      public int h;
      public long waypointId;
   }

   private static class DotVO
   {
      public int x;
      public int y;
      public float radius;
   }

   private static class MediaAdapter extends BaseAdapter
   {

      private Context mContext;
      private List<Uri> mTappedUri;
      private int itemBackground;

      public MediaAdapter(Context ctx, List<Uri> tappedUri)
      {
         mContext = ctx;
         mTappedUri = tappedUri;
         TypedArray a = mContext.obtainStyledAttributes( R.styleable.gallery );
         itemBackground = a.getResourceId( R.styleable.gallery_android_galleryItemBackground, 0 );
         a.recycle();

      }

      public int getCount()
      {
         return mTappedUri.size();
      }

      public Object getItem( int position )
      {
         return mTappedUri.get( position );
      }

      public long getItemId( int position )
      {
         return position;
      }

      public View getView( int position, View convertView, ViewGroup parent )
      {
         ImageView imageView = new ImageView( mContext );
         int uriResource = getResourceForMedia( mTappedUri.get( position ) );
         imageView.setImageResource( uriResource );
         imageView.setScaleType( ImageView.ScaleType.FIT_XY );
         imageView.setBackgroundResource( itemBackground );
         return imageView;

      }
   }

   public Overlay getGoogleOverlay()
   {
      return this;
   }

   public org.osmdroid.views.overlay.Overlay getOSMOverlay()
   {
      return osmOverlay;
   }
   
   org.osmdroid.views.overlay.Overlay osmOverlay = new org.osmdroid.views.overlay.Overlay(mLoggerMap) {

      @Override
      protected void onDraw( Canvas canvas, org.osmdroid.views.MapView view )
      {
         SegmentOverlay.this.draw( canvas );
         mProjection.setProjection(view.getProjection());
      }

      @Override
      protected void onDrawFinished( Canvas arg0, org.osmdroid.views.MapView arg1 )
      {
         // noop
      }
      
      public boolean onSingleTapUp(MotionEvent e, org.osmdroid.views.MapView openStreetMapView) 
      {
         int x = (int) e.getX();
         int y = (int) e.getY();
         GeoPoint tappedGeoPoint = mProjection.fromPixels(x, y);
         return SegmentOverlay.this.commonOnTap(tappedGeoPoint );
      }

      
   };
}
