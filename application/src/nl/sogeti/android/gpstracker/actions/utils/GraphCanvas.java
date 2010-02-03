package nl.sogeti.android.gpstracker.actions.utils;

import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GraphCanvas extends View
{
   private static final String TAG = "GraphCanvas";
   private Uri mUri;
   private Bitmap mRenderBuffer;
   private Canvas mRenderCanvas;
   private Context mContext;
   
   public GraphCanvas(Context context)
   {
      super(context);
      mContext = context;
   }   
   public GraphCanvas( Context context, AttributeSet attrs )
   {
      super(context, attrs);
      mContext = context;
   }
   public GraphCanvas( Context context, AttributeSet attrs, int defStyle )
   {
      super(context, attrs, defStyle);
      mContext = context;
   }

   @Override
   protected void onDraw( Canvas canvas )
   {
      super.onDraw(canvas);
      if( mRenderBuffer == null || mRenderBuffer.getWidth() != canvas.getWidth() || mRenderBuffer.getHeight() != canvas.getHeight() )
      {
         int mHeight = canvas.getHeight();
         int mWidth = canvas.getWidth();
         mRenderBuffer = Bitmap.createBitmap( mWidth, mHeight, Config.ARGB_8888 );
         mRenderCanvas = new Canvas( mRenderBuffer );
      }
      else
      {
         mRenderBuffer.eraseColor( Color.TRANSPARENT );
      }
      Log.d( TAG, "Draw"+canvas.getWidth()+"x"+canvas.getHeight() );
      //drawSingleGraphOnBuffer( canvas, new String[] { Waypoints.TIME, Waypoints.SPEED } );
      drawSingleGraphOnBuffer( canvas, new String[] { Waypoints.TIME, Waypoints.LATITUDE } );
      //canvas.drawBitmap( mRenderBuffer, 0, 0, null );
   }
   
   public void setUri( Uri uri )
   {
      mUri = uri;
      this.postInvalidate();
   }
   
   private void drawSingleGraphOnBuffer( Canvas canvas, String[] params )
   {
      ContentResolver resolver = mContext.getApplicationContext().getContentResolver();
      Uri waypointsUri = Uri.withAppendedPath( mUri, "/waypoints" );
      Cursor waypoints = null;
      int width = canvas.getWidth();
      int height = canvas.getHeight();
      double[] values = new double[width];
      int[] valueDepth = new int[width];
      double maxValue = Double.MIN_VALUE;
      double minValue = Double.MAX_VALUE;
      try 
      {
         
         
         waypoints = resolver.query( 
               waypointsUri, 
               params, 
               null, null, null );
         if( waypoints.moveToLast() )
         {
            long mEndTime = waypoints.getLong( 0 );
            waypoints.moveToFirst();
            long mStartTime = waypoints.getLong( 0 );
            long duration = mEndTime - mStartTime;
            do 
            {
               long time = waypoints.getLong( 0 );
               double speed = waypoints.getDouble( 1 );
               int i = (int) ((time-mStartTime)*(width-1) / duration);
               valueDepth[i]++;
               values[i] = values[i]+((speed-values[i])/valueDepth[i]);
            }
            while( waypoints.moveToNext() );
         }
      }
      finally
      {
         if( waypoints != null )
         {
            waypoints.close();
         }
      }
      
      for( int x=0;x<values.length;x++)
      {
         if( valueDepth[x] > 0 )
         {
            if( values[x] > maxValue )
            {
               maxValue = values[x];
            }
            if( values[x] < minValue )
            {
               minValue = values[x];
            }
         }
      }
      
      Paint routePaint = new Paint();
      routePaint.setPathEffect( new CornerPathEffect( 10 ) );
      routePaint.setStyle( Paint.Style.STROKE );
      routePaint.setStrokeWidth( 6 );
      routePaint.setAntiAlias( true );
      routePaint.setColor(Color.GREEN);
      Path mPath;
      mPath = new Path();
      mPath.moveTo( 0f, (float) ( height - ((values[0]-minValue)*height)/(maxValue-minValue) ) );
      for( int x=0;x<values.length;x++)
      {
         double y = height - ((values[x]-minValue)*height)/(maxValue-minValue) ;
         if( valueDepth[x] > 0 )
         {
            Log.d( TAG, "Path to ("+x+","+y+")");
            mPath.lineTo( (float)x, (float) y );
         }
      }      
      canvas.drawPath( mPath, routePaint );
   }

}
