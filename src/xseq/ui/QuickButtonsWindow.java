/*
 * QuickButtonsWindow.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2005 Operational Dynamics
 */
package xseq.ui;

import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ToggleButton;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import xseq.client.ProcedureClient;
import xseq.domain.State;

/**
 * A small, undecorated, always-on-top window holding 5 coloured toggle buttons
 * representing the state of a person with respect to their current task(s).
 * 
 * @author Andrew Cowie
 */
public class QuickButtonsWindow
{
	LibGlade		_glade			= null;
	Window			_top			= null;

	// the StateButtons, in particular, need to be accessible from outside
	StateButtons	_stateButtons	= null;

	/**
	 * Create a new QuickButtonsWindow Window.
	 * 
	 * @param p
	 *            Used to extract the list of <step>tags.
	 */
	public QuickButtonsWindow() {

		try {
			_glade = new LibGlade("share/quickbuttons.glade", this);
		} catch (FileNotFoundException e) {
			// If it can't find that glade file, we have an app
			// configuration problem or worse some UI bug, and need to abort.
			e.printStackTrace();
			ProcedureClient.abort("Can't find glade file for QuickButtonsWindow.");
		} catch (Exception e) {
			e.printStackTrace();
			ProcedureClient.abort("An internal error occured trying to read and process the glade file for QuickButtonsWindow.");
		}
		_top = (Window) _glade.getWidget("quickbuttons");
		_top.hide();

		_top.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {
			}

			public boolean lifeCycleQuery(LifeCycleEvent event) {
				ProcedureClient.ui.shutdown();
				return false;
			}
		});

		/*
		 * build buttons
		 */
		_stateButtons = new StateButtons("quick");

		for (int i = 0; i < State.NUM_BUTTONS; i++) {
			ToggleButton tb = (ToggleButton) _glade.getWidget("state" + State.colours[i] + "_togglebutton");

			if (tb == null) {
				throw new DebugException("didn't get a widget from LibGlade");
			}

			_stateButtons.addToSet(tb, i);
		}
		_stateButtons.setIconOnActive(_top);

		/*
		 * Initialize the menu items
		 */
		MenuItem hide = (MenuItem) _glade.getWidget("hide_menuitem");
		hide.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent event) {
				_top.hide();
			}
		});
		MenuItem quit = (MenuItem) _glade.getWidget("quit_menuitem");
		quit.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent event) {
				ProcedureClient.ui.shutdown();
			}
		});

		/*
		 * Didn't we set all this in glade? Anyway, be clear about it.
		 */
		_top.setDecorated(false);
		_top.setSkipTaskbarHint(true);
		_top.setSkipPagerHint(true);

		/*
		 * Need to do a recursive show in order to get the widget fully mapped
		 * so that the size calculations return correct numbers, but then
		 * immediately hide again, hopfully missing a draw cycle before the move
		 * is done.
		 */
		_top.showAll();
		_top.hide();

		/*
		 * I'm undecided whether this should go to far bottom right on a multi
		 * headed setup, or the "present" monitor, ie, where the other Windows
		 * ended up. This is certainly much easier than tracking throug the Gdk
		 * classes, and on a single monitor it's a moot point anyway.
		 */
		int s_w = _top.getScreen().getWidth();
		int s_h = _top.getScreen().getHeight();

		int w = _top.getWindow().getWidth();
		int h = _top.getWindow().getHeight();

		_top.move(s_w - w - 15, s_h - h - 50);
		/*
		 * Now we can show. The keep above hint needs to be [re]set. Apparently
		 * hiding causes it to be lost.
		 */
		_top.setKeepAbove(true);
		_top.present();
	}
}