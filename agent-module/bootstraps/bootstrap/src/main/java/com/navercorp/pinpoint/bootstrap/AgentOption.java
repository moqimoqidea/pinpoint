/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.bootstrap;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author emeroad
 */
public interface AgentOption {

    Instrumentation getInstrumentation();

    Properties getProperties();

    Map<String, String> getAgentArgs();

    Path getAgentPath();

    List<Path> getPluginJars();

    List<Path> getBootstrapJarPaths();

    Map<String, Object> toMap();
}
