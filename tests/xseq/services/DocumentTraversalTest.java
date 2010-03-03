/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2005-2010 Operational Dynamics Consulting, Pty Ltd
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
import org.w3c.dom.Element;

/**
 * TODO One sentance class summary. TODO Class description here.
 * 
 * @author Andrew Cowie
 */
public class DocumentTraversalTest extends TestCase
{
    String xml = null;

    Document doc = null;

    ElementIndex index = null;

    public void setUp() {
        xml = "<procedure>" + "<section>" + "<step>" + "<name who=\"joe\">" + "<task>Blah</task>"
                + "<task>Fee fi fo fum</task>" + "</name>" + "</step>" + "</section>" + "</procedure>";

        doc = XmlUtils.xmlStringToDOM(xml);
        XmlUtils.addIDs(doc);
        // XmlUtils.debugPrintXml(doc);
        index = new ElementIndex(doc);
    }

    public void testGetParentElement() {

        Element secondTask = index.getElementById("n5");
        assertNotNull(secondTask);
        assertEquals("The test data changed - wrong tag", "task", secondTask.getTagName());
        assertEquals("The test data changed - wrong id", "n5", secondTask.getAttribute("id"));
        /*
         * Now test the getParentElement method
         */
        Element supposedParent = XmlUtils.getParentElement(secondTask);
        // should result in name node, n4

        assertEquals("n3", supposedParent.getAttribute("id"));
        assertEquals("name", supposedParent.getTagName());

        /*
         * Once more
         */
        Element nextSupposedParent = XmlUtils.getParentElement(supposedParent);
        assertEquals("step", nextSupposedParent.getTagName());
        /*
         * And now up to the top
         */
        Element nextSupposedParentSection = XmlUtils.getParentElement(nextSupposedParent);
        assertEquals("section", nextSupposedParentSection.getTagName());
        Element nextSupposedParentProcedure = XmlUtils.getParentElement(nextSupposedParentSection);
        assertEquals("procedure", nextSupposedParentProcedure.getTagName());

        Element shouldBeTop = XmlUtils.getParentElement(nextSupposedParentProcedure);
        assertNull(shouldBeTop);

    }

    public void testGetParentElementByName() {
        Element secondTask = index.getElementById("n5");

        /*
         * Check the two argument form of getParentElement()
         */
        Element target = XmlUtils.getParentElement(secondTask, "section");
        assertNotNull(target);
        assertEquals("n1", target.getAttribute("id"));
    }
}
