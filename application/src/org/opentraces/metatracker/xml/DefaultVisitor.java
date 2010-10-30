/*
 * Copyright (C) 2010  Just Objects B.V.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentraces.metatracker.xml;

/**
 * Implements a Visitor that does nothin'
 * $Id: DefaultVisitor.java,v 1.4 2003/01/06 00:23:49 just Exp $
 *
 * @author $Author: just $ - Just van den Broecke - Just Objects B.V. &copy;
 */

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class DefaultVisitor implements Visitor
{
	public void visitDocumentPre(Document document)
	{
	}

	public void visitDocumentPost(Document document)
	{
	}

	public void visitElementPre(Element element)
	{
	}

	public void visitElementPost(Element element)
	{
	}

	public void visitText(Text element)
	{
	}
}

/*
 * $Log: DefaultVisitor.java,v $
 * Revision 1.4  2003/01/06 00:23:49  just
 * moved devenv to linux
 *
 * Revision 1.3  2002/11/14 20:25:20  just
 * reformat of code only
 *
 * Revision 1.2  2000/08/10 19:26:58  just
 * changes for comments only
 *
 *
 */

