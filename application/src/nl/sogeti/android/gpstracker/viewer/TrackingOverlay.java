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
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * Creates an overlay that can draw a single segment of connected 
 * waypoints
 *
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class TrackingOverlay extends Overlay
{
   public static final int MIDDLE = 0 ;
   public static final int FIRST = 1 ;
   public static final int LAST = 2 ;
   public static final String TAG = TrackingOverlay.class.getName();

   private ContentResolver mResolver;
   private Point mStartPoint ;
   private Point mEndPoint ;
   private Point mRecylcePoint ;
   private Point mPrevPoint ;
   private Path mPath ;
   private int mPlace = TrackingOverlay.MIDDLE ;
   private Projection mProjection;
   private Uri mTrackUri;
   private Context mCtx;
   
   private int skip ;
   private int mCalculatedPoints;
   private Canvas mCanvas; 

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
      this.mCanvas = canvas;

      // The current state with all the Points must be recalculated 
      // because the projecting of the map me be different then
      // the last call (the map moved, redraw the route to move along)
      this.mProjection = mapView.getProjection();
      transformAllWaypointsToPath();
      this.mProjection = null;

      // Just the rendering bits left to do
      Paint routePaint =  new Paint() ;
      routePaint.setPathEffect( new CornerPathEffect(10));
      routePaint.setColor( Color.RED );
      routePaint.setStyle(Paint.Style.STROKE);
      routePaint.setStrokeWidth( 5 );
      routePaint.setAntiAlias( true );
      
      this.mCanvas.drawPath(this.mPath, routePaint );

      Bitmap bitmap;
      if(this.mPlace==FIRST || this.mPlace==FIRST+LAST )
      {         
         bitmap = BitmapFactory.decodeResource( this.mCtx.getResources(), R.drawable.stip2 );
         this.mCanvas.drawBitmap( bitmap, this.mStartPoint.x-8, this.mStartPoint.y-8, new Paint() );
      }
      if(this.mPlace==LAST || this.mPlace==FIRST+LAST)
      {
         bitmap = BitmapFactory.decodeResource( this.mCtx.getResources(), R.drawable.stip );
         this.mCanvas.drawBitmap( bitmap, this.mEndPoint.x-5,  this.mEndPoint.y-5, new Paint() );

      }
      Log.d( TAG, "Transformerd number of points: "+ mCalculatedPoints );
      
      super.draw( this.mCanvas, mapView, shadow );
      this.mCanvas = null;
   }

   /**
    * 
    * Convert the cursor from the GPSTracking provider
    * into Points on the Path 
    * 
    * @see Cursor Cursor used as input
    * @see Point Point used as transformation target
    * @see Path Path used as drawable line
    *  
    */
   private void transformAllWaypointsToPath()
   {
      Cursor trackCursor = null ;
      mCalculatedPoints = 0; 
      try 
      {
         trackCursor = this.mResolver.query(
               this.mTrackUri, 
               new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE }, 
               null, null, null);
         if( trackCursor.moveToFirst() )
         { 
            transformSingleWaypointToCurrentPoint(trackCursor.getDouble( 0 ),trackCursor.getDouble( 1 ));
            this.mStartPoint.set( this.mRecylcePoint.x, this.mRecylcePoint.y );
            this.mPath.moveTo( this.mRecylcePoint.x, this.mRecylcePoint.y );
            
            do
            {
               transformSingleWaypointToCurrentPoint(trackCursor.getDouble( 0 ),trackCursor.getDouble( 1 ));
               addCurrentPointToPath();
            } 
            while( trackCursor.move( skip ) );
            
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
    * 
    * The the waypoint in the cursor is converted into 
    * the the point based on the projection
    * 
    */
   private void transformSingleWaypointToCurrentPoint(double lat, double lon)
   {
      int microLatitude = (int) ( lat * 1E6d );
      int microLongitude = (int) ( lon * 1E6d );
      this.mProjection.toPixels(new GeoPoint(microLatitude, microLongitude), this.mRecylcePoint);        
      mCalculatedPoints++;
   }

   private void addCurrentPointToPath()
   {
      if( this.mRecylcePoint.x <= 0 || this.mRecylcePoint.x <= 0 || this.mRecylcePoint.y > this.mCanvas.getHeight() || this.mRecylcePoint.x > this.mCanvas.getWidth()  )
      {
         skip++;
      }
      
      
      int diff = Math.abs( this.mRecylcePoint.x - this.mPrevPoint.x ) + Math.abs( this.mRecylcePoint.y - this.mPrevPoint.y ) ;
      if( diff > 20 && skip > 1) 
      {
         skip--;     
      }
      else
      {
         skip++;
      }
      
      this.mPath.lineTo(this.mRecylcePoint.x, this.mRecylcePoint.y);
      
      this.mPrevPoint.x = this.mRecylcePoint.x;
      this.mPrevPoint.y = this.mRecylcePoint.y;
   }


   /**
    * Set the mPlace to the specified value.
    *
    * @param place The mPlace to set.
    */
   public void setPlace( int place )
   {
      this.mPlace += place;
   }

}
