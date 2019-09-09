package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.eclipse.core.runtime.RegistryFactory;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.internal.KnownRecordingOptions;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

		RecordingService rs = new RecordingService(config.host, config.port, config.recordingOptions,
				config.eventConfiguration);

		rs.start();

		for (int i : new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) {
			InputStream is = rs.openRecording();
			Files.copy(is, (new File("/tmp/dump" + i + ".jfr")).toPath(), StandardCopyOption.REPLACE_EXISTING);
			is.close();
			Thread.sleep(1000);
		}

		rs.stop();
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

		// parsing options
		for (Map.Entry<String, String> option : options.entrySet()) {
			switch (option.getKey()) {
			case "h":
			case "help":
				printHelp(System.err);
				System.exit(0);
			case "destinationFile":
			case "dumpOnExit":
			case "maxAge":
			case "maxSize":
			case "name":
				try {
					config.recordingOptions.putPersistedString(option.getKey(), option.getValue());
				} catch (QuantityConversionException e) {
					throw new IllegalArgumentException(e);
				}
				break;
			case "eventConfiguration":
				try {
					eventConfigurationInput = new FileInputStream(option.getValue());
				} catch (FileNotFoundException e) {
					throw new IllegalArgumentException(e);
				}
				break;
			default:
				throw new IllegalArgumentException("Unrecognized option: " + option.getKey());
			}
		}

		if (eventConfigurationInput == null) {
			eventConfigurationInput = Main.class.getResourceAsStream("default.jfc");
		}
		try {
			config.eventConfiguration = new EventConfiguration(EventConfiguration.createModel(eventConfigurationInput));
		} catch (IOException | ParseException e) {
			throw new IllegalArgumentException(e);
		}

		// parsing positional arguments
		if (arguments.size() < 1) {
			throw new IllegalArgumentException("Too few arguments");
		}

		if (arguments.size() > 2) {
			throw new IllegalArgumentException("Too many arguments");
		}

		config.host = arguments.get(0);
		config.port = arguments.size() > 1 ? Integer.parseInt(arguments.get(1)) : JfrConnection.DEFAULT_PORT;

		return config;
	}

	private static void printHelp(PrintStream ps) {
		ps.println("Usage of Prometheus JFR exporter:");
		ps.println("	program <host> [port] [option...]");
		ps.println();
		ps.println("Options:");
		ps.println("	-eventConfiguration <path>	a location where a .jfc configuration can be found");
		ps.println("	-destinationFile <path>		a location where data is written on recording stop");
		ps.println("	-dumpOnExit <bool>			set this recording to dump to disk when the JVM exits");
		ps.println("	-maxAge <time>				how far back data is kept in the disk repository");
		ps.println("	-maxSize <size>				how much data is kept in the disk repository");
		ps.println("	-name <name> 				a human-readable name (for example, \"My Recording\")");

	}

	static class Config {
		String host;
		int port;
		IMutableConstrainedMap<String> recordingOptions = KnownRecordingOptions.OPTION_DEFAULTS_V2
				.emptyWithSameConstraints();
		EventConfiguration eventConfiguration;
	}
}
