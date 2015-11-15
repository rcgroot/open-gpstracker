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
package nl.sogeti.android.gpstracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import java.util.Locale;

import nl.sogeti.android.gpstracker.R;

/**
 * Collection of methods to provide metric and imperial data based on locale or
 * overridden by configuration
 *
 * @author rene (c) Feb 2, 2010, Sogeti B.V.
 * @version $Id$
 */
public class UnitsI18n {
    private Context mContext;
    private double mConversion_from_mps_to_speed;
    private double mConversion_from_meter_to_distance;
    private double mConversion_from_meter_to_height;
    private String mSpeed_unit;
    private String mDistance_unit;
    private String mHeight_unit;
    private UnitsChangeListener mListener;
    private boolean needsUnitFlip;
    private int mUnits;
    private OnSharedPreferenceChangeListener mPreferenceListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(Constants.UNITS)) {
                initBasedOnPreferences(sharedPreferences);
                if (mListener != null) {
                    mListener.onUnitsChange();
                }
            }
        }
    };

    public UnitsI18n(Context ctx, UnitsChangeListener listener) {
        this(ctx);
        mListener = listener;
    }

    public UnitsI18n(Context ctx) {
        mContext = ctx;
        initBasedOnPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
    }

    private void initBasedOnPreferences(SharedPreferences sharedPreferences) {
        mUnits = Integer.parseInt(sharedPreferences.getString(Constants.UNITS, Integer.toString(Constants
                .UNITS_DEFAULT)));
        switch (mUnits) {
            case (Constants.UNITS_DEFAULT):
                setToDefault();
                break;
            case (Constants.UNITS_IMPERIAL):
                setToImperial();
                break;
            case (Constants.UNITS_METRIC):
                setToMetric();
                break;
            case (Constants.UNITS_NAUTIC):
                setToMetric();
                overrideWithNautic(mContext.getResources());
                break;
            case (Constants.UNITS_METRICPACE):
                setToMetric();
                overrideWithPace(mContext.getResources());
                break;
            case (Constants.UNITS_IMPERIALPACE):
                setToImperial();
                overrideWithPaceImperial(mContext.getResources());
                break;
            case Constants.UNITS_IMPERIALSURFACE:
                setToImperial();
                overrideWithSurfaceImperial();
                break;
            case Constants.UNITS_METRICSURFACE:
                setToMetric();
                overrideWithSurfaceMetric();
                break;
            default:
                setToDefault();
                break;
        }
    }

    private void setToDefault() {
        Resources resources = mContext.getResources();
        init(resources);
    }

    private void setToImperial() {
        Resources resources = mContext.getResources();
        Configuration config = resources.getConfiguration();
        Locale oldLocale = config.locale;
        config.locale = Locale.US;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        init(resources);
        config.locale = oldLocale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void setToMetric() {
        Resources resources = mContext.getResources();
        Configuration config = resources.getConfiguration();
        Locale oldLocale = config.locale;
        config.locale = new Locale("");
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        init(resources);
        config.locale = oldLocale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void overrideWithNautic(Resources resources) {
        TypedValue outValue = new TypedValue();
        resources.getValue(R.raw.conversion_from_mps_to_knot, outValue, false);
        mConversion_from_mps_to_speed = outValue.getFloat();
        resources.getValue(R.raw.conversion_from_meter_to_nauticmile, outValue, false);
        mConversion_from_meter_to_distance = outValue.getFloat();

        mSpeed_unit = resources.getString(R.string.knot_unitname);
        mDistance_unit = resources.getString(R.string.nautic_unitname);
    }

    private void overrideWithPace(Resources resources) {
        needsUnitFlip = true;
        mSpeed_unit = resources.getString(R.string.pace_unitname);
    }

    private void overrideWithPaceImperial(Resources resources) {
        needsUnitFlip = true;
        mSpeed_unit = resources.getString(R.string.pace_unitname_imperial);
    }

    private void overrideWithSurfaceImperial() {
        float width = getWidthPreference();
        Resources resources = mContext.getResources();
        TypedValue outValue = new TypedValue();
        resources.getValue(R.raw.conversion_from_mps_to_acres_hour, outValue, false);
        mConversion_from_mps_to_speed = outValue.getFloat() * width;
        mSpeed_unit = resources.getString(R.string.surface_unitname_imperial);
    }

    private void overrideWithSurfaceMetric() {
        float width = getWidthPreference();
        Resources resources = mContext.getResources();
        TypedValue outValue = new TypedValue();
        resources.getValue(R.raw.conversion_from_mps_to_hectare_hour, outValue, false);
        mConversion_from_mps_to_speed = outValue.getFloat() * width;
        mSpeed_unit = resources.getString(R.string.surface_unitname_metric);
    }

    /**
     * Based on a given Locale prefetch the units conversions and names.
     *
     * @param resources Resources initialized with a Locale
     */
    private void init(Resources resources) {
        TypedValue outValue = new TypedValue();
        needsUnitFlip = false;
        resources.getValue(R.raw.conversion_from_mps, outValue, false);
        mConversion_from_mps_to_speed = outValue.getFloat();
        resources.getValue(R.raw.conversion_from_meter, outValue, false);
        mConversion_from_meter_to_distance = outValue.getFloat();
        resources.getValue(R.raw.conversion_from_meter_to_height, outValue, false);
        mConversion_from_meter_to_height = outValue.getFloat();

        mSpeed_unit = resources.getString(R.string.speed_unitname);
        mDistance_unit = resources.getString(R.string.distance_unitname);
        mHeight_unit = resources.getString(R.string.distance_smallunitname);
    }

    private float getWidthPreference() {
        return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(mContext).getString
                ("units_implement_width", "12"));
    }

    public double conversionFromMeterAndMiliseconds(double meters, long miliseconds) {
        float seconds = miliseconds / 1000f;
        return conversionFromMetersPerSecond(meters / seconds);
    }

    public double conversionFromMetersPerSecond(double mps) {
        double speed = mps * mConversion_from_mps_to_speed;
        if (needsUnitFlip) // Flip from "x per hour" to "minutes per x"
        {
            if (speed > 1) // Nearly no speed return 0 as if there is no speed
            {
                speed = (1 / speed) * 60.0;
            } else {
                speed = 0;
            }
        }
        return speed;
    }

    public double conversionFromMeter(double meters) {
        double value = meters * mConversion_from_meter_to_distance;
        return value;
    }

    public double conversionFromLocalToMeters(double localizedValue) {
        double meters = localizedValue / mConversion_from_meter_to_distance;
        return meters;
    }

    public double conversionFromMeterToHeight(double meters) {
        return meters * mConversion_from_meter_to_height;
    }

    public String getDistanceUnit() {
        return mDistance_unit;
    }

    public String getHeightUnit() {
        return mHeight_unit;
    }

    public boolean isUnitFlipped() {
        return needsUnitFlip;
    }

    public void setUnitsChangeListener(UnitsChangeListener unitsChangeListener) {
        mListener = unitsChangeListener;
        if (mListener != null) {
            initBasedOnPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
            PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener
                    (mPreferenceListener);
        } else {
            PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener
                    (mPreferenceListener);
        }
    }

    /**
     * Format a speed using the current unit and flipping
     *
     * @param speed
     * @param decimals format a bit larger showing decimals or seconds
     * @return
     */
    public String formatSpeed(double speed, boolean decimals) {
        String speedText;
        if (mUnits == Constants.UNITS_METRICPACE || mUnits == Constants.UNITS_IMPERIALPACE) {
            if (decimals) {
                speedText = String.format("%02d %s",
                        (int) speed,
                        this.getSpeedUnit());
            } else {
                speedText = String.format("%02d:%02d %s",
                        (int) speed,
                        (int) ((speed - (int) speed) * 60), // convert decimal to seconds
                        this.getSpeedUnit());
            }
        } else {
            if (decimals) {
                speedText = String.format("%.2f %s", speed, this.getSpeedUnit());
            } else {
                speedText = String.format("%.0f %s", speed, this.getSpeedUnit());
            }

        }
        return speedText;
    }

    public String getSpeedUnit() {
        return mSpeed_unit;
    }

    /**
     * Interface definition for a callback to be invoked when the preference for
     * units changed.
     *
     * @author rene (c) Feb 14, 2010, Sogeti B.V.
     * @version $Id$
     */
    public interface UnitsChangeListener {
        /**
         * Called when the unit data has changed.
         */
        void onUnitsChange();
    }
}
