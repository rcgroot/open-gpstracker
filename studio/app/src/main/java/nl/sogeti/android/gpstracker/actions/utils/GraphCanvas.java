/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.actions.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import java.text.DateFormat;
import java.util.Date;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.service.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.service.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;

/**
 * Calculate and draw graphs of track data
 *
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 * @version $Id$
 */
public class GraphCanvas extends View {
    public static final int TIMESPEEDGRAPH = 0;
    public static final int DISTANCESPEEDGRAPH = 1;
    public static final int TIMEALTITUDEGRAPH = 2;
    public static final int DISTANCEALTITUDEGRAPH = 3;
    float density = Resources.getSystem().getDisplayMetrics().density;
    private Uri mUri;
    private Bitmap mRenderBuffer;
    private Canvas mRenderCanvas;
    private Context mContext;
    private UnitsI18n mUnits;
    private int mGraphType = -1;
    private long mEndTime;
    private long mStartTime;
    private double mDistance;
    private int mHeight;
    private int mWidth;
    private int mMinAxis;
    private int mMaxAxis;
    private double mMinAlititude;
    private double mMaxAlititude;
    private double mHighestSpeedNumber;
    private double mDistanceDrawn;
    private long mStartTimeDrawn;
    private long mEndTimeDrawn;
    private Paint whiteText;
    private Paint ltgreyMatrixDashed;
    private Paint greenGraphLine;
    private Paint dkgreyMatrixLine;
    private Paint whiteCenteredText;
    private Paint dkgrayLargeType;

