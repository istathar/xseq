/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2005, 2008 Operational Dynamics
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
package xseq.ui;

import generic.util.Debug;
import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnome.gdk.Pixbuf;
import org.gnome.gtk.Image;
import org.gnome.gtk.ToggleButton;
import org.gnome.gtk.ToggleToolButton;
import org.gnome.gtk.VBox;
import org.gnome.gtk.Widget;
import org.gnome.gtk.Window;

import xseq.client.ProcedureClient;
import xseq.domain.State;

/**
 * An abstraction of the logic behind the apparent RadioAction of the five
 * coloured togglebuttons representing the state of a person's task(s). This
 * is intended for use by both the five ToggleButtons QuickButtonsWindow
 * (where the code originated) and the five ToggleToolBottons in
 * DetailsWindow.
 * 
 * <P>
 * Assumptions (requirements):
 * <UL>
 * <LI>That buttons are named in glade files under the convention <colour>_
 * <type>, such that colour matches one of the constants listed below, eg
 * red_togglebutton</LI>
 * </UL>
 * 
 * @author Andrew Cowie
 */
public class StateButtons implements ToggleButton.Toggled
{
    Window top = null;

    private ToggleButton[] buttons = null;

    private Image[] images = null;

    // The Image and Button widgets are most certainly instance variables, but
    // the two pixbuf sets only need to be loaded once, hence class variables.
    private static Pixbuf[] offPixbufs = null;

    private static Pixbuf[] onPixbufs = null;

    // button depressed
    private int current;

    private boolean setIcon = false;

    private String which = null;

    /**
     * @param which
     *            An artbitrary label about which set of StateButtons this is,
     *            used for debugging output.
     */
    public StateButtons(String which) {
        this.which = which;
        /*
         * initialize arrays
         */
        buttons = new ToggleButton[State.NUM_BUTTONS];
        images = new Image[State.NUM_BUTTONS];

        /*
         * Load the button images
         */
        // we don't do this in a static {...} block because we need Gtk
        // initialized.
        if (offPixbufs == null) {
            offPixbufs = new Pixbuf[State.NUM_BUTTONS];
            onPixbufs = new Pixbuf[State.NUM_BUTTONS];

            for (int i = 0; i < State.NUM_BUTTONS; i++) {

                try {
                    offPixbufs[i] = new Pixbuf("share/pixmaps/" + State.colours[i]
                            + "statebutton-solid.png");
                    onPixbufs[i] = new Pixbuf("share/pixmaps/" + State.colours[i]
                            + "statebutton-full.png");
                } catch (FileNotFoundException e1) {
                    ProcedureClient.abort("Can't find required image" + e1.getMessage());
                }
            }
        }

        /*
         * This means the first signal to come in will cause an activate
         * event.
         */
        current = -1;
    }

    /**
     * Shortcut to change state of appropriate widget class. setState ends up
     * generating a click/toggle signal, which is dealt with by the listener
     * below.
     */
    public void activate(int state) {
        /*
         * The nature of GTK is that multiple events can be generated for one
         * "action" as the user might conceive of it. More importantly, that
         * we have the event handler below call the thing in
         * ProcedureUserInterface that calls this results in a crazy cascade
         * of yet more events. Avoid this by returning if we're already set.
         */
        if (buttons[state].getActive()) {
            return;
        }

        /*
         * Need the buttons to be sensitive to be able to toggle them
         */
        // Debug.print("listeners", _which + " sensistive all buttons");
        for (int i = 0; i < State.NUM_BUTTONS; i++) {
            buttons[i].setSensitive(true);
        }

        /*
         * Toggle the button
         */
        // Debug.print("listeners", _which + " set " + state + " true");
        buttons[state].setActive(true);

        /*
         * Reset the sensitives
         */
        // Debug.print("listeners", _which + " reset sensistives");
        if (state == State.STANDBY) {
            buttons[0].setSensitive(true);
            for (int i = 1; i < State.NUM_BUTTONS; i++) {
                buttons[i].setSensitive(false);
            }
        } else {
            buttons[0].setSensitive(false);
            for (int i = 1; i < State.NUM_BUTTONS; i++) {
                buttons[i].setSensitive(true);
            }
        }
    }

    /**
     * Configure the ToggleButtons with their label graphics and their event
     * listeners.
     * <P>
     * You might think that this would be easier to do directly in glade, and
     * you'd be right, except for one problem: LibGlade is extraordinarily
     * picky about where it gets image files from. It can be done, but images
     * must be in the same directory as the glade files or in a child
     * directory thereof, or in an absolute path referenced location; this
     * puts tight constraints on where the files can be in a shipped
     * installation.
     * 
     * <P>
     * The assumption is made that the glade file specified a ToggleButton
     * with no children (Label or Image) set - the case in QuickButtonsWindow
     * 
     * @param button
     *            the ToggleButton to be directly parsed and added to the
     *            StateButton set
     * @param index
     *            the State colour index number, 0 .. State.NUM_BUTTONS-1
     */
    public void addToSet(ToggleButton button, int index) {
        if (button == null) {
            throw new DebugException("null ToggleButton passed in.");
        }
        /*
         * Store our reference to it
         */
        buttons[index] = button;

        /*
         * Construct the Image, with an initial pixbuf, and add it to the
         * ToggleButton
         */
        images[index] = new Image(offPixbufs[index]);

        button.add(images[index]);

        /*
         * Add the listener which contains the RadioAction type logic
         */
        button.connect(this);
    }

