/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2004 Operational Dynamics
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

/**
 * Debug control (as used by the xseq program). Debugging output is synchronous
 * on purpose. It is frighteningly difficult to debug mutlithreaded applications,
 * and even tougher to diagnose problems in antonomous event driven GUI
 * programs. The price paid, however, is that actually writing the output will
 * block the program, and worse, if the output is going to a terminal which is
 * slow to scroll (ie gnome-terminal), then having excessive logging (ie that
 * which results in deubg statements being printed from any tight loop) will
 * destructively slow your program down.
 * 
 * <P>
 * So having gone on about the downside, here's the upside. You can activate
 * debugging at runtime with a command line argument. You can specify various
 * different tags ("groups") by which your logging can be selected.
 * 
 * <P>
 * You must initialize the debugging subsystem by calling Debug.init() early on
 * in your program. Then, for any group name you wish to have entries under, you
 * must Debug.register() which does some internal parsing. Debug.setProgname()
 * is a good idea.
 * 
 * @author Andrew Cowie
 */
public class Debug
{
	private static final String	DEBUG_SWITCH		= "--debug=";

	// Maps from strings to string lengths.
	private static ArrayList	activeGroups		= null;
	private static HashMap		groupText			= null;
	private static HashMap		groupSpaces			= null;
	private static String		progname			= "<progname not set>";

	/**
	 * The max width of the debug group output to screen. Groups names which are
	 * shorter will be blank padded so that the actual debug output is column
	 * aligned.
	 */
	public static final int		MAX_GROUP_LENGTH	= 10;
	private static boolean		debugOn				= false;
	private static boolean		allActive			= false;

	static {
		activeGroups = new ArrayList();
		groupText = new HashMap();
		groupSpaces = new HashMap();

		String env = System.getProperty("debug");

		init(env);
	}

	/**
	 * Parse the command line for a debug argument and initialize Debug
	 * subsystem if found. This calls {@link init(String)}to actually parse the
	 * value of the debug argument if one is found on the command line.
	 * 
	 * <P>
	 * Obviously there is a little duplication here, because without a doubt the
	 * calling program is going to have to parse the args, but that's life.
	 * 
	 * @param args
	 *            The command line args [ie from main()]
	 * @returns A new String[] containing the command line without the debug arg
	 *          that was parsed by this method.
	 */
	public static String[] init(String[] args) {
		ArrayList newargs = new ArrayList(args.length);

		for (int i = 0; i < args.length; i++) {

			if (args[i].startsWith(DEBUG_SWITCH)) {
				Debug.init(args[i].substring(DEBUG_SWITCH.length()));
			} else {
				newargs.add(args[i]);
			}
		}

		return (String[]) newargs.toArray(new String[0]);
	}

	/**
	 * Complete the initialization of the Debug system by parsing a string
	 * containing a list of the which groups of debug outout you want displayed.
	 * 
	 * <P>
	 * Comming up with a widely applicable way of initializing the debug system
	 * has proved tricky.
	 * 
	 * <P>
	 * This method is called from the static initializer of the Debug class
	 * [which picks up java property "debug" (which you can specify with a
	 * -Ddebug= option to a Java VM)]. It is also called by the other
	 * {@link Debug.init(String[])}constructor if you choose to use that to
	 * process the command line arguments. You can also call it explicitly on
	 * your own.
	 * 
	 * @param groups
	 *            A comma separated list of debug groups you wish active, or
	 *            "all" to activate all debug groups.
	 */
	public static void init(String groups) {
		if (groups == null) {
			return;
		}

		debugOn = true;

		int index = 0;
		ArrayList items = new ArrayList();

		while (index < groups.length()) {
			String chunk;
			int comma;

			if ((comma = groups.indexOf(",", index)) != -1) {
				chunk = groups.substring(index, comma);
				index = comma + 1;
			} else {
				chunk = groups.substring(index);
				index = groups.length();
			}
			items.add(chunk);
		}

		Iterator iter = items.iterator();
		while (iter.hasNext()) {
			String item = (String) iter.next();

			if (item.equals("all")) {
				allActive = true;
				return;
			} else {
				activeGroups.add(item);
			}
		}

	}

	/**
	 * Activate debugging on a given group. This is only for inline dubugging
	 * when coding.
	 * 
	 * @param group
	 *            the debug group to be activated
	 */
	public static void activate(String group) {
		activeGroups.add(group);
	}

	/**
	 * Print a debug message to console.
	 * 
	 * @param group
	 *            A string specifying the group this message belongs to. Make
	 *            sure you {@link activate}it if you want to see it!
	 * @param msg
	 *            The message to be output
	 */
	public static void print(String group, String msg) {
		if (debugOn == true) {
			if (allActive == true || activeGroups.contains(group)) {
				// if it's not actually registered, then there will be
				// no formatting information for it.
				if (groupText.get(group) == null) {
					System.out.println(progname
							+ ": PROGRAMMER ERROR: Debug.print was called with unregistered group \""
							+ group + "\", message \"" + msg + "\"");
				} else {
					System.out.println(progname + ": [" + groupText.get(group)
							+ "] " + groupSpaces.get(group) + msg);
				}
			}
		}
	}

	/**
	 * @param type
	 *            A string name for the group of debug messages to be activated
	 */
	public static void register(String group) {
		int len = MAX_GROUP_LENGTH - group.length();
		String truncatedGroupName = group.substring(0,
				(MAX_GROUP_LENGTH > group.length()
						? group.length()
				: MAX_GROUP_LENGTH));

		StringBuffer buf = new StringBuffer("");
		for (int i = 0; i < len; i++) {
			buf.append(" ");
		}

		groupText.put(group, truncatedGroupName);
		groupSpaces.put(group, buf.toString());
	}

	public static void setProgname(String progname) {
		Debug.progname = progname;
	}

}