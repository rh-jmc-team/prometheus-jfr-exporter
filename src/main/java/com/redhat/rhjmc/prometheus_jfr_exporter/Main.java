package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.eclipse.core.runtime.RegistryFactory;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
	public static void main(String[] tokens) throws Exception {
		System.setProperty("org.openjdk.jmc.common.security.manager", SecurityManager.class.getName());
		RegistryFactory.setDefaultRegistryProvider(new RegistryProvider());

		Config config = null;
		try {
			config = parseCommand(tokens);
		} catch (IllegalArgumentException e) {
			e.printStackTrace(System.err);
			System.err.println();

			printHelp(System.err);

			System.exit(1);
		}

		RecordingService rs = new RecordingService(config.jmxAddr, config.recordingOptions, config.eventConfiguration);
		rs.start();

		HttpService hs = new HttpService(config.httpAddr);
		hs.start();

		JfrCollector collector = new JfrCollector(rs);
		collector.register();
	}

	private static Config parseCommand(String[] tokens) {
		Map<String, String> options = new HashMap<>();
		List<String> arguments = new ArrayList<>();
		{
			String key = null;
			for (String token : tokens) {
				if (token.indexOf("-") == 0) {
					key = token.substring(1);
					options.put(key, "");
					continue;
				}

				if (key != null) {
					options.put(key, token);
					key = null;
					continue;
				}

				arguments.add(token);
			}
		}

		Config config = new Config();
		InputStream eventConfigurationInput = null;

		// look for help
		if (options.containsKey("h") || options.containsKey("help")) {
			printHelp(System.err);
			System.exit(0);
		}

		// parsing positional arguments
		if (arguments.size() < 1) {
			throw new IllegalArgumentException("Too few arguments");
		}

		if (arguments.size() > 2) {
			throw new IllegalArgumentException("Too many arguments");
		}

		// parsing jmx "hostname:port"
		config.jmxAddr = parseHost(arguments.get(0), "localhost", JfrConnection.DEFAULT_PORT);

		// parsing http "hostname:port"
		if (arguments.size() == 2) {
			config.httpAddr = parseHost(arguments.get(1), "0.0.0.0", 8080);
		}

		// parsing options
		for (Map.Entry<String, String> option : options.entrySet()) {
			String key = option.getKey();
			String value = option.getValue();
			switch (option.getKey()) {
			case "disk":
			case "dumpOnExit":
				if (value == null || "".equals(value)) {
					value = "true";
				}
			case "maxAge":
			case "maxSize":
			case "name":
				try {
					config.recordingOptions.putPersistedString(key, value);
				} catch (QuantityConversionException e) {
					throw new IllegalArgumentException("Invalid option argument for " + key + ": " + value, e);
				}
				break;
			case "eventConfiguration":
				try {
					eventConfigurationInput = new FileInputStream(value);
				} catch (FileNotFoundException e) {
					throw new IllegalArgumentException("Event configuration not found: " + value, e);
				}
				break;
			default:
				throw new IllegalArgumentException("Unrecognized option: " + key);
			}
		}

		if (eventConfigurationInput == null) {
			eventConfigurationInput = Main.class.getResourceAsStream("default.jfc");
		}
		try {
			config.eventConfiguration = new EventConfiguration(EventConfiguration.createModel(eventConfigurationInput));
		} catch (IOException | ParseException e) {
			throw new IllegalArgumentException("Invalid event configuration input", e);
		}

		return config;
	}

	private static InetSocketAddress parseHost(String host, String defaultHostname, int defaultPort) {
		String hostname = defaultHostname;
		int port = defaultPort;

		String[] hostnamePort = host.split(":");
		if (hostnamePort.length > 2) {
			throw new IllegalArgumentException("invalid host: " + host);
		}
		if (hostnamePort[0].length() > 0) {
			hostname = hostnamePort[0];
		}
		if (hostnamePort.length > 1 && hostnamePort[1].length() > 0) {
			try {
				port = Integer.parseInt(hostnamePort[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid host: " + host);
			}
		}

		return new InetSocketAddress(hostname, port);
	}

	private static void printHelp(PrintStream ps) {
		ps.println("Usage of Prometheus JFR exporter:");
		ps.println("  program <[jmxHostname]:[jmxPort]> [[httpHostname]:[httpPort]] [option...]");
		ps.println();
		ps.println("Options:");
		ps.println("  -eventConfiguration <path>  a location where a .jfc configuration can be found");
		ps.println("  -disk [bool]                set this recording to continuously flush to the disk repository");
		ps.println("  -dumpOnExit [bool]          set this recording to dump to disk when the JVM exits");
		ps.println("  -maxAge <time>              how far back data is kept in the disk repository");
		ps.println("  -maxSize <size>             how much data is kept in the disk repository");
		ps.println("  -name <name>                a human-readable name (for example, \"My Recording\")");
	}

	static class Config {
		InetSocketAddress jmxAddr;
		InetSocketAddress httpAddr = new InetSocketAddress("0.0.0.0", 8080);

		IMutableConstrainedMap<String> recordingOptions = KnownRecordingOptions.OPTION_DEFAULTS_V2
				.emptyWithSameConstraints();
		EventConfiguration eventConfiguration;
	}
}
