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
package xseq.domain;

import generic.util.DebugException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import xseq.services.ElementIndex;
import xseq.services.IdIndex;
import xseq.services.XmlUtils;

/**
 * Encapsulation of a loaded procedure.
 * 
 * 
 * @author Andrew Cowie
 */
public class Procedure
{
    private Document _dom = null;

    private ElementIndex _elementIndex = null;

    private IdIndex _stepIndex = null;

    public Procedure(String xml) {
        if (xml == null) {
            throw new DebugException(
                    "You shouldn't have been able to attempt to instantiate a Procedure with a null String!");
        }
        /*
         * turn the source XML into a DOM
         */
        _dom = XmlUtils.xmlStringToDOM(xml);
        XmlUtils.addIDs(_dom);

        // there really only is 1 <procedure>. No need to number it right now,
        // but en verra. If we ever have mulitple <procedure>s per document,
        // then we'll have to have a higher parent node of some sort.

        NodeList sections = _dom.getElementsByTagName("section");
        int num_sections = sections.getLength();
        for (int j = 0; j < num_sections; j++) {
            Element section = (Element) sections.item(j);

            section.setAttribute("num", XmlUtils.indexToType(j + 1, 'I'));
        }

        /*
         * steps number consecutively across the entire procedure document;
         * they don't restart across section boundaries.
         */
        NodeList steps = _dom.getElementsByTagName("step");
        int num_steps = steps.getLength();
        for (int k = 0; k < num_steps; k++) {
            Element step = (Element) steps.item(k);

            step.setAttribute("num", XmlUtils.indexToType(k + 1, '1'));

            NodeList names = step.getElementsByTagName("name");
            int num_names = names.getLength();
            for (int m = 0; m < num_names; m++) {
                Element name = (Element) names.item(m);

                /*
                 * We skip setting a num attribute on name as it doesn't have
                 * a canonical number
                 */
                NodeList tasks = name.getElementsByTagName("task");
                int num_tasks = tasks.getLength();
                for (int n = 0; n < num_tasks; n++) {
                    Element task = (Element) tasks.item(n);

                    task.setAttribute("num", XmlUtils.indexToType(n + 1, 'a'));
                }
            }
        }

        /*
         * Build indexes
         */
        _elementIndex = new ElementIndex(_dom);
        _stepIndex = new IdIndex(_dom, "step");
    }

    /**
     * @return
     */
    public Document getDOM() {
        return _dom;
    }

    /**
     * Set a task as done, by ID.
     * 
     * @param taskId
     * @return the next task [id] in this step, or null if all the tasks in
     *         this step for this person are done.
     */
    public String setTaskAsDone(String taskId) {
        Element task = _elementIndex.getElementById(taskId);

        task.setAttribute("status", "done");

        return getNextTaskId(taskId);
    }

    /**
     * Get the next task ID in this step
     * 
     * @param thisTaskId
     * @return null if all the tasks for [this person in] this step are
     *         complete (as judged by sequential ordering; preceding tasks
     *         before thisTaskId are assumed to already be complete).
     */
    public String getNextTaskId(String thisTaskId) {
        Element task = _elementIndex.getElementById(thisTaskId);

        return getNextTask(task);
    }

    /*
     * Actually do the work. This is admittedly tricky code as you have to
     * fight through the non-Element Nodes to get to the next Element
     * sibbling.
     */
    private String getNextTask(Element thisTask) {
        Node sibling = thisTask;

        while ((sibling = sibling.getNextSibling()) != null) {
            if (sibling.getNodeType() != Node.ELEMENT_NODE) {
                // System.err.println("Sibling node not an Element, its " +
                // sibling.getNodeType() + " " + sibling.toString());
                continue;
            } else {
                break;
            }
        }
        Element nextTask = (Element) sibling;

        if (nextTask == null) {
            // if null it's ok - just means last task in this step/name.
            return null;
        } else {
            return nextTask.getAttribute("id");
        }
    }

    /**
     * Find out whether the current {procedure, section, step, name} is
     * complete.
     * 
     * @param taskId
     * @return true if all of the current tasks for this hierarchy are done.
     */
    public boolean isProcedureDone(String taskId) {
        return isDone(taskId, "procedure");
    }

    public boolean isSectionDone(String taskId) {
        return isDone(taskId, "section");
    }

    public boolean isStepDone(String taskId) {
        return isDone(taskId, "step");
    }

    public boolean isNameDone(String taskId) {
        // will only be one step up, but same logic as other is...Done methods
        return isDone(taskId, "name");
    }

