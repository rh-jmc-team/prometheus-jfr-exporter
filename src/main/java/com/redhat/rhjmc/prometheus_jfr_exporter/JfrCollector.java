package com.redhat.rhjmc.prometheus_jfr_exporter;

import io.prometheus.client.Collector;

import java.util.List;

public class JfrCollector extends Collector { // TODO: implement Collector.Describable?
	@Override
	public List<MetricFamilySamples> collect() {
		return null;
	}
}
