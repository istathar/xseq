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

import generic.util.Debug;

import org.gnome.gtk.Gtk;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import xseq.domain.Procedure;
import xseq.domain.State;

/**
 * This is just an expedient holding ground for app specific global UI code.
 * Probably going to move somewhere.
 * 
 * @author Andrew Cowie
 */
public class ProcedureUserInterface
{
    public OverviewWindow _overview = null;

    public DetailsWindow _details = null;

    private QuickButtonsWindow _quick = null;

    // used to govern what is displayed in the various status windows, and are
    // keyed by the activate* methods.
    private int _currentSection;

    private int _numSections;

    // hold a reference to the Procedure that this UI is currently
    // displaying.
    public Procedure _procedure;

    public String _currentStepId;

    // which participant am I?
    private String _whoAmI;

    // used when I have a current task.
    private String _myCurrentTaskId;

    private int _myCurrentState;

    /**
     * Instantiate the various windows that comprise the UI.
     * 
     * @param p
     *            The Procedure to be instantiated
     */
    public ProcedureUserInterface(Procedure p) {
        this._procedure = p;

        Document dom = p.getDOM();
        NodeList sections = dom.getElementsByTagName("section");
        _currentSection = 0;
        _numSections = sections.getLength();

        _currentStepId = null;
        _myCurrentTaskId = null;
        _myCurrentState = -1;

        _overview = new OverviewWindow(p);
        _details = new DetailsWindow(p);
        _quick = new QuickButtonsWindow();

        _details.initialGrabFocus();
    }

    /**
     * Call in points for moving the UI around the sections of a procedure.
     */

    public void activatePrevSection() {
        _currentSection--;
        activateSection();
    }

    public void activateCurrentSection() {
        /*
         * This is repeatitive - we end up doing almost the same code again
         * below.
         */
        if (_currentStepId == null) {
            _currentSection = 0;
        } else {
            String currentSectionId = _procedure.getParentId(_currentStepId, "section");

            Document dom = _procedure.getDOM();
            NodeList sections = dom.getElementsByTagName("section");

            for (int i = 0; i < _numSections; i++) {
                Element section = (Element) sections.item(i);
                if (section.getAttribute("id").equals(currentSectionId)) {
                    _currentSection = i;
                }
            }
        }
        activateSection();
    }

    public void activateNextSection() {
        _currentSection++;
        activateSection();
    }

    public void activateSection(int i) {
        _currentSection = i;
        activateSection();
    }

    /**
     * Display the section that contains (is) a specified ID
     */
    public void activateSection(String id) {
        String sectionId = _procedure.getParentId(id, "section");

        Document dom = _procedure.getDOM();
        NodeList sections = dom.getElementsByTagName("section");
        for (int i = 0; i < _numSections; i++) {
            Element section = (Element) sections.item(i);
            if (section.getAttribute("id").equals(sectionId)) {
                activateSection(i);
                return;
            }
        }
    }

    /**
     * Activate what is set in _currentSection.
     */
    private void activateSection() {
        /*
         * bounds check, including the perfectly normal cases of trying to go
         * too far. It's here, just so we can generalize the logic for use in
         * other events rather than the next/previous case of the toolbuttons.
         */
        if (_currentSection <= 0) {
            _currentSection = 0;
        } else if (_currentSection >= (_numSections - 1)) {
            _currentSection = (_numSections - 1); // annoying, zero
            // origin...
        }

        boolean isCurrentStepInSection = false;

        if (_currentStepId == null) {
            if (_currentSection == 0) {
                isCurrentStepInSection = true;
            }
        } else {
            String currentSectionId = _procedure.getParentId(_currentStepId, "section");

            Document dom = _procedure.getDOM();
            NodeList sections = dom.getElementsByTagName("section");

            Element section = (Element) sections.item(_currentSection);
            if (section.getAttribute("id").equals(currentSectionId)) {
                isCurrentStepInSection = true;
            }
        }

        /*
         * and now, finally, realize it.
         */
        _details.activateSection(_currentSection, isCurrentStepInSection);
        _overview.activateSection(_currentSection, isCurrentStepInSection);
    }

    public void setUser(String who) {
        // TODO validation of input
        // TODO authentication of some sort?
        this._whoAmI = who;

    }

    public String getUser() {
        return this._whoAmI;
    }

    /**
     * This is the entry point called by event handlers. It simply calls
     * setButtonState(), unless this is an event is marking a task as done,
     * then we propagate that information as well.
     */
    public void setMyState(int state) {
        Debug.print("events", "asked for state " + state + ", ui state was " + _myCurrentState);
        if (_myCurrentState != state) {
            Debug.print("events", "so decided to setButtonState()");
            /*
             * This prevents the return trips from calling this again.
             */
            _myCurrentState = state;

            /*
             * DUE TO SECOND SET OF StateButtons, THIS CAUSES RECURSION BACK
             * TO THIS METHOD!!!
             */
            setButtonState(state);

            /*
             * As this can (does) get called multiple times; we use
             * _currentTaskId being set as the marker that it actually needs
             * action.
             */
            if ((state == State.DONE) && (_myCurrentTaskId != null)) {
                Debug.print("events", "further, ui decided to call setTaskAsDone(" + _myCurrentTaskId
                        + ")");
                setTaskAsDone(_myCurrentTaskId);
            }
        } else {
            Debug.print("events", "No action needed");
        }
        Debug.print("events", "ui state now " + _myCurrentState);
    }