    private boolean isDone(String taskId, String parentTagName) {
        Element task = _elementIndex.getElementById(taskId);

        Element parent = XmlUtils.getParentElement(task, parentTagName);

        NodeList tasks = parent.getElementsByTagName("task");
        for (int i = 0; i < tasks.getLength(); i++) {
            Element t = (Element) tasks.item(i);
            if (!(t.getAttribute("status").equals("done"))) {
                return false;
            }
        }
        return true;
    }

    /*
     * This is a little more straight forward, as tasks are singular and are
     * either done or not.
     */
    public boolean isTaskDone(String taskId) {
        Element task = _elementIndex.getElementById(taskId);
        if (task == null) {
            throw new DebugException("You asked for a task, " + taskId + ", that isn't there!");
        }
        String status = task.getAttribute("status");
        return ((status != null) && (status.equals("done")));
    }

    /**
     * Utility method to get the id of a given parent working from a given
     * child node (ie, a task). This is needed for things like looking up
     * TextTags to adjust the presentation in DetailsWindow.
     * 
     * @param id
     *            the ID string of the task you want the parent step of
     * @param parentTagName
     *            the tag name of the parent element to go up for.
     * @return the ID string of the found parent.
     */
    public String getParentId(String id, String parentTagName) {
        Element task = _elementIndex.getElementById(id);
        Element parent = XmlUtils.getParentElement(task, parentTagName);
        if (parent == null) {
            // TODO, yes, this could be a null return, but this should be used
            // under controlled circumstances.
            throw new DebugException("You asked for a <" + parentTagName + "> parent of element " + id
                    + ". There was't one.");
        }
        return parent.getAttribute("id");
    }

    /**
     * Decend down from a node as specified by ID, and find the first Task's
     * ID. This is used as we cross from one step or section to the next, to
     * pull out the next task to be lit up.
     * 
     * @param parentId
     *            the ID string of the parent node to descend from.
     * @return The ID string describing the found task, or null if no task is
     *         found as a child of the specified node.
     */
    public String getFirstTaskId(String parentId) {
        Element parent = _elementIndex.getElementById(parentId);
        return getFirstTaskId(parent);
    }

    /**
     * Decend down from an Element, and find the first Task's ID. This is as a
     * utility method used to pull out the next task to be lit up.
     * 
     * @param parent
     *            the Element to be used as the parent node to descend from.
     * @return The ID string describing the found task, or null if no task is
     *         found as a child of the specified node.
     */
    public String getFirstTaskId(Element parent) {
        NodeList tasks = parent.getElementsByTagName("task");
        if (tasks.getLength() == 0) {
            return null;
        }
        Element firstTask = (Element) tasks.item(0);

        return firstTask.getAttribute("id");
    }

    /**
     * Given a step by ID, find the first task for a given person.
     * 
     * @param stepId
     *            the ID of the enclosing step
     * @param who
     *            the string indicating the name/@who whose tasks we want the
     *            first of.
     * @return That person's first task's ID, or null if none.
     */
    public String getFirstTaskId(String stepId, String who) {
        Element step = _elementIndex.getElementById(stepId);
        NodeList names = step.getElementsByTagName("name");
        for (int i = 0; i < names.getLength(); i++) {
            Element name = (Element) names.item(i);
            if (name.getAttribute("who").equals(who)) {
                NodeList tasks = name.getElementsByTagName("task");
                if (tasks.getLength() == 0) {
                    /*
                     * who is mentioned, but has no tasks. Weird, but not
                     * illegal.
                     */
                    return null;
                }
                Element firstTask = (Element) tasks.item(0);
                return firstTask.getAttribute("id");
            }
        }
        /*
         * person who wasn't found in this step, so, no tasks.
         */
        return null;
    }

    /**
     * Get the ID of the next step.
     * 
     * @param currentStepId
     *            an (any) ID string from the previous step.
     * @return the ID string of the next step, or null if we don't know of
     *         one.
     */
    public String getNextStepId(String currentStepId) {
        return _stepIndex.getNextId(currentStepId);
    }

    /**
     * Quick method to determine whether a task (specified by ID), belongs to
     * a particular person.
     * 
     * @param taskId
     *            the ID string of the task
     * @param who
     *            the person who we are asking about
     */
    public boolean isTaskMine(String taskId, String who) {
        String nameId = getParentId(taskId, "name");
        Element name = _elementIndex.getElementById(nameId);
        if (name.getAttribute("who").equals(who)) {
            return true;
        } else {
            return false;
        }
    }
}
