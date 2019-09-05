package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public class RecordingService {

	private IConstrainedMap<String> mRecordingOptions;
	private IConstrainedMap<EventOptionID> mEventOptions;

	private IFlightRecorderService mService;
	private IRecordingDescriptor mRecordingDescriptor;

	private IQuantity mLastScrape = UnitLookup.EPOCH_MS.quantity(System.currentTimeMillis());

	public RecordingService(String host, InputStream eventOptionInput)
			throws IOException, InterruptedException, ServiceNotAvailableException, QuantityConversionException,
			ParseException {
		this(host, JfrConnection.DEFAULT_PORT, eventOptionInput);
	}

	public RecordingService(String host, int port, InputStream eventOptionFile)
			throws IOException, InterruptedException, ServiceNotAvailableException, QuantityConversionException,
			ParseException {
		mService = new JfrConnection(host, port).getService();

		// TODO: load configurations
		mRecordingOptions = new RecordingOptionsBuilder(mService).build();
		XMLModel model = EventConfiguration.createModel(eventOptionFile);
		mEventOptions = new EventConfiguration(model)
				.getEventOptions(mService.getDefaultEventOptions().emptyWithSameConstraints());
	}

	public void start() throws FlightRecorderException {
		if (mRecordingDescriptor != null) {
			throw new RuntimeException("Recording service already started");
		}

		mRecordingDescriptor = mService.start(mRecordingOptions, mEventOptions);
	}

	public void stop() throws FlightRecorderException {
		if (mRecordingDescriptor == null) {
			throw new RuntimeException("Recording service already stopped");
		}

		mService.close(mRecordingDescriptor);
		mRecordingDescriptor = null;
	}

	public InputStream openRecording() throws FlightRecorderException {
		IQuantity now = UnitLookup.EPOCH_MS.quantity(System.currentTimeMillis());
		InputStream is = mService.openStream(mRecordingDescriptor, mLastScrape, now, false);
		mLastScrape = now;

		return is;
	}
}
