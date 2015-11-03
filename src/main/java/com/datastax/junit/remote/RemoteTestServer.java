/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.junit.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.rmi.transport.proxy.RMIMasterSocketFactory;

/**
 * Server starts services required to the remote JUnit test execution. Under the hood,
 * it uses good old Java RMI for communication with the client JVM.
 */
public class RemoteTestServer
{
    public static final int DEFAULT_PORT = 4567;
    public static final String NAME = "remoteRunnerFactory";

    private final static Logger log = LoggerFactory.getLogger(RemoteTestServer.class);

    private final int port;
    private InetAddress bindAddrees;
    private Registry registry;

    /**
     *
     * @param port where RMI registry should listen at
     * @throws Exception
     */
    public RemoteTestServer(int port) throws Exception
    {
        this.port = port;
    }

    /**
     * Creates the test infrastructure having RMI registry on the port 4567
     * @throws Exception
     */
    public RemoteTestServer() throws Exception
    {
        this(DEFAULT_PORT);
    }

    /**
     *
     * @param socketAddress RMI registry will listen at the given socket
     * @throws Exception
     */
    public RemoteTestServer(InetSocketAddress socketAddress) throws Exception
    {
        this(socketAddress.getPort());
        this.bindAddrees = socketAddress.getAddress();
    }

    /**
     * Starts the test services
     * @throws Exception
     */
    public void start() throws Exception
    {
        if (bindAddrees == null) {
           registry = LocateRegistry.createRegistry(port);
        } else {
            SocketFactory socketFactory = new SocketFactory(bindAddrees);
            registry = LocateRegistry.createRegistry(port, socketFactory, socketFactory);
        }
        RunnerFactory factory = new DefaultRunnerFactory();

        registry.rebind(NAME, factory);
        log.info("Remote Test Runner service started, the RMI service registry listening at {}:{}", (bindAddrees == null ? "0.0.0.0": bindAddrees), port);
    }

    /**
     * Stops the services
     * @throws Exception
     */
    public void stop() throws Exception
    {
        registry.unbind(NAME);
        UnicastRemoteObject.unexportObject(registry, true);
    }

    public static void main(String[] args)
    {
        try
        {
            new RemoteTestServer().start();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static class SocketFactory extends RMIMasterSocketFactory
    {
        private final InetAddress bindAddress;

        public SocketFactory(InetAddress bindAddress)
        {
            this.bindAddress = bindAddress;
        }

        @Override public ServerSocket createServerSocket(int port) throws IOException
        {
            return new ServerSocket(port, 50, bindAddress);
        }
    }
}
