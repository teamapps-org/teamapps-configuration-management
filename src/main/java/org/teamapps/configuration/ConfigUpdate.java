package org.teamapps.configuration;

import org.teamapps.message.protocol.message.Message;

public class ConfigUpdate {

	private final String service;
	private final Message config;

	public ConfigUpdate(String service, Message config) {
		this.service = service;
		this.config = config;
	}

	public String getService() {
		return service;
	}

	public Message getConfig() {
		return config;
	}
}
