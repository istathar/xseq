/*
 * TestControlWindow.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005 Operational Dynamics
 */
package xseq.ui;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;

import org.gnu.gdk.Pixbuf;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.HBox;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.VBox;
import org.gnu.gtk.VButtonBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import xseq.client.ProcedureClient;
import xseq.domain.Procedure;

/**
 * A test harness to aid working through procedures. Until the full front end
 * for ProcedureClient is written, this Window serves to help progress the
 * application when testing.
 * 
 * <P>
 * In particular, it emulates the selection of identity (which participant are
 * you?), commencement of the procedure, and then allows the other Participants
 * to be simulated (specifically, them completing tasks, allowing the
 * application to move from one <step>to the next).
 * 
 * <P>
 * It's entirely possible that much of the code here may end up in the master
 * control window once that is written.
 * 
 * @author Andrew Cowie
 */
public class TestControlWindow
{
	LibGlade		_glade			= null;
	Window			_top			= null;

	private VBox	_pick_vbox		= null;
	private VBox	_start_vbox		= null;
	private VBox	_others_vbox	= null;

	/**
	 * Create a new TestControlWindow Window.
	 * 
	 * @param p
	 *            Used to extract the list of <name>s, and to get information
	 *            about which tasks are whose.
	 */
	public TestControlWindow(Procedure p) {

		try {
			_glade = new LibGlade("share/testcontrol.glade", this);
		} catch (FileNotFoundException e) {
			// If it can't find that glade file, we have an app
			// configuration problem or worse some UI bug, and need to abort.
			e.printStackTrace();
			ProcedureClient.abort("Can't find glade file for TestControlWindow.");
		} catch (Exception e) {
			e.printStackTrace();
			ProcedureClient.abort("An internal error occured trying to read and process the glade file for DetailsWindow.");
		}
		_top = (Window) _glade.getWidget("testcontrol");
		_top.hide();

		// didn't we set this in glade?
		_top.setDecorated(true);
		_top.setKeepAbove(false);
		_top.setSkipTaskbarHint(false);
		_top.setSkipPagerHint(false);

		_top.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {
			}

			public boolean lifeCycleQuery(LifeCycleEvent event) {
				ProcedureClient.ui.shutdown();
				return false;
			}
		});

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

		NodeList names = p.getDOM().getElementsByTagName("name");
		int num_names = names.getLength();
		/*
		 * A list of the names of participants
		 */
		HashSet participants = new HashSet(num_names, 0.1f);

		for (int i = 0; i < num_names; i++) {
			Element name = (Element) names.item(i);
			String who = name.getAttribute("who");
			participants.add(who);
		}
		/*
		 * Add a radio button group with the names, so that an active player can
		 * be selected. pick_vbox starts out sensitive.
		 */
		_pick_vbox = (VBox) _glade.getWidget("pick_vbox");
		VButtonBox pick_vbuttonbox = (VButtonBox) _glade.getWidget("pick_vbuttonbox");
		// this one won't actually be displayed, but forms the foundation of the
		// radiobutton group; otherwise difficult to dynamically instantiate.
		RadioButton rb0 = new RadioButton((RadioButton) null, "None selected", false);

		Iterator iter = participants.iterator();

		RadioButton[] pickRadioButtons = new RadioButton[participants.size() + 1];
		pickRadioButtons[0] = rb0;
		int j = 1;

		while (iter.hasNext()) {
			String person = (String) iter.next();
			// the RadioButton API says you give one to add another to its group
			pickRadioButtons[j] = new RadioButton(rb0, person, false);
			pickRadioButtons[j].addListener(new ButtonListener() {
				public void buttonEvent(ButtonEvent event) {
					if (event.isOfType(ButtonEvent.Type.CLICK)) {
						RadioButton rb = (RadioButton) event.getSource();
						if (rb.getState()) {
							//ie, if I've been made active
							/*
							 * And finally do something with the label...
							 */
							_pick_vbox.setSensitive(false);
							_start_vbox.setSensitive(true);

							ProcedureClient.ui.setUser(rb.getLabel());
						}
					}
				}
			});
			pick_vbuttonbox.packStart(pickRadioButtons[j]);
			j++;
		}

		/*
		 * Now a handler for the procedure start button
		 */

		_start_vbox = (VBox) _glade.getWidget("start_vbox");
		Button start_button = (Button) _glade.getWidget("start_button");
		start_button.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent event) {
				if (event.isOfType(ButtonEvent.Type.CLICK)) {
					/*
					 * gray out this button (can only start once)
					 */
					_start_vbox.setSensitive(false);

					ProcedureClient.ui.startProcedure();

					/*
					 * And now events can flow from others, but we need to
					 * disable the button for the user picked above, because
					 * that's what the colour state buttons are for!
					 */
					_others_vbox.setSensitive(true);

					// This code would be easier below as the others buttons are
					// constructed, but setSensitive cascades to children, and
					// so the setting would be overrriden by the above line.
					VButtonBox others_vbuttonbox = (VButtonBox) _glade.getWidget("others_vbuttonbox");

					Widget[] children = others_vbuttonbox.getChildren();
					Button candidateButton = null;
					String whoAmI = ProcedureClient.ui.getUser();
					for (int m = 0; m < children.length; m++) {
						if (children[m] instanceof Button) {
							// this is really ugly code, but the consequence
							// of using buttons with images+labels that aren't
							// from the stock library.
							candidateButton = (Button) children[m];
							HBox hbox = (HBox) candidateButton.getChild();
							Widget[] buttonParts = hbox.getChildren();

							for (int p = 0; p < buttonParts.length; p++) {
								if (buttonParts[p] instanceof Label) {
									if (((Label) buttonParts[p]).getLabel().equals(whoAmI)) {
										candidateButton.setSensitive(false);
										return;
									}
								}
							}
						}
					}
				}
			}
		});

		/*
		 * Instantiate buttons for the tasks as completed by others
		 */
		_others_vbox = (VBox) _glade.getWidget("others_vbox");
		VButtonBox others_vbuttonbox = (VButtonBox) _glade.getWidget("others_vbuttonbox");

		iter = participants.iterator();

		Button[] taskButtons = new Button[participants.size()];
		int k = 0;

		while (iter.hasNext()) {
			String person = (String) iter.next();

			/*
			 * If the buttons were just simple automatically labelled ones, then
			 * all of the following logic could be replaced with
			 * 
			 * taskButtons[k] = new Button(person, false);
			 * 
			 * Alas. Because buttons with icons [stock or otherwise] are
			 * compound widgets, we have to progres down into them to get their
			 * label or set their image. This should probably be abstracted out
			 * somehow.
			 */
			taskButtons[k] = new Button();
			HBox hbox = new HBox(false, 2);
			taskButtons[k].add(hbox);
			Image apply = new Image(GtkStockItem.APPLY, IconSize.BUTTON);
			hbox.packStart(apply);
			Label label = new Label(person, false);
			hbox.packStart(label);

			taskButtons[k].addListener(new ButtonListener() {
				public void buttonEvent(ButtonEvent event) {
					if (event.isOfType(ButtonEvent.Type.CLICK)) {
						String who = null;
						Button b = (Button) event.getSource();
						HBox h = (HBox) b.getChild();
						Widget[] children = h.getChildren();
						for (int i = 0; i < children.length; i++) {
							if (children[i] instanceof Label) {
								who = ((Label) children[i]).getLabel();
							}
						}
						next(who);
					}
				}
			});

			others_vbuttonbox.packStart(taskButtons[k]);
			k++;
		}

		_top.move(2, 490);
		
		// necessary to get the new buttons mapped.
		_top.showAll();
		_top.present();
	}

	/**
	 * This is terribly messy, but necessary since there isn't a authoritative
	 * central notion of current Step or current Task - things happen
	 * asynchronously, within the rules of Procedure, as governed by the UI code
	 * for what actually can be pressed and cause an taskId complete event to be
	 * propagated.
	 * 
	 * <P>
	 * So, instead, we cumbersomely fake it. We have a list of tasks IDs; when
	 * "someone" presses an others button, we look to see what the next task is
	 * for that person in whatever the UI thinks is the currentStep, and when we
	 * find one which isn't done, make it so.
	 */
	private void next(String buttonPresser) {
		Procedure p = ProcedureClient.ui._procedure;

		String taskId = p.getFirstTaskId(ProcedureClient.ui._currentStepId, buttonPresser);

		if (taskId == null) {
			return;
		}

		while ((taskId != null) && p.isTaskDone(taskId)) {
			taskId = p.getNextTaskId(taskId);
		}

		if (taskId == null) {
			return;
		} else {
			ProcedureClient.ui.setTaskAsDone(taskId);
		}

		if (p.isProcedureDone(taskId)) {
			ProcedureClient.ui.stopProcedure();
			_others_vbox.setSensitive(false);
		}
	}
}