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

package zk.rgw.http.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import zk.rgw.common.util.ZipUtil;

@Slf4j
public abstract class PluginClassLoader extends URLClassLoader {

    protected final Path pluginHomeDir;

    protected final String pluginName;

    protected final String pluginVersion;

    private final Path pluginDir;

    protected PluginClassLoader(Path pluginHomeDir, String pluginName, String pluginVersion) throws PluginLoadException {
        super(pluginName + "-" + pluginVersion, new URL[0], PluginClassLoader.class.getClassLoader());
        this.pluginHomeDir = pluginHomeDir;
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
        this.pluginDir = this.pluginHomeDir.resolve(pluginName).resolve(pluginVersion);
        this.init();
    }

    private void init() throws PluginLoadException {
        List<URI> uriList = new ArrayList<>();

        if (Files.notExists(pluginDir)) {
            log.info("Plugin {}:{} not installed yet, going to install it.", pluginHomeDir, getName());
            install();
        }

        try (Stream<Path> stream = Files.list(pluginDir);) {
            stream.forEach(path -> {
                final URI uri = path.toUri();
                if (!uri.toString().endsWith(".jar")) {
                    return;
                }
                uriList.add(uri);
            });
        } catch (IOException e) {
            throw new PluginLoadException("Failed to list directory " + pluginDir, e);
        }

        for (URI uri : uriList) {
            try {
                addURL(uri.toURL());
            } catch (MalformedURLException e) {
                String message = String.format("Failed to find all jar files for plugin %s:%s", pluginHomeDir.toString(), getName());
                throw new PluginLoadException(message, e);
            }
        }
    }

    private void install() {
        Path zipFile = pluginHomeDir.resolve(getName() + ".zip");
        try (InputStream ins = pluginZipDownloadStream()) {
            Files.deleteIfExists(zipFile);
            Files.copy(ins, zipFile);
            ZipUtil.unzip(zipFile.toString(), pluginDir.toString());
            Files.delete(zipFile);
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    protected abstract InputStream pluginZipDownloadStream();

}
