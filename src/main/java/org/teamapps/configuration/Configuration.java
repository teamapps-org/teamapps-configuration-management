package org.teamapps.configuration;

import org.teamapps.message.protocol.message.Message;
import org.teamapps.message.protocol.model.PojoObjectDecoder;

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

	public synchronized static void initializeProgramArguments(String[] args) {
		if (instance == null) {
			instance = new Configuration(args);
		}
	}

	private Configuration(String[] args) {
		init(args);
	}

	private void init(String[] args) {
		System.getenv().entrySet().stream()
				.filter(entry -> ConfigKeyOverride.checkKey(entry.getKey(), true))
				.map(entry -> new ConfigKeyOverride(entry.getKey(), entry.getValue(), true))
				.forEach(configKeyOverride -> overridesMap.computeIfAbsent(configKeyOverride.getService(), s -> new ArrayList<>()).add(configKeyOverride));
		if (args != null) {
			for (String arg : args) {
				int pos = arg.indexOf('=');
				if (pos > 0) {
					String key = arg.substring(0, pos).replace("-", "").trim();
					String value = arg.substring(pos + 1).trim();
					if (ConfigKeyOverride.checkKey(key, false)) {
						ConfigKeyOverride configKeyOverride = new ConfigKeyOverride(key, value, false);
						overridesMap.computeIfAbsent(configKeyOverride.getService(), s -> new ArrayList<>()).add(configKeyOverride);
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

	private void readConfig() throws IOException {
		if (configPath.exists() && configPath.isDirectory()) {
			List<File> configFiles = Arrays.stream(configPath.listFiles())
					.filter(f -> f.getName().endsWith(".xml"))
					.filter(f -> f.length() > 0)
					.toList();

			for (File configFile : configFiles) {
				String serviceName = configFile.getName().substring(0, configFile.getName().length() - 4);
				String xml = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
				//todo add to map
			}
		}
	}

	private void handleConfigUpdate(String serviceName, Message message) {
		List<ConfigUpdateConsumer<?>> updateConsumerList = listenerMap.get(serviceName);
		if (updateConsumerList != null) {
			for (ConfigUpdateConsumer<?> configUpdateConsumer : updateConsumerList) {
				new Thread(() -> configUpdateConsumer.handleUpdate(message)).start();
			}
		}
	}


	private <CONFIG extends Message> CONFIG initServiceConfig(String serviceName, PojoObjectDecoder<CONFIG> decoder) {
		CONFIG config = decoder.defaultMessage();
		/*
			todo:
			read config
			apply arguments and environment variables
		 */
		return config;
	}

	public <CONFIG extends Message> CONFIG getConfig(String serviceName, PojoObjectDecoder<CONFIG> decoder) {
		Message message = configCache.get(serviceName.toLowerCase());
		if (message == null) {
			message = initServiceConfig(serviceName, decoder);
			configCache.put(serviceName.toLowerCase(), message);
		}
		return decoder.remap(message);
	}

	public <CONFIG extends Message> void addConfigUpdateListener(Consumer<CONFIG> updateConsumer, String serviceName, PojoObjectDecoder<CONFIG> decoder) {
		listenerMap.computeIfAbsent(serviceName.toLowerCase(), s -> new CopyOnWriteArrayList<>()).add(new ConfigUpdateConsumer<>(updateConsumer, serviceName, decoder));
	}

}
