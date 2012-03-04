package nl.sogeti.android.gpstracker.viewer.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public abstract class AsyncOverlay extends Overlay
{

    private static final String TAG = "GG.AsyncOverlay";

    /**
     * Handler postable object to schedule an recalculation
     */
    private final Runnable mCalculator = new Runnable()
        {
            public void run()
            {
                AsyncOverlay.this.considerCalculate();
            }
        };

    /**
     * Handler provided by the MapActivity to recalculate graphics
     */
    private Handler mHandler;

    protected Projection mProjection;

    private GeoPoint mGeoTopLeft;

    private GeoPoint mGeoBottumRight;

    private int mWidth;

    private int mHeight;

    private Bitmap mActiveBitmap;

    private GeoPoint mActiveTopLeft;
    
    private Point mActivePointTopLeft;
    
    private boolean mRecalculationFlag;

    private Bitmap mCalculationBitmap;

    private Canvas mCalculationCanvas;
        
    private Paint mPaint;

    private MapView mMapView;

    AsyncOverlay(MapView mapView, Handler handler)
    {
        mMapView = mapView;
        mProjection = mMapView.getProjection();
        mHandler = handler;
        mWidth = 1;
        mHeight = 1;
        mPaint = new Paint();
        mRecalculationFlag = true;
        mActiveBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mActiveTopLeft = new GeoPoint(0, 0);
        mActivePointTopLeft = new Point();
        mCalculationBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCalculationCanvas = new Canvas(mCalculationBitmap);
    }

    protected void postRecalculation()
    {
        mRecalculationFlag = true;
        asyncCalculate();
    }
    
    /**
     * Schedule an async recalculation of the image based on the datasource
     */
    public void asyncCalculate()
    {
        mHandler.removeCallbacks(mCalculator);
        mHandler.post(mCalculator);
    }
    
    protected void considerCalculate()
    {
        GeoPoint oldTopLeft = mGeoTopLeft;
        GeoPoint oldBottumRight = mGeoBottumRight;
        mGeoTopLeft = mProjection.fromPixels( 0, 0 );
        mGeoBottumRight = mProjection.fromPixels( mWidth, mHeight );

        if( mCalculationBitmap.getWidth() != mWidth || mCalculationBitmap.getHeight() != mHeight )
        {
            mCalculationBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            mCalculationCanvas.setBitmap(mCalculationBitmap);
            mRecalculationFlag = true;
        }
        
        if( mRecalculationFlag
                || oldTopLeft == null 
                || oldBottumRight == null 
                || mGeoTopLeft.getLatitudeE6() / 100 != oldTopLeft.getLatitudeE6() / 100 
                || mGeoTopLeft.getLongitudeE6() / 100 != oldTopLeft.getLongitudeE6() / 100
                || mGeoBottumRight.getLatitudeE6() / 100 != oldBottumRight.getLatitudeE6() / 100 
                || mGeoBottumRight.getLongitudeE6() / 100 != oldBottumRight.getLongitudeE6() / 100 )
        {
            mCalculationBitmap.eraseColor(Color.TRANSPARENT);
            calculate( mCalculationCanvas );
            synchronized (mActiveBitmap)
            {
                Bitmap oldActiveBitmap = mActiveBitmap;
                mActiveBitmap = mCalculationBitmap;
                mActiveTopLeft = mGeoTopLeft;
                mCalculationBitmap = oldActiveBitmap;
                mCalculationCanvas.setBitmap(mCalculationBitmap);
            }
            mRecalculationFlag = false;
            mMapView.postInvalidate();
        }
    }

    protected abstract void calculate( Canvas asyncBuffer );

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow)
    {
        mWidth = canvas.getWidth();
        mHeight = canvas.getHeight();
        mProjection = mapView.getProjection();
        if (!shadow)
        {
            draw(canvas);
        }
    }
    
    private void draw(Canvas canvas)
    {
        asyncCalculate();
        synchronized (mActiveBitmap)
        {
            mProjection.toPixels(mActiveTopLeft, mActivePointTopLeft);
            canvas.drawBitmap(mActiveBitmap, mActivePointTopLeft.x, mActivePointTopLeft.y, mPaint);
        }
    }
    
    protected boolean isPointOnScreen(Point point)
    {
        return point.x < 0 || point.y < 0 || point.x > mWidth || point.y > mHeight;
    }

    protected boolean isGeoPointOnScreen(GeoPoint geopoint)
    {
        boolean onscreen = true;
        if (geopoint != null && mGeoTopLeft != null && mGeoBottumRight != null)
        {
            onscreen = onscreen && mGeoTopLeft.getLatitudeE6() > geopoint.getLatitudeE6();
            onscreen = onscreen && mGeoBottumRight.getLatitudeE6() < geopoint.getLatitudeE6();
            if (mGeoTopLeft.getLongitudeE6() < mGeoBottumRight.getLongitudeE6())
            {
                onscreen = onscreen && mGeoTopLeft.getLongitudeE6() < geopoint.getLongitudeE6();
                onscreen = onscreen && mGeoBottumRight.getLongitudeE6() > geopoint.getLongitudeE6();
            }
            else
            {
                onscreen = onscreen
                        && (mGeoTopLeft.getLongitudeE6() < geopoint.getLongitudeE6() || mGeoBottumRight
                                .getLongitudeE6() > geopoint.getLongitudeE6());
            }
        }
        return onscreen;
    }
}
