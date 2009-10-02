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
   public static final int MIDDLE = 0;
   public static final int FIRST = 1;
   public static final int LAST = 2;
   public static final String TAG = TrackingOverlay.class.getName();

   private static final int DRAW_NONE = 0;
   private static final int DRAW_MEASURED = 1;
   private static final int DRAW_CALCULATED = 2;
   public static final int draw_speed = DRAW_CALCULATED;

   private ContentResolver mResolver;
   private Point mStartPoint;
   private Point mEndPoint;
   
   private Point mRecylcePoint;
   private Location mLocation;

   private Point mPrevPoint;   
   private int mPrevPosition;
   private int mPrevXcol;
   private int mPrevYcol;
   private Location mPrevLocation;
   
   
   private Path mPath;
   private int mPlacement = TrackingOverlay.MIDDLE;
   private Projection mProjection;
   private Uri mTrackUri;
   private Context mCtx;
   private int mListPosition;
   private int mStepSize;
   private int mCalculatedPoints;
   private Canvas mCanvas;
   private Shader mShader;
   private double mAvgSpeed;

   TrackingOverlay(Context cxt, ContentResolver resolver, Uri trackUri)
   {
      super();
      this.mCtx = cxt;
      this.mPath = new Path();
      this.mResolver = resolver;
      this.mTrackUri = trackUri;
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.google.android.maps.Overlay#draw(android.graphics.Canvas, com.google.android.maps.MapView, boolean)
    */
   @Override
   public void draw( Canvas canvas, MapView mapView, boolean shadow )
   {
      // Holder of the onscreen Points are reset
      this.mStartPoint = new Point();
      this.mEndPoint = new Point();
      this.mRecylcePoint = new Point();
      this.mPrevPoint = new Point();
      this.mPath.rewind();
      this.mListPosition = 0;
      this.mStepSize = 10;
      mPrevXcol = 2;
      mPrevYcol = 2;
      this.mCanvas = canvas;
      this.mShader = null;

      // The current state with all the Points must be recalculated
      // because the projecting of the map me be different then
      // the last call (the map moved, redraw the route to move along)
      this.mProjection = mapView.getProjection();
      transformAllWaypointsToPath();
      this.mProjection = null;

      // Just the rendering bits left to do
      Paint routePaint = new Paint();
      routePaint.setPathEffect( new CornerPathEffect( 10 ) );
      switch( draw_speed ) 
      {
         case( DRAW_CALCULATED ):
         case( DRAW_MEASURED ):
            routePaint.setShader( this.mShader );
            break;
         case( DRAW_NONE ):
            routePaint.setColor( Color.RED );
            break;
      }
      routePaint.setStyle( Paint.Style.STROKE );
      routePaint.setStrokeWidth( 5 );
      routePaint.setAntiAlias( true );

      this.mCanvas.drawPath( this.mPath, routePaint );

      Bitmap bitmap;
      if( this.mPlacement == FIRST || this.mPlacement == FIRST + LAST )
      {
         bitmap = BitmapFactory.decodeResource( this.mCtx.getResources(), R.drawable.stip2 );
         this.mCanvas.drawBitmap( bitmap, this.mStartPoint.x - 8, this.mStartPoint.y - 8, new Paint() );
      }
      if( this.mPlacement == LAST || this.mPlacement == FIRST + LAST )
      {
         bitmap = BitmapFactory.decodeResource( this.mCtx.getResources(), R.drawable.stip );
         this.mCanvas.drawBitmap( bitmap, this.mEndPoint.x - 5, this.mEndPoint.y - 5, new Paint() );

      }
      Log.d( TAG, "Transformerd number of points: " + mCalculatedPoints );

      super.draw( this.mCanvas, mapView, shadow );
      this.mCanvas = null;
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

   /**
    * Convert the cursor from the GPSTracking provider into Points on the Path
    * 
    * @see Cursor Cursor used as input
    * @see Point Point used as transformation target
    * @see Path Path used as drawable line
    */
   private void transformAllWaypointsToPath()
   {
      Cursor trackCursor = null;
      mCalculatedPoints = 0;
      try
      {
         switch ( draw_speed )
         {
            case ( DRAW_CALCULATED ):
            case ( DRAW_MEASURED ):
               try
               {
                  trackCursor = this.mResolver.query( this.mTrackUri, new String[] { "avg(" + Waypoints.SPEED + ")" }, null, null, null );
                  if( trackCursor.moveToLast() )
                  {
                     mAvgSpeed = trackCursor.getDouble( 0 );
                     if( mAvgSpeed == 0 )
                     {
                        mAvgSpeed = 33.33d; 
                     }
                     //Log.d( TAG, "Avgspeed = " + mAvgSpeed );
                  }
               }
               finally
               {
                  if( trackCursor != null )
                  {
                     trackCursor.close();
                  }
               }
               trackCursor = this.mResolver.query( this.mTrackUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.SPEED, Waypoints.TIME }, null, null, null );
               break;
            case ( DRAW_NONE ):
               trackCursor = this.mResolver.query( this.mTrackUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE }, null, null, null );
               break;
         }
         if( trackCursor.moveToFirst() )
         {
            transformSingleWaypointToCurrentPoint( trackCursor.getDouble( 0 ), trackCursor.getDouble( 1 ) );
            this.mStartPoint.set( this.mRecylcePoint.x, this.mRecylcePoint.y );
            this.mPath.moveTo( this.mRecylcePoint.x, this.mRecylcePoint.y );

            while (moveToNextWayPoint( trackCursor ))
            {
               transformSingleWaypointToCurrentPoint( trackCursor.getDouble( 0 ), trackCursor.getDouble( 1 ) );
               switch (draw_speed)
               {
                  case DRAW_NONE:
                     drawPointToPath( -1d );
                     break;
                  case DRAW_MEASURED:
                     drawPointToPath( trackCursor.getDouble( 2 ) );
                     break;
                  case DRAW_CALCULATED:
                     double speed = -1d;
                     this.mLocation =  new Location( this.getClass().getName() );
                     this.mLocation.setLatitude( trackCursor.getDouble( 0 ) );
                     this.mLocation.setLongitude( trackCursor.getDouble( 1 ) );
                     this.mLocation.setTime( trackCursor.getLong( 3 ) );
                     if( this.mPrevLocation != null )
                     {                        
                        float distance = this.mPrevLocation.distanceTo( this.mLocation );
                        float seconds = ((this.mLocation.getTime()-this.mPrevLocation.getTime()) / 1000f);
                        speed = distance / seconds;
                        //Log.d( TAG, "Calculated speed:"+speed+" for seconds: "+seconds+" over distance "+distance );
                     }                     
                     drawPointToPath( speed );
                     break;
                  default:
                     drawPointToPath( -1d );
                     break;
               }
            }

            this.mEndPoint.set( this.mRecylcePoint.x, this.mRecylcePoint.y );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
   }

   /**
    * The the waypoint in the cursor is converted into the the point based on the projection
    */
   private void transformSingleWaypointToCurrentPoint( double lat, double lon )
   {
      int microLatitude = (int) ( lat * 1E6d );
      int microLongitude = (int) ( lon * 1E6d );
      this.mProjection.toPixels( new GeoPoint( microLatitude, microLongitude ), this.mRecylcePoint );

      mCalculatedPoints++;
   }

   private void drawPointToPath( double speed )
   {
      if( correctCurrentPoint() )
      {
         return;
      }

      this.mPath.lineTo( this.mRecylcePoint.x, this.mRecylcePoint.y );

      if( speed > 0 && ( inFrame( this.mRecylcePoint.x, this.mRecylcePoint.y ) || inFrame( this.mPrevPoint.x, this.mPrevPoint.y ) ) )
      {
         int greenfactor = (int) Math.min( ( 127 * speed ) / mAvgSpeed, 255 );
         int redfactor = 255 - greenfactor;
         int currentColor = Color.rgb( redfactor, greenfactor, 0 );
         Shader s = new LinearGradient( this.mPrevPoint.x, this.mPrevPoint.y, extendPoint( this.mPrevPoint.x, this.mRecylcePoint.x ), extendPoint( this.mPrevPoint.y, this.mRecylcePoint.y ),
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

      // Determine how much line this new point adds
      int diff = Math.abs( this.mRecylcePoint.x - this.mPrevPoint.x ) + Math.abs( this.mRecylcePoint.y - this.mPrevPoint.y );
      adjustStepSize( diff );
      
      this.mPrevLocation = this.mLocation;
      this.mPrevPoint.x = this.mRecylcePoint.x;
      this.mPrevPoint.y = this.mRecylcePoint.y;
   }

   private float extendPoint( int x1, int x2 )
   {
      int diff = x2 - x1;
      int next = x2 + diff;
      return next;
   }

   private boolean inFrame( int x, int y )
   {
      return x > 0 && y > 0 && x < mCanvas.getWidth() && y < mCanvas.getHeight();
   }

   private boolean moveToNextWayPoint( Cursor trackCursor )
   {
      if( mListPosition > trackCursor.getCount() )
      {
         return false;
      }
      mPrevPosition = mListPosition;
      mListPosition += mStepSize;
      if( mListPosition > trackCursor.getCount() )
      {
         return trackCursor.moveToLast();
      }
      else
      {
         return trackCursor.moveToPosition( mListPosition );
      }

   }

   private boolean correctCurrentPoint()
   {
      int currentXcol, currentYcol;
      // Determine whether this new point lies compared to the viewing frame
      if( this.mRecylcePoint.x > this.mCanvas.getWidth() )
      {
         currentXcol = 3;
      }
      else if( this.mRecylcePoint.x >= 0 )
      {
         currentXcol = 2;
      }
      else if( this.mRecylcePoint.x < 0 )
      {
         currentXcol = 1;
      }
      else
      {
         currentXcol = -1;
      }

      if( this.mRecylcePoint.y > this.mCanvas.getHeight() )
      {
         currentYcol = 3;
      }
      else if( this.mRecylcePoint.y >= 0 )
      {
         currentYcol = 2;
      }
      else if( this.mRecylcePoint.y < 0 )
      {
         currentYcol = 1;
      }
      else
      {
         currentYcol = -1;
      }

      if( mPrevXcol != 2 || mPrevYcol != 2 )  // Outside the frame
      {

//         Log.d( TAG, "Outside: Picking up the step size from "+mStepSize+" to "+(mStepSize+5) );
         mStepSize += 5;
      }

      if( ( mPrevXcol != currentXcol || mPrevYcol != currentYcol ) )
      {
         boolean crossed = false; 
         switch( mPrevXcol ) 
         {
            case(1):
               if( currentXcol>1 ) 
               {
                  switch( mPrevYcol )
                  {
                     case(1):
                        crossed = currentYcol > 1;
                        break;
                     case(2):
                        crossed = true;
                        break;
                     case(3):
                        crossed = currentYcol < 3;
                        break;
                  }
               };
               break;
            case(2):
               switch( mPrevYcol )
               {
                  case(1):
                     crossed = currentYcol > 1;
                     break;
                  case(2):
                     crossed =  true;
                     break;
                  case(3):
                     crossed = currentYcol < 3;
                     break;
               }
               break;
            case(3):
               if( currentXcol<3 )
               {
                  switch( mPrevYcol )
                  {
                     case(1):
                        crossed = currentYcol > 1;
                        break;
                     case(2):
                        crossed = true;
                        break;
                     case(3):
                        crossed = currentYcol < 3;
                        break;
                  }
               };
               break;
         }
         
         //Log.d( TAG, "Switching kwadrant from ("+mPrevXcol+","+mPrevYcol+") to ("+currentXcol+","+currentYcol+") + and crossed: "+crossed);
         
         if( crossed && mListPosition > ( mPrevPosition + 1 ) )
         {
            
            mListPosition = mPrevPosition;
//            int smallerstep = Math.max( 1,  mStepSize / 2 );
//            Log.d( TAG, "Crossing: Taking back the step size from "+mStepSize+" to "+1 );
            mStepSize = 1;
            return true;
         }
      }
      //Log.d( TAG, "Point "+ mListPosition +"  remained in quadrant ("+currentXcol+","+currentYcol+")" );
      mPrevXcol = currentXcol;
      mPrevYcol = currentYcol;
      return false;
   }

   private void adjustStepSize( int diff )
   {
      if( diff > 20 && mStepSize > 1 )
      {

//         Log.d( TAG, "Big steps: Taking back the step size from "+mStepSize+" to "+(mStepSize-1) );
         mStepSize--;
      }
      else if( diff < 10 )
      {

//         Log.d( TAG, "Small steps: Picking up the step size from "+mStepSize+" to "+(mStepSize*2) );
         mStepSize *= 2;
      }
   }
}