    /**
     * Add a ToggleToolButton with its graphics and event listeners. Note that
     * ToggleToolButton, as constructed by glade, contains a single Toggle
     * Button as a child, itself containing a VBox. We extract these in
     * sequence (using some very voodoo code) to get at the ToggleButton,
     * which we then hang onto a reference to, and then pass to the original
     * add(ToggleButton) method
     * 
     * <P>
     * The assumption is made that the glade file specified a ToggleButton
     * with no children (Label or Image) set.
     * 
     * @param button
     *            the ToggleButton to be directly parsed and added to the
     *            StateButton set
     * @param index
     *            the State colour index number, 0 .. State.NUM_BUTTONS-1
     */
    public void addToSet(ToggleToolButton button, int index) {
        if (button == null) {
            throw new DebugException("null ToggleToolButton passed in.");
        }

        /*
         * We descend into the ToggleToolButton to a) get the ToggleButton,
         * and then b) construct an Image widget and add it appropriately.
         */

        Widget w = button.getChild();
        System.out.println("DEBUG: w is a " + w.toString());

        ToggleButton tb = (ToggleButton) button.getChild();

        VBox vbox = (VBox) tb.getChild();

        /*
         * Our reference to the actual ToggleButton
         */
        buttons[index] = tb;

        /*
         * Constuct the Image, with an initial (off) pixbuf. Because
         * ToolButtons are implemented as composites with VBox( ; Label ), we
         * add to the VBox and push the Image to the top.
         */
        images[index] = new Image(offPixbufs[index]);

        vbox.packStart(images[index], true, true, 0);
        vbox.reorderChild(images[index], 0);

        /*
         * Now that _buttons and _index are set, add the listener to implement
         * the RadioAction behaviour.
         */
        tb.connect(this);

        // necessary to make anything actually appear in the rendered widgets.
        button.showAll();
    }

    /**
     * Implement the logic to emulate RadioAction behaviour between a the
     * group of ToggleButtons that this StateButtons object represents. This
     * method is what implements ToggleListener
     * 
     * <P>
     * This contains all the logic to manage switches from one state to
     * another. It's a bit complicated as one event will cause another event
     * in order to complete the sequence.
     */
    public void onToggled(ToggleButton source) {
        ToggleButton toggle = null;

        int index = -1;

        /*
         * Figure out which button the signal came from
         */
        for (int i = 0; i < State.NUM_BUTTONS; i++) {
            if (source == buttons[i]) {
                index = i;
                Debug.print("listeners", which + " " + State.colours[i]
                        + " received TOGGLE event, state now " + buttons[i].getActive() + ". I "
                        + (index == current ? "am" : "am not") + " current.");
            }
        }
        toggle = buttons[index];

        /*
         * If the toggle signal came from the currently active widget, then it
         * was in response to either an unselect request (which we are going
         * to emit a signal so we can ignore), that signal (in which case we
         * do nothing).
         * 
         * If a different widget is active, then we activate this button, and
         * send a signal to the active one to disengage.
         */
        if (index == current) {
            if (toggle.getActive() == false) {
                /*
                 * ie, if I got unclicked...
                 */
                Debug.print("listeners", which + " ...sending click to " + State.colours[index]
                        + " to counteract");
                // aka click(); generates TOGGLE signal
                toggle.setActive(true);
            } else {
                // stay activated; ignore
            }
        } else {
            if (toggle.getActive() == true) {
                /*
                 * ie, I'm not the current button, some other is, and I'm
                 * being told to activate.
                 */
                int old = current;
                current = index;
                /*
                 * Having changed the _current index deactivate
                 * [setState(false), which generates a click/toggle] the old
                 * button. Because it's no longer _current, it will respect
                 * the click[toggle] signal and deactivate, the other side of
                 * this if / else block.
                 */

                // (initial conditions safety check)
                if ((old >= 0) && (old < State.NUM_BUTTONS)) {
                    // aka click(); generates TOGGLE signal
                    buttons[old].setActive(false);
                }
                /*
                 * Then, when contol returns here, change my graphic to be
                 * "on".
                 */
                Debug.print("listeners", which + " " + State.colours[index]
                        + " ...activating with full image");
                images[index].setImage(onPixbufs[index]);

                /*
                 * And, for cosmetic fun, set the Window icon to the current
                 * graphic
                 */
                if (setIcon) {
                    top.setIcon(onPixbufs[index]);
                }

                /*
                 * Send the signal to any other StateButtons out there. (one
                 * will end up back here, and be ignored)
                 */
                Debug.print("events", which + " " + State.colours[index] + " calling ui.setMyState("
                        + index + ")");
                ProcedureClient.ui.setMyState(index);
            } else {
                Debug.print("listeners", which + " " + State.colours[index]
                        + "...deactivating with outline image");
                images[index].setImage(offPixbufs[index]);
            }
        }
    }

    /**
     * Tells StateButtons to set the Icon of the parent Window to the new
     * active image when the state toggles. This is here specifically for
     * QuickButtons; it's easier than trying to registering another Listener
     * within the class that uses StateButtons.
     */
    public void setIconOnActive(Window top) {
        if (top == null) {
            throw new DebugException("passed a null top Window object");
        }
        this.top = top;
        setIcon = true;
    }
}
