/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2004-2005 Operational Dynamics
 * Copyright © 2010 Operational Dynamics Consulting, Pty Ltd
 *
 * The code in this file, and the program it is a part of, is made available
 * to you by its authors as open source software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License version
 * 2 ("GPL") as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GPL for more details.
 *
 * You should have received a copy of the GPL along with this program. If not,
 * see http://www.gnu.org/licenses/. The authors of this program may be
 * contacted through http://research.operationaldynamics.com/projects/xseq/.
 */
package xseq.services;

import generic.util.AlphaNumberFormat;
import generic.util.DebugException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.darwinsys.RomanNumberFormat;

/**
 * This class manages the loading of an XML document, adding XML ID attributes,
 * and sibbling numbering.
 * <P>
 * Tools to parse XML blobs and map them across to the data model used by the
 * UI. While xseq is an XML centric system, the actual display to of the
 * Graphical User Interface of course has to be accomplished though the API
 * provided by whatever widget kit we are using, in this case, java-gnome.
 * 
 * @author Andrew Cowie
 */
public class XmlUtils
{

	/**
	 * No constructor at the moment - static methods... or, if it needs a
	 * distinct setup setup and then processing step, then it would become and
	 * instance [factory]
	 */

	/**
	 * 
	 * @param xml
	 *            A single String object containing the XML document to be
	 *            loaded and transformed into a Model
	 */
	public static Document xmlStringToDOM(String xml) {
		if (xml == null) {
			// self-defence.
			throw new NullPointerException("Why are you trying to create a model out of an empty source?");
		}

		InputSource source = new InputSource(new StringReader(xml));

		/*
		 * TODO FIXME. What a mess. This is necessary to give the parser a root
		 * where to look for relative paths from, ie, to find the DTDs as
		 * specified relatively in the XML. This is going to need a rather
		 * significant makeover once we make this thing installable.
		 * 
		 * Or could use System property "user.dir"
		 * 
		 * STUPIDITY WARNING - there is no reason (other than thread safety,
		 * which is an accidental consequence) to instantiate and configure the
		 * parser *every* bloody time this is called. If it ends up getting
		 * called a lot, then abstract out the parser setup to somewhere else.
		 */

		File cwd = new File("");

		source.setSystemId("file://" + cwd.getAbsolutePath() + "/");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false); // FIXME

		DocumentBuilder parser = null;
		Document dom = null;

		try {
			parser = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			// TODO - be nicer, but if the parser can't initialize, then that's
			// just plain bad, so stop.
			pce.printStackTrace();
			System.exit(1);
		}

		try {
			dom = parser.parse(source);
		} catch (SAXException se) {
			/*
			 * If we hit this exception, it in all likelihood means we had a
			 * basic parsing error - which could well mean that the thing we're
			 * parsing isn't XML.
			 */
			throw new IllegalArgumentException(se.getMessage());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			// TODO and do ... what?
		}

