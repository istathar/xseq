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

import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnome.gdk.Event;
import org.gnome.gdk.Pixbuf;
import org.gnome.glade.Glade;
import org.gnome.glade.XML;
import org.gnome.gtk.Button;
import org.gnome.gtk.Editable;
import org.gnome.gtk.Entry;
import org.gnome.gtk.FileChooserDialog;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.Image;
import org.gnome.gtk.Label;
import org.gnome.gtk.RadioButton;
import org.gnome.gtk.ResponseType;
import org.gnome.gtk.Widget;
import org.gnome.gtk.Window;

import xseq.client.ProcedureClient;

import static org.gnome.gtk.FileChooserAction.OPEN;

/**
 * At the 0.2 stage, Need to specify either a procedure to be run, or the
 * built in example procedure.
 * 
 * This will be deprecated by 0.4, as a proper initalization front end will
 * have been written.
 * 
 * @author Andrew Cowie
 */
public class TestLoadWindow
{
    private static final String DEMO_SOURCE_DIR = "doc/examples";

    private static final String DEMO_XML_FILE = "simpleProcedure_v1_Example.xml";

    XML _glade = null;

    Window _top = null;

    FileChooserDialog _chooser = null;

    Entry _filename_entry = null;

    RadioButton _builtin_radiobutton = null;

    RadioButton _specify_radiobutton = null;

    /**
     * Create a new TestLoadlWindow Window.
     * 
     */
    public TestLoadWindow() {

        try {
            _glade = Glade.parse("share/testload.glade", null);
        } catch (FileNotFoundException e) {
            // If it can't find that glade file, we have an app
            // configuration problem or worse some UI bug, and need to abort.
            e.printStackTrace();
            ProcedureClient.abort("Can't find glade file for TestLoadWindow.");
        } catch (Exception e) {
            e.printStackTrace();
            ProcedureClient.abort("An internal error occured trying to read and process the glade file for DetailsWindow.");
        }

        /*
         * Quickly hide the windows while we do further construction on them.
         * Will leave the FileChooser hidden until called up by button...
         */
        _top = (Window) _glade.getWidget("testload");
        _top.hide();

        _chooser = new FileChooserDialog("Load Procedure", _top, OPEN);
        _chooser.hide();

        /*
         * Now start setting window properties.
         */

        _top.setDecorated(true);
        _top.setKeepAbove(false);
        _top.setSkipTaskbarHint(false);
        _top.setSkipPagerHint(false);

        _top.connect(new Window.DeleteEvent() {
            public boolean onDeleteEvent(Widget source, Event event) {
                Gtk.mainQuit();
                System.exit(0);
                return false;
            }
        });

        /*
         * Proceed with completing main window adding necessary widgets.
         */

        Pixbuf construction_pixbuf = null;
        try {
            construction_pixbuf = new Pixbuf("share/pixmaps/underconstruction.png");
        } catch (Exception e1) {
            e1.printStackTrace();
            System.exit(1);
        }
        _top.setIcon(construction_pixbuf);

        Image construction_image = (Image) _glade.getWidget("construction");
        construction_image.setImage(construction_pixbuf);

        Label title_lable = (Label) _glade.getWidget("title_label");
        title_lable.setLabel("<big><big>xseq version " + ProcedureClient.VERSION + "</big></big>");

        /*
         * For some reason, glade keeps overriding what button group the
         * radios belong to - so get handles and reset it by hand here.
         */
        _builtin_radiobutton = (RadioButton) _glade.getWidget("builtin_radiobutton");
        _builtin_radiobutton.setActive(true);
        _specify_radiobutton = (RadioButton) _glade.getWidget("specify_radiobutton");

        _filename_entry = (Entry) _glade.getWidget("filename_entry");
        _filename_entry.connect(new Editable.Changed() {
            public void onChanged(Editable source) {
                _specify_radiobutton.setActive(true);
            }
        });

        Button select_button = (Button) _glade.getWidget("select_button");

        select_button.connect(new Button.Clicked() {
            public void onClicked(Button source) {
                _chooser.showAll();
                _chooser.present();

                ResponseType response = _chooser.run();

                /*
                 * There seems to be some ResponseType is returned on closing
                 * a window.
                 */
                if (response == ResponseType.NONE) {
                    // System.out.println("ResponseType.NONE");
                    _chooser.hide();
                }
                if (response == ResponseType.DELETE_EVENT) {
                    // System.out.println("ResponseType.DELETE_EVENT");
                    _chooser.hide();
                }

                /*
                 * CANCEL button...
                 */
                if (response == ResponseType.CANCEL) {
                    // System.out.println("ResponseType.CANCEL");
                    _chooser.hide();
                }

                /*
                 * And, actually use the selected value if OK or <Enter> are
                 * pressed...
                 */
                if (response == ResponseType.OK) {
                    // System.out.println("ResponseType.OK");
                    _chooser.hide();

                    String newFilename = _chooser.getFilename();

                    if (newFilename != null) {
                        _filename_entry.setText(newFilename);
                        /*
                         * put the cursor at the end (ie, no kidding that the
                         * file is in /home/andrew... show me the file I
                         * picked!)
                         */
                        _filename_entry.setPosition(newFilename.length());

                        /*
                         * activate the specify radio button (nothing worse
                         * than implicitly selecting something and the UI not
                         * keeping up)
                         */
                        _specify_radiobutton.activate();
                    }
                }
            }
        });

        Button _init_button = (Button) _glade.getWidget("init_button");

        _init_button.connect(new Button.Clicked() {
            public void onClicked(Button source) {
                String filename = null;
                if (_builtin_radiobutton.getActive() == true) {
                    filename = DEMO_SOURCE_DIR + "/" + DEMO_XML_FILE;
                } else if (_specify_radiobutton.getActive() == true) {
                    filename = _filename_entry.getText();
                } else {
                    throw new DebugException("Oops - managed to not have either radio button selected!");
                }

                try {
                    WindowRunner.loadAndRun(filename, _top);

                    _top.hide();
                    _top = null;
                    _chooser.hide();
                    _chooser = null;
                } catch (FileNotFoundException fnfe) {
                    // try again
                }
            }
        });

        Button _cancel_button = (Button) _glade.getWidget("cancel_button");

        _cancel_button.connect(new Button.Clicked() {
            public void onClicked(Button source) {
                Gtk.mainQuit();
                System.exit(1);
            }
        });

        _top.showAll();
        _top.present();
    }
}
