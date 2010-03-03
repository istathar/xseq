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
package xseq.network;

import generic.util.Debug;
import generic.util.DebugException;

import java.net.ConnectException;
import java.net.ProtocolException;

/**
 * Holds or points to all the pertinent information regarding our connection
 * to the Jabber network and to whom the master client is. A singleton is held
 * by ProcedureClient, which provides an entry point to all the communication
 * APIs in the xseq.network services layer.
 * 
 * @author Andrew Cowie
 */
public class NetworkConnection
{
    /**
     * Used as the resource to be appended to jabber user ids to identify this
     * client as specific and different from other clients (IM, for example).
     */
    public static final String RESOURCE = "Xseq";

    /*
     * Connection parameters
     */
    private String _jabberUsername = null;

    private String _jabberServer = null;

    private String _jabberPassword = null;

    /*
     * Jabber details
     */
    private SSLXMPPConnection _conn = null;

    private boolean _isVerified;

    public NetworkConnection(String username, String server, String password) {
        setJabberConfig(username, server, password);
    }

    /**
     * Set the Jabber connection parameters. Not that the act of setting any
     * new values on parameters causes the validated flag to be unset.
     * 
     * @param username
     * @param server
     * @param password
     */
    public void setJabberConfig(String username, String server, String password) {
        if (username != null) {
            if (!username.equals(_jabberUsername)) {
                _jabberUsername = username;
                _isVerified = false;
            }
        }
        if (server != null) {

            if (!server.equals(_jabberServer)) {
                _jabberServer = server;
                _isVerified = false;
            }
        }
        if (password != null) {
            if (!password.equals(_jabberPassword)) {
                _jabberPassword = password;
                _isVerified = false;
            }
        }
    }

    /**
     * Establish a connection to the Jabber server. Follows a degredation
     * cascade - if we can't get a proper SSL connection then we're going to
     * error.
     * 
     * @return
     * @throws ConnectException
     *             if unable to establish a connection
     * @throws ProtocolException
     *             in the unusual case that we can get a plain text connection
     *             but not an SSL one.
     */
    public void connect() throws ConnectException, ProtocolException {
        try {
            _conn = new SSLXMPPConnection(_jabberServer);

            if (!_conn.isSecureConnection()) {
                // could change this to XMPPException...
                _conn.close();
                throw new DebugException(
                        "Don't have a secure Jabber connection, but constructor didn't throw an exception either!?!");
            }
        } catch (XMPPException securexe) {
            if (Thread.currentThread().isInterrupted()) {
                Debug.print("jabber", "interrupted!");
                throw new RuntimeException();
            }
            Debug.print("jabber", "unable to get SSL connection: " + securexe.getMessage());

            try {
                XMPPConnection unencrypted = new XMPPConnection(_jabberServer);
                if (unencrypted.isConnected()) {
                    unencrypted.close();
                    throw new ProtocolException();
                }
            } catch (XMPPException plainxe) {
                // ignore, we were just trying to be helpful.
                Debug.print("jabber", "unable to get plain connection either: " + plainxe.getMessage());
            }

            throw new ConnectException();
        }
    }

    /**
     * Actually login to the Jabber server
     * 
     * @throws IllegalArgumentException
     *             if the login credentials supplied fail to do the trick.
     */
    public void login() throws IllegalArgumentException {
        if (_conn == null) {
            throw new DebugException(
                    "How did you manage to try to login() without having first connect()ed?");
        }
        try {
            _conn.login(_jabberUsername, _jabberPassword, RESOURCE);
        } catch (XMPPException loginxe) {
            Debug.print("jabber", "unable to login: " + loginxe.getMessage());
            throw new IllegalArgumentException("Unable to login to server");
        }

        _isVerified = true;
    }

    public boolean isJabberValidated() {
        return _isVerified;
    }

}
