/*
 * DetailWindow.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005, 2008 Operational Dynamics
 */
package xseq.ui;

import generic.util.Debug;
import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnome.gdk.Event;
import org.gnome.glade.Glade;
import org.gnome.glade.XML;
import org.gnome.gtk.Label;
import org.gnome.gtk.TextBuffer;
import org.gnome.gtk.TextIter;
import org.gnome.gtk.TextMark;
import org.gnome.gtk.TextTag;
import org.gnome.gtk.TextTagTable;
import org.gnome.gtk.TextView;
import org.gnome.gtk.ToggleToolButton;
import org.gnome.gtk.ToolButton;
import org.gnome.gtk.Widget;
import org.gnome.gtk.Window;
import org.gnome.pango.Scale;
import org.gnome.pango.Underline;
import org.gnome.pango.Weight;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import xseq.client.ProcedureClient;
import xseq.domain.Procedure;
import xseq.domain.State;
import xseq.services.TextMarkIndex;
import xseq.services.XmlUtils;

/**
 * The window which displays the details of of the step elements currently
 * being exectuted. Note that we [normally only?] instantiate one of these, as
 * this window's UI provides for "moving to the next step (in the next
 * section)" and "forward/back buttons".
 * 
 * @author Andrew Cowie
 */
public class DetailsWindow
{
    // we cache a number of widgets to save grinding through the glade code
    // every time we need access to one of them.
    XML glade = null;

    Window top = null;

    Label section_label = null;

    TextView section_textview = null;

    ToolButton prev_toolbutton = null;

    ToolButton current_toolbutton = null;

    ToolButton next_toolbutton = null;

    // the StateButtons, in particular, need to be accessible from outside
    StateButtons stateButtons = null;

    // holds a TextBuffer for each of the Sections in the document;
    // we switch between them as we navigate.
    private TextBuffer[] buffers = null;

    private String[] titles = null;

    private int numSections;

    // while this is indeed duplicative of what ProcedureClient.ui is holding,
    // this speeds up moving across the buffer arrays in the showAs() methods.
    private int currentSection;

    // we reuse the same tag (markup) table for each TextBuffer.
    private TextTagTable table = null;

    private TextMarkIndex sectionMarkIndex = null;

    private TextMarkIndex stepMarkIndex = null;

    private TextMarkIndex nameMarkIndex = null;

    private TextMarkIndex taskMarkIndex = null;

