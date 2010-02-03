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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * Draws the current location and some relative directions on the map
 * 
 * @version $Id:$
 * @author rene (c) Feb 2, 2010, Sogeti B.V.
 */
public class DirectionsOverlay extends Overlay
{

   private Bitmap mRenderBuffer;
   private Canvas mRenderCanvas;
   private Context mContext;
   private float mDirection;
   
   private final android.view.animation.Interpolator mOriginsInterpolator = new android.view.animation.Interpolator()
   {


      public float getInterpolation( float input )
      {
         return 50;
      }
   };
   private final android.view.animation.Interpolator mDirectionInterpolator = new android.view.animation.Interpolator()
   {

      public float getInterpolation( float input )
      {
         return 120;
      }
   };
   
   DirectionsOverlay( Context ctx )
   {
      mContext = ctx;
   }

   @Override
   public void draw( Canvas canvas, MapView mapView, boolean shadow )
   {
      super.draw( canvas, mapView, shadow );
      if( shadow )
      {
         //         Log.d( TAG, "No shadows to draw" );
      }
      else
      {
         Projection mProjection = mapView.getProjection();
         if( mRenderBuffer == null || mRenderBuffer.getWidth() != canvas.getWidth() || mRenderBuffer.getHeight() != canvas.getHeight() )
         {
            //               Log.d( TAG, "Fresh buffers" );
            mRenderBuffer = Bitmap.createBitmap( canvas.getWidth(), canvas.getHeight(), Config.ARGB_8888 );
            mRenderCanvas = new Canvas( mRenderBuffer );
            mRenderCanvas.rotate( 30 );
         }
         else
         {
            mRenderBuffer.eraseColor( Color.TRANSPARENT );
         }
         
         renderArrows();
         
         canvas.drawBitmap( mRenderBuffer, 0, 0, null );
      }
   }

   private void renderArrows()
   {
      Bitmap bitmap = BitmapFactory.decodeResource( mContext.getResources(), R.drawable.location_arrow );
      mRenderCanvas.drawBitmap( bitmap, 200, 100, new Paint() );
   }
}
