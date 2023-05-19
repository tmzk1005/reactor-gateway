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

package zk.rgw.common.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;

import zk.rgw.common.conf.CommonConfiguration;
import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.log.Log4j2Configurator;

@Log4j2
public abstract class ServerBootstrap<C extends CommonConfiguration> {

    public static final String RGW_HOME = "RGW_HOME";

    protected final String[] bootstrapArgs;

    protected String rgwHome;

    protected Path rgwHomePath;

    protected final Properties confProperties = new Properties();

    protected final C configuration;

    protected final String appName;

    protected ServerBootstrap(String[] bootstrapArgs, C configuration, String appName) {
        this.bootstrapArgs = bootstrapArgs;
        this.configuration = configuration;
        this.appName = appName;
    }

    protected void bootstrap() {
        checkRgwHome();
        preParseArgs();
        parseConfFile();
        parseBootstrapArgs();
        setupLogger();
        start();
    }

    private void checkRgwHome() {
        rgwHome = System.getenv(RGW_HOME);
        if (Objects.isNull(rgwHome)) {
            throw noStackTrace(new RgwRuntimeException("System environment variable RGW_HOME required!"));
        }
        rgwHomePath = Paths.get(rgwHome);
        if (Files.notExists(rgwHomePath) || !Files.isDirectory(rgwHomePath)) {
            throw noStackTrace(new RgwRuntimeException("System environment variable RGW_HOME " + rgwHome + " is not an exist directory."));
        }
        configuration.setRgwHome(rgwHomePath);
    }

    private void preParseArgs() {
        for (String arg : bootstrapArgs) {
            if (arg.startsWith("--log.path=")) {
                configuration.setLogPath(arg.substring(11));
            } else if (arg.startsWith("--log.level=")) {
                String logLevelStr = arg.substring(12);
                configuration.setLogLevel(parseLogLevelStr(logLevelStr));
            } else if (arg.startsWith("--conf=")) {
                configuration.setConfFile(arg.substring(7));
            }
        }
    }

    private CommonConfiguration.LogLevel parseLogLevelStr(String logLevelStr) {
        CommonConfiguration.LogLevel logLevel;
        try {
            logLevel = CommonConfiguration.LogLevel.valueOf(logLevelStr);
        } catch (IllegalArgumentException ile) {
            logLevel = configuration.getLogLevel();
        }
        return logLevel;
    }

    private void parseConfFile() {
        String confFile = configuration.getConfFile();
        Path confFilePath = Paths.get(confFile);
        if (!confFilePath.isAbsolute()) {
            confFilePath = rgwHomePath.resolve(confFilePath);
        }
        try (InputStream inputStream = Files.newInputStream(confFilePath)) {
            confProperties.load(inputStream);
        } catch (IOException ioException) {
            throw new RgwRuntimeException("Failed to load configurations from properties file " + confFilePath, ioException);
        }
    }

    private void parseBootstrapArgs() {
        CommandLine commandLine = new CommandLine(configuration);
        commandLine.setUsageHelpLongOptionsMaxWidth(40);
        commandLine.setDefaultValueProvider(new CommandLine.PropertiesDefaultProvider(confProperties));
        int exitCode = commandLine.execute(bootstrapArgs);
        if (exitCode != 0 || !configuration.isInited()) {
            System.exit(exitCode);
        }
    }

    private void setupLogger() {
        Log4j2Configurator.configure(Level.getLevel(configuration.getLogLevel().name()), appName, configuration.getLogPath());
    }

    protected void start() {
        Server server = newServerInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Received TERMINATE signal, process is going to stop after do some clean job.");
            server.stop();
            LogManager.shutdown();
        }));
        server.start();
    }

    protected abstract Server newServerInstance();

    private static RuntimeException noStackTrace(RuntimeException exception) {
        exception.setStackTrace(new StackTraceElement[0]);
        return exception;
    }

}