    /**
     * The glade code to instantiate the Gtk window and attach basic handlers.
     * The more specific XML processing logic is in the public constructor,
     * above, which calls this.
     */
    DetailsWindow() {
        try {
            glade = Glade.parse("share/details.glade", null);
        } catch (FileNotFoundException e) {
            // If it can't find that glade file, we have an app
            // configuration problem or worse some UI bug, and need to abort.
            e.printStackTrace();
            ProcedureClient.abort("Can't find glade file for DetailsWindows.");
        } catch (Exception e) {
            e.printStackTrace();
            ProcedureClient.abort("An internal error occured trying to read and process the glade file for DetailsWindow.");
        }
        top = (Window) glade.getWidget("details");
        top.hide();

        top.connect(new Window.DELETE_EVENT() {
            public boolean onDeleteEvent(Widget source, Event event) {
                Debug.print("listeners", "calling end_program() to initiate app termination");
                close_window();
                return false;
            }
        });

        /*
         * the glade file has all sorts of useful parameters set, notably that
         * this textview is not editable, and the horizonal scrollbar is
         * never.
         */
        section_textview = (TextView) glade.getWidget("section_textview");

        section_label = (Label) glade.getWidget("currentsectiontitle_label");
        prev_toolbutton = (ToolButton) glade.getWidget("prev_toolbutton");
        current_toolbutton = (ToolButton) glade.getWidget("current_toolbutton");
        next_toolbutton = (ToolButton) glade.getWidget("next_toolbutton");

        /*
         * Following the recommended optimization, create a TextTagTable for
         * use in the TextBuffers we will create later. First up are the tags
         * used for layout
         */

        table = new TextTagTable();

        tags.section = new TextTag(table);
        tags.section.setScale(Scale.X_LARGE);
        tags.section.setWeight(Weight.BOLD);

        tags.step = new TextTag(table);
        tags.step.setScale(Scale.LARGE);
        tags.step.setWeight(Weight.BOLD);

        tags.name = new TextTag(table);
        tags.name.setUnderline(Underline.SINGLE);
        tags.name.setIndent(5);

        tags.taskNum = new TextTag(table);
        tags.taskNum.setIndent(-16);
        tags.taskNum.setLeftMargin(15);

        tags.task = new TextTag(table);
        tags.task.setLeftMargin(15);

        /*
         * And now the tags used to change the visible emphasis of elements as
         * a procedure is being worked through.
         */
        final String BLACK = "#090D2A";
        final String DARKGRAY = "#6F6F6F";
        final String LIGHTGRAY = "#AAAAAA";
        final String YELLOW = "yellow";

        tags.upcomingTask = new TextTag(table);
        // unused

        tags.currentTask = new TextTag(table);
        tags.currentTask.setBackground(YELLOW); // TODO IMPROVE ME

        tags.doneTask = new TextTag(table);
        tags.doneTask.setStrikethrough(true);

        tags.upcomingStep = new TextTag(table);
        tags.upcomingStep.setForeground(DARKGRAY);

        tags.currentStep = new TextTag(table);
        tags.currentStep.setForeground(BLACK);

        tags.doneStep = new TextTag(table);
        tags.doneStep.setForeground(LIGHTGRAY);

        /*
         * build the StateButtons underneath the ToggleToolButtons.
         */
        stateButtons = new StateButtons("details");

        for (int i = 0; i < State.NUM_BUTTONS; i++) {
            ToggleToolButton toggletoolbutton = (ToggleToolButton) glade.getWidget("state"
                    + State.colours[i] + "_toggletoolbutton");

            if (toggletoolbutton == null) {
                throw new DebugException("didn't get a widget from LibGlade");
            }

            stateButtons.addToSet(toggletoolbutton, i);
        }

        top.resize(1, 600);
        top.move(450, 5);
        top.present();
    }

    // this will later evolve into a setProcedure()?
    public DetailsWindow(Procedure p) {
        this();

        Document dom = p.getDOM();

        NodeList sections = dom.getElementsByTagName("section");
        numSections = sections.getLength();

        if (numSections == 0) {
            throw new DebugException(
                    "How did you manage to get here with a procedure with no <sections>?");
        }

        /*
         * Initialize the TextMark indecies. They are populated in
         * sectionToBuffer().
         */
        sectionMarkIndex = new TextMarkIndex(dom, "section");
        stepMarkIndex = new TextMarkIndex(dom, "step");
        nameMarkIndex = new TextMarkIndex(dom, "name");
        taskMarkIndex = new TextMarkIndex(dom, "task");

        /*
         * The entire UI strategy here revolves around turning <section>
         * elements into TextView widgets. We will display these UI element;
         * one at a time, paging between sections.
         * 
         * FUTURE the section # -> mark indexes allowing us to find a buffer
         * to know if we need to [re] construct one on update, but for now
         * <procedures> are static once loaded.
         * 
         * FUTURE If we ever support dynamic procedure changes, then we'll
         * need to convert this from an array to some sort of dynamic
         * Collection.
         */
        buffers = new TextBuffer[numSections];
        titles = new String[numSections];

        for (int i = 0; i < numSections; i++) {
            Element section = (Element) sections.item(i);

            titles[i] = section.getAttribute("num") + ". " + section.getAttribute("title");
            buffers[i] = sectionToBuffer(section);
        }
        /*
         * And start up at the first section.
         */
        prev_toolbutton.setSensitive(false);
        if (numSections > 1) {
            next_toolbutton.setSensitive(true);
        } else {
            next_toolbutton.setSensitive(false);
        }
        activateSection(0, true);
    }

    public void close_window() {
        top.hide();
        // TODO testing only. REMOVE and replace with closing (deactivating)
        // this window only...
        ProcedureClient.ui.shutdown();
    }

