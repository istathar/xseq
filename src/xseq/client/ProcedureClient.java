/*
 * ProcedureClient.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005 Operational Dynamics
 */
package xseq.client;

import xseq.network.NetworkConnection;
import xseq.ui.ProcedureUserInterface;

/**
 * This will become the main class for running the client side of the xseq
 * system for Procedures. Right now, it holds the static instance of the user
 * interface so that other classes can find it as they do foward and backwards
 * callbacks.
 * 
 * @author Andrew Cowie
 */
public class ProcedureClient
{
	public static ProcedureUserInterface	ui		= null;

	public static NetworkConnection			net		= null;

	public final static String				VERSION	= "0.3.1";

	public static void main(String[] args) {
	}

	/**
	 * Go down hard. TODO Do we want to System.exit() here?
	 * 
	 * @param string
	 *            Message to display on abort
	 */
	public static void abort(String message) {
		System.err.println(message);
		if (ui != null) {
			ui.shutdown();
		}
	}

}
