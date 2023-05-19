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

package zk.rgw.common.log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

public final class Log4j2Configurator {

    public static final String LAYOUT_PATTERN = "%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%-6.-6pid] %-20.-20logger{1.}[line:%-4line] [%thread] - %msg%n";

    public static final String APPENDER_NAME_CONSOLE = "console";

    public static final String APPENDER_NAME_FILE = "file";

    private Log4j2Configurator() {
    }

    public static String highlightPattern(String pattern) {
        return "%highlight{" + pattern + "}";
    }

    public static void configure(Level level, String appName, String logPath) {
        configure(level, LAYOUT_PATTERN, logPath, appName, 4, 100, 100);
    }

    @SuppressWarnings("PMD.CloseResource")
    public static void configure(
            Level level,
            String layoutPattern,
            String logFileDir,
            String logFileName,
            int timeTriggerInterval,
            int fileMaxSize,
            int fileMaxKeepCount
    ) {
        configureCurrent(level, layoutPattern);
        LoggerContext loggerContext = LoggerContext.getContext(false);
        Configuration configuration = loggerContext.getConfiguration();
        removeOriginAppenders(configuration);
        configuration.getRootLogger().setLevel(level);
        setConsoleAppender(configuration, layoutPattern);
        // 初始化Console成功后,先updateLoggers一下,以便在初始化fileAppender时发生文件权限问题时能记录日至到Console
        loggerContext.updateLoggers();
        setFileAppender(
                configuration, layoutPattern, logFileDir, logFileName,
                timeTriggerInterval, fileMaxSize, fileMaxKeepCount
        );
        loggerContext.updateLoggers();

        disableShutdownHook();
    }

    private static void disableShutdownHook() {
        LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory log4jContextFactory) {
            ShutdownCallbackRegistry shutdownCallbackRegistry = log4jContextFactory.getShutdownCallbackRegistry();
            if (shutdownCallbackRegistry instanceof DefaultShutdownCallbackRegistry defaultShutdownCallbackRegistry) {
                defaultShutdownCallbackRegistry.stop();
            }
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    public static void configureCurrent(Level level, String layoutPattern) {
        LoggerContext loggerContext = LoggerContext.getContext(true);
        Configuration configuration = loggerContext.getConfiguration();
        removeOriginAppenders(configuration);
        configuration.getRootLogger().setLevel(level);
        setConsoleAppender(configuration, layoutPattern);
        loggerContext.updateLoggers();
    }

    private static void removeOriginAppenders(Configuration configuration) {
        Set<String> originAppenderNames = configuration.getRootLogger().getAppenders().keySet();
        for (String appendName : originAppenderNames) {
            configuration.getRootLogger().removeAppender(appendName);
        }
    }

    private static void setConsoleAppender(final Configuration configuration, String layoutPattern) {
        if (!configuration.getRootLogger().getAppenders().containsKey(APPENDER_NAME_CONSOLE)) {
            ConsoleAppender consoleAppender = consoleAppender(highlightPattern(layoutPattern));
            configuration.addAppender(consoleAppender);
            consoleAppender.start();
            configuration.getRootLogger().addAppender(consoleAppender, null, null);
        }
    }

    private static void setFileAppender(
            final Configuration configuration,
            String layoutPattern,
            String logFileDir,
            String logFileName,
            int timeTriggerInterval,
            int fileMaxSize,
            int fileMaxKeepCount
    ) {
        if (!configuration.getRootLogger().getAppenders().containsKey(APPENDER_NAME_FILE)) {
            Path path = Paths.get(logFileDir).toAbsolutePath();
            if (Files.notExists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException ioException) {
                    LoggerContext.getContext().getRootLogger().error(
                            "Directory {} not exist and failed to create it, so no logs will be write to there.", logFileDir
                    );
                    return;
                }
            } else if (!Files.isWritable(path)) {
                LoggerContext.getContext().getRootLogger().error(
                        "Directory {} exists but current process has no permission to write to it, so no logs will be write to there.", logFileDir
                );
                return;
            }
            String finalLogFileName = logFileName + ".log";
            String historyFileNamePattern = logFileName + "_%d{yyyy-MM-dd}_%i.log.gz";
            RollingRandomAccessFileAppender fileAppender = fileAppender(
                    path.resolve(finalLogFileName).toString(), path.resolve(historyFileNamePattern).toString(),
                    layoutPattern, timeTriggerInterval, fileMaxSize, fileMaxKeepCount
            );
            configuration.addAppender(fileAppender);
            fileAppender.start();
            configuration.getRootLogger().addAppender(fileAppender, null, null);
        }
    }

    private static ConsoleAppender consoleAppender(String pattern) {
        return ConsoleAppender.newBuilder()
                .setName(APPENDER_NAME_CONSOLE)
                .setLayout(PatternLayout.newBuilder().withPattern(pattern).build())
                .build();
    }

    private static RollingRandomAccessFileAppender fileAppender(
            String filePath, String fileNamePattern, String layoutPattern, int timeTriggerInterval, int fileMaxSize, int fileMaxKeepCount
    ) {
        return RollingRandomAccessFileAppender.newBuilder()
                .setName(APPENDER_NAME_FILE)
                .withFileName(filePath)
                .withFilePattern(fileNamePattern)
                .setLayout(PatternLayout.newBuilder().withPattern(layoutPattern).build())
                .withPolicy(TimeBasedTriggeringPolicy.newBuilder().withInterval(timeTriggerInterval).build())
                .withPolicy(SizeBasedTriggeringPolicy.createPolicy(fileMaxSize + "MB"))
                .withStrategy(DefaultRolloverStrategy.newBuilder().withMax(String.valueOf(fileMaxKeepCount)).build())
                .build();
    }

}
