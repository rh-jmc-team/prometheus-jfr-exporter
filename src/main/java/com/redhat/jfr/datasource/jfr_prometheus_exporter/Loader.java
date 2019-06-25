package com.redhat.jfr.datasource.jfr_prometheus_exporter;

import com.sun.tools.attach.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Loader {
    private static final Logger LOGGER = Util.getLogger();

    public static void main(String[] args) {
        LOGGER.fine("main invoked with arg: " + Arrays.toString(args));

        String agentJarPath = null;
        try {
            agentJarPath = (Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            if (!agentJarPath.contains(".jar")) {
                throw new RuntimeException("Cannot locate the exporter agent jar");
            }
        } catch (Exception e) {
            LOGGER.severe(e.getLocalizedMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        List<VirtualMachineDescriptor> jvms = VirtualMachine.list();
        System.out.println("Select a JVM to attach the exporter agent:");
        for (int i = 0; i < jvms.size(); i++) {
            System.out.printf("%d\t%s\n", i, jvms.get(i).displayName());
        }

        Scanner kbd = new Scanner(System.in);
        int i = kbd.nextInt();

        if (i >= jvms.size()) {
            LOGGER.severe("Invalid selection");
            System.exit(1);
        }

        try {
            VirtualMachine target = VirtualMachine.attach(jvms.get(i).id());
            target.loadAgent(agentJarPath, args.length > 0 ? args[0] : null);
        } catch (AttachNotSupportedException | IOException e) {
            LOGGER.severe("Cannot attach agent jar: " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } catch (AgentLoadException | AgentInitializationException e) {
            LOGGER.severe("Cannot initialize agent jar: " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        LOGGER.info("exporter agent attached to target JVM");
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.fine("premain invoked with arg: " + agentArgs);
        LOGGER.info("JFR Prometheus Exporter agent statically loaded");

        Agent.initAgent(agentArgs);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        LOGGER.fine("agentmain invoked with arg: " + agentArgs);
        LOGGER.info("JFR Prometheus Exporter agent dynamically loaded");

        Agent.initAgent(agentArgs);
    }
}
