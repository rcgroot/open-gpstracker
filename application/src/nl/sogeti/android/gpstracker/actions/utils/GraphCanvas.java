package nl.sogeti.android.gpstracker.actions.utils;

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
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
import android.location.Location;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GraphCanvas extends View
{
   private static final String TAG = "GraphCanvas";
   public static final int TIMESPEEDGRAPH = 0;
   public static final int DISTANCESPEEDGRAPH = 1;
   public static final int TIMEALTITUDEGRAPH = 2;
   public static final int DISTANCEALTITUDEGRAPH = 3;
   private Uri mUri;
   private Bitmap mRenderBuffer;
   private Canvas mRenderCanvas;
   private Context mContext;
   private UnitsI18n mUnits;
   private int mGraphType = TIMESPEEDGRAPH;
   private long mEndTime;
   private long mStartTime;
   private double mDistance;
   private int mHeight;
   private int mMinAxis;
   private int mMaxAxis;
   
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
   
   public void setData( Uri uri, long startTime, long endTime, double distance, UnitsI18n units )
   {     
      mUri = uri;
      mStartTime = startTime;
      mEndTime = endTime;
      mDistance = distance;
      mUnits = units;
      renderGraph();
   }
   
   public void setType( int graphType)
   {
      if( mGraphType != graphType )
      {
         mGraphType = graphType;
         renderGraph();
      }
   }
   
   @Override
   protected void onSizeChanged( int w, int h, int oldw, int oldh )
   {
      super.onSizeChanged( w, h, oldw, oldh );
      
      if( mRenderBuffer == null || mRenderBuffer.getWidth() != w || mRenderBuffer.getHeight() != h )
      {
         mRenderBuffer = Bitmap.createBitmap( w, h, Config.ARGB_8888 );
         mRenderCanvas = new Canvas( mRenderBuffer );
         renderGraph();
      }
   }

   @Override
   protected void onDraw( Canvas canvas )
   {
      super.onDraw(canvas);
      canvas.drawBitmap( mRenderBuffer, 0, 0, null );
   }

   private void renderGraph()
   {
      Log.d( TAG, "renderGraph() on "+mRenderBuffer );
      if( mRenderBuffer != null )
      {
         mRenderBuffer.eraseColor( Color.TRANSPARENT );
         switch( mGraphType )
         {
            case( TIMESPEEDGRAPH ):
               drawTimeAxisGraphOnCanvas( new String[] { Waypoints.TIME, Waypoints.SPEED } );
               drawSpeeds();
               break;
            case( DISTANCESPEEDGRAPH ):
               drawDistanceAxisGraphOnCanvas(  new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.SPEED } );
               drawSpeeds();
               break;
            case( TIMEALTITUDEGRAPH ):
               drawTimeAxisGraphOnCanvas( new String[] { Waypoints.TIME, Waypoints.ALTITUDE } );
               drawAltitudes();
               break;
            case( DISTANCEALTITUDEGRAPH ):
               drawDistanceAxisGraphOnCanvas( new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.ALTITUDE } );
               drawAltitudes();
               break;
            default:
               break;
         }
      }
      postInvalidate();
   }
      
   private void drawDistanceAxisGraphOnCanvas( String[] params )
   {
      ContentResolver resolver = mContext.getApplicationContext().getContentResolver();
      Uri segmentsUri = Uri.withAppendedPath( mUri, "/segments" );
      Uri waypointsUri = null;
      Cursor segments = null;
      Cursor waypoints = null;
      int width = mRenderCanvas.getWidth()-5;
      mHeight = mRenderCanvas.getHeight()-10;
      double[][] values ;
      int[][] valueDepth;
      double maxValue = 1;
      double minValue = 0;
      double distance = 0;
      try 
      {
         segments = resolver.query( 
               segmentsUri, 
               new String[]{ Segments._ID }, 
               null, null, null );
         values = new double[segments.getCount()][width];
         valueDepth = new int[segments.getCount()][width];
         if( segments.moveToFirst() )
         {
            do
            {
               int p = segments.getPosition();
               long segmentId = segments.getLong( 0 );
               waypointsUri = Uri.withAppendedPath( segmentsUri, segmentId+"/waypoints" );
               try
               {
                  waypoints = resolver.query( 
                     waypointsUri, 
                     params, 
                     null, null, null );
                  if( waypoints.moveToFirst() )
                  {
                     Location lastLocation = null;
                     Location currentLocation = null;
                     do 
                     {
                        currentLocation = new Location( this.getClass().getName() );
                        currentLocation.setLongitude( waypoints.getDouble( 0 ) );
                        currentLocation.setLatitude( waypoints.getDouble( 1 ) );
                        if( lastLocation != null )
                        {
                           distance += lastLocation.distanceTo( currentLocation );
                        }
                        lastLocation = currentLocation;
                        double value = waypoints.getDouble( 2 );
                        if( value > 1 )
                        {
                           int i = (int) ((distance)*(width-1) / mDistance);
                           valueDepth[p][i]++;
                           values[p][i] = values[p][i]+((value-values[p][i])/valueDepth[p][i]);
                        }
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
            }
            while( segments.moveToNext() );
         }
      }
      finally
      {
         if( segments != null )
         {
            segments.close();
         }
      }
      for( int p=0;p<values.length;p++)
      {
         for( int x=0;x<values[p].length;x++)
         {
            if( valueDepth[p][x] > 0 )
            {
               values[p][x] = translateValue( values[p][x] );               
               if( values[p][x] > maxValue )
               {
                  maxValue = values[p][x];
               }
            }
         }
      }
      mMinAxis = 4 * (int)(minValue / 4);
      mMaxAxis = 4 + 4 * (int)(maxValue / 4);
      
      drawGraph( width, values, valueDepth );      
   }
   
   private void drawTimeAxisGraphOnCanvas( String[] params )
   {
      ContentResolver resolver = mContext.getApplicationContext().getContentResolver();
      Uri segmentsUri = Uri.withAppendedPath( mUri, "/segments" );
      Uri waypointsUri = null;
      Cursor segments = null;
      Cursor waypoints = null;
      int width = mRenderCanvas.getWidth()-5;
      mHeight = mRenderCanvas.getHeight()-10;
      long duration = mEndTime - mStartTime;
      double[][] values ;
      int[][] valueDepth;
      double maxValue = 1;
      double minValue = 0;
      try 
      {
         segments = resolver.query( 
               segmentsUri, 
               new String[]{ Segments._ID }, 
               null, null, null );
         values = new double[segments.getCount()][width];
         valueDepth = new int[segments.getCount()][width];
         if( segments.moveToFirst() )
         {
            do
            {
               int p = segments.getPosition();
               long segmentId = segments.getLong( 0 );
               waypointsUri = Uri.withAppendedPath( segmentsUri, segmentId+"/waypoints" );
               try
               {
                  waypoints = resolver.query( 
                     waypointsUri, 
                     params, 
                     null, null, null );
                  if( waypoints.moveToFirst() )
                  {
                     do 
                     {
                        long time = waypoints.getLong( 0 );
                        double value = waypoints.getDouble( 1 );
                        if( value > 1 )
                        {
                           int i = (int) ((time-mStartTime)*(width-1) / duration);
                           valueDepth[p][i]++;
                           values[p][i] = values[p][i]+((value-values[p][i])/valueDepth[p][i]);
                        }
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
            }
            while( segments.moveToNext() );
         }
      }
      finally
      {
         if( segments != null )
         {
            segments.close();
         }
      }
      for( int p=0;p<values.length;p++)
      {
         for( int x=0;x<values[p].length;x++)
         {
            if( valueDepth[p][x] > 0 )
            {
               values[p][x] = translateValue( values[p][x] );               
               if( values[p][x] > maxValue )
               {
                  maxValue = values[p][x];
               }
               if( values[p][x] < minValue )
               {
                  minValue = values[p][x];
               }
            }
         }
      }
      mMinAxis = 4 * (int)(minValue / 4);
      mMaxAxis = 4 + 4 * (int)(maxValue / 4);
      
      drawGraph( width, values, valueDepth );
   }

   private void drawSpeeds()
   {
      Paint white = new Paint();
      white.setColor( Color.WHITE );
      white.setAntiAlias( true );
      mRenderCanvas.drawText( String.format( "%d %s", mMinAxis, mUnits.getSpeedUnit() )  , 8,  mHeight, white );
      mRenderCanvas.drawText( String.format( "%d %s", (mMaxAxis+mMinAxis)/2, mUnits.getSpeedUnit() ) , 8,  5+mHeight/2, white );
      mRenderCanvas.drawText( String.format( "%d %s", mMaxAxis, mUnits.getSpeedUnit() ), 8,  15, white );
   }
   
   private void drawAltitudes()
   {
      Paint white = new Paint();
      white.setColor( Color.WHITE );
      white.setAntiAlias( true );
      mRenderCanvas.drawText( String.format( "%d %s", mMinAxis, mUnits.getDistanceSmallUnit() )  , 8,  mHeight, white );
      mRenderCanvas.drawText( String.format( "%d %s", (mMaxAxis+mMinAxis)/2, mUnits.getDistanceSmallUnit() ) , 8,  5+mHeight/2, white );
      mRenderCanvas.drawText( String.format( "%d %s", mMaxAxis, mUnits.getDistanceSmallUnit() ), 8,  15, white );
   }
   private double translateValue( double val )
   {
      switch( mGraphType )
      {
         case( TIMESPEEDGRAPH ):
         case( DISTANCESPEEDGRAPH ):
            val = mUnits.conversionFromMetersPerSecond( val );
            break;
         case( TIMEALTITUDEGRAPH ):
         case( DISTANCEALTITUDEGRAPH ):
            val = mUnits.conversionFromMeterToSmall( val );
            break;
         default:
            break;
      }
      return val;

   }
   
   private void drawGraph( int width, double[][] values, int[][] valueDepth )
   {
      // Matrix
      Paint ltgrey = new Paint();
      ltgrey.setColor( Color.LTGRAY );
      ltgrey.setStrokeWidth( 1 );
      // Horizontals
      ltgrey.setPathEffect( new DashPathEffect( new float[]{2,4}, 0 ) );
      mRenderCanvas.drawLine( 5, 5           , 5+width, 5           , ltgrey );
      mRenderCanvas.drawLine( 5, 5+mHeight/4  , 5+width, 5+mHeight/4  , ltgrey );
      mRenderCanvas.drawLine( 5, 5+mHeight/2  , 5+width, 5+mHeight/2  , ltgrey );
      mRenderCanvas.drawLine( 5, 5+mHeight/4*3, 5+width, 5+mHeight/4*3, ltgrey );
      // Verticals
      mRenderCanvas.drawLine( 5+width/4  , 5, 5+width/4  , 5+mHeight, ltgrey );
      mRenderCanvas.drawLine( 5+width/2  , 5, 5+width/2  , 5+mHeight, ltgrey );
      mRenderCanvas.drawLine( 5+width/4*3, 5, 5+width/4*3, 5+mHeight, ltgrey );
      mRenderCanvas.drawLine( 5+width-1   , 5, 5+width-1 , 5+mHeight, ltgrey );
      
      // The line
      Paint routePaint = new Paint();
      routePaint.setPathEffect( new CornerPathEffect( 8 ) );
      routePaint.setStyle( Paint.Style.STROKE );
      routePaint.setStrokeWidth( 4 );
      routePaint.setAntiAlias( true );
      routePaint.setColor(Color.GREEN);
      Path mPath;
      mPath = new Path();
      for( int p=0;p<values.length;p++)
      {
         int start = 0;
         while( valueDepth[p][start] == 0 && start < values[p].length-1 )
         {
            start++;
         }
         mPath.moveTo( (float)start+5, 5f+ (float) ( mHeight - ( ( values[p][start]-mMinAxis )*mHeight ) / ( mMaxAxis-mMinAxis ) ) );
         for( int x=start;x<values[p].length;x++)
         {
            double y =   mHeight - ( ( values[p][x]-mMinAxis )*mHeight ) / ( mMaxAxis-mMinAxis ) ;
            if( valueDepth[p][x] > 0 )
            {
               mPath.lineTo( (float)x+5, (float) y+5 );
            }
         }
      }
      mRenderCanvas.drawPath( mPath, routePaint );
      
      // Axis's
      Paint dkgrey = new Paint();
      dkgrey.setColor( Color.DKGRAY );
      dkgrey.setStrokeWidth( 2 );
      mRenderCanvas.drawLine( 5, 5       , 5       , 5+mHeight, dkgrey );
      mRenderCanvas.drawLine( 5, 5+mHeight, 5+width, 5+mHeight, dkgrey );
   }

}
