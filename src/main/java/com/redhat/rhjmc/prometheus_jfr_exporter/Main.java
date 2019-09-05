package com.redhat.rhjmc.prometheus_jfr_exporter;

import org.eclipse.core.runtime.RegistryFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {
	public static void main(String[] args) throws Exception {
		System.setProperty("org.openjdk.jmc.common.security.manager", SecurityManager.class.getName());

		RegistryFactory.setDefaultRegistryProvider(new RegistryProvider());

		RecordingService rs = new RecordingService("localhost", 0, "/tmp/default.jfc");
		rs.start();

		for (int i : new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}) {
			InputStream is = rs.openRecording();
			Files.copy(is, (new File("/tmp/dump" + i + ".jfr")).toPath(), StandardCopyOption.REPLACE_EXISTING);
			is.close();
			Thread.sleep(1000);
		}

		rs.stop();
	}
}
