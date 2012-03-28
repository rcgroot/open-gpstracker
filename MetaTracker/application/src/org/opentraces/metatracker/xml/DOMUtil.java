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

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility methods for working with a DOM tree.
 * $Id: DOMUtil.java,v 1.18 2004/09/10 14:20:50 just Exp $
 *
 * @author $Author: just $ - Just van den Broecke - Just Objects B.V. &copy;
 */
public class DOMUtil
{


	/**
	 * Clears all childnodes in document
	 */
	public static void clearDocument(Document document)
	{
		NodeList nodeList = document.getChildNodes();
		if (nodeList == null)
		{
			return;
		}
		int len = nodeList.getLength();
		for (int i = 0; i < len; i++)
		{
			document.removeChild(nodeList.item(i));
		}
	}

	/**
	 * Create empty document TO BE DEBUGGED!.
	 */
	public static Document createDocument()
	{
		DocumentBuilder documentBuilder = null;
		//	System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		try
		{
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException pce)
		{
			warn("ParserConfigurationException: " + pce);
			return null;
		}

		return documentBuilder.newDocument();
	}

	/**
	 * Copies all attributes from one element to another in the official way.
	 */
	public static void copyAttributes(Element elementFrom, Element elementTo)
	{
		NamedNodeMap nodeList = elementFrom.getAttributes();
		if (nodeList == null)
		{
			// No attributes to copy: just return
			return;
		}

		Attr attrFrom = null;
		Attr attrTo = null;

		// Needed as factory to create attrs
		Document documentTo = elementTo.getOwnerDocument();
		int len = nodeList.getLength();

		// Copy each attr by making/setting a new one and
		// adding to the target element.
		for (int i = 0; i < len; i++)
		{
			attrFrom = (Attr) nodeList.item(i);

			// Create an set value
			attrTo = documentTo.createAttribute(attrFrom.getName());
			attrTo.setValue(attrFrom.getValue());

			// Set in target element
			elementTo.setAttributeNode(attrTo);
		}
	}


	public static Element getFirstElementByTagName(Document document, String tag)
	{
		// Get all elements matching the tagname
		NodeList nodeList = document.getElementsByTagName(tag);
		if (nodeList == null)
		{
			p("no list of elements with tag=" + tag);
			return null;
		}

		// Get the first if any.
		Element element = (Element) nodeList.item(0);
		if (element == null)
		{
			p("no element for tag=" + tag);
			return null;
		}
		return element;
	}

	public static Element getElementById(Document document, String id)
	{
		return getElementById(document.getDocumentElement(), id);
	}

	public static Element getElementById(Element element, String id)
	{
		return getElementById(element.getChildNodes(), id);
	}

	/**
	 * Get Element that has attribute id="xyz".
	 */
	public static Element getElementById(NodeList nodeList, String id)
	{
		// Note we should really use the Query here !!

		Element element = null;
		int len = nodeList.getLength();
		for (int i = 0; i < len; i++)
		{
			Node node = (Node) nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				element = (Element) node;
				if (((Element) node).getAttribute("id").equals(id))
				{
					// found it !
					break;
				}
			}
		}

		// returns found element or null
		return element;
	}

	public static Document parse(InputStream anInputStream)
	{
		Document document;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(anInputStream);

		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return document;
	}

	/**
	 * Prints an XML DOM.
	 */
	public static void printAsXML(Document document, PrintWriter printWriter)
	{
		new TreeWalker(new XMLPrintVisitor(printWriter)).traverse(document);
	}

	/**
	 * Prints an XML DOM.
	 */
	public static String dom2String(Document document)
	{
		StringWriter sw = new StringWriter();

		DOMUtil.printAsXML(document, new PrintWriter(sw));
		return sw.toString();
	}

	/**
	 * Replaces an element in document.
	 */
	public static void replaceElement(Element newElement, Element oldElement)
	{
		// Must be 1
		Node parent = oldElement.getParentNode();
		if (parent == null)
		{
			warn("replaceElement: no parent of oldElement found");
			return;
		}

		// Create a copy owned by the document
		ElementCopyVisitor ecv = new ElementCopyVisitor(oldElement.getOwnerDocument(), newElement);
		Element newElementCopy = ecv.getCopy();

		// Replace the old element with the new copy
		parent.replaceChild(newElementCopy, oldElement);
	}

	/**
	 * Write Document structure to XML file.
	 */
	static public void document2File(Document document, String fileName)
	{
		new TreeWalker(new XMLPrintVisitor(fileName)).traverse(document);
	}

	public static void warn(String s)
	{
		p("DOMUtil: WARNING " + s);
	}

	public static void p(String s)
	{
		// System.out.println("DOMUtil: "+s);
	}

}

/*
 * $Log: DOMUtil.java,v $
 * Revision 1.18  2004/09/10 14:20:50  just
 * expandIncludes() tab to 2 spaces
 *
 * Revision 1.17  2004/09/10 12:48:11  just
 * ok
 *
 * Revision 1.16  2003/01/06 00:23:49  just
 * moved devenv to linux
 *
 * Revision 1.15  2002/11/14 20:25:20  just
 * reformat of code only
 *
 * Revision 1.14  2002/06/18 10:30:02  just
 * no rel change
 *
 * Revision 1.13  2001/08/01 15:20:23  kstroke
 * fix for expand includes (added rootDir)
 *
 * Revision 1.12  2001/02/17 14:28:16  just
 * added comments and changed interface for expandIds()
 *
 * Revision 1.11  2000/12/09 14:35:35  just
 * added parse() method with optional DTD validation
 *
 * Revision 1.10  2000/09/21 22:37:20  just
 * removed print statements
 *
 * Revision 1.9  2000/08/28 00:07:46  just
 * changes for introduction of EntityResolverImpl
 *
 * Revision 1.8  2000/08/24 10:11:12  just
 * added XML file verfication
 *
 * Revision 1.7  2000/08/10 19:26:58  just
 * changes for comments only
 *
 *
 */

