package org.teamapps.configuration;

import java.util.ArrayList;
import java.util.List;

public class ConfigKeyOverride {
	private final String service;
	private final List<String> path = new ArrayList<>();
	private final String key;
	private final String value;

	public static boolean checkKey(String rawKey, boolean environmentVariable) {
		return rawKey != null && !rawKey.isBlank() && split(rawKey, environmentVariable).length > 1;
	}

	public ConfigKeyOverride(String rawKey, String value, boolean environmentVariable) {
		String[] parts = split(rawKey, environmentVariable);
		this.service = parts[0];
		this.key = parts[parts.length - 1];
		this.value = value;
		if (parts.length > 2) {
			for (int i = 1; i < parts.length - 2; i++) {
				path.add(parts[i]);
			}
		}
	}

	private static String[] split(String rawKey, boolean environmentVariable) {
		if (environmentVariable) {
			return rawKey.toLowerCase().split("__");
		} else {
			return rawKey.toLowerCase().split("\\.");
		}
	}

	public boolean withPath() {
		return !path.isEmpty();
	}

	public String getService() {
		return service;
	}

	public List<String> getPath() {
		return path;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
}
