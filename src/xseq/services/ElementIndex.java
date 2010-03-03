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

import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Construct and maintain the index for lookups between IDs and DOM Elements.
 * String IDs are the heart of the xseq system. This class builds and
 * maintains a [admittely simple] lookup table between these IDs and the DOM
 * Elements they point to.
 * 
 * <P>
 * Note that
 * 
 * <PRE>
 * 
 * Element element = doc.getElementById(id);
 * 
 * </PRE>
 * 
 * doesn't work worth a damn. This seems to always return null; I poked
 * through the sources, and I've got a hunch that it may well be unimplemented
 * - they may be trapping an excpetion internally.
 * 
 * So, instead, we will manually implement a search care of this class, which
 * we were going to end up needing anyway.
 * 
 * At the moment there is no <I>need </I> to extend this from common Index
 * supertype, but the various Indexes do follow the same form and essentially
 * the same algorithms, so if I can abstract it I will.
 * 
 * @author Andrew Cowie
 */
public class ElementIndex
{
    private HashMap _map = null;

    /**
     * Construct a new index. Note that (at the moment) the index is NOT
     * automatically rebuilt if nodes (elements) are addded to the DOM tree
     * further on in the runtime of the program. FUTURE.
     * 
     * @param doc
     *            a DOM Document in which the id attributes were already
     *            present or were added by the routines in XmlUtils.
     */
    public ElementIndex(Document doc) {
        this(doc, "*");
    }

    /**
     * Construct an index on all the nodes in a DOM document matching tagName.
     * 
     * @param doc
     * @param tagName
     */
    public ElementIndex(Document doc, String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException(
                    "if the two argument form of ElementIndex's constructor is used, the second argument must be the tag name of an Element to index on");
        }
        NodeList list = doc.getElementsByTagName(tagName);
        int length = list.getLength();

        /*
         * since we know the number of elements we're dealing with, and since
         * we (at the moment) aren't going past this, we choose a very low
         * load factor to improve performance.
         */
        _map = new HashMap(length, (float) 0.01);

        for (int i = 0; i < length; i++) {
            Element element = (Element) list.item(i);
            String id = element.getAttribute("id");

            // getAttribute returns an empty string if not present; if that's
            // the case, then we don't want it in the Index.
            if (id.equals("")) {
                continue;
            }

            // if the map already has this ID value, then that's bad; blow an
            // exception.
            if (_map.containsKey(id)) {
                throw new IllegalArgumentException(
                        "As we were building an index of XML IDs, we encountered an Element with an ID attribute which was already in the index. IDs must be unique!");
            }
            _map.put(id, element);
        }
    }

    /**
     * Look up an Element in the DOM tree by ID. Use the familiar (in the DOM
     * sense) name for this method, even though really we're just wrapping
     * Collection Map methods.
     * 
     * @param id
     *            the identifier to lookup in the Index
     * @return the associated element, or null if not present.
     */
    public Element getElementById(String id) {
        // get() returns null if the key doesn't match a value, which is fine.
        return (Element) _map.get(id);
    }
}
