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
package nl.sogeti.android.gpstracker.streaming;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.speech.tts.TextToSpeech;

import nl.sogeti.android.gpstracker.service.R;
import nl.sogeti.android.gpstracker.service.util.ExternalConstants;
import nl.sogeti.android.log.Log;

public class VoiceOver extends BroadcastReceiver implements TextToSpeech.OnInitListener {
    private static VoiceOver sVoiceOver = null;
    private TextToSpeech mTextToSpeech;
    private int mVoiceStatus = -1;
    private Context mContext;

    public VoiceOver(Context ctx) {
        mContext = ctx.getApplicationContext();
        mTextToSpeech = new TextToSpeech(mContext, this);
    }

    public static synchronized void initStreaming(Context ctx) {
        if (sVoiceOver != null) {
            shutdownStreaming(ctx);
        }
        sVoiceOver = new VoiceOver(ctx);

        IntentFilter filter = new IntentFilter(ExternalConstants.STREAM_BROADCAST);
        ctx.registerReceiver(sVoiceOver, filter);
    }

    public static synchronized void shutdownStreaming(Context ctx) {
        if (sVoiceOver != null) {
            ctx.unregisterReceiver(sVoiceOver);
            sVoiceOver.onShutdown();
            sVoiceOver = null;
        }
    }

    private void onShutdown() {
        mVoiceStatus = -1;
        mTextToSpeech.shutdown();
    }

    @Override
    public void onInit(int status) {
        mVoiceStatus = status;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mVoiceStatus == TextToSpeech.SUCCESS) {
            int meters = intent.getIntExtra(ExternalConstants.EXTRA_DISTANCE, 0);
            int minutes = intent.getIntExtra(ExternalConstants.EXTRA_TIME, 0);
            String myText = context.getString(R.string.voiceover_speaking, minutes, meters);
            mTextToSpeech.speak(myText, TextToSpeech.QUEUE_ADD, null);
        } else {
            Log.w(this, "Voice stream failed TTS not ready");
        }
    }
}