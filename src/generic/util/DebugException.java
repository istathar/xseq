/*
 * DebugException.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005 Operational Dynamics
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