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

import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnome.gdk.Event;
import org.gnome.glade.Glade;
import org.gnome.glade.XML;
import org.gnome.gtk.MenuItem;
import org.gnome.gtk.ToggleButton;
import org.gnome.gtk.Widget;
import org.gnome.gtk.Window;

import xseq.client.ProcedureClient;
import xseq.domain.State;

/**
 * A small, undecorated, always-on-top window holding 5 coloured toggle
 * buttons representing the state of a person with respect to their current
 * task(s).
 * 
 * @author Andrew Cowie
 */
public class QuickButtonsWindow
{
    XML _glade = null;

    Window _top = null;

    // the StateButtons, in particular, need to be accessible from outside
    StateButtons _stateButtons = null;

    /**
     * Create a new QuickButtonsWindow Window.
     * 
     * @param p
     *            Used to extract the list of <step>tags.
     */
    public QuickButtonsWindow() {

        try {
            _glade = Glade.parse("share/quickbuttons.glade", null);
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

        _top.connect(new Window.DeleteEvent() {
            public boolean onDeleteEvent(Widget source, Event event) {
                ProcedureClient.ui.shutdown();
                return false;
            }
        });

        /*
         * build buttons
         */
        _stateButtons = new StateButtons("quick");

        for (int i = 0; i < State.NUM_BUTTONS; i++) {
            ToggleButton tb = (ToggleButton) _glade.getWidget("state" + State.colours[i]
                    + "_togglebutton");

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
        hide.connect(new MenuItem.Activate() {
            public void onActivate(MenuItem source) {
                _top.hide();
            }
        });
        MenuItem quit = (MenuItem) _glade.getWidget("quit_menuitem");
        quit.connect(new MenuItem.Activate() {
            public void onActivate(MenuItem source) {
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
         * immediately hide again, hopfully missing a draw cycle before the
         * move is done.
         */
        _top.showAll();
        _top.hide();

        /*
         * I'm undecided whether this should go to far bottom right on a multi
         * headed setup, or the "present" monitor, ie, where the other Windows
         * ended up. This is certainly much easier than tracking throug the
         * Gdk classes, and on a single monitor it's a moot point anyway.
         */
        int s_w = _top.getScreen().getWidth();
        int s_h = _top.getScreen().getHeight();

        int w = _top.getAllocation().getWidth();
        int h = _top.getAllocation().getHeight();

        _top.move(s_w - w - 15, s_h - h - 50);
        /*
         * Now we can show. The keep above hint needs to be [re]set.
         * Apparently hiding causes it to be lost.
         */
        _top.setKeepAbove(true);
        _top.present();
    }
}
