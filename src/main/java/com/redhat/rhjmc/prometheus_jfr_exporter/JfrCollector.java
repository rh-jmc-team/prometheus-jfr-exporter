package com.redhat.rhjmc.prometheus_jfr_exporter;

import io.prometheus.client.Collector;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JfrCollector extends Collector { // TODO: implement Collector.Describable?
	private RecordingService mRecordingService;

	public JfrCollector() {
	}

	public JfrCollector(RecordingService rs) {
		this();

		setRecordingService(rs);
	}

	public void setRecordingService(RecordingService rs) {
		mRecordingService = rs;
	}

	@Override
	public List<MetricFamilySamples> collect() {
		try {
			return doCollect();
		} catch (Exception e) {
			// TODO: log exception
			e.printStackTrace(System.err);
			return Collections.emptyList();
		}
	}

	public List<MetricFamilySamples> doCollect()
			throws FlightRecorderException, IOException, CouldNotLoadRecordingException, EmptyRecordingException {
		if (mRecordingService == null) {
			return Collections.emptyList();
		}

		InputStream is = mRecordingService.openRecording();

		IItemCollection items = JfrLoaderToolkit.loadEvents(is);

		if (!items.hasItems()) {
			throw new EmptyRecordingException("Recording has no events recorded");
		}

		List<MetricFamilySamples> metrics = new ArrayList<>();

		IMemberAccessor<?, IItem> endTimeAccessor = ItemToolkit.accessor(JfrAttributes.END_TIME);
		for (IItemIterable itemIterable : items) {
			// each itemIterable is an array of events sharing a single type.
			IType<IItem> type = itemIterable.getType();
			String metricName = type.getIdentifier().replaceAll("\\.", "_").replaceAll("\\$", ":");
			Collector.Type metricType = Type.GAUGE;
			String metricDescription = type.getDescription();

			List<MetricFamilySamples.Sample> samples = new ArrayList<>();

			List<IAttribute<?>> attributes = type.getAttributes();
			for (IAttribute<?> attribute : attributes) {
				IMemberAccessor<?, IItem> accessor = ItemToolkit.accessor(attribute);
				if (JfrAttributes.START_TIME.getIdentifier().equals(attribute.getIdentifier())) {
					continue;
				}
				if (JfrAttributes.END_TIME.getIdentifier().equals(attribute.getIdentifier())) {
					continue;
				}
				if (JfrAttributes.EVENT_TYPE.getIdentifier().equals(attribute.getIdentifier())) {
					continue;
				}

				for (IItem item : itemIterable) {
					if (!(accessor.getMember(item) instanceof IQuantity)) {
						break;
					}

					double data = ((IQuantity) accessor.getMember(item)).doubleValue();
					if (Double.isNaN(data)) {
						continue;
					}

					IQuantity endTime = ((IQuantity) endTimeAccessor.getMember(item));
					try {
						samples.add(new MetricFamilySamples.Sample(metricName, Collections.singletonList("attribute"),
								Collections.singletonList(attribute.getIdentifier()), data,
								endTime.longValueIn(UnitLookup.EPOCH_MS)));
					} catch (QuantityConversionException e) {
						// this should never happen
						throw new RuntimeException(e);
					}
				}
			}

			metrics.add(new MetricFamilySamples(metricName, metricType, metricDescription, samples));
		}

		return metrics;
	}

	/*
	private void updateLatestMetrics() throws IOException, CouldNotLoadRecordingException, EmptyRecordingException {
		InputStream is =

				IItemCollection items = JfrLoaderToolkit.loadEvents(is);

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
				if (JfrAttributes.EVENT_TYPE.getIdentifier().equals(attribute.getIdentifier())) {
					continue;
				}

				String query =
						type.getIdentifier().replaceAll("\\.", "_").replaceAll("\\$", ":") + "{attribute=\"" + attribute
								.getIdentifier() + "\"}";

				Double latestData = null;
				IQuantity latestEndTime = null;
				for (IItem item : itemIterable) {
					if (!(accessor.getMember(item) instanceof IQuantity)) {
						break;
					}

					Double data = ((IQuantity) accessor.getMember(item)).doubleValue();
					IQuantity endTime = ((IQuantity) endTimeAccessor.getMember(item));
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
	 */

	static class EmptyRecordingException extends Exception {
		EmptyRecordingException(String msg) {
			super(msg);
		}
	}
}
