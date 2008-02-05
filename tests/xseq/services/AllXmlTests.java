/*
 * AllXmlTests.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2004-2005 Operational Dynamics
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
		//$JUnit-BEGIN$
		suite.addTestSuite(LoadXmlToDocumentTest.class);
		suite.addTestSuite(XmlIdsTest.class);
		suite.addTestSuite(XmlNumsTest.class);
		suite.addTestSuite(DocumentTraversalTest.class);
		suite.addTestSuite(IdIndexTest.class);
		//$JUnit-END$
		return suite;
	}
}