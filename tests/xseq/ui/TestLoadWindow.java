/*
 * TestControlWindow.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005 Operational Dynamics
 */
package xseq.ui;

import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnu.gdk.Pixbuf;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Entry;
import org.gnu.gtk.FileChooserDialog;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.ResponseType;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

import xseq.client.ProcedureClient;

/**
 * At the 0.2 stage, Need to specify either a procedure to be run, or the built
 * in example procedure.
 * 
 * This will be deprecated by 0.4, as a proper initalization front end will have
 * been written.
 * 
 * @author Andrew Cowie
 */
public class TestLoadWindow
{
	private static final String	DEMO_SOURCE_DIR			= "doc/examples";
	private static final String	DEMO_XML_FILE			= "simpleProcedure_v1_Example.xml";

	LibGlade					_glade					= null;
	Window						_top					= null;
	FileChooserDialog			_chooser				= null;

	Entry						_filename_entry			= null;
	RadioButton					_builtin_radiobutton	= null;
	RadioButton					_specify_radiobutton	= null;

	/**
	 * Create a new TestLoadlWindow Window.
	 *  
	 */
	public TestLoadWindow() {

		try {
			_glade = new LibGlade("share/testload.glade", this);
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

		_chooser = (FileChooserDialog) _glade.getWidget("filechooser");
		_chooser.hide();

		/*
		 * Now start setting window properties.
		 */

		_top.setDecorated(true);
		_top.setKeepAbove(false);
		_top.setSkipTaskbarHint(false);
		_top.setSkipPagerHint(false);

		_top.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {
			}

			public boolean lifeCycleQuery(LifeCycleEvent event) {
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
		construction_image.set(construction_pixbuf);

		Label title_lable = (Label) _glade.getWidget("title_label");
		title_lable.setMarkup("<big><big>xseq version " + ProcedureClient.VERSION + "</big></big>");

		/*
		 * For some reason, glade keeps overriding what button group the radios
		 * belong to - so get handles and reset it by hand here.
		 */
		_builtin_radiobutton = (RadioButton) _glade.getWidget("builtin_radiobutton");
		_builtin_radiobutton.setState(true);
		_specify_radiobutton = (RadioButton) _glade.getWidget("specify_radiobutton");

		_filename_entry = (Entry) _glade.getWidget("filename_entry");
		_filename_entry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent event) {
				if (event.getType() == EntryEvent.Type.CHANGED) {
					_specify_radiobutton.setState(true);
				}
			}
		});

		Button select_button = (Button) _glade.getWidget("select_button");

		select_button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.getType() == ButtonEvent.Type.CLICK) {

					_chooser.showAll();
					_chooser.present();

					int response = _chooser.run();

					/*
					 * There seems to be some ResponseType is returned on
					 * closing a window.
					 */
					if (response == ResponseType.NONE.getValue()) {
						// System.out.println("ResponseType.NONE");
						_chooser.hide();
					}
					if (response == ResponseType.DELETE_EVENT.getValue()) {
						// System.out.println("ResponseType.DELETE_EVENT");
						_chooser.hide();
					}

					/*
					 * CANCEL button...
					 */
					if (response == ResponseType.CANCEL.getValue()) {
						// System.out.println("ResponseType.CANCEL");
						_chooser.hide();
					}

					/*
					 * And, actually use the selected value if OK or <Enter> are
					 * pressed...
					 */
					if (response == ResponseType.OK.getValue()) {
						// System.out.println("ResponseType.OK");
						_chooser.hide();

						String newFilename = _chooser.getFilename();

						if (newFilename != null) {
							_filename_entry.setText(newFilename);
							/*
							 * put the cursor at the end (ie, no kidding that
							 * the file is in /home/andrew... show me the file I
							 * picked!)
							 */
							_filename_entry.setCursorPosition(newFilename.length());

							/*
							 * activate the specify radio button (nothing worse
							 * than implicitly selecting something and the UI
							 * not keeping up)
							 */
							_specify_radiobutton.activate();
						}
					}
				}
			}
		});

		Button _init_button = (Button) _glade.getWidget("init_button");

		_init_button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.getType() == ButtonEvent.Type.CLICK) {

					String filename = null;
					if (_builtin_radiobutton.getState() == true) {
						filename = DEMO_SOURCE_DIR + "/" + DEMO_XML_FILE;
					} else if (_specify_radiobutton.getState() == true) {
						filename = _filename_entry.getText();
					} else {
						throw new DebugException("Oops - managed to not have either radio button selected!");
					}

					try {
						WindowRunner.loadAndRun(filename, _top);

						_top.destroy();
						_chooser.destroy();
					} catch (FileNotFoundException fnfe) {
						// try again
					}
				}
			}
		});

		Button _cancel_button = (Button) _glade.getWidget("cancel_button");

		_cancel_button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.getType() == ButtonEvent.Type.CLICK) {
					Gtk.mainQuit();
					System.exit(1);
				}
			}
		});

		_top.showAll();
		_top.present();
	}
}