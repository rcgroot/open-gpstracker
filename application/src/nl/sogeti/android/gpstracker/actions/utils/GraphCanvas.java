package nl.sogeti.android.gpstracker.actions.utils;

import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.util.AttributeSet;
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
   protected void onSizeChanged( int w, int h, int oldw, int oldh )
   {
      super.onSizeChanged( w, h, oldw, oldh );
      
      if( mRenderBuffer == null || mRenderBuffer.getWidth() != w || mRenderBuffer.getHeight() != h )
      {
         mRenderBuffer = Bitmap.createBitmap( w, h, Config.ARGB_8888 );
         mRenderCanvas = new Canvas( mRenderBuffer );
      }
      else
      {
         mRenderBuffer.eraseColor( Color.TRANSPARENT );
      }
   }

   @Override
   protected void onDraw( Canvas canvas )
   {
      super.onDraw(canvas);

//      Log.d( TAG, "Draw"+canvas.getWidth()+"x"+canvas.getHeight() );
      drawSingleGraphOnBuffer( mRenderCanvas, new String[] { Waypoints.TIME, Waypoints.SPEED } );
      //drawSingleGraphOnBuffer( mRenderCanvas, new String[] { Waypoints.TIME, Waypoints.ALTITUDE } );
      //drawSingleGraphOnBuffer( mRenderCanvas, new String[] { Waypoints.TIME, Waypoints.LATITUDE } ); // has more numbers in the emulator
      canvas.drawBitmap( mRenderBuffer, 0, 0, null );
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
      int width = canvas.getWidth()-5;
      int height = canvas.getHeight()-10;
      double[] values = new double[width];
      int[] valueDepth = new int[width];
      int maxValue = Integer.MIN_VALUE;
      int minValue = Integer.MAX_VALUE;
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
               maxValue = (int) values[x];
            }
            if( values[x] < minValue )
            {
               minValue = (int) values[x];
            }
         }
      }
      minValue = 4 * (minValue / 4);
      maxValue = 4 * (maxValue / 4) + 4;
//      Paint red = new Paint();
//      red.setColor(  Color.RED );
//      canvas.drawCircle( 5, 5, 5, red );
//      canvas.drawCircle( canvas.getWidth()-5, 5, 5, red );
//      canvas.drawCircle( 5, canvas.getHeight()-5, 5, red );
//      canvas.drawCircle( canvas.getWidth()-5, canvas.getHeight()-5, 5, red );
      
      Paint grey = new Paint();
      grey.setColor( Color.LTGRAY );
      grey.setStrokeWidth( 1 );
      // Horizontals
      grey.setPathEffect( new DashPathEffect( new float[]{2,4}, 0 ) );
      canvas.drawLine( 5, 5           , 5+width, 5           , grey );
      canvas.drawLine( 5, 5+height/4  , 5+width, 5+height/4  , grey );
      canvas.drawLine( 5, 5+height/2  , 5+width, 5+height/2  , grey );
      canvas.drawLine( 5, 5+height/4*3, 5+width, 5+height/4*3, grey );
      // Verticals
      canvas.drawLine( 5+width/4  , 5, 5+width/4  , 5+height, grey );
      canvas.drawLine( 5+width/2  , 5, 5+width/2  , 5+height, grey );
      canvas.drawLine( 5+width/4*3, 5, 5+width/4*3, 5+height, grey );
      canvas.drawLine( 5+width-1   , 5, 5+width-1 , 5+height, grey );
      
      Paint routePaint = new Paint();
      routePaint.setPathEffect( new CornerPathEffect( 8 ) );
      routePaint.setStyle( Paint.Style.STROKE );
      routePaint.setStrokeWidth( 4 );
      routePaint.setAntiAlias( true );
      routePaint.setColor(Color.GREEN);
      Path mPath;
      mPath = new Path();
      mPath.moveTo( 5f, 5f+ (float) ( height - ((values[0]-minValue)*height)/(maxValue-minValue) ) );
      for( int x=0;x<values.length;x++)
      {
         double y = height - ((values[x]-minValue)*height)/(maxValue-minValue) ;
         if( valueDepth[x] > 0 )
         {
//            Log.d( TAG, "Path to ("+x+","+y+")");
            mPath.lineTo( (float)x+5, (float) y+5 );
         }
      }      
      canvas.drawPath( mPath, routePaint );
      
      grey = new Paint();
      grey.setColor( Color.DKGRAY );
      grey.setStrokeWidth( 2 );
      canvas.drawLine( 5, 5       , 5      , 5+height, grey );
      canvas.drawLine( 5, 5+height, 5+width, 5+height, grey );
      
      Paint white = new Paint();
      white.setColor( Color.WHITE );
      white.setAntiAlias( true );
      canvas.drawText( ""+minValue  , 8,  height, white );
      canvas.drawText( ""+(maxValue+minValue)/2 , 8,  5+height/2, white );
      canvas.drawText( ""+maxValue, 8,  15, white );
   }

}
