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

import junit.framework.TestCase;

import org.w3c.dom.Document;

/**
 * Exercise the IdIndex class.
 * 
 * @author Andrew Cowie
 */
public class IdIndexTest extends TestCase
{
    String xml = null;

    Document dom = null;

    public void setUp() {
        xml = "<procedure>" + "<section>" + "<step>" + "<name who=\"joe\">" + "<task>Blah</task>"
                + "<task>Fee fi fo fum</task>" + "</name>" + "<name who=\"fred\">"
                + "<task>Bling</task>" + "<task>MoreBling</task>" + "</name>" + "</step>"
                + "<step><name who=\"scarlet\"><task>Jumping up and down</task></name></step>"
                + "</section>" + "</procedure>";

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
