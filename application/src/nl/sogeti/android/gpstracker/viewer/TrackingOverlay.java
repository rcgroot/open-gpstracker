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

import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Shader;
import android.net.Uri;

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
   public static final String TAG = TrackingOverlay.class.getName();

   public static final int DRAW_GREEN = 0;
   public static final int DRAW_RED = 1;
   public static final int DRAW_MEASURED = 2;
   public static final int DRAW_CALCULATED = 3;
   public static final int DRAW_DOTS = 4;
   public static final String TRACKCOLORING = "trackcoloring";
   private int trackColoringMethod = DRAW_CALCULATED;

   private ContentResolver mResolver;
   private Projection mProjection;
   
   private Uri mWaypointsUri;
 
   private Path mPath;
   private Canvas mCanvas;
   private Shader mShader;
   
   private Point mScreenPoint;
   
   TrackingOverlay( Context cxt, ContentResolver resolver, Uri waypointsUri, int color, double avgSpeed, MapView mapView )
   {
      super();
      this.trackColoringMethod = color;
      this.mPath = new Path();
      this.mResolver = resolver;
      this.mWaypointsUri = waypointsUri;
   }  

   @Override
   public void draw( Canvas canvas, MapView mapView, boolean shadow )
   {
      drawPath( canvas, mapView, shadow );
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
      mProjection = mapView.getProjection();

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

      super.draw( this.mCanvas, mapView, shadow );
      this.mCanvas = null;
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
      try
      {
         trackCursor = this.mResolver.query( this.mWaypointsUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.SPEED, Waypoints.TIME }, null, null, null );
         
         if( trackCursor.moveToFirst() )
         {
            GeoPoint mStartPoint = extractGeoPoint( trackCursor );
            moveToGeoPoint( mStartPoint );
         }
         
         int points = trackCursor.getCount();
         for( int i=1; i<points ; i++ )
         {
            trackCursor.moveToPosition( i );
            GeoPoint geoPoint = extractGeoPoint( trackCursor  );  
            lineToGeoPoint( geoPoint, -1d );     
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
   
   

   
   private GeoPoint extractGeoPoint( Cursor trackCursor )
   {
      int microLatitude = (int) ( trackCursor.getDouble( 0 ) * 1E6d );
      int microLongitude = (int) ( trackCursor.getDouble( 1 ) * 1E6d );
      return new GeoPoint( microLatitude, microLongitude );
   }
   
   /**
    * Of this line a path part to the given geopoint 
    * 
    * @param geoPoint
    * @param speed
    */
   private void lineToGeoPoint( GeoPoint geoPoint, double speed )
   {
      this.mProjection.toPixels( geoPoint, this.mScreenPoint );     
      this.mPath.lineTo( this.mScreenPoint.x, this.mScreenPoint.y );
   }
   
   private void moveToGeoPoint( GeoPoint geoPoint )
   {
      this.mProjection.toPixels( geoPoint, this.mScreenPoint );
      this.mPath.moveTo( this.mScreenPoint.x, this.mScreenPoint.y );
   }  
}
