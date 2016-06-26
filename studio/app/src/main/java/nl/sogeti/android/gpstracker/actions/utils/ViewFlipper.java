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

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import timber.log.Timber;

/**
 * Work around based on input from the comment section of
 * <a href="http://code.google.com/p/android/issues/detail?can=2&q=6191&colspec=ID%20Type%20Status%20Owner%20Summary
 * %20Stars&id=6191">Issue 6191</a>
 *
 * @author rene (c) May 8, 2010, Sogeti B.V.
 * @version $Id$
 */
public class ViewFlipper extends android.widget.ViewFlipper {
    public ViewFlipper(Context context) {
        super(context);
    }

    public ViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * On api level 7 unexpected exception occur during orientation switching.
     * These are java.lang.IllegalArgumentException: Receiver not registered: android.widget.ViewFlipper$id
     * exceptions. On level 7, 8 and 9 devices these are ignored.
     */
    @Override
    protected void onDetachedFromWindow() {
        if (Build.VERSION.SDK_INT > 7) {
            try {
                super.onDetachedFromWindow();
            } catch (IllegalArgumentException e) {
                Timber.w("Android project issue 6191 workaround.");
            /* Quick catch and continue on api level 7+, the Eclair 2.1 / 2.2 */
            } finally {
                super.stopFlipping();
            }
        } else {
            super.onDetachedFromWindow();
        }
    }
}
