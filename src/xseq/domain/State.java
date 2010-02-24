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
package xseq.domain;

import generic.util.DebugException;

/**
 * The five states that a person (specifically their tasks) can be in. This
 * underlies the 5 couloured buttons managed by xseq.ui.StateButtons and used by
 * xseq.ui.DetailsWindow and xseq.ui.QuickButtons.
 * 
 * @author Andrew Cowie
 */
public class State
{
	private int				_current	= -1;

	/**
	 * black
	 */
	public static final int	STANDBY		= 0;
	/**
	 * green
	 */
	public static final int	DONE		= 1;
	/**
	 * blue
	 */
	public static final int	WORKING		= 2;
	/**
	 * yellow
	 */
	public static final int	PROBLEM		= 3;
	/**
	 * red
	 */
	public static final int	CRITICAL	= 4;
	public static final int	NUM_BUTTONS	= 5;

	public static String[]	colours		= new String[] {
			"black", "green", "blue", "yellow", "red"
										};

	public State(int colour) {
		set(colour);
	}

	public void set(int colour) {
		if ((colour >= 0) && (colour < NUM_BUTTONS)) {
			_current = colour;
		} else {
			throw new DebugException("an out-of-bounds colour index was passed to State's constructor");
		}
	}

	public int get() {
		return _current;
	}
}