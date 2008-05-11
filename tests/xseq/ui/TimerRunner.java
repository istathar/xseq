/*
 * WindowRunner.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2005,2008 Operational Dynamics
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
