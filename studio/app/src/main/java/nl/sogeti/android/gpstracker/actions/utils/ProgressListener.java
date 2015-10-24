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

import android.app.Activity;
import android.net.Uri;

/**
 * Interface to which a Activity / Context can conform to receive progress
 * updates from async tasks
 *
 * @author rene (c) May 29, 2011, Sogeti B.V.
 * @version $Id:$
 */
public interface ProgressListener
{
   void setIndeterminate(boolean indeterminate);

   /**
    * Signifies the start of background task, will be followed by setProgress(int) calls.
    */
   void started();

   /**
    * Set the progress on the scale of 0...10000
    *
    * @param value
    * @see Activity.setProgress
    * @see Window.PROGRESS_END
    */
   void setProgress(int value);

   /**
    * Signifies end of background task and the location of the result
    *
    * @param result
    */
   void finished(Uri result);

   void showError(String task, String errorMessage, Exception exception);
}