    /**
     * Given a <section>Element, generate the underlying data structures which
     * presents the details of the <steps>in that <section>. In addition to
     * the TextBuffer which backs the TextView, TextMarks are created for each
     * element and are added to an index for later use in controlling the
     * markup in the display of the section.
     * 
     * @return a TextBuffer containing the UI for the section that was
     *         processed.
     */
    public TextBuffer sectionToBuffer(Element section) {
        TextBuffer buf = new TextBuffer(table);
        TextIter iter = buf.getIterStart();

        /*
         * Run through the <step> <name> <task> element groups and call the
         * appropriate convenience methods to build the display.
         */

        // this fetches decendent nodes [only]
        NodeList steps = section.getElementsByTagName("step");
        int num_steps = steps.getLength();

        if (num_steps == 0) {
            buf.insert(iter, "No step items listed for this section!?!");
            return buf;
        }

        for (int i = 0; i < num_steps; i++) {
            Element step = (Element) steps.item(i);
            String stepId = step.getAttribute("id");

            TextMark stepStartMark = buf.createMark(iter, true);
            // the third argument, a String, refers to a TextTag to apply
            buf.insert(iter, step.getAttribute("num") + ". ", tags.step);
            buf.insert(iter, step.getAttribute("title") + "\n", tags.step);

            NodeList names = step.getElementsByTagName("name");
            int num_names = names.getLength();

            for (int j = 0; j < num_names; j++) {
                Element name = (Element) names.item(j);
                String nameId = name.getAttribute("id");

                TextMark nameStartMark = buf.createMark(iter, true);
                buf.insert(iter, name.getAttribute("who") + "\n", tags.name);

                NodeList tasks = name.getElementsByTagName("task");
                int num_tasks = tasks.getLength();

                for (int k = 0; k < num_tasks; k++) {
                    Element task = (Element) tasks.item(k);
                    String taskId = task.getAttribute("id");

                    TextMark taskStartMark = buf.createMark(iter, true);
                    buf.insert(iter, task.getAttribute("num") + ". ", tags.taskNum);
                    buf.insert(iter, XmlUtils.getElementText(task) + "\n", tags.task);
                    TextMark taskEndMark = buf.createMark(iter, true);

                    taskMarkIndex.addMarks(taskId, taskStartMark, taskEndMark);
                }
                TextMark nameEndMark = buf.createMark(iter, true);
                nameMarkIndex.addMarks(nameId, nameStartMark, nameEndMark);
            }
            TextMark stepEndMark = buf.createMark(iter, true);
            stepMarkIndex.addMarks(stepId, stepStartMark, stepEndMark);

            showStepAsUpcoming(stepId);
        }
        return buf;
    }

    /**
     * This method, like all the activate{Prev,Next} methods, are the
     * callbacks invoked by the clicked handlers (as spec'd in the .glade
     * file). It calls the ProcedureUserInterface method of the same name,
     * which then callsback to this and other Window classes to affect the
     * necessary changes.
     */
    // have to be public so libglade signal connect can find them.
    public void activatePrevSection_cb() {
        // This will callback to the activateSection() routine here, but it
        // will
        // also call other Windows' activateSection routines.
        ProcedureClient.ui.activatePrevSection();
    }

    public void activateNextSection_cb() {
        // This will callback to the activateSection() routine here, but it
        // will
        // also call other Windows' activateSection routines.
        ProcedureClient.ui.activateNextSection();
    }

    public void activateCurrentSection_cb() {
        // This will callback to the activateSection() routine here, but it
        // will
        // also call other Windows' activateSection routines.
        ProcedureClient.ui.activateCurrentSection();
    }

    public void activateSection(String sectionId) {
    // TODO probably needs a section ID to Mark index, then, doesn't it!

    // and then call activateSection(int)
    }

