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
package nl.sogeti.android.gpstracker.actions;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.integration.ServiceConstants;
import nl.sogeti.android.gpstracker.integration.ServiceManager;
import nl.sogeti.android.gpstracker.settings.Helper;
import nl.sogeti.android.log.Log;

/**
 * Empty Activity that pops up the dialog to name the track
 *
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 * @version $Id$
 */
public class ControlTracking extends AppCompatActivity {
    public static final String EXTRA_DEFAULT_NAME = "EXTRA_DEFAULT_NAME";
    private final View.OnClickListener mLoggingControlListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ControlTracking.this);
            int id = v.getId();
            Intent intent = new Intent();
            switch (id) {
                case R.id.logcontrol_start: {
                    int precision = Integer.valueOf(preferences.getString(Helper.PRECISION_PREFERENCE, Integer.toString(ServiceConstants.LOGGING_NORMAL)));
                    int interval = Integer.valueOf(preferences.getString(Helper.CUSTOMPRECISIONTIME_PREFERENCE, "1"));
                    float distance = Float.valueOf(preferences.getString(Helper.CUSTOMPRECISIONDISTANCE_PREFERENCE, "1"));
                    String trackName = null;
                    if (getIntent().getBooleanExtra(EXTRA_DEFAULT_NAME, false)) {
                        trackName = NameTrack.createDefaultTrackName(ControlTracking.this);
                    }
                    ServiceManager.startGPSLogging(ControlTracking.this, precision, interval, distance, trackName);
                    // Create data for the caller that a new track has been started
                    ComponentName caller = ControlTracking.this.getCallingActivity();
                    if (caller != null) {
                        setResult(RESULT_OK, intent);
                    }
                    break;
                }
                case R.id.logcontrol_pause:
                    ServiceManager.pauseGPSLogging(ControlTracking.this);
                    setResult(RESULT_OK, intent);
                    break;
                case R.id.logcontrol_resume: {
                    int precision = Integer.valueOf(preferences.getString(Helper.PRECISION_PREFERENCE, Integer.toString(ServiceConstants.LOGGING_NORMAL)));
                    int interval = Integer.valueOf(preferences.getString(Helper.CUSTOMPRECISIONTIME_PREFERENCE, "1"));
                    float distance = Float.valueOf(preferences.getString(Helper.CUSTOMPRECISIONDISTANCE_PREFERENCE, "1"));
                    ServiceManager.resumeGPSLogging(ControlTracking.this, precision, interval, distance);
                    setResult(RESULT_OK, intent);
                    break;
                }
                case R.id.logcontrol_stop:
                    ServiceManager.stopGPSLogging(ControlTracking.this);
                    setResult(RESULT_OK, intent);
                    break;
                default:
                    setResult(RESULT_CANCELED, intent);
                    break;
            }
            finish();
        }
    };
    private ServiceManager mLoggerServiceManager;
    private Button start;
    private Button pause;
    private Button resume;
    private Button stop;
    private boolean paused;
    private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            setResult(RESULT_CANCELED, new Intent());
            finish();
        }
    };
    private DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            if (!paused) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setVisible(false);
        paused = false;
        mLoggerServiceManager = new ServiceManager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoggerServiceManager.startup(this, new Runnable() {
            @Override
            public void run() {
                showDialog(0);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLoggerServiceManager.shutdown(this);
        paused = true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = getLayoutInflater();
        ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        View view = factory.inflate(R.layout.logcontrol, rootView, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_tracking_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(R.string.btn_cancel, mDialogClickListener)
                .setOnDismissListener(dismissListener)
                .setView(view);
        start = (Button) view.findViewById(R.id.logcontrol_start);
        pause = (Button) view.findViewById(R.id.logcontrol_pause);
        resume = (Button) view.findViewById(R.id.logcontrol_resume);
        stop = (Button) view.findViewById(R.id.logcontrol_stop);
        start.setOnClickListener(mLoggingControlListener);
        pause.setOnClickListener(mLoggingControlListener);
        resume.setOnClickListener(mLoggingControlListener);
        stop.setOnClickListener(mLoggingControlListener);

        return builder.create();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        updateDialogState(mLoggerServiceManager.getLoggingState());
    }


    private void updateDialogState(int state) {
        switch (state) {
            case ServiceConstants.STATE_STOPPED:
                start.setEnabled(true);
                pause.setEnabled(false);
                resume.setEnabled(false);
                stop.setEnabled(false);
                break;
            case ServiceConstants.STATE_LOGGING:
                start.setEnabled(false);
                pause.setEnabled(true);
                resume.setEnabled(false);
                stop.setEnabled(true);
                break;
            case ServiceConstants.STATE_PAUSED:
                start.setEnabled(false);
                pause.setEnabled(false);
                resume.setEnabled(true);
                stop.setEnabled(true);
                break;
            default:
                Log.w(this, String.format("State %d of logging, enabling and hope for the best....", state));
                start.setEnabled(false);
                pause.setEnabled(false);
                resume.setEnabled(false);
                stop.setEnabled(false);
                break;
        }
    }
}
