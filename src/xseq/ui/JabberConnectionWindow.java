/*
 * JabberConnectionWindow.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2005 Operational Dynamics
 */
package xseq.ui;

import generic.util.Debug;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.ProtocolException;

import org.gnome.gdk.Event;
import org.gnome.gdk.Pixbuf;
import org.gnome.glade.Glade;
import org.gnome.glade.XML;
import org.gnome.gtk.Button;
import org.gnome.gtk.Label;
import org.gnome.gtk.MessageType;
import org.gnome.gtk.ProgressBar;
import org.gnome.gtk.Widget;
import org.gnome.gtk.Window;

import xseq.client.ProcedureClient;
import xseq.network.NetworkConnection;

/**
 * Establish a connection to the Jabber server. Unusual design concept - put
 * the UI and the Worker thread in the same class!
 * 
 * @author Andrew Cowie
 */
public class JabberConnectionWindow extends Thread
{

    /*
     * Cached widgets and UI elements
     */
    private XML _glade = null;

    Window _top = null;

    private ProgressBar _bar = null;

    private Button _cancel_button = null;

    private Label _details_label = null;

    /*
     * instance variables, for passing from this thread to Gtk main thread
     */
    private NetworkConnection _net = null;

    private Thread _worker = this;

    private boolean _stop = false;

    public JabberConnectionWindow(String username, String server, String password) {
        /*
         * give what will become the worker thread a name.
         */
        super("jabberconnection");

        /*
         * pass in the values we are going to evaluate so they can be tested
         * by this class's worker thread
         */
        _net = new NetworkConnection(username, server, password);

        try {
            _glade = Glade.parse("share/jabberconnection.glade", null);
        } catch (FileNotFoundException e) {
            // If it can't find that glade file, we have an app
            // configuration problem or worse some UI bug, and need to abort.
            e.printStackTrace();
            ProcedureClient.abort("Can't find glade file for JabberConnectionWindow.");
        } catch (Exception e) {
            e.printStackTrace();
            ProcedureClient.abort("An internal error occured trying to read and process the glade file for the JabberConnectionWindow.");
        }
        _top = (Window) _glade.getWidget("jabberconnection");
        _top.hide();

        /*
         * If the window is closed in advance of a connection being
         * successfully established, then terminate the worker thread. TODO
         */
        _top.connect(new Window.DELETE_EVENT() {
            public boolean onDeleteEvent(Widget source, Event event) {
                return false;
            }
        });
        _cancel_button = (Button) _glade.getWidget("cancel_button");
        _cancel_button.connect(new Button.CLICKED() {
            public void onClicked(Button source) {
                // _top.hide();
                // _top.destroy();
                Debug.print("listeners", "cancel button hit");
                _stop = true;
                _worker.interrupt();
            }
        });

        /*
         * Some boilerplate code to grab an image, and use it as icon.
         */
        Pixbuf jabber_pixbuf = null;
        try {
            jabber_pixbuf = new Pixbuf("share/pixmaps/jabber-48x48.png");
        } catch (Exception e) {
            e.printStackTrace();
            // TODO be nicer!
            System.exit(1);
        }
        _top.setIcon(jabber_pixbuf);

        /*
         * display the window,
         */

        _top.showAll();
        _top.present();

        /*
         * cache some lookups,
         */
        _bar = (ProgressBar) _glade.getWidget("connection_progressbar");
        _details_label = (Label) _glade.getWidget("details_label");

        /*
         * and launch the worker thread.
         */
        Debug.print("threads", "launching JabberConnection worker thread");
        _worker = this;
        _worker.start();

    }

    /**
     * Implenetation of Thread's required Runnable interface - this is the
     * Worker, so is outside the GUI code.
     */
    public void run() {
        this.setPriority(2);

        /* Let main UI catch up */
        try {
            Thread.sleep(20);
        } catch (InterruptedException ie) {
            //
        }

        try {
            progress(5, "Connecting to Jabber server...");
            _net.connect();
            progress(25, "Connected. Logging in...");

            _net.login();
            progress(60, "Authenticated. FIXME Now what?");

        } catch (ConnectException ce) {
            error("Couldn't establish a secure connection to the Jabber server.\n\n"
                    + "<i>Troubleshooting suggestions:</i>\n"
                    + "Did you specify the right server [domain]?\n"
                    + "If it's a local server, is it running?\n"
                    + "If it's a remote or public server, is your networking up?\n"
                    + "There's not a firewall blocking you, is there?");
            cancel();
            return;
        } catch (ProtocolException pe) {
            error("We were able to connect to the Jabber server you specified, but we weren't able to get a secure connection. xseq requires you use an SSL enabled Jabber server");
            cancel();
            return;
        } catch (IllegalArgumentException iae) {
            error("The supplied credentials didn't let us authenticate to the Jabber server. Check the username and password you supplied, and make sure you've got the right server");
            // TODO register?!?
            cancel();
            return;
        }
        done();
        return;
    }

    /*
     * The following methods are called from the worker thread which is
     * OUTSIDE the Gtk main thread, so any GUI updates must be done via
     * CustomEvents.
     */
    /**
     * Update the progress bar with an arbitrary 0-100 value representing the
     * percentage done.
     */
    private void progress(final int fraction, final String message) {
        Debug.print("threads", "(Worker) updating progress bar, " + fraction + "%");
        _bar.setFraction((double) fraction / 100);
        _details_label.setLabel("<i>" + message + "</i>");
    }

    private void error(final String details) {
        Debug.print("threads", "(Worker) launching error dialog");
        ModalDialog error = new ModalDialog("Unable to login to server", details, MessageType.ERROR);
        error.run();
    }

    private void cancel() {
        Debug.print("threads", "(Worker) cancelling");
        Debug.print("threads", "(GTK)    cancelling");
        _top.hide();
    }

    /**
     * Record that these parameters have successfully validated (and that we
     * have an open connection to the Jabber message pump). Then cause this
     * progress window to close.
     * 
     */
    private void done() {
        Debug.print("threads", "(Worker) done");
        _bar.setFraction(1.0);

        /*
         * Make the validated NetworkConnection the one held for use by the
         * rest of the application.
         */
        ProcedureClient.net = _net;

        /*
         * And, after a short interval, clost this window.
         */
        try {
            Thread.sleep(200);
        } catch (InterruptedException ie) {
            //
        }
        _top.hide();
        _top = null;
    }

    private void setStop() {
        _stop = true;
    }
}
