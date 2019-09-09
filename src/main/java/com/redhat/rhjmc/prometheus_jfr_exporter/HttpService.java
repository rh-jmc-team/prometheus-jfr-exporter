package com.redhat.rhjmc.prometheus_jfr_exporter;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpService {
	private InetSocketAddress mSocketAddr;
	private HTTPServer mHTTPServer;

	public HttpService(InetSocketAddress addr) {
		mSocketAddr = addr;
	}

	public void start() throws IOException {
		if (mHTTPServer != null) {
			throw new RuntimeException("HTTP Service already started");
		}
		mHTTPServer = new HTTPServer(mSocketAddr, CollectorRegistry.defaultRegistry);
	}

	public void stop() {
		if (mHTTPServer == null) {
			throw new RuntimeException("HTTP Service already stopped");
		}
		mHTTPServer.stop();
		mHTTPServer = null;
	}
}
