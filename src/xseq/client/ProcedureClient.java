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
