package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IConnectionListener;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.internal.JMXConnectionDescriptor;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.internal.FlightRecorderServiceFactory;
import org.openjdk.jmc.ui.common.security.InMemoryCredentials;

import javax.management.remote.JMXServiceURL;
import java.net.MalformedURLException;

public class JfrConnection implements AutoCloseable {

	private static final String URL_FORMAT = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";
	public static final int DEFAULT_PORT = 9091;

	private final String host;
	private final int port;
	private final RJMXConnection rjmxConnection;
	private final IConnectionHandle handle;
	private final IFlightRecorderService service;

	public JfrConnection(String host, int port)
			throws ConnectionException, ServiceNotAvailableException, InterruptedException {
		this.host = host;
		this.port = port;
		this.rjmxConnection = attemptConnect(host, port, 0);
		this.handle = new DefaultConnectionHandle(rjmxConnection, "RJMX Connection", new IConnectionListener[0]);
		this.service = new FlightRecorderServiceFactory().getServiceInstance(handle);
	}

	public IConnectionHandle getHandle() {
		return this.handle;
	}

	public IFlightRecorderService getService() {
		return this.service;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public void disconnect() {
		this.rjmxConnection.close();
	}

	@Override
	public void close() {
		this.disconnect();
	}

	private RJMXConnection attemptConnect(String host, int port, int maxRetry)
			throws ConnectionException, InterruptedException {
		JMXServiceURL url;
		try {
			url = new JMXServiceURL(String.format(URL_FORMAT, host, port));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("illegal host or port", e);
		}

		JMXConnectionDescriptor cd = new JMXConnectionDescriptor(url, new InMemoryCredentials(null, null));
		ServerDescriptor sd = new ServerDescriptor(null, "Container", null);

		int attempts = 0;
		while (true) {
			try {
				RJMXConnection conn = new RJMXConnection(cd, sd, JfrConnection::failConnection);
				if (!conn.connect()) {
					failConnection();
				}
				return conn;
			} catch (ConnectionException e) {
				attempts++;
				System.out.println(String.format("Connection attempt %d failed.", attempts));
				if (attempts >= maxRetry) {
					System.out.println("Too many failed connections. Aborting.");
					throw e;
				} else {
					System.out.println(e);
				}
				Thread.sleep(500);
			}
		}
	}

	private static void failConnection() {
		throw new RuntimeException("Connection Failed");
	}
}
