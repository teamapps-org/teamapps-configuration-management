package org.teamapps.configuration;

import org.teamapps.config.test.TestConfig;

import java.io.IOException;

import static org.junit.Assert.*;

public class ConfigurationTest {

	@org.junit.Test
	public void getConfiguration() throws IOException {
		TestConfig config = Configuration.getConfiguration().getConfig("testservice", TestConfig.getMessageDecoder());
		System.out.println(config.toXml(true, null));
	}

	@org.junit.Test
	public void addConfigUpdateListener() {
	}
}