/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Mar 10, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.util;

import nl.sogeti.android.gpstracker.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Mar 10, 2012, Sogeti B.V.
 */
public class SlidingIndicatorView extends View
{
   private static final String TAG = "OGT.SlidingIndicatorView";
   private float mMinimum = 0;
   private float mMaximum = 100 ;
   private float mValue = 0;
   private Drawable mIndicator;
   private int mIntrinsicHeight;
   
   public SlidingIndicatorView(Context context)
   {
      super(context);
   }

   public SlidingIndicatorView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }

   public SlidingIndicatorView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
   }

   @Override
   protected void onDraw(Canvas canvas)
   {
      super.onDraw(canvas);
      
      if( mIndicator == null )
      {
         mIndicator = getResources().getDrawable(R.drawable.stip);
         mIntrinsicHeight = mIndicator.getIntrinsicHeight();
         mIndicator.setBounds(0, 0, getWidth(), mIntrinsicHeight);
      }
      int height = getHeight();
      float scale = Math.abs( mValue/(mMaximum-mMinimum) );
      float y = height - height*scale;
      float translate = y-mIntrinsicHeight;
      canvas.save();
      canvas.translate(0, translate);
      mIndicator.draw(canvas);
      canvas.restore();
   }
   

   public float getMin()
   {
      return mMinimum;
   }

   public void setMin(float min)
   {
      if(mMaximum-mMinimum == 0 )
      {
         Log.w(TAG, "Minimum and maximum difference must be greater then 0");
         return;
      }
      this.mMinimum = min;
   }

   public float getMax()
   {
      return mMaximum;
   }

   public void setMax(float max)
   {
      if(mMaximum-mMinimum == 0 )
      {
         Log.w(TAG, "Minimum and maximum difference must be greater then 0");
         return;
      }
      this.mMaximum = max;
   }

   public float getValue()
   {
      return mValue;
   }

   public void setValue(float value)
   {
      this.mValue = value;
   }
}
