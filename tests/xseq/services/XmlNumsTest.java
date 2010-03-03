package xseq.services;
 * XML Sequences for mission critical IT procedures
 *

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
public class XmlNumsTest extends TestCase
{
	Document	doc	= null;

	public static void main(String[] args) {
		junit.textui.TestRunner.run(XmlNumsTest.class);
	}

	//	protected void setUp() throws Exception {
	//		super.setUp();
	//	}

	public void testIndexToType() {
		String str;

		str = XmlUtils.indexToType(3, '1');
		assertEquals("'1' test failed", "3", str);

		str = XmlUtils.indexToType(9, 'I');
		assertEquals("'I' test failed", "IX", str);

		str = XmlUtils.indexToType(24, 'i');
		assertEquals("'i' test failed", "xxiv", str);

		str = XmlUtils.indexToType(1, 'a');
		assertEquals("'i' test failed", "a", str);

		str = XmlUtils.indexToType(53, 'a');
		assertEquals("'i' test failed", "ba", str);

		str = XmlUtils.indexToType(52, 'A');
		assertEquals("'i' test failed", "AZ", str);

		str = XmlUtils.indexToType(400, 'A');
		assertEquals("'i' test failed", "OJ", str);
	}

	public void testAddNumbers() {
		/*
		 * basic test
		 */
		String xml = "<step>" + "<task>" + "</task>" + "<task>" + "</task>" + "</step>";

		doc = XmlUtils.xmlStringToDOM(xml);
		NodeList nodes = doc.getElementsByTagName("step");
		Element element = (Element) nodes.item(0);

		XmlUtils.addNumbers(element, "task", '1');

		NodeList tasks = doc.getElementsByTagName("task");
		for (int i = 0; i < tasks.getLength(); i++) {
			Element task = (Element) tasks.item(i);
			assertEquals("node's num attribute is wrong", Integer.toString(i + 1), task.getAttribute("num"));
		}
	}

	public void testAssignXmlNums() {
		String xml = null;
		try {
			xml = XmlUtils.fileToString(xseq.services.XmlIdsTest.SOURCE_XML);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Couldn't get the test procedure");
		}

		/*
		 * The constructor of Procedure exercises the same logic as
		 * addNumbers(), although in fact doesn't use it and simply calls
		 * indexToType() directly. It runs through the Document's elements and
		 * assign (add) number attributes to peer sibblings.
		 *  
		 */

		Procedure p = new Procedure(xml);

		/*
		 * Now evaluate the result.
		 */
		doc = p.getDOM();

		/*
		 * Before doing the tests, spit the processed XML file out to the tmp
		 * directory so it can be examined later (and so it doesn't clutter up
		 * the otherwise clean JUnit output).
		 */
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(XmlIdsTest.DEST_XML + "_with-IDs+nums");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			fail("couldn't open file to output XML file iwth IDs and Numbers");
		}
		XmlUtils.debugPrintXml(doc, fos);

		/*
		 * NOW evaluate the result :)
		 */
		NodeList list = doc.getElementsByTagName("section");
		int length = list.getLength();
		int count = 0;

		for (int i = 0; i < length; i++) {
			Element element = (Element) list.item(i);

			String num = element.getAttribute("num");
			if (!num.equals("")) {
				count++;
			}
		}
		assertEquals("The number of sections with num attributes did not match the number of <section> elements!",
				length, count);
	}
}