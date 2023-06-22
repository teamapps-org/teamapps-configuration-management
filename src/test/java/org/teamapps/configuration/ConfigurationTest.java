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