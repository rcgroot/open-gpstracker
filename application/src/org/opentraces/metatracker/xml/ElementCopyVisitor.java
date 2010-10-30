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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Makes deep copy of an Element for another Document.
 * $Id: ElementCopyVisitor.java,v 1.4 2003/01/06 00:23:49 just Exp $
 *
 * @author $Author: just $ - Just van den Broecke - Just Objects B.V. &copy;
 */
public class ElementCopyVisitor extends DefaultVisitor
{
	Document ownerDocument;
	Element elementOrig;
	Element elementCopy;
	Element elementPointer;
	int level = 0;

	public ElementCopyVisitor(Document theOwnerDocument, Element theElement)
	{
		ownerDocument = theOwnerDocument;
		elementOrig = theElement;
	}

	public Element getCopy()
	{
		new TreeWalker(this).traverse(elementOrig);
		return elementCopy;
	}

	public void visitDocumentPre(Document document)
	{
		p("visitDocumentPre:  level=" + level);
	}

	public void visitDocumentPost(Document document)
	{
		p("visitDocumentPost: level=" + level);
	}

	public void visitElementPre(Element element)
	{
		p("visitElementPre: " + element.getTagName() + " level=" + level);

		// Create the copy; must use target document as factory
		Element newElement = ownerDocument.createElement(element.getTagName());

		// If first time we need to create the copy
		if (elementCopy == null)
		{
			elementCopy = newElement;
		} else
		{
			elementPointer.appendChild(newElement);
		}

		// Always point to the last created and appended element
		elementPointer = newElement;
		level++;
	}

	public void visitElementPost(Element element)
	{
		p("visitElementPost: " + element.getTagName() + " level=" + level);
		DOMUtil.copyAttributes(element, elementPointer);
		level--;
		if (level == 0) return;
		// Always transfer attributes if any
		if (level > 0)
		{
			elementPointer = (Element) elementPointer.getParentNode();
		}
	}

	public void visitText(Text element)
	{
		// Create the copy; must use target document as factory
		Text newText = ownerDocument.createTextNode(element.getData());

		// If first time we need to create the copy
		if (elementPointer == null)
		{
			p("ERROR no element copy");
			return;
		} else
		{
			elementPointer.appendChild(newText);
		}
	}

	private void p(String s)
	{
		//System.out.println("ElementCopyVisitor: "+s);
	}
}

/*
 * $Log: ElementCopyVisitor.java,v $
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

