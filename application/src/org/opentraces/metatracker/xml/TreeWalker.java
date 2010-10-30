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
 * This class does a pre-order walk of a DOM tree or Node, calling an ElementVisitor
 * interface as it goes.
 * <p>The numbered nodes in the trees below indicate the order of traversal given
 * the specified <code>startNode</code> of &quot;1&quot;.
 * <pre>
 *
 *                 1              x              x
 *                / \            / \            / \
 *               2   6          1   x          x   x
 *              /|\   \        /|\   \        /|\   \
 *             3 4 5   7      2 3 4   x      x 1 x   x
 *
 * </pre>
 * $Id: TreeWalker.java,v 1.5 2003/01/06 00:23:49 just Exp $
 *
 * @author $Author: just $ - Just van den Broecke - Just Objects B.V. &copy;
 */

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class TreeWalker
{
	private Visitor visitor;
	private Node topNode;

	/**
	 * Constructor.
	 *
	 * @param
	 */
	public TreeWalker(Visitor theVisitor)
	{
		visitor = theVisitor;
	}

	/**
	 * Disabled default constructor.
	 */
	private TreeWalker()
	{
	}

	/**
	 * Perform a pre-order traversal non-recursive style.
	 */
	public void traverse(Node node)
	{
		// Remember the top node
		if (topNode == null)
		{
			topNode = node;
		}

		while (node != null)
		{
			visitPre(node);

			Node nextNode = node.getFirstChild();
			while (nextNode == null)
			{
				visitPost(node);

				// We are ready after post-visiting the topnode
				if (node == topNode)
				{
					return;
				}

				try
				{
					nextNode = node.getNextSibling();
				}
				catch (IndexOutOfBoundsException e)
				{
					nextNode = null;
				}

				if (nextNode == null)
				{
					node = node.getParentNode();
					if (node == null)
					{
						nextNode = node;
						break;
					}
				}
			}
			node = nextNode;
		}
	}

	protected void visitPre(Node node)
	{
		switch (node.getNodeType())
		{
			case Node.DOCUMENT_NODE:
				visitor.visitDocumentPre((Document) node);
				break;
			case Node.ELEMENT_NODE:
				visitor.visitElementPre((Element) node);
				break;
			case Node.TEXT_NODE:
				visitor.visitText((Text) node);
				break;
			// Not yet
			case Node.ENTITY_REFERENCE_NODE:
				System.out.println("ENTITY_REFERENCE_NODE");
			default:
				break;
		}
	}

	protected void visitPost(Node node)
	{
		switch (node.getNodeType())
		{
			case Node.DOCUMENT_NODE:
				visitor.visitDocumentPost((Document) node);
				break;
			case Node.ELEMENT_NODE:
				visitor.visitElementPost((Element) node);
				break;
			case Node.TEXT_NODE:
				break;
			default:
				break;
		}
	}
}


/*
 * $Log: TreeWalker.java,v $
 * Revision 1.5  2003/01/06 00:23:49  just
 * moved devenv to linux
 *
 * Revision 1.4  2002/11/14 20:25:20  just
 * reformat of code only
 *
 * Revision 1.3  2000/08/10 19:26:58  just
 * changes for comments only
 *
 *
 */
