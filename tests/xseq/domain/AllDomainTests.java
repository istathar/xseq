/*
 * AllDomainTests.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2005 Operational Dynamics
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
		//$JUnit-BEGIN$
		suite.addTestSuite(ProcedureTest.class);
		//$JUnit-END$
		return suite;
	}
}