    /**
     * Send the appropriate signals to the various windows that have
     * StateButtons displayed. There is a check on the buttons' side to only
     * take action if they are not, in fact, active.
     */
    private void setButtonState(int state) {
        _details.stateButtons.activate(state);
        _quick._stateButtons.activate(state);
    }

    /**
     * Entry point to order initiation of a Procedure.
     * 
     * <P>
     * TODO start timers (record timestamps somewhere?)
     */
    public void startProcedure() {
        _details.top.present();
        /*
         * Get user back to beginning.
         */
        activateSection(0);

        /*
         * Set first <step> as current.
         */
        String firstTaskId = _procedure.getFirstTaskId("n0");
        String firstStepId = _procedure.getParentId(firstTaskId, "step");

        startStep(firstStepId);
    }

    /**
     * TODO is this "porcedure is finished"? Or "regardless of state, stop
     * this damn thing"?
     * <P>
     * TODO stop timers
     */
    public void stopProcedure() {
        _currentStepId = null;
    }

    /**
     * Assumed that the step is not already marked done...
     * 
     * @param stepId
     *            the id of the step to show as current.
     */
    private void startStep(String stepId) {
        _currentStepId = stepId;
        _details.showStepAsCurrent(stepId);

        String taskId = _procedure.getFirstTaskId(stepId, _whoAmI);
        if (taskId != null) {
            startMyTask(taskId);
        }
    }

    /**
     * Used to fire up a task, specifically if it belongs to this user.
     * 
     * @param taskId
     *            a task ID string, assumed to belong to _who.
     */
    public void startMyTask(String taskId) {
        _myCurrentTaskId = taskId;
        _details.showTaskAsCurrent(taskId);
        _details.top.present();
        setButtonState(State.WORKING);
    }

    /**
     * Centralized entry point to mark a tast as done and to instruct the UI
     * to update the relevent displays accordingly.
     * 
     * <P>
     * It seems that the actual direct action of updating the Procedure should
     * be somewhere else... but then, maybe the _procedure should be held
     * somewhere else. Then again, they're all references, so who cares. This
     * is an (the?) instance class for the client, and it's a event driven
     * user interface.
     * 
     * @param taskId
     *            The task which is to be set as done.
     */
    public void setTaskAsDone(String taskId) {
        Document doc = _procedure.getDOM();

        // in case it was mine...
        if (_procedure.isTaskMine(taskId, _whoAmI)) {
            _myCurrentTaskId = null;
        }

        /*
         * Update the DOM tree; conveniently returns the next Task in line (or
         * null) the consequences of which we deal with below.
         */
        String nextTaskId = _procedure.setTaskAsDone(taskId);

        /*
         * Update the UI for this task
         */
        _details.showTaskAsDone(taskId);

        /*
         * Start working through what else has to change as a consequence of
         * this task being done.
         */
        if (nextTaskId == null) {
            // this is functionally equivalent to calling isNameDone(). Do we
            // need to call that anyway?

            if (_procedure.isStepDone(taskId)) {
                String stepId = _procedure.getParentId(taskId, "step");
                _details.showStepAsDone(stepId);
                setButtonState(State.STANDBY);

                /*
                 * Fire up the next step
                 */
                String nextStepId = _procedure.getNextStepId(stepId);
                if (nextStepId == null) {
                    if (_procedure.isProcedureDone(taskId)) {
                        stopProcedure();
                        return;
                    }
                } else {
                    startStep(nextStepId);
                }

                /*
                 * taskId is still the old, just completed one - this is
                 * backwards here so that the logic controlling the
                 * CurrentStep buttons (which relies on currentStepId) will
                 * work as we cross section boundaries.
                 */

                if (_procedure.isSectionDone(taskId)) {
                    // This seems silly, but it's necessary to get to the
                    // current section page before advancing one.
                    activateSection(taskId);
                    activateNextSection();
                }
            }

        } else {
            if (_procedure.isTaskMine(nextTaskId, _whoAmI)) {
                startMyTask(nextTaskId);
            }
        }
    }

    /**
     * Be aware that calling this you should expect to loose exectution
     * control as this will cause the Gtk.main() loop to exit and control to
     * go to the statement following it. Some threads may in fact return here,
     * so you should not follow a call to shutdown() with any code and let
     * methods return immediately following the call.
     */
    public void shutdown() {
        // TODO attempt to free any instantiated Windows, close network
        // connections, and what not.
        Gtk.mainQuit();
        // it's somewhat indeterminate about whether exectution control will
        // ever return here; in any case we don't [need to] depend on it.
    }
}
