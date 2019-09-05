package org.openjdk.jmc.ui;

import java.util.logging.Logger;

// Polyfill so we don't have to include org.openjdk.jmc:org.openjdk.jmc.ui as a dependency
public class UIPlugin {
	public static UIPlugin getDefault() {
		return new UIPlugin();
	}

	public Logger getLogger() {
		return Logger.getLogger(UIPlugin.class.getSimpleName());
	}
}
