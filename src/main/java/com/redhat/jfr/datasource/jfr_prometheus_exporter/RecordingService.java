package com.redhat.jfr.datasource.jfr_prometheus_exporter;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.openjdk.jmc.common.item.*;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

public class RecordingService {
    private Configuration mConfiguration;
    private Map<String, String> mSettings;

    private Recording mRecording;
    private Instant mThen; // TODO: properly maintain time interval

    private Map<String, Double> mLatestMetrics = new HashMap<>();

    public RecordingService(Configuration configuration) {
        mConfiguration = configuration;
    }

    public RecordingService(Map<String, String> settings) {
        mSettings = settings;
    }

    public RecordingService() throws IOException, ParseException {
        this(Configuration.getConfiguration("default"));
    }

    public void start() {
        if (mSettings != null) {
            mRecording = new Recording(mSettings);
        } else {
            mRecording = new Recording(mConfiguration);
        }

        mRecording.start();
        mThen = Instant.now();
    }

    public void stop() {
        mRecording.stop();
    }

    private void updateLatestMetrics() throws IOException, CouldNotLoadRecordingException, EmptyRecordingException {
        Recording dump = mRecording.copy(true);
        InputStream is = dump.getStream(mThen, Instant.now());

        if (dump.getSize() == 0) {
            throw new EmptyRecordingException("Recording " + dump.getId() + " has no events recorded");
        }

        IItemCollection items = JfrLoaderToolkit.loadEvents(is);

        if (!items.hasItems()) {
            throw new EmptyRecordingException("Recording " + dump.getId() + " has no events recorded");
        }

        Map<String, Double> result = new HashMap<>();

        IMemberAccessor<?, IItem> endTimeAccessor = ItemToolkit.accessor(JfrAttributes.END_TIME);
        for (IItemIterable itemIterable : items) {
            // each itemIterable is an array of events sharing a single type.
            IType<IItem> type = itemIterable.getType();
            List<IAttribute<?>> attributes = type.getAttributes();
            for (IAttribute<?> attribute : attributes) {
                IMemberAccessor<?, IItem> accessor = ItemToolkit.accessor(attribute);
                if (JfrAttributes.START_TIME.getIdentifier().equals(attribute.getIdentifier())) {
                    continue;
                }
                if (JfrAttributes.END_TIME.getIdentifier().equals(attribute.getIdentifier())) {
                    continue;
                }
                if(JfrAttributes.EVENT_TYPE.getIdentifier().equals(attribute.getIdentifier())) {
                    continue;
                }

                String query =
                        type.getIdentifier()
                                .replaceAll("\\.", "_")
                                .replaceAll("\\$", ":")
                                + "{attribute=\"" + attribute.getIdentifier()
                                + "\"}";

                Double latestData = null;
                IQuantity latestEndTime = null;
                for (IItem item : itemIterable) {
                    if (!(accessor.getMember(item) instanceof IQuantity)) {
                        break;
                    }

                    Double data = ((IQuantity) accessor.getMember(item)).doubleValue();
                    IQuantity endTime = ((IQuantity)endTimeAccessor.getMember(item));
                    if (data.isNaN()) {
                        continue;
                    }

                    if (latestData == null || endTime.compareTo(latestEndTime) > 0) {
                        latestData = data;
                        latestEndTime = endTime;
                    }
                }

                if (latestData != null) {
                    result.put(query, latestData);
                }
            }
        }

        for (Map.Entry<String, Double> lastMetric : mLatestMetrics.entrySet()) {
            result.putIfAbsent(lastMetric.getKey(), lastMetric.getValue());
        }
        mLatestMetrics = result;

        mThen = Instant.now();
    }

    public String dumpPrometheusMetrics() throws IOException, CouldNotLoadRecordingException, EmptyRecordingException {
        try {
            updateLatestMetrics();
        } catch (EmptyRecordingException e) {
            if (mLatestMetrics.size() == 0) {
                throw e;
            }
        }

        StringBuilder sb = new StringBuilder();
        List<Map.Entry<String, Double>> entries = new ArrayList(mLatestMetrics.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, Double> entry : entries) {
            sb.append(String.format("%s %f\n", entry.getKey(), entry.getValue()));
        }

        return sb.toString();
    }

    class EmptyRecordingException extends Exception {

        public EmptyRecordingException(String msg) {
            super(msg);
        }
    }
}
