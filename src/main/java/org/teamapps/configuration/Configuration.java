package org.teamapps.configuration;

import org.teamapps.message.protocol.message.Message;
import org.teamapps.message.protocol.model.AttributeDefinition;
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
	private File configPath = new File("./config/");

	public synchronized static Configuration getConfiguration() {
		if (instance == null) {
			instance = new Configuration(null);
		}
		return instance;
	}

	public synchronized static Configuration getConfiguration(String[] args) {
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

	private <CONFIG extends Message> CONFIG readConfig(String serviceName, PojoObjectDecoder<CONFIG> decoder) throws IOException {
		if (!configPath.exists()) {
			configPath.mkdir();
		}
		File configFile = Arrays.stream(configPath.listFiles())
				.filter(f -> f.getName().toLowerCase().equals(serviceName.toLowerCase() + ".xml"))
				.findAny()
				.orElse(null);

		CONFIG config;
		if (configFile != null) {
			String xml = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
			config = decoder.decode(xml, null);
		} else {
			config = decoder.defaultMessage();
			String xml = config.toXml();
			Files.writeString(new File(configPath, serviceName + ".xml").toPath(), xml, StandardCharsets.UTF_8);
		}
		return config;
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
					//todo find path
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
