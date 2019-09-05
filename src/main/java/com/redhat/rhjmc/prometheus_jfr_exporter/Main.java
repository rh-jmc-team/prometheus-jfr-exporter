package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.eclipse.core.runtime.RegistryFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

		RecordingService rs = new RecordingService(config.host, config.port, config.eventOptions);
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

		if (arguments.size() < 1) { // TODO: confirm max arg count
			throw new IllegalArgumentException("Too few arguments");
		}

		if (arguments.size() > 2) { // TODO: confirm max arg count
			throw new IllegalArgumentException("Too many arguments");
		}

		config.host = arguments.get(0);
		config.port = arguments.get(1) == null ? JfrConnection.DEFAULT_PORT : Integer.parseInt(arguments.get(1));

		for (Map.Entry<String, String> option : options.entrySet()) {
			switch (option.getKey()) {
			case "max-size":
				try {
					config.maxSize = Long.parseLong(option.getValue());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(e);
				}
				break;
			case "max-age":
				try {
					config.maxAge = Long.parseLong(option.getValue());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(e);
				}
				break;
			case "event-options":
				try {
					config.eventOptions = new FileInputStream(option.getValue());
				} catch (FileNotFoundException e) {
					throw new IllegalArgumentException(e);
				}
			default:
				throw new IllegalArgumentException("Unrecognized option: " + option.getKey());
			}
		}

		if (config.eventOptions == null) {
			config.eventOptions = Main.class.getResourceAsStream("default.jfc");
		}

		return config;
	}

	private static void printHelp(PrintStream ps) {
		ps.println("Usage of Prometheus JFR exporter:");
		ps.println("	program <host> [port] [option...]");
		ps.println();
		ps.println("Options:");
		ps.println("	--max-size <long>		how much data is kept in the disk repository");
		ps.println("	--max-age <long>		how far back data is kept in the disk repository");
	}

	static class Config {
		String host;
		Integer port;
		Long maxAge;
		Long maxSize;
		InputStream eventOptions;
	}
}