    /**
     * Cause the display to advance to the specified section.
     * 
     * @param index
     *            The zero origin index of which step is to be shown.
     * @param containsCurrentStep
     *            whether or not this Section is to show itself as containing
     *            the current Step.
     */
    public void activateSection(int index, boolean containsCurrentStep) {
        if ((index < 0) && (index >= numSections)) {
            throw new DebugException(
                    "DetailsWindow's activateSection() was called with an illegal section number, "
                            + index);
        }

        /*
         * If we're now at the beginning, turn off the prev button
         */
        if (index == 0) {
            prev_toolbutton.setSensitive(false);
        } else {
            prev_toolbutton.setSensitive(true);
        }
        /*
         * If we're at now at the end, turn off the next button
         */
        if (index == (numSections - 1)) {
            next_toolbutton.setSensitive(false);
        } else {
            next_toolbutton.setSensitive(true);
        }

        /*
         * If the current section contains the current step, then we don't
         * need the "Current Step" button hot; otherwise we do.
         */
        if (containsCurrentStep) {
            current_toolbutton.setSensitive(false);
        } else {
            current_toolbutton.setSensitive(true);
        }

        /*
         * Now, do the UI alterations. First, bring up the appropriate
         * TextBuffer, then set the title.
         */
        section_textview.setBuffer(buffers[index]);
        section_textview.showAll();

        section_label.setLabel("<span size=\"xx-large\">" + titles[index] + "</span>");
        currentSection = index;
    }

    /**
     * As the last thing before main runs, we ask to grab focus. Called by
     * whatever runner. HACK. Does it even work?
     */
    public void initialGrabFocus() {
        section_textview.grabFocus();
    }

    /**
     * Display the current <step>visually as done, and display the next
     * <step>as current.
     */
    public void advanceStep() {
    /*
     * Get the current step's marks
     */

    /*
     * Apply tags to make it completed
     */

    /*
     * Get next steps's marks
     */

    /*
     * Apply tags to make it current.
     */

    // TODO code me if necessary
    }

    /**
     * Set the display of a task to upcoming, current, or done
     * 
     * @param taskId
     *            which <task>to change, by XML ID.
     */
    public void showTaskAsUpcoming(String taskId) {
        showAs(taskId, taskMarkIndex, null, tags.upcomingTask);
    }

    public void showTaskAsCurrent(String taskId) {
        showAs(taskId, taskMarkIndex, tags.upcomingTask, tags.currentTask);
    }

    public void showTaskAsDone(String taskId) {
        showAs(taskId, taskMarkIndex, tags.currentTask, tags.doneTask);
    }

    /**
     * Set the display of a step to upcoming, current, or done
     * 
     * @param stepId
     *            the <step>to show as [state]
     */
    public void showStepAsUpcoming(String stepId) {
        showAs(stepId, stepMarkIndex, null, tags.upcomingStep);
    }

    public void showStepAsCurrent(String stepId) {
        showAs(stepId, stepMarkIndex, tags.upcomingStep, tags.currentStep);
    }

    public void showStepAsDone(String stepId) {
        showAs(stepId, stepMarkIndex, tags.currentStep, tags.doneStep);
    }

    /**
     * Change the display of a visual element (section, step, name, task). You
     * list the tag name you want removed, and the tag you want to apply in
     * its place.
     * 
     * @param id
     *            which element block to change, by XML ID.
     * @param tagRemove
     *            the TextTag formatting to apply (the name given when the tag
     *            was created). Use null if you just want to add a tag.
     * @param tagAdd
     *            the TextTag formatting to apply (the name given when the tag
     *            was created). Use null if you just want to remove a tag.
     */
    private void showAs(String id, TextMarkIndex markIndex, TextTag tagRemove, TextTag tagAdd) {
        /*
         * Mark can tell us what buffer it is in, so if we replaced the index
         * use with this:
         */
        // TextMark startMark = _buffers[_currentSection].getMark(stepId +
        // "start");
        // TextIter startIter = _buffers[_currentSection].getIter(startMark);
        /*
         * then we could go directly. However, by going through the index we
         * aren't reliant on _currentSection, and as a result could mark an
         * arbitrary section as done, which we will need in order to process
         * incoming events from other people.
         */

        TextBuffer buf;
        TextMark startMark, endMark;

        startMark = markIndex.getStartMarkById(id);
        buf = startMark.getBuffer();

        endMark = markIndex.getEndMarkById(id);

        if (tagRemove != null) {
            buf.removeTag(tagRemove, startMark.getIter(), endMark.getIter());
        }
        if (tagAdd != null) {
            buf.applyTag(tagAdd, startMark.getIter(), endMark.getIter());
        }
    }

}

/*
 * Just a naming convenience.
 */
class tags
{
    static TextTag section, step, name, task;

    static TextTag taskNum, upcomingTask, currentTask, doneTask;

    static TextTag upcomingStep, currentStep, doneStep;
}
