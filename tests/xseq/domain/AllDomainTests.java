/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2005-2010 Operational Dynamics Consulting, Pty Ltd
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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Wrapper test suite around the all the unit tests that exercise the logic in
 * the individual Domain classes.
 * 
 * @author Andrew Cowie
 */
public class AllDomainTests
{

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllDomainTests.suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for xseq.domain");
        // $JUnit-BEGIN$
        suite.addTestSuite(ProcedureTest.class);
        // $JUnit-END$
        return suite;
    }
}
