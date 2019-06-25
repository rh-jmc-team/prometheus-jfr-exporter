package com.redhat.jfr.datasource.jfr_prometheus_exporter;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {
	public static Logger getLogger(Class callerClass) {
		Logger logger = Logger.getLogger(callerClass.getName());

		if (ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp")) {
			// DEBUG: set logger level to finest if a debug agent attached
			logger.setLevel(Level.FINEST);
			logger.getParent().getHandlers()[0].setLevel(Level.FINEST);
		}

		return logger;
	}

	public static Logger getLogger() {
		return getLogger(Thread.currentThread().getStackTrace()[1].getClass());
	}
}
