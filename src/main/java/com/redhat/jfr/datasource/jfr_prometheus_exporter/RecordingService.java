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

    public String dumpPrometheusMetrics() throws IOException, CouldNotLoadRecordingException, EmptyRecordingException {
        Recording dump = mRecording.copy(true);
        InputStream is = dump.getStream(mThen, Instant.now());

        if (dump.getSize() == 0) {
            throw new EmptyRecordingException("Recording " + dump.getId() + " has no events recorded");
        }

        IItemCollection items = JfrLoaderToolkit.loadEvents(is);

        if (!items.hasItems()) {
            throw new EmptyRecordingException("Recording " + dump.getId() + " has no events recorded");
        }

        Map<String, List<Double>> result = new HashMap<>();

        StringBuilder sb = new StringBuilder();
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
                                .replaceAll("\\$", "_")
                                + "{attribute=\"" + attribute.getIdentifier()
                                + "\"}";
                for (IItem item : itemIterable) {
                    if (!(accessor.getMember(item) instanceof IQuantity)) {
                        break;
                    }

                    Double data = ((IQuantity) accessor.getMember(item)).doubleValue();
                    if (data.isNaN()) {
                        continue;
                    }

                    result.compute(query, (k, v) -> {
                        if (v == null) {
                            v = new LinkedList<>();
                        }
                        v.add(data);
                        return v;
                    });
                }
            }
        }

        List<Map.Entry<String, List<Double>>> entries = new ArrayList(result.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, List<Double>> entry : entries) {
            String query = entry.getKey();
            double sum = 0;
            for (Double item : entry.getValue()) {
                sum += item;
            }

            double avg = sum / entry.getValue().size();
            if (entry.getValue().size() == 0) {
                avg = 0;
            }

            sb.append(query).append(" ").append(String.format("%f", avg)).append("\n");
        }

        mThen = Instant.now();

        return sb.toString();
    }

    class EmptyRecordingException extends Exception {

        public EmptyRecordingException(String msg) {
            super(msg);
        }
    }
}
