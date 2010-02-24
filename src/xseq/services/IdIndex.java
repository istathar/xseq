/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2005 Operational Dynamics
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

import generic.util.DebugException;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * An index allowing rapid lookup of the next element ID in a set of peers.
 * Frequently, we need to jump from one element to the next in the document.
 * Doing so strictly in the DOM tree is tricky as you have to deal with all the
 * permutations of hierarchies possible.
 * 
 * <P>
 * Implemented as two collections. A first is a hash going from ID String to an
 * array index number, which then is used to rapidly do lookups in the
 * sequential List of IDs, which then allows easy determination of the "next"
 * peer's ID.
 * 
 * <P>
 * This was necessary in part because "n9" comes before "n10", so Tree sorting
 * was no good.
 * 
 * @author Andrew Cowie
 */
public class IdIndex
{
	private HashMap		_idToArrayIndex	= null;
	private ArrayList	_arrayIndexToId	= null;

	/**
	 * Construct an index on all the nodes in a DOM document matching tagName.
	 */
	public IdIndex(Document doc, String tagName) {
		NodeList list = doc.getElementsByTagName(tagName);
		int length = list.getLength();

		if (length == 0) {
			throw new IllegalArgumentException("The DOM document being indexed doesn't have any " + tagName
					+ " elements.");
		}

		_idToArrayIndex = new HashMap(length, (float) 0.01);
		_arrayIndexToId = new ArrayList(length);

		for (int i = 0; i < length; i++) {
			Element e = (Element) list.item(i);
			String id = e.getAttribute("id");
			if (id == null) {
				throw new IllegalArgumentException("The DOM document being indexed lacks id tags on all elements");
			}

			_idToArrayIndex.put(id, new Integer(i));
			_arrayIndexToId.add(id);
		}
	}

	/**
	 * Get the ID string for the next peer element from the sequential index.
	 * 
	 * @param currentId
	 * @return
	 */

	public String getNextId(String currentId) {
		Integer index = (Integer) _idToArrayIndex.get(currentId);
		if (index == null) {
			throw new DebugException("Trying to lookup an id in an IdIndex that should be in its map, but isn't");
		}
		int i = index.intValue();

		/*
		 * Next. Should avoid IndexOutOfBoundsException.
		 */
		i++;

		if (i == _arrayIndexToId.size()) {
			return null;
		}

		String nextId;
		try {
			nextId = (String) _arrayIndexToId.get(i);
		} catch (IndexOutOfBoundsException ioobe) {
			throw new DebugException("Despite reasonable checks, still managed to ask for an array index out of bounds");
		}
		return nextId;
	}
}