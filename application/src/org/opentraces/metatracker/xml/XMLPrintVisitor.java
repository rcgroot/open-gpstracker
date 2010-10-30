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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * XMLPrintVisitor implements the Visitor interface in the visitor design pattern for the
 * purpose of printing in HTML-like format the various DOM-Nodes.
 * <p>In HTML-like printing, only the following Nodes are printed:
 * <DL>
 * <DT>Document</DT>
 * <DD>Only the doctype provided on this constructor is written (i.e. no XML declaration, &lt;!DOCTYPE&gt;, or internal DTD).</DD>
 * <DT>Element</DT>
 * <DD>All element names are uppercased.</DD>
 * <DD>Empty elements are written as <code>&lt;BR&gt;</code> instead of <code>&lt;BR/&gt;</code>.</DD>
 * <DT>Attr</DT>
 * <DD>All attribute names are lowercased.</DD>
 * <DT>Text</DT>
 * </DL>
 * <p/>
 * <p>The following sample code uses the XMLPrintVisitor on a hierarchy of nodes:
 * <pre>
 * <p/>
 * PrintWriter printWriter = new PrintWriter();
 * Visitor htmlPrintVisitor = new XMLPrintVisitor(printWriter);
 * TreeWalker treeWalker = new TreeWalker(htmlPrintVisitor);
 * treeWalker.traverse(document);
 * printWriter.close();
 * <p/>
 * </pre>
 * <p/>
 * <P>By default, this doesn't print non-specified attributes.</P>
 *
 * @author $Author: just $ - Just van den Broecke - Just Objects B.V. &copy;
 * @version $Id: XMLPrintVisitor.java,v 1.8 2003/01/06 00:23:49 just Exp $
 * @see Visitor
 * @see TreeWalker
 */
public class XMLPrintVisitor implements Visitor
{
	protected Writer writer = null;

	protected int level = 0;
	protected String doctype = null;


	/**
	 * Constructor for customized encoding and doctype.
	 *
	 * @param writer   The character output stream to use.
	 * @param encoding Java character encoding in use by <VAR>writer</VAR>.
	 * @param doctype  String to be printed at the top of the document.
	 */
	public XMLPrintVisitor(Writer writer, String encoding, String doctype)
	{
		this.writer = writer;
		this.doctype = doctype;
		// this.isPrintNonSpecifiedAttributes = false;
	}

	/**
	 * Constructor for customized encoding.
	 *
	 * @param writer   The character output stream to use.
	 * @param encoding Java character encoding in use by <VAR>writer</VAR>.
	 */
	public XMLPrintVisitor(Writer writer, String encoding)
	{
		this(writer, encoding, null);
	}

	/**
	 * Constructor for default encoding.
	 *
	 * @param writer The character output stream to use.
	 */
	public XMLPrintVisitor(Writer writer)
	{
		this(writer, null, null);
	}

	/**
	 * Constructor for default encoding.
	 *
	 * @param fileName the filepath to write to
	 */
	public XMLPrintVisitor(String fileName)
	{
		try
		{
			writer = new FileWriter(fileName);
		} catch (IOException ioe)
		{
		}
	}

	/**
	 * Writes the <var>doctype</var> from the constructor (if any).
	 *
	 * @param document Node print as HTML.
	 */
	public void visitDocumentPre(Document document)
	{
	}

	/**
	 * Flush the writer.
	 *
	 * @param document Node to print as HTML.
	 */
	public void visitDocumentPost(Document document)
	{
		write("\n");
		flush();
	}

	/**
	 * Creates a formatted string representation of the start of the specified <var>element</var> Node
	 * and its associated attributes, and directs it to the print writer.
	 *
	 * @param element Node to print as XML.
	 */
	public void visitElementPre(Element element)
	{
		this.level++;
		write("<" + element.getTagName());
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++)
		{
			Attr attr = (Attr) attributes.item(i);
			visitAttributePre(attr);
		}
		write(">\n");
	}

	/**
	 * Creates a formatted string representation of the end of the specified <var>element</var> Node,
	 * and directs it to the print writer.
	 *
	 * @param element Node to print as XML.
	 */
	public void visitElementPost(Element element)
	{
		String tagName = element.getTagName();
		// if (element.hasChildNodes()) {
		write("</" + tagName + ">\n");
		// }
		level--;
	}

	/**
	 * Creates a formatted string representation of the specified <var>attribute</var> Node
	 * and its associated attributes, and directs it to the print writer.
	 * <p>Note that TXAttribute Nodes are not parsed into the document object hierarchy by the
	 * XML4J parser; attributes exist as part of a Element Node.
	 *
	 * @param attr attr to print.
	 */
	public void visitAttributePre(Attr attr)
	{
		write(" " + attr.getName() + "=\"" + attr.getValue() + "\"");
	}

	/**
	 * Creates a formatted string representation of the specified <var>text</var> Node,
	 * and directs it to the print writer.  CDATASections are respected.
	 *
	 * @param text Node to print with format.
	 */
	public void visitText(Text text)
	{
		if (this.level > 0)
		{
			write(text.getData());
		}
	}

	private void write(String s)
	{
		try
		{
			writer.write(s);
		} catch (IOException e)
		{
		}
	}

	private void flush()
	{
		try
		{
			writer.flush();
		} catch (IOException e)
		{
		}
	}
}

