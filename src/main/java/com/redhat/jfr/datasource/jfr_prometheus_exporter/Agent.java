package com.redhat.jfr.datasource.jfr_prometheus_exporter;

import jdk.jfr.Configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Agent {
    private static final Logger LOGGER = Util.getLogger();

    public static void initAgent(String agentArgs) {
        LOGGER.fine("init agent");

        if (agent != null) {
            throw new RuntimeException("agent already initialized");
        }

        String hostname = "0.0.0.0";
        int port = 8080;
        Configuration config = null;
        try {
            config = Configuration.getConfiguration("default");
        } catch (IOException | ParseException e) {
            // intentionally empty
        }
        Map<String, String> settings = new HashMap<>();

        if (agentArgs != null && !"".equals(agentArgs)) {
            String[] args = agentArgs.split(":");
            for (String arg : args) {
                int idx = arg.indexOf('=');
                String key = idx != -1 ? arg.substring(0, idx): arg ;
                String value = idx != -1 ? arg.substring(arg.indexOf('=') + 1) : null;

                if (value == null || "".equals(value)) {
                    throw new IllegalArgumentException("no value for " + key + " given");
                }

                switch (key) {
                    case "hostname":
                        hostname = value;
                        break;
                    case "port":
                        port = Integer.parseInt(value);
                        break;
                    case "config":
                        if (value.contains("=")) {
                            // building settings from key-value pairs
                            for (String setting : value.split(",")) {
                                try {
                                    String[] pair = setting.trim().split("=");
                                    settings.put(pair[0], pair[1]);
                                } catch (IndexOutOfBoundsException e) {
                                    throw new IllegalArgumentException("unable to parse setting: " + setting);
                                }
                            }

                            continue;
                        }

                        try {
                            config = Configuration.getConfiguration(value);
                        } catch (IOException e) {
                            try {
                                config = Configuration.create(Paths.get(".").resolve(value));
                            } catch (IOException | ParseException ex) {
                                throw new RuntimeException(ex);
                            }
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                }
            }
        }

        try {
            if (settings.size() == 0) {
                agent = new Agent(hostname, port, config);
            } else {
                agent = new Agent(hostname, port, settings);
            }
        } catch (IOException e) {
            LOGGER.severe("Cannot load configuration: " + e.getLocalizedMessage());
            throw new UncheckedIOException(e);
        }
    }

    private static Agent agent;

    private RecordingService mRecordingService;
    private Server mServer;

    private Agent(String hostname, int port, Configuration configuration) throws IOException {
        mRecordingService = new RecordingService(configuration);

        init(hostname, port);
    }

    private Agent(String hostname, int port,  Map<String, String> settings) throws IOException {
        mRecordingService = new RecordingService(settings);

        init(hostname, port);
    }

    private void init(String hostname, int port) throws IOException {
        mServer = new Server(hostname, port);

        mServer.setRecordingService(mRecordingService);

        mRecordingService.start();
        mServer.start();

        LOGGER.info("Prometheus scrape endpoint running on http://" + hostname + ":" + port + "/metrics");
    }

    public static Agent getAgent() {
        return agent;
    }
}
