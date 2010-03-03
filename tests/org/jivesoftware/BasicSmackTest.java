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
package org.jivesoftware;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * This unit test requires that that there is a jabber server at localhost,
 * that it speak SSL (we'll verify that) and that it support account creation.
 * A test user will be created and destroyed.
 * 
 * <P>
 * This is in tests/org/jivesoftware mostly for organization reasons; the
 * package name itself is somewhat arbitrary, and does represent where the
 * software we're testing came from. Notably, it not being (in this case)
 * org.jivesoftware.smack means the imports below are explicit and obvious,
 * and more importantly, helps us make sure we're not using package scope
 * classes or methods.
 * 
 * @author Andrew Cowie
 */
public class BasicSmackTest extends TestCase
{
    private static String SERVER = "localhost";

    private static String NODE = null;

    private static String RESOURCE = "SmackUnitTests";

    private static String PASSWORD = "obvious";

    /*
     * a short wait used before closing connections to let the jabber streams
     * catch up (so they can be viewed in the debugger without cutting them
     * short)
     */
    private static int WAIT = 400; // milliseconds

    /**
     * Change DEBUG_ENABLED to true to see JiveSoftware's built-in debugger.
     */
    public void setUp() {
        XMPPConnection.DEBUG_ENABLED = false;

        if (NODE == null) {
            NODE = "unittest-" + System.currentTimeMillis();
        }
    }

    /**
     * Somewhat unusually for a unit test, if this test fails we exit the VM.
     * There is no point proceeding with the remainder of the unit tests if we
     * can't get to a jabber server.
     */
    public void testInitialPlainConnection() {
        try {
            XMPPConnection conn = new XMPPConnection(SERVER);

            assertTrue(conn.isConnected());

            conn.close();
        } catch (Exception e) {
            System.err.println("Couldn't get a connection to the local jabber server (" + e.getMessage()
                    + ")");
            System.exit(1);
        }
    }

    public void testInitialSSLConnection() {
        try {
            SSLXMPPConnection conn = new SSLXMPPConnection(SERVER);

            assertTrue(conn.isConnected());
            assertTrue(conn.isSecureConnection());

            conn.close();
        } catch (Exception e) {
            fail("Couldn't get an SSL connection to the local jabber server. " + e.getMessage());
        }
    }

    public void testCreateAccount() {
        SSLXMPPConnection conn = null;

        try {
            conn = new SSLXMPPConnection(SERVER);
            assertTrue("Not connected", conn.isConnected());
            assertTrue("Not a secure connection", conn.isSecureConnection());

            AccountManager acctmgr = conn.getAccountManager();
            if (!acctmgr.supportsAccountCreation()) {
                fail("Can't run this unit test - target jabber server must support account creation.");
            }
            try {
                acctmgr.createAccount(NODE, PASSWORD);
            } catch (XMPPException xe) {
                XMPPError err = xe.getXMPPError();

                int code = err.getCode();

                if (code == 409) {
                    fail("Account jid "
                            + NODE
                            + "@"
                            + SERVER
                            + " already taken. That's not a crime, but realy is unexpected seeing as how we're trying to create an account name that's based on the current time. Probably means something else is wrong.");
                } else {
                    fail("Couldn't create test account. Error code: " + err.getCode() + ", message: "
                            + err.getMessage());
                }
            }

            // let the debugging catch up
            Thread.sleep(WAIT);
            conn.close();
        } catch (Exception e) {
            fail("Caught! " + e.getMessage());
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void testSSLLogin() {
        try {
            SSLXMPPConnection conn = new SSLXMPPConnection(SERVER);
            assertTrue(conn.isConnected());
            assertTrue(conn.isSecureConnection());

            try {
                conn.login(NODE, PASSWORD, RESOURCE);
            } catch (XMPPException xe) {
                XMPPError err = xe.getXMPPError();
                fail("Error code: " + err.getCode() + ", message: " + err.getMessage());
            }
            assertTrue(conn.isAuthenticated());

            // let the debugging catch up
            Thread.sleep(WAIT);
            conn.close();
        } catch (Exception e) {
            fail("Caught! " + e.getMessage());
        }
    }

    public void testDeleteAccount() {
        try {
            SSLXMPPConnection conn = new SSLXMPPConnection(SERVER);
            assertTrue(conn.isSecureConnection());

            conn.login(NODE, PASSWORD, RESOURCE);

            AccountManager acctmgr = conn.getAccountManager();
            try {
                acctmgr.deleteAccount();
            } catch (XMPPException xe) {
                XMPPError err = xe.getXMPPError();
                System.err.println("Received error code: " + err.getCode() + ", message: "
                        + err.getMessage());
                System.err.println("Couldn't delete test account from jabber server. That's not a big problem, just means not as tidied up as we should be.");
            }
            Thread.sleep(WAIT);
            conn.close();
        } catch (Exception e) {
            fail("Caught! " + e.getMessage());
        }
    }

    /**
     * This little silly "test" is just to simulate a Press Any Key to
     * Continue in the console if you're using the Smack debugger as it
     * disappears with app termination (rather than holding the app running
     * until its windows is closed)
     * 
     */
    public void testPAKTC() {
        if (!XMPPConnection.DEBUG_ENABLED) {
            return;
        }
        try {
            System.in.read();
        } catch (IOException ioe) {
        }
    }
}
