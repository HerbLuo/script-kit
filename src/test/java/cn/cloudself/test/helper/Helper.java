package cn.cloudself.test.helper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class Helper {
    public static void initLogger() {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ALL).add(builder.newAppenderRef("stdout"));
        builder.add(rootLogger);

        final LayoutComponentBuilder layout = builder.newLayout("PatternLayout").addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %highlight{%-5level}{FATAL=red blink, ERROR=red, WARN=yellow bold, INFO=green, DEBUG=gray, TRACE=blue} %style{%40.40C{1.}-%-4L}{cyan}: %msg%n%ex");
        final AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
        console.add(layout);
        builder.add(console);

        Configurator.initialize(builder.build());
    }
}
