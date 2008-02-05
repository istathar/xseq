/*
 * WindowRunner.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005,2008 Operational Dynamics
 */
package xseq.ui;

import generic.util.Debug;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.gnome.gtk.Gtk;
import org.gnome.gtk.MessageType;
import org.gnome.gtk.Window;

import xseq.client.ProcedureClient;
import xseq.domain.Procedure;
import xseq.domain.State;
import xseq.services.XmlUtils;

/**
 * Run a debug instance of OverviewWindow and DetailsWindow with some test
 * data. This is not batchable (in that the main loop requires user input),
 * and so it not part of the JUnit tests, and is named ...Runner.
 * 
 * @author Andrew Cowie
 */
public class WindowRunner
{

    public static void main(String[] args) {
        Debug.setProgname("windowrunner");
        Debug.register("main");
        Debug.register("events");
        Debug.register("listeners");
        Debug.register("threads");
        Debug.register("jabber");

        args = Debug.init(args);
        Debug.print("main", "Starting WindowRunner");

        Debug.print("main", "initializing Gtk");
        Gtk.init(args);

        /*
         * get a procedure Document
         */
        Debug.print("main", "creating TestLoad");
        TestLoadWindow tl = new TestLoadWindow();

        /*
         * test Jabber window
         */
        // Debug.print("main", "creating JabberConfig");
        // JabberConfigWindow jc = new JabberConfigWindow();
        Debug.print("main", "starting Gtk main loop");
        Gtk.main();
        Debug.print("main", "returned from Gtk main loop");
    }

    public static void loadAndRun(String filename, Window parent) throws FileNotFoundException {
        Debug.print("main", "loading Procedure " + filename);
        String xml = null;
        try {
            xml = XmlUtils.fileToString(filename);
        } catch (FileNotFoundException fnfe) {
            /*
             * No big deal.
             */
            ModalDialog error = new ModalDialog("File not found", fnfe.getMessage() + "\nTry again?",
                    MessageType.WARNING);
            error.run();

            throw fnfe;
        } catch (IOException ioe) {
            /*
             * This is worse - something happened when trying to read. No
             * good.
             */
            ModalDialog error = new ModalDialog("I/O Error trying to read file", ioe.getMessage(),
                    MessageType.ERROR);
            error.run();
            Gtk.mainQuit();
            System.exit(1);
        }

        Procedure p = null;
        try {
            p = new Procedure(xml);
        } catch (IllegalArgumentException iae) {
            // StringBuffer buf = new StringBuffer(iae.getMessage());

            String msg = iae.getMessage();

            /*
             * Take some measures to defend against Pango seeing </tag> - it
             * makes a big mess if it does, thinking its unclosed Pango
             * markup.
             */
            msg = msg.replaceAll(">", "&gt;");
            msg = msg.replaceAll("<", "&lt;");

            ModalDialog error = new ModalDialog(
                    "Invalid Procedure",
                    msg
                            + "\n\n<i>You'll need to fix your document's XML before you can continue.</i> (By the way, this <b>is</b> an <tt>xseq</tt> Procedure, right?)",
                    MessageType.ERROR);
            error.run();
            Gtk.mainQuit();
            System.exit(1);
        }
        Debug.print("main", "creating UI");
        ProcedureClient.ui = new ProcedureUserInterface(p);

        Debug.print("main", "creating TestControl");
        TestControlWindow tc = new TestControlWindow(p);

        /*
         * set initial state. This may change... especially if black goes
         * away.
         */
        ProcedureClient.ui.setMyState(State.STANDBY);
    }
}
