/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2004-2005 Operational Dynamics
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
package generic.util;

/**
 * A simple exception to use to indicate the code did something unexpected while
 * being initially authored. Code should not ship with any DebugExceptions
 * present - if it's a real exception, then change it to something more
 * appropriate and trap it properly.
 * 
 * @author Andrew Cowie
 */
public class DebugException extends RuntimeException
{

	/**
	 *  
	 */
	public DebugException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public DebugException(String arg0) {
		super(arg0);
		warning();
	}

	/**
	 * @param arg0
	 */
	public DebugException(Throwable arg0) {
		super(arg0);
		warning();
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public DebugException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		warning();
	}

	private void warning() {
		System.err.println("Internal Note: This exception should not be present in a released version.");
		System.err.println("Its presence indicates a bug - both in the code itself and in this not being");
		System.err.println("a more sensible exception");
	}
}