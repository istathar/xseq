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

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;

import org.gnome.gdk.Event;
import org.gnome.gdk.Pixbuf;
import org.gnome.glade.Glade;
import org.gnome.glade.XML;
import org.gnome.gtk.Button;
import org.gnome.gtk.HBox;
import org.gnome.gtk.IconSize;
import org.gnome.gtk.Image;
import org.gnome.gtk.Label;
import org.gnome.gtk.RadioButton;
import org.gnome.gtk.RadioGroup;
import org.gnome.gtk.Stock;
import org.gnome.gtk.VBox;
import org.gnome.gtk.VButtonBox;
import org.gnome.gtk.Widget;
import org.gnome.gtk.Window;
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
 * you?), commencement of the procedure, and then allows the other
 * Participants to be simulated (specifically, them completing tasks, allowing
 * the application to move from one <step>to the next).
 * 
 * <P>
 * It's entirely possible that much of the code here may end up in the master
 * control window once that is written.
 * 
 * @author Andrew Cowie
 */
public class TestControlWindow
{
    XML _glade = null;

    Window _top = null;

    private VBox _pick_vbox = null;

    private VBox _start_vbox = null;

    private VBox _others_vbox = null;

    /**
     * Create a new TestControlWindow Window.
     * 
     * @param p
     *            Used to extract the list of <name>s, and to get information
     *            about which tasks are whose.
     */
    public TestControlWindow(Procedure p) {

        try {
            _glade = Glade.parse("share/testcontrol.glade", null);
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

        _top.connect(new Window.DeleteEvent() {
            public boolean onDeleteEvent(Widget source, Event event) {
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
        construction_image.setImage(construction_pixbuf);

        NodeList names = p.getDOM().getElementsByTagName("name");
        int num_names = names.getLength();
        /*
         * A list of the names of participants
         */
        HashSet<String> participants = new HashSet<String>(num_names, 0.1f);

        for (int i = 0; i < num_names; i++) {
            Element name = (Element) names.item(i);
            String who = name.getAttribute("who");
            participants.add(who);
        }
        /*
         * Add a radio button group with the names, so that an active player
         * can be selected. pick_vbox starts out sensitive.
         */
        _pick_vbox = (VBox) _glade.getWidget("pick_vbox");
        VButtonBox pick_vbuttonbox = (VButtonBox) _glade.getWidget("pick_vbuttonbox");
        // this one won't actually be displayed, but forms the foundation of
        // the
        // radiobutton group; otherwise difficult to dynamically instantiate.
        RadioGroup group = new RadioGroup();
        RadioButton rb0 = new RadioButton(group, "None selected");

        Iterator<String> iter = participants.iterator();

        RadioButton[] pickRadioButtons = new RadioButton[participants.size() + 1];
        pickRadioButtons[0] = rb0;
        int j = 1;

        while (iter.hasNext()) {
            String person = iter.next();
            // the RadioButton API says you give one to add another to its
            // group
            pickRadioButtons[j] = new RadioButton(group, person);
            pickRadioButtons[j].connect(new Button.Clicked() {
                public void onClicked(Button source) {
                    RadioButton rb = (RadioButton) source;
                    if (rb.getActive()) {
                        // ie, if I've been made active
                        /*
                         * And finally do something with the label...
                         */
                        _pick_vbox.setSensitive(false);
                        _start_vbox.setSensitive(true);

                        ProcedureClient.ui.setUser(rb.getLabel());
                    }
                }
            });
            pick_vbuttonbox.packStart(pickRadioButtons[j], true, true, 0);
            j++;
        }

        /*
         * Now a handler for the procedure start button
         */

        _start_vbox = (VBox) _glade.getWidget("start_vbox");
        Button start_button = (Button) _glade.getWidget("start_button");
        start_button.connect(new Button.Clicked() {
            public void onClicked(Button source) {
                /*
                 * gray out this button (can only start once)
                 */
                _start_vbox.setSensitive(false);

                ProcedureClient.ui.startProcedure();

                /*
                 * And now events can flow from others, but we need to disable
                 * the button for the user picked above, because that's what
                 * the colour state buttons are for!
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
            String person = iter.next();

            /*
             * If the buttons were just simple automatically labelled ones,
             * then all of the following logic could be replaced with
             * 
             * taskButtons[k] = new Button(person, false);
             * 
             * Alas. Because buttons with icons [stock or otherwise] are
             * compound widgets, we have to progres down into them to get
             * their label or set their image. This should probably be
             * abstracted out somehow.
             */
            taskButtons[k] = new Button();
            HBox hbox = new HBox(false, 2);
            taskButtons[k].add(hbox);
            Image apply = new Image(Stock.APPLY, IconSize.BUTTON);
            hbox.packStart(apply, true, true, 0);
            Label label = new Label(person);
            hbox.packStart(label, true, true, 0);

            taskButtons[k].connect(new Button.Clicked() {
                public void onClicked(Button source) {
                    String who = null;
                    Button b = source;
                    HBox h = (HBox) b.getChild();
                    Widget[] children = h.getChildren();
                    for (int i = 0; i < children.length; i++) {
                        if (children[i] instanceof Label) {
                            who = ((Label) children[i]).getLabel();
                        }
                    }
                    next(who);
                }
            });

            others_vbuttonbox.packStart(taskButtons[k], true, true, 0);
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
     * asynchronously, within the rules of Procedure, as governed by the UI
     * code for what actually can be pressed and cause an taskId complete
     * event to be propagated.
     * 
     * <P>
     * So, instead, we cumbersomely fake it. We have a list of tasks IDs; when
     * "someone" presses an others button, we look to see what the next task
     * is for that person in whatever the UI thinks is the currentStep, and
     * when we find one which isn't done, make it so.
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
