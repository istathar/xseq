/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2004-2010 Operational Dynamics Consulting, Pty Ltd
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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.gnome.gtk.Dialog;
import org.gnome.gtk.ErrorMessageDialog;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.WarningMessageDialog;
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
        Dialog error;

        Debug.print("main", "loading Procedure " + filename);
        String xml = null;
        try {
            xml = XmlUtils.fileToString(filename);
        } catch (FileNotFoundException fnfe) {
            /*
             * No big deal.
             */
            error = new WarningMessageDialog(null, "File not found", fnfe.getMessage() + "\nTry again?");
            error.run();

            throw fnfe;
        } catch (IOException ioe) {
            /*
             * This is worse - something happened when trying to read. No
             * good.
             */
            error = new ErrorMessageDialog(null, "I/O Error trying to read file", ioe.getMessage());
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

            error = new ErrorMessageDialog(
                    null,
                    "Invalid Procedure",
                    msg
                            + "\n\n<i>You'll need to fix your document's XML before you can continue.</i> (By the way, this <b>is</b> an <tt>xseq</tt> Procedure, right?)");
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
