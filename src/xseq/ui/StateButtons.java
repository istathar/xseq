/*
 * StateButtons.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2005 Operational Dynamics
 */
package xseq.ui;

import generic.util.Debug;
import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnu.gdk.Pixbuf;
import org.gnu.glib.JGException;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.ToggleButton;
import org.gnu.gtk.ToggleToolButton;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;

import xseq.client.ProcedureClient;
import xseq.domain.State;

/**
 * An abstraction of the logic behind the apparent RadioAction of the five
 * coloured togglebuttons representing the state of a person's task(s). This is
 * intended for use by both the five ToggleButtons QuickButtonsWindow (where the
 * code originated) and the five ToggleToolBottons in DetailsWindow.
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
public class StateButtons implements ToggleListener
{
	Window					_top		= null;

	private ToggleButton[]	_buttons	= null;
	private Image[]			_images		= null;

	// The Image and Button widgets are most certainly instance variables, but
	// the two pixbuf sets only need to be loaded once, hence class variables.
	private static Pixbuf[]	_offPixbufs	= null;
	private static Pixbuf[]	_onPixbufs	= null;

	// button depressed
	private int				_current;
	private boolean			_setIcon	= false;

	private String			_which		= null;

	/**
	 * @param which
	 *            An artbitrary label about which set of StateButtons this is,
	 *            used for debugging output.
	 */
	public StateButtons(String which) {
		this._which = which;
		/*
		 * initialize arrays
		 */
		_buttons = new ToggleButton[State.NUM_BUTTONS];
		_images = new Image[State.NUM_BUTTONS];

		/*
		 * Load the button images
		 */
		// we don't do this in a static {...} block because we need Gtk
		// initialized.
		if (_offPixbufs == null) {
			_offPixbufs = new Pixbuf[State.NUM_BUTTONS];
			_onPixbufs = new Pixbuf[State.NUM_BUTTONS];

			for (int i = 0; i < State.NUM_BUTTONS; i++) {

				try {
					_offPixbufs[i] = new Pixbuf("share/pixmaps/" + State.colours[i] + "statebutton-solid.png");
					_onPixbufs[i] = new Pixbuf("share/pixmaps/" + State.colours[i] + "statebutton-full.png");
				} catch (FileNotFoundException e1) {
					ProcedureClient.abort("Can't find required image" + e1.getMessage());
				} catch (JGException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		/*
		 * This means the first signal to come in will cause an activate event.
		 */
		_current = -1;
	}

	/**
	 * Shortcut to change state of appropriate widget class. setState ends up
	 * generating a click/toggle signal, which is dealt with by the listener
	 * below.
	 */
	public void activate(int state) {
		/*
		 * The nature of GTK is that multiple events can be generated for one
		 * "action" as the user might conceive of it. More importantly, that we
		 * have the event handler below call the thing in ProcedureUserInterface
		 * that calls this results in a crazy cascade of yet more events. Avoid
		 * this by returning if we're already set.
		 */
		if (_buttons[state].getState()) {
			return;
		}

		/*
		 * Need the buttons to be sensitive to be able to toggle them
		 */
		// Debug.print("listeners", _which + " sensistive all buttons");
		for (int i = 0; i < State.NUM_BUTTONS; i++) {
			_buttons[i].setSensitive(true);
		}

		/*
		 * Toggle the button
		 */
		// Debug.print("listeners", _which + " set " + state + " true");
		_buttons[state].setState(true);

		/*
		 * Reset the sensitives
		 */
		// Debug.print("listeners", _which + " reset sensistives");
		if (state == State.STANDBY) {
			_buttons[0].setSensitive(true);
			for (int i = 1; i < State.NUM_BUTTONS; i++) {
				_buttons[i].setSensitive(false);
			}
		} else {
			_buttons[0].setSensitive(false);
			for (int i = 1; i < State.NUM_BUTTONS; i++) {
				_buttons[i].setSensitive(true);
			}
		}
	}

	/**
	 * Configure the ToggleButtons with their label graphics and their event
	 * listeners.
	 * <P>
	 * You might think that this would be easier to do directly in glade, and
	 * you'd be right, except for one problem: LibGlade is extraordinarily picky
	 * about where it gets image files from. It can be done, but images must be
	 * in the same directory as the glade files or in a child directory thereof,
	 * or in an absolute path referenced location; this puts tight constraints
	 * on where the files can be in a shipped installation.
	 * 
	 * <P>
	 * The assumption is made that the glade file specified a ToggleButton with
	 * no children (Label or Image) set - the case in QuickButtonsWindow
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
		_buttons[index] = button;

		/*
		 * Construct the Image, with an initial pixbuf, and add it to the
		 * ToggleButton
		 */
		_images[index] = new Image(_offPixbufs[index]);

		button.add(_images[index]);

		/*
		 * Add the listener which contains the RadioAction type logic
		 */
		button.addListener(this);
	}

	/**
	 * Add a ToggleToolButton with its graphics and event listeners. Note that
	 * ToggleToolButton, as constructed by glade, contains a single Toggle
	 * Button as a child, itself containing a VBox. We extract these in sequence
	 * (using some very voodoo code) to get at the ToggleButton, which we then
	 * hang onto a reference to, and then pass to the original add(ToggleButton)
	 * method
	 * 
	 * <P>
	 * The assumption is made that the glade file specified a ToggleButton with
	 * no children (Label or Image) set.
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
		 * We descend into the ToggleToolButton to a) get the ToggleButton, and
		 * then b) construct an Image widget and add it appropriately.
		 */

		Widget w = button.getChild();
		System.out.println("DEBUG: w is a " + w.getName());

		ToggleButton tb = (ToggleButton) button.getChild();

		VBox vbox = (VBox) tb.getChild();

		/*
		 * Our reference to the actual ToggleButton
		 */
		_buttons[index] = tb;

		/*
		 * Constuct the Image, with an initial (off) pixbuf. Because ToolButtons
		 * are implemented as composites with VBox( ; Label ), we add to the
		 * VBox and push the Image to the top.
		 */
		_images[index] = new Image(_offPixbufs[index]);

		vbox.packStart(_images[index]);
		vbox.reorderChild(_images[index], 0);

		/*
		 * Now that _buttons and _index are set, add the listener to implement
		 * the RadioAction behaviour.
		 */
		tb.addListener(this);

		// necessary to make anything actually appear in the rendered widgets.
		button.showAll();
	}

	/**
	 * Implement the logic to emulate RadioAction behaviour between a the group
	 * of ToggleButtons that this StateButtons object represents. This method is
	 * what implements ToggleListener
	 * 
	 * <P>
	 * This contains all the logic to manage switches from one state to another.
	 * It's a bit complicated as one event will cause another event in order to
	 * complete the sequence.
	 * 
	 * @see org.gnu.gtk.event.ToggleListener#toggleEvent(org.gnu.gtk.event.ToggleEvent)
	 */
	public void toggleEvent(ToggleEvent event) {
		ToggleButton toggle = null;

		if (event.getType() == ToggleEvent.Type.TOGGLED) {
			int index = -1;

			/*
			 * Figure out which button the signal came from
			 */
			for (int i = 0; i < State.NUM_BUTTONS; i++) {
				if (event.getSource() == _buttons[i]) {
					index = i;
					Debug.print("listeners", _which + " " + State.colours[i] + " received TOGGLE event, state now "
							+ _buttons[i].getState() + ". I " + (index == _current ? "am" : "am not") + " current.");
				}
			}
			toggle = _buttons[index];

			/*
			 * If the toggle signal came from the currently active widget, then
			 * it was in response to either an unselect request (which we are
			 * going to emit a signal so we can ignore), that signal (in which
			 * case we do nothing).
			 * 
			 * If a different widget is active, then we activate this button,
			 * and send a signal to the active one to disengage.
			 */
			if (index == _current) {
				if (toggle.getState() == false) {
					/*
					 * ie, if I got unclicked...
					 */
					Debug.print("listeners", _which + " ...sending click to " + State.colours[index] + " to counteract");
					// aka click(); generates TOGGLE signal
					toggle.setState(true);
				} else {
					// stay activated; ignore
				}
			} else {
				if (toggle.getState() == true) {
					/*
					 * ie, I'm not the current button, some other is, and I'm
					 * being told to activate.
					 */
					int old = _current;
					_current = index;
					/*
					 * Having changed the _current index deactivate
					 * [setState(false), which generates a click/toggle] the old
					 * button. Because it's no longer _current, it will respect
					 * the click[toggle] signal and deactivate, the other side
					 * of this if / else block.
					 */

					// (initial conditions safety check)
					if ((old >= 0) && (old < State.NUM_BUTTONS)) {
						// aka click(); generates TOGGLE signal
						_buttons[old].setState(false);
					}
					/*
					 * Then, when contol returns here, change my graphic to be
					 * "on".
					 */
					Debug.print("listeners", _which + " " + State.colours[index] + " ...activating with full image");
					_images[index].set(_onPixbufs[index]);

					/*
					 * And, for cosmetic fun, set the Window icon to the current
					 * graphic
					 */
					if (_setIcon) {
						_top.setIcon(_onPixbufs[index]);
					}

					/*
					 * Send the signal to any other StateButtons out there. (one
					 * will end up back here, and be ignored)
					 */
					Debug.print("events", _which + " " + State.colours[index] + " calling ui.setMyState(" + index + ")");
					ProcedureClient.ui.setMyState(index);
				} else {
					Debug.print("listeners", _which + " " + State.colours[index] + "...deactivating with outline image");
					_images[index].set(_offPixbufs[index]);
				}
			}
		}
	}

	/**
	 * Tells StateButtons to set the Icon of the parent Window to the new active
	 * image when the state toggles. This is here specifically for QuickButtons;
	 * it's easier than trying to registering another Listener within the class
	 * that uses StateButtons.
	 */
	public void setIconOnActive(Window top) {
		if (top == null) {
			throw new DebugException("passed a null top Window object");
		}
		_top = top;
		_setIcon = true;
	}
}