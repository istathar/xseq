/*
 * AllNetworkTests.java
 * 
 * See LICENCE file for usage and redistribution terms
 * Copyright (c) 2005 Operational Dynamics
 */
package xseq.network;

import org.jivesoftware.BasicSmackTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit tests for the network layer.
 * 
 * @author Andrew Cowie
 */
public class AllNetworkTests
{

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllNetworkTests.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for xseq.network");
		//$JUnit-BEGIN$
		suite.addTestSuite(BasicSmackTest.class);
		//$JUnit-END$
		return suite;
	}
}