    public GraphCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphCanvas(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;

        whiteText = new Paint();
        whiteText.setColor(Color.WHITE);
        whiteText.setAntiAlias(true);
        whiteText.setTextSize((int) (density * 12));

        whiteCenteredText = new Paint();
        whiteCenteredText.setColor(Color.WHITE);
        whiteCenteredText.setAntiAlias(true);
        whiteCenteredText.setTextAlign(Paint.Align.CENTER);
        whiteCenteredText.setTextSize((int) (density * 12));

        ltgreyMatrixDashed = new Paint();
        ltgreyMatrixDashed.setColor(Color.LTGRAY);
        ltgreyMatrixDashed.setStrokeWidth(1);
        ltgreyMatrixDashed.setPathEffect(new DashPathEffect(new float[]{2, 4}, 0));

        greenGraphLine = new Paint();
        greenGraphLine.setPathEffect(new CornerPathEffect(8));
        greenGraphLine.setStyle(Paint.Style.STROKE);
        greenGraphLine.setStrokeWidth(4);
        greenGraphLine.setAntiAlias(true);
        greenGraphLine.setColor(Color.GREEN);

        dkgreyMatrixLine = new Paint();
        dkgreyMatrixLine.setColor(Color.DKGRAY);
        dkgreyMatrixLine.setStrokeWidth(2);

        dkgrayLargeType = new Paint();
        dkgrayLargeType.setColor(Color.LTGRAY);
        dkgrayLargeType.setAntiAlias(true);
        dkgrayLargeType.setTextAlign(Paint.Align.CENTER);
        dkgrayLargeType.setTextSize((int) (density * 21));
        dkgrayLargeType.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * Set the dataset for which to draw data. Also provide hints and helpers.
     *
     * @param uri
     * @param startTime
     * @param endTime
     * @param distance
     * @param minAlititude
     * @param maxAlititude
     * @param maxSpeed
     * @param units
     */
    public void setData(Uri uri, StatisticsCalulator calc) {
        boolean rerender = false;
        if (uri.equals(mUri)) {
            double distanceDrawnPercentage = mDistanceDrawn / mDistance;
            double duractionDrawnPercentage = (double) ((1d + mEndTimeDrawn - mStartTimeDrawn) / (1d + mEndTime -
                    mStartTime));
            rerender = distanceDrawnPercentage < 0.99d || duractionDrawnPercentage < 0.99d;
        } else {
            if (mRenderBuffer == null && super.getWidth() > 0 && super.getHeight() > 0) {
                initRenderBuffer(super.getWidth(), super.getHeight());
            }
            rerender = true;
        }

        mUri = uri;
        mUnits = calc.getUnits();

        mMinAlititude = mUnits.conversionFromMeterToHeight(calc.getMinAltitude());
        mMaxAlititude = mUnits.conversionFromMeterToHeight(calc.getMaxAltitude());

        if (mUnits.isUnitFlipped()) {
            mHighestSpeedNumber = 1.5 * mUnits.conversionFromMetersPerSecond(calc.getAverageStatisicsSpeed());
        } else {
            mHighestSpeedNumber = mUnits.conversionFromMetersPerSecond(calc.getMaxSpeed());
        }
        mStartTime = calc.getStarttime();
        mEndTime = calc.getEndtime();
        mDistance = calc.getDistanceTraveled();

        if (rerender) {
            renderGraph();
        }
    }

    private void initRenderBuffer(int w, int h) {
        mRenderBuffer = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        mRenderCanvas = new Canvas(mRenderBuffer);
    }

    private synchronized void renderGraph() {
        if (mRenderBuffer != null && mUri != null) {
            mRenderBuffer.eraseColor(Color.TRANSPARENT);
            switch (mGraphType) {
                case (TIMESPEEDGRAPH):
                    setupSpeedAxis();
                    drawGraphType();
                    drawTimeAxisGraphOnCanvas(new String[]{Waypoints.TIME, Waypoints.SPEED}, Constants
                            .MIN_STATISTICS_SPEED);
                    drawSpeedsTexts();
                    drawTimeTexts();
                    break;
                case (DISTANCESPEEDGRAPH):
                    setupSpeedAxis();
                    drawGraphType();
                    drawDistanceAxisGraphOnCanvas(new String[]{Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.SPEED
                    }, Constants.MIN_STATISTICS_SPEED);
                    drawSpeedsTexts();
                    drawDistanceTexts();
                    break;
                case (TIMEALTITUDEGRAPH):
                    setupAltitudeAxis();
                    drawGraphType();
                    drawTimeAxisGraphOnCanvas(new String[]{Waypoints.TIME, Waypoints.ALTITUDE}, -1000d);
                    drawAltitudesTexts();
                    drawTimeTexts();
                    break;
                case (DISTANCEALTITUDEGRAPH):
                    setupAltitudeAxis();
                    drawGraphType();
                    drawDistanceAxisGraphOnCanvas(new String[]{Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints
                            .ALTITUDE}, -1000d);
                    drawAltitudesTexts();
                    drawDistanceTexts();
                    break;
                default:
                    break;
            }
            mDistanceDrawn = mDistance;
            mStartTimeDrawn = mStartTime;
            mEndTimeDrawn = mEndTime;
        }

        postInvalidate();
    }

    private void setupSpeedAxis() {
        mMinAxis = 0;
        mMaxAxis = 4 + 4 * (int) (mHighestSpeedNumber / 4);

        mWidth = mRenderCanvas.getWidth() - 5;
        mHeight = mRenderCanvas.getHeight() - 10;
    }

    private void drawGraphType() {
        //float density = Resources.getSystem().getDisplayMetrics().density;
        String text;
        switch (mGraphType) {
            case (TIMESPEEDGRAPH):
                text = mContext.getResources().getString(R.string.graphtype_timespeed);
                break;
            case (DISTANCESPEEDGRAPH):
                text = mContext.getResources().getString(R.string.graphtype_distancespeed);
                break;
            case (TIMEALTITUDEGRAPH):
                text = mContext.getResources().getString(R.string.graphtype_timealtitude);
                break;
            case (DISTANCEALTITUDEGRAPH):
                text = mContext.getResources().getString(R.string.graphtype_distancealtitude);
                break;
            default:
                text = "UNKNOWN GRAPH TYPE";
                break;
        }
        mRenderCanvas.drawText(text, 5 + mWidth / 2, 5 + mHeight / 8, dkgrayLargeType);

    }

    private void drawTimeAxisGraphOnCanvas(String[] params, double minValue) {
        ContentResolver resolver = mContext.getContentResolver();
        Uri segmentsUri = Uri.withAppendedPath(mUri, "segments");
        Uri waypointsUri = null;
        Cursor segments = null;
        Cursor waypoints = null;
        long duration = 1 + mEndTime - mStartTime;
        double[][] values;
        int[][] valueDepth;
        try {
            segments = resolver.query(
                    segmentsUri,
                    new String[]{Segments._ID},
                    null, null, null);
            int segmentCount = segments.getCount();
            values = new double[segmentCount][mWidth];
            valueDepth = new int[segmentCount][mWidth];
            if (segments.moveToFirst()) {
                for (int segment = 0; segment < segmentCount; segment++) {
                    segments.moveToPosition(segment);
                    long segmentId = segments.getLong(0);
                    waypointsUri = Uri.withAppendedPath(segmentsUri, segmentId + "/waypoints");
                    try {
                        waypoints = resolver.query(
                                waypointsUri,
                                params,
                                null, null, null);
                        if (waypoints.moveToFirst()) {
                            do {
                                long time = waypoints.getLong(0);
                                double value = waypoints.getDouble(1);
                                if (value != 0 && value > minValue && segment < values.length) {
                                    int x = (int) ((time - mStartTime) * (mWidth - 1) / duration);
                                    if (x > 0 && x < valueDepth[segment].length) {
                                        valueDepth[segment][x]++;
                                        values[segment][x] = values[segment][x] + ((value - values[segment][x]) /
                                                valueDepth[segment][x]);
                                    }
                                }
                            }
                            while (waypoints.moveToNext());
                        }
                    } finally {
                        if (waypoints != null) {
                            waypoints.close();
                        }
                    }
                }

            }
        } finally {
            if (segments != null) {
                segments.close();
            }
        }
        for (int p = 0; p < values.length; p++) {
            for (int x = 0; x < values[p].length; x++) {
                if (valueDepth[p][x] > 0) {
                    values[p][x] = translateValue(values[p][x]);
                }
            }
        }
        drawGraph(values, valueDepth);
    }

    private void drawSpeedsTexts() {
        mRenderCanvas.drawText(String.format("%d %s", mMinAxis, mUnits.getSpeedUnit()), 8, mHeight, whiteText);
        mRenderCanvas.drawText(String.format("%d %s", (mMaxAxis + mMinAxis) / 2, mUnits.getSpeedUnit()), 8, 3 + mHeight
                / 2, whiteText);
        mRenderCanvas.drawText(String.format("%d %s", mMaxAxis, mUnits.getSpeedUnit()), 8, 7 + whiteText.getTextSize(),
                whiteText);
    }

    private void drawTimeTexts() {
        DateFormat timeInstance = android.text.format.DateFormat.getTimeFormat(this.getContext().getApplicationContext());
        String start = timeInstance.format(new Date(mStartTime));
        String half = timeInstance.format(new Date((mEndTime + mStartTime) / 2));
        String end = timeInstance.format(new Date(mEndTime));

        Path yAxis;
        yAxis = new Path();
        yAxis.moveTo(5, 5 + mHeight / 2);
        yAxis.lineTo(5, 5);
        mRenderCanvas.drawTextOnPath(start, yAxis, 0, whiteCenteredText.getTextSize(), whiteCenteredText);
        yAxis = new Path();
        yAxis.moveTo(5 + mWidth / 2, 5 + mHeight / 2);
        yAxis.lineTo(5 + mWidth / 2, 5);
        mRenderCanvas.drawTextOnPath(half, yAxis, 0, -3, whiteCenteredText);
        yAxis = new Path();
        yAxis.moveTo(5 + mWidth - 1, 5 + mHeight / 2);
        yAxis.lineTo(5 + mWidth - 1, 5);
        mRenderCanvas.drawTextOnPath(end, yAxis, 0, -3, whiteCenteredText);
    }

    /**
     * @param params
     * @param minValue Minimum value of params[1] that will be drawn
     */
    private void drawDistanceAxisGraphOnCanvas(String[] params, double minValue) {
        ContentResolver resolver = mContext.getContentResolver();
        Uri segmentsUri = Uri.withAppendedPath(mUri, "segments");
        Uri waypointsUri = null;
        Cursor segments = null;
        Cursor waypoints = null;
        double[][] values;
        int[][] valueDepth;
        double distance = 1;
        try {
            segments = resolver.query(
                    segmentsUri,
                    new String[]{Segments._ID},
                    null, null, null);
            int segmentCount = segments.getCount();
            values = new double[segmentCount][mWidth];
            valueDepth = new int[segmentCount][mWidth];
            if (segments.moveToFirst()) {
                for (int segment = 0; segment < segmentCount; segment++) {
                    segments.moveToPosition(segment);
                    long segmentId = segments.getLong(0);
                    waypointsUri = Uri.withAppendedPath(segmentsUri, segmentId + "/waypoints");
                    try {
                        waypoints = resolver.query(
                                waypointsUri,
                                params,
                                null, null, null);
                        if (waypoints.moveToFirst()) {
                            Location lastLocation = null;
                            Location currentLocation = null;
                            do {
                                currentLocation = new Location(this.getClass().getName());
                                currentLocation.setLongitude(waypoints.getDouble(0));
                                currentLocation.setLatitude(waypoints.getDouble(1));
                                // Do no include obvious wrong 0.0 lat 0.0 long, skip to next value in while-loop
                                if (currentLocation.getLatitude() == 0.0d || currentLocation.getLongitude() == 0.0d) {
                                    continue;
                                }
                                if (lastLocation != null) {
                                    distance += lastLocation.distanceTo(currentLocation);
                                }
                                lastLocation = currentLocation;
                                double value = waypoints.getDouble(2);
                                if (value != 0 && value > minValue && segment < values.length) {
                                    int x = (int) ((distance) * (mWidth - 1) / mDistance);
                                    if (x > 0 && x < valueDepth[segment].length) {
                                        valueDepth[segment][x]++;
                                        values[segment][x] = values[segment][x] + ((value - values[segment][x]) /
                                                valueDepth[segment][x]);
                                    }
                                }
                            }
                            while (waypoints.moveToNext());
                        }
                    } finally {
                        if (waypoints != null) {
                            waypoints.close();
                        }
                    }
                }
            }
        } finally {
            if (segments != null) {
                segments.close();
            }
        }
        for (int segment = 0; segment < values.length; segment++) {
            for (int x = 0; x < values[segment].length; x++) {
                if (valueDepth[segment][x] > 0) {
                    values[segment][x] = translateValue(values[segment][x]);
                }
            }
        }
        drawGraph(values, valueDepth);
    }

    private void drawDistanceTexts() {
        String start = String.format("%.0f %s", mUnits.conversionFromMeter(0), mUnits.getDistanceUnit());
        String half = String.format("%.0f %s", mUnits.conversionFromMeter(mDistance) / 2, mUnits.getDistanceUnit());
        String end = String.format("%.0f %s", mUnits.conversionFromMeter(mDistance), mUnits.getDistanceUnit());

        Path yAxis;
        yAxis = new Path();
        yAxis.moveTo(5, 5 + mHeight / 2);
        yAxis.lineTo(5, 5);
        mRenderCanvas.drawTextOnPath(start, yAxis, 0, whiteText.getTextSize(), whiteCenteredText);
        yAxis = new Path();
        yAxis.moveTo(5 + mWidth / 2, 5 + mHeight / 2);
        yAxis.lineTo(5 + mWidth / 2, 5);
        mRenderCanvas.drawTextOnPath(half, yAxis, 0, -3, whiteCenteredText);
        yAxis = new Path();
        yAxis.moveTo(5 + mWidth - 1, 5 + mHeight / 2);
        yAxis.lineTo(5 + mWidth - 1, 5);
        mRenderCanvas.drawTextOnPath(end, yAxis, 0, -3, whiteCenteredText);
    }

    private void setupAltitudeAxis() {
        mMinAxis = -4 + 4 * (int) (mMinAlititude / 4);
        mMaxAxis = 4 + 4 * (int) (mMaxAlititude / 4);

        mWidth = mRenderCanvas.getWidth() - 5;
        mHeight = mRenderCanvas.getHeight() - 10;
    }

    private void drawAltitudesTexts() {
        mRenderCanvas.drawText(String.format("%d %s", mMinAxis, mUnits.getHeightUnit()), 8, mHeight, whiteText);
        mRenderCanvas.drawText(String.format("%d %s", (mMaxAxis + mMinAxis) / 2, mUnits.getHeightUnit()), 8, 5 +
                mHeight / 2, whiteText);
        mRenderCanvas.drawText(String.format("%d %s", mMaxAxis, mUnits.getHeightUnit()), 8, 15, whiteText);
    }

    private double translateValue(double val) {
        switch (mGraphType) {
            case (TIMESPEEDGRAPH):
            case (DISTANCESPEEDGRAPH):
                val = mUnits.conversionFromMetersPerSecond(val);
                break;
            case (TIMEALTITUDEGRAPH):
            case (DISTANCEALTITUDEGRAPH):
                val = mUnits.conversionFromMeterToHeight(val);
                break;
            default:
                break;
        }
        return val;

    }

    private void drawGraph(double[][] values, int[][] valueDepth) {
        // Matrix
        // Horizontals
        mRenderCanvas.drawLine(5, 5, 5 + mWidth, 5, ltgreyMatrixDashed); // top
        mRenderCanvas.drawLine(5, 5 + mHeight / 4, 5 + mWidth, 5 + mHeight / 4, ltgreyMatrixDashed); // 2nd
        mRenderCanvas.drawLine(5, 5 + mHeight / 2, 5 + mWidth, 5 + mHeight / 2, ltgreyMatrixDashed); // middle
        mRenderCanvas.drawLine(5, 5 + mHeight / 4 * 3, 5 + mWidth, 5 + mHeight / 4 * 3, ltgreyMatrixDashed); // 3rd
        // Verticals
        mRenderCanvas.drawLine(5 + mWidth / 4, 5, 5 + mWidth / 4, 5 + mHeight, ltgreyMatrixDashed); // 2nd
        mRenderCanvas.drawLine(5 + mWidth / 2, 5, 5 + mWidth / 2, 5 + mHeight, ltgreyMatrixDashed); // middle
        mRenderCanvas.drawLine(5 + mWidth / 4 * 3, 5, 5 + mWidth / 4 * 3, 5 + mHeight, ltgreyMatrixDashed); // 3rd
        mRenderCanvas.drawLine(5 + mWidth - 1, 5, 5 + mWidth - 1, 5 + mHeight, ltgreyMatrixDashed); // right

        // The line
        Path mPath;
        int emptyValues = 0;
        mPath = new Path();
        for (int p = 0; p < values.length; p++) {
            int start = 0;
            while (valueDepth[p][start] == 0 && start < values[p].length - 1) {
                start++;
            }
            mPath.moveTo((float) start + 5, 5f + (float) (mHeight - ((values[p][start] - mMinAxis) * mHeight) /
                    (mMaxAxis - mMinAxis)));
            for (int x = start; x < values[p].length; x++) {
                double y = mHeight - ((values[p][x] - mMinAxis) * mHeight) / (mMaxAxis - mMinAxis);
                if (valueDepth[p][x] > 0) {
                    if (emptyValues > mWidth / 10) {
                        mPath.moveTo((float) x + 5, (float) y + 5);
                    } else {
                        mPath.lineTo((float) x + 5, (float) y + 5);
                    }
                    emptyValues = 0;
                } else {
                    emptyValues++;
                }
            }
        }
        mRenderCanvas.drawPath(mPath, greenGraphLine);

        // Axis's
        mRenderCanvas.drawLine(5, 5, 5, 5 + mHeight, dkgreyMatrixLine);
        mRenderCanvas.drawLine(5, 5 + mHeight, 5 + mWidth, 5 + mHeight, dkgreyMatrixLine);
    }

    public synchronized void clearData() {
        mUri = null;
        mUnits = null;
        mRenderBuffer = null;
    }

    public int getType() {
        return mGraphType;
    }

    public void setType(int graphType) {
        if (mGraphType != graphType) {
            mGraphType = graphType;
            renderGraph();
        }
    }

    @Override
    protected synchronized void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            initRenderBuffer(w, h);
            renderGraph();
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRenderBuffer != null) {
            canvas.drawBitmap(mRenderBuffer, 0, 0, null);
        }
    }

}
