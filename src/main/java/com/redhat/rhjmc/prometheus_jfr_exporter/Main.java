package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.eclipse.core.runtime.RegistryFactory;
import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {
	public static void main(String[] args) throws Exception {
		System.setProperty("org.openjdk.jmc.common.security.manager", SecurityManager.class.getName());

		RegistryFactory.setDefaultRegistryProvider(new RegistryProvider());
		JFRConnection conn = new JFRConnection("localhost", 5555);

		IFlightRecorderService service = conn.getService();

		IConstrainedMap<String> recordingOptions = new RecordingOptionsBuilder(service)
				// TODO: add options
				.build();

		InputStream in = new FileInputStream("/tmp/default.jfc");
		XMLModel model = EventConfiguration.createModel(in);
		IConstrainedMap<EventOptionID> eventOptions = new EventConfiguration(model)
				.getEventOptions(service.getDefaultEventOptions().emptyWithSameConstraints());

		IRecordingDescriptor recording = service.start(recordingOptions, eventOptions);

		Thread.sleep(10 * 1000);

		service.stop(recording);
		InputStream is = service.openStream(recording, true);

		Files.copy(is, (new File("/tmp/dump.jfr")).toPath(), StandardCopyOption.REPLACE_EXISTING);

		is.close();
	}
}
