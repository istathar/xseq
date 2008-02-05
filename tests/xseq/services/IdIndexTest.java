/*
 * IdIndexTest.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004 Operational Dynamics
 */
package xseq.services;

import junit.framework.TestCase;

import org.w3c.dom.Document;

/**
 * Exercise the IdIndex class.
 * 
 * @author Andrew Cowie
 */
public class IdIndexTest extends TestCase
{
	String		xml	= null;
	Document	dom	= null;

	public void setUp() {
		xml = "<procedure>" + "<section>" + "<step>" + "<name who=\"joe\">" + "<task>Blah</task>"
				+ "<task>Fee fi fo fum</task>" + "</name>" + "<name who=\"fred\">" + "<task>Bling</task>"
				+ "<task>MoreBling</task>" + "</name>" + "</step>"
				+ "<step><name who=\"scarlet\"><task>Jumping up and down</task></name></step>" + "</section>"
				+ "</procedure>";

		dom = XmlUtils.xmlStringToDOM(xml);
		XmlUtils.addIDs(dom);

	}

	public void testGetNextIdStep() {
		IdIndex index = new IdIndex(dom, "step");

		assertEquals("n9", index.getNextId("n2"));
		assertNull(index.getNextId("n9"));
	}

	public void testGetNextIdTask() {
		IdIndex index = new IdIndex(dom, "task");

		assertEquals("n5", index.getNextId("n4"));
		assertEquals("n7", index.getNextId("n5"));
		assertEquals("n8", index.getNextId("n7"));
		assertEquals("n11", index.getNextId("n8"));
		assertNull(index.getNextId("n11"));
	}
}