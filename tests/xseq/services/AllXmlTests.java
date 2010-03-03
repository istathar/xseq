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
package xseq.services;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Wrapper test suite around the all the unit tests that exercise the Xml
 * utilities.
 * 
 * @author Andrew Cowie
 */
public class AllXmlTests
{

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllXmlTests.suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for xseq.services");
        // $JUnit-BEGIN$
        suite.addTestSuite(LoadXmlToDocumentTest.class);
        suite.addTestSuite(XmlIdsTest.class);
        suite.addTestSuite(XmlNumsTest.class);
        suite.addTestSuite(DocumentTraversalTest.class);
        suite.addTestSuite(IdIndexTest.class);
        // $JUnit-END$
        return suite;
    }
}
