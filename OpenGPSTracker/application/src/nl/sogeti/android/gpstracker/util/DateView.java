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

import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * An implementation for the XML element DateView that alters the textview in the 
 * formating of the text when displaying a date in ms from 1970.
 *
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class DateView extends TextView
{
   private Date mDate;

   /**
    * Constructor: create a new DateView.
    * @param context
    */
   public DateView(Context context)
   {
      super( context );
   }

   /**
    * Constructor: create a new DateView.
    * @param context
    * @param attrs
    */
   public DateView(Context context, AttributeSet attrs)
   {
      super( context, attrs );
   }

   /**
    * Constructor: create a new DateView.
    * @param context
    * @param attrs
    * @param defStyle
    */
   public DateView(Context context, AttributeSet attrs, int defStyle)
   {
      super( context, attrs, defStyle );
   }

   /*
    * (non-Javadoc)
    * @see android.widget.TextView#setText(java.lang.CharSequence, android.widget.TextView.BufferType)
    */
   @Override
   public void setText( CharSequence charSeq, BufferType type )
   {  
      // Behavior for the graphical editor
      if( this.isInEditMode() )
      {
         super.setText( charSeq, type );
         return;
      }
      
      
      long longVal;
      if( charSeq.length() == 0 ) 
      {
         longVal = 0l ;
      }
      else 
      {
         try 
         {
            longVal = Long.parseLong(charSeq.toString()) ;
         }
         catch(NumberFormatException e) 
         {
            longVal = 0l;
         }
      }
      this.mDate = new Date( longVal );
      
      DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(this.getContext().getApplicationContext());
      DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this.getContext().getApplicationContext());
      String text = timeFormat.format(this.mDate) + " " + dateFormat.format(mDate);
      super.setText( text, type );
   }

}
