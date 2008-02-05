/*
 * DetailWindow.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005 Operational Dynamics
 */
package xseq.ui;

import generic.util.Debug;
import generic.util.DebugException;

import java.io.FileNotFoundException;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Label;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextMark;
import org.gnu.gtk.TextTag;
import org.gnu.gtk.TextTagTable;
import org.gnu.gtk.TextView;
import org.gnu.gtk.ToggleToolButton;
import org.gnu.gtk.ToolButton;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.pango.Scale;
import org.gnu.pango.Underline;
import org.gnu.pango.Weight;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import xseq.client.ProcedureClient;
import xseq.domain.Procedure;
import xseq.domain.State;
import xseq.services.TextMarkIndex;
import xseq.services.XmlUtils;

/**
 * The window which displays the details of of the step elements currently being
 * exectuted. Note that we [normally only?] instantiate one of these, as this
 * window's UI provides for "moving to the next step (in the next section)" and
 * "forward/back buttons".
 * 
 * @author Andrew Cowie
 */
public class DetailsWindow
{
	// we cache a number of widgets to save grinding through the glade code
	// every time we need access to one of them.
	LibGlade				_glade				= null;
	Window					_top				= null;
	Label					_section_label		= null;
	TextView				_section_textview	= null;

	ToolButton				_prev_toolbutton	= null;
	ToolButton				_current_toolbutton	= null;
	ToolButton				_next_toolbutton	= null;

	// the StateButtons, in particular, need to be accessible from outside
	StateButtons			_stateButtons		= null;

	// holds a TextBuffer for each of the Sections in the document;
	// we switch between them as we navigate.
	private TextBuffer[]	_buffers			= null;
	private String[]		_titles				= null;
	private int				_numSections;
	// while this is indeed duplicative of what ProcedureClient.ui is holding,
	// this speeds up moving across the buffer arrays in the showAs() methods.
	private int				_currentSection;

	// we reuse the same tag (markup) table for each TextBuffer.
	private TextTagTable	_tagTable			= null;
	private TextMarkIndex	_sectionMarkIndex	= null;
	private TextMarkIndex	_stepMarkIndex		= null;
	private TextMarkIndex	_nameMarkIndex		= null;
	private TextMarkIndex	_taskMarkIndex		= null;

