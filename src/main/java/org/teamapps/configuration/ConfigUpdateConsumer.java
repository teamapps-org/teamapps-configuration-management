package org.teamapps.configuration;

import org.teamapps.message.protocol.message.Message;
import org.teamapps.message.protocol.model.PojoObjectDecoder;

import java.util.function.Consumer;

public class ConfigUpdateConsumer<CONFIG extends Message> {

	private final Consumer<CONFIG> consumer;
	private final String service;
	private final PojoObjectDecoder<CONFIG> decoder;

	public ConfigUpdateConsumer(Consumer<CONFIG> consumer, String service, PojoObjectDecoder<CONFIG> decoder) {
		this.consumer = consumer;
		this.service = service;
		this.decoder = decoder;
	}

	public void handleUpdate(Message update) {
		CONFIG config = decoder.remap(update);
		consumer.accept(config);
	}

	public Consumer<CONFIG> getConsumer() {
		return consumer;
	}

	public String getService() {
		return service;
	}

	public PojoObjectDecoder<CONFIG> getDecoder() {
		return decoder;
	}
}
