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
package xseq.ui;

import generic.util.Debug;

import org.gnome.gtk.Gtk;

/**
 * Run a debug instance of OverviewWindow and DetailsWindow with some test
 * data. This is not batchable (in that the main loop requires user input),
 * and so it not part of the JUnit tests, and is named ...Runner.
 * 
 * @author Andrew Cowie
 */
public class TimerRunner
{

    public static void main(String[] args) {
        Debug.setProgname("timerrunner");
        Debug.register("main");
        Debug.register("events");
        Debug.register("listeners");
        Debug.register("threads");
        Debug.register("timers");
        Debug.register("jabber");

        args = Debug.init(args);
        Debug.print("main", "Starting TimerRunner");

        Debug.print("main", "initializing Gtk");
        Gtk.init(args);

        /*
         * test Jabber window
         */
        Debug.print("main", "creating JabberConnection");
        JabberConnectionWindow jcw = new JabberConnectionWindow("andrew", "localhost", "test");

        Debug.print("main", "starting Gtk main loop");
        Gtk.main();
    }
}
