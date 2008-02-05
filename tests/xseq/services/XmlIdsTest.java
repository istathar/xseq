package xseq.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * TODO One sentance class summary. TODO Class description here.
 * 
 * @author Andrew Cowie
 */
public class XmlIdsTest extends TestCase
{
    // HARDCODE
    private static final String TEST_SOURCE_DIR = "tests/xseq/services";

    private static final String TEST_DEST_DIR = "tmp";

    private static final String TEST_XML_FILE = "simpleProcedure_v1_Example.xml";

    public static final String SOURCE_XML = TEST_SOURCE_DIR + "/" + TEST_XML_FILE;

    public static final String DEST_XML = TEST_DEST_DIR + "/" + TEST_XML_FILE;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(XmlIdsTest.class);
    }

    public void testAssignXmlIds() {
        String str = null;
        Document doc = null;

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

        doc = XmlUtils.xmlStringToDOM(str);

        // TODO do a verification that the document has type ID defined? In
        // fact, move this to XmlUtils.

        /*
         * now run through the Document's elements and assign (add) ID
         * attributes
         */

        XmlUtils.addIDs(doc);

        /*
         * I'd like a more isolated way to test this, as using the Index to
         * assess it introduces a second variant. For a preliminary check,
         * just count the ids, and check against the number of Elements.
         */

        NodeList list = doc.getElementsByTagName("*");
        int length = list.getLength();

        int count = 0;

        for (int i = 0; i < length; i++) {
            Element element = (Element) list.item(i);

            String id = element.getAttribute("id");

            if (!id.equals("")) {
                count++;
            }
        }
        assertEquals("The number of elements with IDs did not equal the number of elements!", length,
                count);

        // XmlUtils.debugPrintXml(doc);
    }

    public void testDocumentIndexConstructor() {
        /*
         * Setup
         */
        Document doc = null;
        try {
            String str = XmlUtils.fileToString(SOURCE_XML);
            doc = XmlUtils.xmlStringToDOM(str);
            XmlUtils.addIDs(doc);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        /*
         * The constructor we're testing in this Unit test
         */
        ElementIndex index = new ElementIndex(doc);

        /*
         * Evaluate the result.
         */
        HashSet set = new HashSet();
        int num = doc.getElementsByTagName("*").getLength();

        for (int i = 0; i < num; i++) {
            // According to DOM spec, ID is a String. Worse, according to the
            // xmllint program, ID must be alphanumeric, not just numeric!
            // HARDCODE so construct an index by the same scheme
            // used in XmlUtils.AddXmlIds()
            String id = "n" + i;

            Element element = index.getElementById(id);

            if (element == null) {
                fail("we asked for id " + id + " but got null back, indicating it wasn't actually there");
            }

            /*
             * the test is that we assume that each Element Object (Objects
             * are pointers) will only be returned once. So we stick the
             * returned Elements into the Set, and if any such add operation
             * returns true (indicating that object is already present), then
             * we fail out. Given the Map semantics underlying ElementIndex, I
             * can't imagine this happening anymore - it's an old test. In
             * fact, in view of the exception being thrown in the constructor
             * of ElementIndex, I think this is deprecated.
             */
            if (set.add(element) == false) {
                fail("an element already fetched by ID, " + id + ", was returned a second time");
            }
        }
        if (set.size() != num) {
            fail("in counting the IDs in the ElementIndex we didn't match the number of elements.");
        }

    }
}