		return dom;
	}

	/**
	 * Read the contents of a file into a single Java String.
	 * 
	 * @param filename
	 *            The file to be read in
	 * @return a String containing the contents of filename
	 * @throws FileNotFoundException
	 *             and IOException on a read error. These are propegated rather
	 *             than simply trapped here bcause if reading in files is
	 *             somewhat critical to the app and if something goes wrong, the
	 *             app needs to take remedial action, probably with user
	 *             feedback.
	 */

	public static String fileToString(String filename) throws IOException, FileNotFoundException {

		BufferedReader in = new BufferedReader(new FileReader(filename));

		StringBuffer sb = new StringBuffer();
		String str = null;
		while ((str = in.readLine()) != null) {
			// oops, that looses newlines, so put 'em back in
			sb.append(str);
			sb.append('\n');
		}

		in.close();

		return sb.toString();
	}

	/**
	 * Add XML IDs to a Document. This method is one of the hearts of the xseq
	 * system. It runs through a DOM Document tree, and for each element found
	 * adds (updates) an ID attribute called "id".
	 * 
	 * <P>
	 * For now the value is a simple sequence.
	 * 
	 * @param doc
	 *            The DOM Document to which IDs are added
	 */
	public static void addIDs(Document doc) {
		// * means all elements
		NodeList nodelist = doc.getElementsByTagName("*");

		// does this need to be thread protected?

		for (int i = 0; i < nodelist.getLength(); i++) {
			Element element = (Element) nodelist.item(i);

			element.setAttribute("id", "n" + i);
		}
	}

	/**
	 * Add canonical numbers to sibbling nodes in a Document. Depricated, in
	 * fact - we moved the functionality (in one line) to xseq.domain.Procedure
	 * 
	 * @param element
	 *            DOM Element whose children will be processed.
	 * @param childName
	 *            the tag name of the child Element sibblingsto seek out.
	 * @param type
	 *            The numbering type {'1', 'i', 'I', 'a', 'A'}
	 */
	public static void addNumbers(Element element, String childName, int type) {
		NodeList nodelist = element.getElementsByTagName(childName);

		for (int i = 0; i < nodelist.getLength(); i++) {
			Element child = (Element) nodelist.item(i);

			child.setAttribute("num", indexToType(i + 1, type));
		}
	}

	/**
	 * Given a number, convert it to the canonical numbering form fo the
	 * specified type.
	 * 
	 * <UL>
	 * <LI>1,2,3,4,....
	 * <LI>i, ii, iii, iv, ...
	 * <LI>I, II, III, IV, ....
	 * <LI>a, b, c, ..., z, aa, ab, ..., az, ba, ...
	 * <LI>A, B, C, ..., Z, AA, AB, ..., AZ, BA, ...
	 * </UL>
	 * 
	 * @param index
	 *            The number to convert. Note that it must be a counting number,
	 *            ie, > 0.
	 * @param type
	 *            The numbering type; values are '1', 'i', 'I', 'a', or 'A'.
	 * @return the text representing the number.
	 *  
	 */
	public static String indexToType(int index, int type) {
		String str;

		if (index < 0) {
			throw new DebugException("numbering must begin at 1 (ie, there's no zero in a,b,c,... or i,ii,...)");
		}

		switch (type) {
		case '1':
			return new Integer(index).toString();

		case 'i':
		case 'I':
			RomanNumberFormat rf = new RomanNumberFormat();
			str = rf.format(index);
			if (type == 'i') {
				str = str.toLowerCase();
			}
			return str;

		case 'a':
		case 'A':
			AlphaNumberFormat af = new AlphaNumberFormat();
			str = af.format(index);
			if (type == 'A') {
				str = str.toUpperCase();
			}
			return str;

		default:
			throw new DebugException(
					"You need to call XmlUtils.indexToType with a single character as the type, one of 1,i,I,a,A");
		}
	}

	/**
	 * Clumsy method to output a DOM to stdout. This version uses the
	 * javax.xml.transform APIs. The two argument version is here so that output
	 * can be sent to a file for verification without cluttering up the screen
	 * during unit tests
	 */
	public static void debugPrintXml(Document dom, OutputStream out) {

		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = factory.newTransformer();
		} catch (TransformerConfigurationException tce) {
			// TODO - be nicer!
			tce.printStackTrace();
			System.exit(1);
		}

		DOMSource source = new DOMSource(dom);
		StreamResult result = new StreamResult(out);

		/*
		 * A little bit of API safety - I don't necessarily trust that all these
		 * abstract methods are actually implemented.
		 */
		if (source == null) {
			throw new DebugException("no source, doh!");
		}
		if (result == null) {
			throw new DebugException("no result, doh!");
		}
		if (transformer == null) {
			throw new DebugException("no trasnformer, doh!");
		}

		try {
			transformer.transform(source, result);
		} catch (TransformerException te) {
			// TODO - be nicer!
			te.printStackTrace();
			System.exit(1);
		}
	}

	public static void debugPrintXml(Document dom) {
		debugPrintXml(dom, System.out);
	}

	/**
	 * Pull the text out of a simple <element>text </element> XML construct.
	 * 
	 * @param element
	 *            The element from which to extract the text.
	 * @return The extracted text
	 */
	public static String getElementText(Element element) {

		Node child = element.getFirstChild();

		if (child.getNodeType() != Node.TEXT_NODE) {
			throw new DebugException("FIXME Not a Text Node");
		}

		String str = child.getNodeValue();
		if (str == null) {
			throw new DebugException("FIXME The Text Node was empty. (isn't that allowed?)");
		}
		return str;
	}

	/**
	 * Move up the DOM tree and return the parent Element of the given argument.
	 * This is essentially a convenience Element-only wrapper around the methods
	 * on the Node interface.
	 * 
	 * @param e
	 *            The element to look from.
	 * @return The element which is the parent of the argument, or null if we're
	 *         at the top of the tree.
	 */
	public static Element getParentElement(Element e) {
		Node parentNode = e.getParentNode();

		while (parentNode != null) {
			switch (parentNode.getNodeType()) {
			case Node.ELEMENT_NODE:
				return (Element) parentNode;
			case Node.DOCUMENT_NODE:

				return null;
			default:
				parentNode = parentNode.getParentNode();
			}
		}

		// if we make it this far, something very strange indeed. Note that this
		// is different than getting to the top without encountering a
		// DOCUMENT_NODE.
		throw new DebugException(
				"getParentElement()'s Node.getParent() returned null, meaning we somehow missed the top of the DOM tree");
	}

	/**
	 * Look up the DOM tree Elements for a particular parent (ie, rather than
	 * just hopping up one level, hop upwards multiple layers).
	 * 
	 * @param e
	 *            The element to start from
	 * @param tagName
	 *            The name of the tag to search upwards for
	 * @return the element up parent tree the with the tag name as specified, or
	 *         null if not found
	 */
	public static Element getParentElement(Element e, String tagName) {
		Element candidate = getParentElement(e);

		while (candidate != null) {
			if (candidate.getTagName().equals(tagName)) {
				return candidate;
			}
			candidate = getParentElement(candidate);
		}
		// we reached the top; not found.
		return null;
	}
}