/*
 * Copyright 2023 zoukang, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zk.rgw.gateway.plugin;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import zk.rgw.http.plugin.PluginClassLoader;
import zk.rgw.http.plugin.PluginLoadException;

public class CustomPluginClassLoader extends PluginClassLoader {

    private static final Path PLUGIN_HOME = Paths.get("plugins");

    private static final Map<String, PluginClassLoader> INSTANCES = new HashMap<>(2);

    private CustomPluginClassLoader(Path pluginHomeDir, String pluginName, String pluginVersion) throws PluginLoadException {
        super(pluginHomeDir, pluginName, pluginVersion);
    }

    @Override
    protected InputStream pluginZipDownloadStream() {
        // TODO
        return null;
    }

    @SuppressWarnings("java:S3824")
    public static synchronized PluginClassLoader getInstance(String name, String version) throws PluginLoadException {
        String key = name + "###" + version;
        if (!INSTANCES.containsKey(key)) {
            INSTANCES.put(key, new CustomPluginClassLoader(PLUGIN_HOME, name, version));
        }
        return INSTANCES.get(key);
    }

}