	/**
	 * The glade code to instantiate the Gtk window and attach basic handlers.
	 * The more specific XML processing logic is in the public constructor,
	 * above, which calls this.
	 */
	DetailsWindow() {
		try {
			_glade = new LibGlade("share/details.glade", this);
		} catch (FileNotFoundException e) {
			// If it can't find that glade file, we have an app
			// configuration problem or worse some UI bug, and need to abort.
			e.printStackTrace();
			ProcedureClient.abort("Can't find glade file for DetailsWindows.");
		} catch (Exception e) {
			e.printStackTrace();
			ProcedureClient.abort("An internal error occured trying to read and process the glade file for DetailsWindow.");
		}
		_top = (Window) _glade.getWidget("details");
		_top.hide();

		_top.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {
				Debug.print("listeners", "details LifeCyleEvent: " + event.getType().getName());
			}

			public boolean lifeCycleQuery(LifeCycleEvent event) {
				Debug.print("listeners", "details LifeCyleQuery: " + event.getType().getName());
				Debug.print("listeners", "calling end_program() to initiate app termination");
				close_window();
				return false;
			}
		});

		/*
		 * the glade file has all sorts of useful parameters set, notably that
		 * this textview is not editable, and the horizonal scrollbar is never.
		 */
		_section_textview = (TextView) _glade.getWidget("section_textview");

		_section_label = (Label) _glade.getWidget("currentsectiontitle_label");
		_prev_toolbutton = (ToolButton) _glade.getWidget("prev_toolbutton");
		_current_toolbutton = (ToolButton) _glade.getWidget("current_toolbutton");
		_next_toolbutton = (ToolButton) _glade.getWidget("next_toolbutton");

		/*
		 * Following the recommended optimization, create a TextTagTable for use
		 * in the TextBuffers we will create later. First up are the tags used
		 * for layout
		 */
		TextBuffer blank = new TextBuffer();
		TextTag tag = null;
		tag = blank.createTag("section");
		tag.setScale(Scale.X_LARGE);
		tag.setWeight(Weight.BOLD);

		tag = blank.createTag("step");
		tag.setScale(Scale.LARGE);
		tag.setWeight(Weight.BOLD);

		tag = blank.createTag("name");
		tag.setUnderline(Underline.SINGLE);
		tag.setIndent(5);

		tag = blank.createTag("task_num");
		tag.setIndent(-16);
		tag.setLeftMargin(15);

		tag = blank.createTag("task");
		tag.setLeftMargin(15);

		/*
		 * And now the tags used to change the visible emphasis of elements as a
		 * procedure is being worked through.
		 */
		final String BLACK = "#090D2A";
		final String DARKGRAY = "#6F6F6F";
		final String LIGHTGRAY = "#AAAAAA";
		final String YELLOW = "yellow";

		tag = blank.createTag("upcoming_task");
		// unused

		tag = blank.createTag("current_task");
		tag.setBackground(YELLOW); // TODO IMPROVE ME

		tag = blank.createTag("done_task");
		tag.setStrikethrough(true);

		tag = blank.createTag("upcoming_step");
		tag.setForeground(DARKGRAY);

		tag = blank.createTag("current_step");
		tag.setForeground(BLACK);

		tag = blank.createTag("done_step");
		tag.setForeground(LIGHTGRAY);

		/*
		 * extract tags for use in future tables
		 */
		_tagTable = blank.getTextTagTable();

		/*
		 * build the StateButtons underneath the ToggleToolButtons.
		 */
		_stateButtons = new StateButtons("details");

		for (int i = 0; i < State.NUM_BUTTONS; i++) {
			ToggleToolButton toggletoolbutton = (ToggleToolButton) _glade.getWidget("state" + State.colours[i]
					+ "_toggletoolbutton");

			if (toggletoolbutton == null) {
				throw new DebugException("didn't get a widget from LibGlade");
			}

			_stateButtons.addToSet(toggletoolbutton, i);
		}

		_top.resize(1, 600);
		_top.move(450, 5);
		_top.present();
	}

	// this will later evolve into a setProcedure()?
	public DetailsWindow(Procedure p) {
		this();

		Document dom = p.getDOM();

		NodeList sections = dom.getElementsByTagName("section");
		_numSections = sections.getLength();

		if (_numSections == 0) {
			throw new DebugException("How did you manage to get here with a procedure with no <sections>?");
		}

		/*
		 * Initialize the TextMark indecies. They are populated in
		 * sectionToBuffer().
		 */
		_sectionMarkIndex = new TextMarkIndex(dom, "section");
		_stepMarkIndex = new TextMarkIndex(dom, "step");
		_nameMarkIndex = new TextMarkIndex(dom, "name");
		_taskMarkIndex = new TextMarkIndex(dom, "task");

		/*
		 * The entire UI strategy here revolves around turning <section>
		 * elements into TextView widgets. We will display these UI element; one
		 * at a time, paging between sections.
		 * 
		 * FUTURE the section # -> mark indexes allowing us to find a buffer to
		 * know if we need to [re] construct one on update, but for now
		 * <procedures> are static once loaded.
		 * 
		 * FUTURE If we ever support dynamic procedure changes, then we'll need
		 * to convert this from an array to some sort of dynamic Collection.
		 */
		_buffers = new TextBuffer[_numSections];
		_titles = new String[_numSections];

		for (int i = 0; i < _numSections; i++) {
			Element section = (Element) sections.item(i);

			_titles[i] = section.getAttribute("num") + ". " + section.getAttribute("title");
			_buffers[i] = sectionToBuffer(section);
		}
		/*
		 * And start up at the first section.
		 */
		_prev_toolbutton.setSensitive(false);
		if (_numSections > 1) {
			_next_toolbutton.setSensitive(true);
		} else {
			_next_toolbutton.setSensitive(false);
		}
		activateSection(0, true);
	}

	public void close_window() {
		_top.hide();
		// TODO testing only. REMOVE and replace with closing (deactivating)
		// this window only...
		ProcedureClient.ui.shutdown();
	}

	/**
	 * Given a <section>Element, generate the underlying data structures which
	 * presents the details of the <steps>in that <section>. In addition to the
	 * TextBuffer which backs the TextView, TextMarks are created for each
	 * element and are added to an index for later use in controlling the markup
	 * in the display of the section.
	 * 
	 * @return a TextBuffer containing the UI for the section that was
	 *         processed.
	 */
	public TextBuffer sectionToBuffer(Element section) {
		TextBuffer buf = new TextBuffer(_tagTable);
		TextIter iter = buf.getStartIter();

		/*
		 * Run through the <step> <name> <task> element groups and call the
		 * appropriate convenience methods to build the display.
		 */

		// this fetches decendent nodes [only]
		NodeList steps = section.getElementsByTagName("step");
		int num_steps = steps.getLength();

		if (num_steps == 0) {
			buf.insertText("No step items listed for this section!?!");
			return buf;
		}

		for (int i = 0; i < num_steps; i++) {
			Element step = (Element) steps.item(i);
			String stepId = step.getAttribute("id");

			TextMark stepStartMark = buf.createMark(stepId + "start", iter, true);
			// the third argument, a String, refers to a TextTag to apply
			buf.insertText(iter, step.getAttribute("num") + ". ", "step");
			buf.insertText(iter, step.getAttribute("title") + "\n", "step");

			NodeList names = step.getElementsByTagName("name");
			int num_names = names.getLength();

			for (int j = 0; j < num_names; j++) {
				Element name = (Element) names.item(j);
				String nameId = name.getAttribute("id");

				TextMark nameStartMark = buf.createMark(nameId + "start", iter, true);
				buf.insertText(iter, name.getAttribute("who") + "\n", "name");

				NodeList tasks = name.getElementsByTagName("task");
				int num_tasks = tasks.getLength();

				for (int k = 0; k < num_tasks; k++) {
					Element task = (Element) tasks.item(k);
					String taskId = task.getAttribute("id");

					TextMark taskStartMark = buf.createMark(taskId + "start", iter, true);
					buf.insertText(iter, task.getAttribute("num") + ". ", "task_num");
					buf.insertText(iter, XmlUtils.getElementText(task) + "\n", "task");
					TextMark taskEndMark = buf.createMark(taskId + "end", iter, true);

					_taskMarkIndex.addMarks(taskId, taskStartMark, taskEndMark);
				}
				TextMark nameEndMark = buf.createMark(nameId + "end", iter, true);
				_nameMarkIndex.addMarks(nameId, nameStartMark, nameEndMark);
			}
			TextMark stepEndMark = buf.createMark(stepId + "end", iter, true);
			_stepMarkIndex.addMarks(stepId, stepStartMark, stepEndMark);

			showStepAsUpcoming(stepId);
		}
		return buf;
	}

	/**
	 * This method, like all the activate{Prev,Next} methods, are the callbacks
	 * invoked by the clicked handlers (as spec'd in the .glade file). It calls
	 * the ProcedureUserInterface method of the same name, which then callsback
	 * to this and other Window classes to affect the necessary changes.
	 */
	// have to be public so libglade signal connect can find them.
	public void activatePrevSection_cb() {
		// This will callback to the activateSection() routine here, but it will
		// also call other Windows' activateSection routines.
		ProcedureClient.ui.activatePrevSection();
	}

	public void activateNextSection_cb() {
		// This will callback to the activateSection() routine here, but it will
		// also call other Windows' activateSection routines.
		ProcedureClient.ui.activateNextSection();
	}

	public void activateCurrentSection_cb() {
		// This will callback to the activateSection() routine here, but it will
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
		if ((index < 0) && (index >= _numSections)) {
			throw new DebugException("DetailsWindow's activateSection() was called with an illegal section number, "
					+ index);
		}

		/*
		 * If we're now at the beginning, turn off the prev button
		 */
		if (index == 0) {
			_prev_toolbutton.setSensitive(false);
		} else {
			_prev_toolbutton.setSensitive(true);
		}
		/*
		 * If we're at now at the end, turn off the next button
		 */
		if (index == (_numSections - 1)) {
			_next_toolbutton.setSensitive(false);
		} else {
			_next_toolbutton.setSensitive(true);
		}

		/*
		 * If the current section contains the current step, then we don't need
		 * the "Current Step" button hot; otherwise we do.
		 */
		if (containsCurrentStep) {
			_current_toolbutton.setSensitive(false);
		} else {
			_current_toolbutton.setSensitive(true);
		}

		/*
		 * Now, do the UI alterations. First, bring up the appropriate
		 * TextBuffer, then set the title.
		 */
		_section_textview.setBuffer(_buffers[index]);
		_section_textview.showAll();

		_section_label.setMarkup("<span size=\"xx-large\">" + _titles[index] + "</span>");
		_currentSection = index;
	}

	/**
	 * As the last thing before main runs, we ask to grab focus. Called by
	 * whatever runner. HACK. Does it even work?
	 */
	public void initialGrabFocus() {
		_section_textview.grabFocus();
	}

	/**
	 * Display the current <step>visually as done, and display the next <step>as
	 * current.
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

		//TODO code me if necessary
	}

	/**
	 * Set the display of a task to upcoming, current, or done
	 * 
	 * @param taskId
	 *            which <task>to change, by XML ID.
	 */
	public void showTaskAsUpcoming(String taskId) {
		showAs(taskId, _taskMarkIndex, null, "upcoming_task");
	}

	public void showTaskAsCurrent(String taskId) {
		showAs(taskId, _taskMarkIndex, "upcoming_task", "current_task");
	}

	public void showTaskAsDone(String taskId) {
		showAs(taskId, _taskMarkIndex, "current_task", "done_task");
	}

	/**
	 * Set the display of a step to upcoming, current, or done
	 * 
	 * @param stepId
	 *            the <step>to show as [state]
	 */
	public void showStepAsUpcoming(String stepId) {
		showAs(stepId, _stepMarkIndex, null, "upcoming_step");
	}

	public void showStepAsCurrent(String stepId) {
		showAs(stepId, _stepMarkIndex, "upcoming_step", "current_step");
	}

	public void showStepAsDone(String stepId) {
		showAs(stepId, _stepMarkIndex, "current_step", "done_step");
	}

	/**
	 * Change the display of a visual element (section, step, name, task). You
	 * list the tag name you want removed, and the tag you want to apply in its
	 * place.
	 * 
	 * @param id
	 *            which element block to change, by XML ID.
	 * @param tagNameRemove
	 *            the TextTag formatting to apply (the name given when the tag
	 *            was created). Use null if you just want to add a tag.
	 * @param tagNameAdd
	 *            the TextTag formatting to apply (the name given when the tag
	 *            was created). Use null if you just want to remove a tag.
	 */
	private void showAs(String id, TextMarkIndex markIndex, String tagNameRemove, String tagNameAdd) {
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
		TextIter startIter, endIter;

		startMark = markIndex.getStartMarkById(id);
		buf = startMark.getBuffer();
		startIter = buf.getIter(startMark);

		endMark = markIndex.getEndMarkById(id);
		endIter = buf.getIter(endMark);

		if (tagNameRemove != null) {
			buf.removeTag(tagNameRemove, startIter, endIter);
		}
		if (tagNameAdd != null) {
			buf.applyTag(tagNameAdd, startIter, endIter);
		}
	}

}