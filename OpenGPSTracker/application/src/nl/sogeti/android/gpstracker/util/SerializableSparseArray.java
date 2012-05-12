/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) May 5, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.util;

import java.io.Serializable;

import android.util.SparseArray;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) May 5, 2012, Sogeti B.V.
 * @param <E>
 */
public class SerializableSparseArray<E> extends SparseArray<E> implements Serializable, Cloneable
{

   /**
    * <code>serialVersionUID</code> indicates/is used for.
    */
   private static final long serialVersionUID = 5388757581035942403L;
 
   @Override
   public SerializableSparseArray<E> clone()
   {
      return (SerializableSparseArray<E>) super.clone();
   }
}
