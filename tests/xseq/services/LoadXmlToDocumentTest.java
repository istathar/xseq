/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2004-2010 Operational Dynamics Consulting, Pty Ltd
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TODO One sentance class summary. TODO Class description here.
 * 
 * @author Andrew Cowie
 */
public class LoadXmlToDocumentTest extends TestCase
{
    private static final String TEST_SOURCE_DIR = "tests/xseq/services";

    private static final String TEST_DEST_DIR = "tmp";

    private static final String TEST_XML_FILE = "simpleProcedure_v1_Example.xml";

    private static final String SOURCE_XML = TEST_SOURCE_DIR + "/" + TEST_XML_FILE;

    private static final String DEST_XML = TEST_DEST_DIR + "/" + TEST_XML_FILE;

    // in other words, we need doc/examples/simpleProcedure_v1_Example.xml to
    // exist

    public static void main(String[] args) {
        junit.textui.TestRunner.run(LoadXmlToDocumentTest.class);
    }

    /**
     * Test the fileToString utility method. This is a bit overdone, but it's
     * a useful test to make sure files are in place, etc. Also, note that it
     * will blow up if done a platform that doesn't have \n as newline. Ah,
     * Java code that won't work on Windows. Gotta love it.
     */
    public void testFileToString() {

        // the single try/catch block is a convenience here, but in real usage
        // we need to deal with these exceptions appropriately and separately

        try {
            String str = XmlUtils.fileToString(SOURCE_XML);

            BufferedWriter out = new BufferedWriter(new FileWriter(DEST_XML));
            out.write(str);
            out.close();

        } catch (FileNotFoundException e) {
            fail("file not found; " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail("not supposed to get IO errors reading files, but should this be fatal? Presumably not");
        }

        Process p = null;
        try {
            p = Runtime.getRuntime().exec("diff " + SOURCE_XML + " " + DEST_XML);
            p.waitFor();
        } catch (IOException e1) {
            fail("blew while trying to diff two xml files");
            e1.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            fail("We were interrupted. How naughty");
        }
        assertNotNull("didn't get a Process object back", p);

        assertEquals("Files didn't diff as the same", p.exitValue(), 0);
    }

    /**
     * Test loading an XML String to a DOM Document. This is the beginning of
     * the services layer -> UI layer transition, so it's important that it be
     * right.
     */
    public void testLoadXmlToDocument() {

        // On the assumption that we will be receiving our XML in string
        // blobs,
        // jump through the hoops to read a[n XML] file into a string.

        // the single try/catch block is a convenience here, but in real usage
        // we need to deal with these exceptions appropriately

        String str = null;
        try {
            str = XmlUtils.fileToString(SOURCE_XML);
        } catch (FileNotFoundException e) {
            fail("file not found; " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail("not supposed to get IO errors reading files");
        }

        if (str == null) {
            // in real life, we would throw a real Exception?
            fail("failed to actually read anything! Empty file? Bad!");
        }

        Document dom = null;

        dom = XmlUtils.xmlStringToDOM(str);
        // exceptions?

        // Do programatic tests on the Document to see if it matches

        Node declaration = dom.getFirstChild();
        assertEquals("first node isn't a document type declaration. ", declaration.getNodeType(),
                Node.DOCUMENT_TYPE_NODE);
        assertEquals(
                "document type declaration isn't procedure (you are using the right test data, right?) ",
                declaration.getNodeName(), "procedure");

        // * means all elements
        NodeList nodelist = dom.getElementsByTagName("*");

        Element procedure = (Element) nodelist.item(0);
        assertEquals("first node of NodeList isn't an element ", procedure.getNodeType(),
                Node.ELEMENT_NODE);
        assertEquals("first element isn't <procedure> as expected ", procedure.getNodeName(),
                "procedure");

        Element title = (Element) nodelist.item(1);
        assertEquals("next element of NodeList isn't an element ", title.getNodeType(),
                Node.ELEMENT_NODE);
        assertEquals("next element of NodeList isn't a <title> ", title.getNodeName(), "title");
        // and so on. Good enough.
    }
}
