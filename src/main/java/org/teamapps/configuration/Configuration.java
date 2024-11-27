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

import org.teamapps.message.protocol.message.AttributeType;
import org.teamapps.message.protocol.message.Message;
import org.teamapps.message.protocol.message.MessageAttribute;
import org.teamapps.message.protocol.model.AttributeDefinition;
import org.teamapps.message.protocol.model.MessageModel;
import org.teamapps.message.protocol.model.PojoObjectDecoder;
import org.teamapps.message.protocol.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class Configuration {

	private static Configuration instance;
	private final Map<String, List<ConfigKeyOverride>> overridesMap = new HashMap<>();
	private final Map<String, List<ConfigUpdateConsumer<?>>> listenerMap = new HashMap<>();
	private final Map<String, Message> configCache = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, String> localConfig = new HashMap<>();
	private File configPath = new File("./config/");

	public synchronized static Configuration getConfiguration() {
		if (instance == null) {
			instance = new Configuration(null);
		}
		return instance;
	}

	public synchronized static Configuration initializeConfiguration(String[] args) {
		if (instance == null) {
			instance = new Configuration(args);
			return instance;
		} else {
			return instance;
		}
	}
	
	private Configuration(String[] args) {
		init(args);
	}

	private void init(String[] args) {
		localConfig.putAll(System.getenv());
		System.getenv().entrySet().stream()
				.filter(entry -> ConfigKeyOverride.checkKey(entry.getKey(), true))
				.map(entry -> new ConfigKeyOverride(entry.getKey(), entry.getValue(), true))
				.forEach(configKeyOverride -> overridesMap.computeIfAbsent(configKeyOverride.getService().toLowerCase(), s -> new ArrayList<>()).add(configKeyOverride));
		if (args != null) {
			for (String arg : args) {
				int pos = arg.indexOf('=');
				if (pos > 0) {
					String key = arg.substring(0, pos).replace("-", "").trim();
					String value = arg.substring(pos + 1).trim();
					localConfig.put(key, value);
					if (ConfigKeyOverride.checkKey(key, false)) {
						ConfigKeyOverride configKeyOverride = new ConfigKeyOverride(key, value, false);
						overridesMap.computeIfAbsent(configKeyOverride.getService().toLowerCase(), s -> new ArrayList<>()).add(configKeyOverride);
					}
				}
			}
		}
		List<ConfigKeyOverride> teamappsKeys = overridesMap.get("teamapps");
		if (teamappsKeys != null) {
			String customPath = teamappsKeys.stream()
					.filter(key -> key.getKey().equals("configpath"))
					.filter(key -> !key.withPath())
					.map(ConfigKeyOverride::getValue)
					.findAny().orElse(null);
			if (customPath != null) {
				this.configPath = new File(customPath);
			}
		}
	}

	public String getLocalConfig(String key) {
		return localConfig.get(key);
	}

	private <CONFIG extends Message> CONFIG readConfig(String serviceName, PojoObjectDecoder<CONFIG> decoder) throws IOException {
		return readConfig(configPath, serviceName, decoder);
	}

	private <CONFIG extends Message> CONFIG readConfig(File path, String serviceName, PojoObjectDecoder<CONFIG> decoder) throws IOException {
		File configFile = new File(path, serviceName.toLowerCase() + ".xml");
//		File configFile = Arrays.stream(path.listFiles())
//				.filter(f -> f.getName().toLowerCase().equals(serviceName.toLowerCase() + ".xml"))
//				.findAny()
//				.orElse(new File(path, serviceName + ".xml"));
		return readConfig(configFile, decoder);
	}

	private static <CONFIG extends Message> CONFIG readConfig(File configFile, PojoObjectDecoder<CONFIG> decoder) throws IOException {
		CONFIG config;
		if (configFile.exists()) {
			String xml = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
			config = decoder.decode(xml, null);
		} else {
			config = decoder.defaultMessage();
			writeConfig(configFile, config);
		}
		return config;
	}

	private static <CONFIG extends Message> void writeConfig(File configFile, CONFIG config) throws IOException {
		if (!configFile.getParentFile().exists()) {
			configFile.getParentFile().mkdir();
		}
		String xml = config.toXml();
		Files.writeString(configFile.toPath(), xml, StandardCharsets.UTF_8);
	}

	private void handleConfigUpdate(String serviceName, Message message) {
		List<ConfigUpdateConsumer<?>> updateConsumerList = listenerMap.get(serviceName);
		if (updateConsumerList != null) {
			for (ConfigUpdateConsumer<?> configUpdateConsumer : updateConsumerList) {
				new Thread(() -> configUpdateConsumer.handleUpdate(message)).start();
			}
		}
	}


	private <CONFIG extends Message> CONFIG initServiceConfig(String serviceName, PojoObjectDecoder<CONFIG> decoder) throws IOException {
		CONFIG config = readConfig(serviceName, decoder);
		List<ConfigKeyOverride> configKeyOverrides = overridesMap.get(serviceName.toLowerCase());
		if (configKeyOverrides != null) {
			for (ConfigKeyOverride keyOverride : configKeyOverrides) {
				String key = keyOverride.getKey();
				List<String> path = keyOverride.getPath();
				if (path.isEmpty()) {
					AttributeDefinition attributeDefinition = config.getModel().getAttributeDefinitions().stream().filter(def -> def.getName().equalsIgnoreCase(key)).findAny().orElse(null);
					if (attributeDefinition != null) {
						config.setAttribute(attributeDefinition.getName(), StringUtils.readFromString(keyOverride.getValue(), attributeDefinition.getType()));
					}
				} else {
					MessageModel pathModel = config.getModel();
					Message pathMessage = config;
					for (String pathElement : path) {
						AttributeDefinition attributeDefinition = pathModel.getAttributeDefinitionByName(pathElement);
						if (attributeDefinition != null && attributeDefinition.getType() != AttributeType.OBJECT_SINGLE_REFERENCE) {
							MessageAttribute messageAttribute = pathMessage.getAttribute(attributeDefinition.getName());
							pathMessage = messageAttribute.getReferencedObject();
							pathModel = attributeDefinition.getReferencedObject();
						} else {
							pathModel = null;
							break;
						}
					}
					if (pathModel != null) {
						AttributeDefinition attributeDefinition = pathModel.getAttributeDefinitions().stream().filter(def -> def.getName().equalsIgnoreCase(key)).findAny().orElse(null);
						if (attributeDefinition != null) {
							pathMessage.setAttribute(attributeDefinition.getName(), StringUtils.readFromString(keyOverride.getValue(), attributeDefinition.getType()));
						}
					}
				}
			}
		}

		return config;
	}

	public <CONFIG extends Message> CONFIG getConfig(String serviceName, PojoObjectDecoder<CONFIG> decoder) {
		try {
			Message message = configCache.get(serviceName.toLowerCase());
			if (message == null) {
				message = initServiceConfig(serviceName, decoder);
				configCache.put(serviceName.toLowerCase(), message);
			}
			return decoder.remap(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <CONFIG extends Message> void addConfigUpdateListener(Consumer<CONFIG> updateConsumer, String serviceName, PojoObjectDecoder<CONFIG> decoder) {
		listenerMap.computeIfAbsent(serviceName.toLowerCase(), s -> new CopyOnWriteArrayList<>()).add(new ConfigUpdateConsumer<>(updateConsumer, serviceName, decoder));
	}

}
