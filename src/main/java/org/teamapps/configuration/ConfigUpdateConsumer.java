/*-
 * ========================LICENSE_START=================================
 * TeamApps Configuration Management
 * ---
 * Copyright (C) 2022 - 2023 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
