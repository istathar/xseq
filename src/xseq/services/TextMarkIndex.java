/*
 * TextMarkIndex.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004 Operational Dynamics
 */
package xseq.services;

import generic.util.DebugException;

import java.util.HashMap;

import org.gnu.gtk.TextMark;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Construct and maintain the index for lookups between IDs and the TextTags
 * which surround that Elements as rendered in the UI. At the moment there is no
 * need to extend this from common Index supertype, although this code does
 * derive from ElementIndex.
 * 
 * @author Andrew Cowie
 */
public class TextMarkIndex
{
	private HashMap	_map	= null;

	/**
	 * Construct a new index. Unlike ElementIndex, this constructor initializes
	 * the backing Collection (a Map) to a proper size, but it remains empty; a
	 * DetailsWindow adds Marks to the Index as it creates the TextBuffer from a
	 * Section. It is anticipated that there will be one index for each of step,
	 * name and task, so that, dpending on events received, different parts of a
	 * display can be affected.
	 * <P>
	 * Note that (at the moment) the index is NOT automatically rebuilt if nodes
	 * (elements) are addded to the DOM tree, Display buffers, or anything else.
	 * FUTURE.
	 * 
	 * @param doc
	 *            The DOM document to scan
	 * @param elementName
	 *            elements to look for. This does not populate the index; it
	 *            merely sizes the backing store appropriately.
	 */
	public TextMarkIndex(Document doc, String elementName) {
		NodeList list = doc.getElementsByTagName(elementName);
		int length = list.getLength();
		if (length == 0) {
			throw new DebugException("You managed to try and build an index on " + elementName
					+ " elements, but there aren't any");
		}
		/*
		 * since we know the number of elements we're dealing with, and since we
		 * (at the moment) aren't going past this, we choose a very low load
		 * factor to improve performance.
		 */
		_map = new HashMap(length, (float) 0.01);
	}

	public void addMarks(String id, TextMark startMark, TextMark endMark) {
		// safety checks: we don't want empty elements in the Index.
		if (id.equals("")) {
			throw new IllegalArgumentException("Attempt to add an entry for an ID attribute which was empty");
		}
		if (_map.containsKey(id)) {
			throw new IllegalArgumentException(
					"Attempt to add an entry for an ID attribute which was already in the index. IDs must be unique!");
		}
		if (startMark == null) {
			throw new DebugException("You need to specifiy a non-null starting mark.");
		}
		// end marks can be null

		MarkPair pair = new MarkPair(startMark, endMark);
		_map.put(id, pair);
	}

	/**
	 * Look up a TextMark by ID. Once you get the mark out, you'll need to make
	 * sure that whatever TextBuffer it is in is on screen, etc.
	 * 
	 * @param id
	 *            the identifier to lookup in the Index
	 * @return the associated element, or null if not present.
	 */
	public TextMark getStartMarkById(String id) {
		MarkPair pair = (MarkPair) _map.get(id);
		// get() returns null if the key doesn't match a value.
		if (pair == null) {
			throw new DebugException("you asked for a Mark [pair] by index for which there is no entry");
		}
		return pair._start;
	}

	public TextMark getEndMarkById(String id) {
		MarkPair pair = (MarkPair) _map.get(id);
		// get() returns null if the key doesn't match a value.
		if (pair == null) {
			throw new DebugException("you asked for a Mark [pair] by index for which there is no entry");
		}
		return pair._end;
	}

}

/**
 * A small structure to hold pairs of marks in the index. There's no need to
 * expose this.
 */

class MarkPair
{
	TextMark	_start;
	TextMark	_end;

	MarkPair(TextMark startMark, TextMark endMark) {
		this._start = startMark;
		this._end = endMark;
	}
}