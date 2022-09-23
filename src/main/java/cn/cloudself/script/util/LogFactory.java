package cn.cloudself.script.util;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public abstract class LogFactory {
    public LogFactory() {
    }

    public static Log getLog(Class<?> clazz) {
        return getLog(clazz.getName());
    }

    public static Log getLog(String name) {
        return LogAdapter.createLog(name);
    }

    public static class LogAdapter {
        private static final String LOG4J_SPI = "org.apache.logging.log4j.spi.ExtendedLogger";
        private static final String LOG4J_SLF4J_PROVIDER = "org.apache.logging.slf4j.SLF4JProvider";
        private static final String SLF4J_SPI = "org.slf4j.spi.LocationAwareLogger";
        private static final String SLF4J_API = "org.slf4j.Logger";
        private static final LogAdapter.LogApi logApi;

        private LogAdapter() {
        }

        public static Log createLog(String name) {
            switch (logApi) {
                case LOG4J:
                    return LogAdapter.Log4jAdapter.createLog(name);
                case SLF4J_LAL:
                    return LogAdapter.Slf4jAdapter.createLocationAwareLog(name);
                case SLF4J:
                    return LogAdapter.Slf4jAdapter.createLog(name);
                default:
                    return LogAdapter.JavaUtilAdapter.createLog(name);
            }
        }

        private static boolean isPresent(String className) {
            try {
                Class.forName(className, false, LogAdapter.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException var2) {
                return false;
            }
        }

        static {
            if (isPresent("org.apache.logging.log4j.spi.ExtendedLogger")) {
                if (isPresent("org.apache.logging.slf4j.SLF4JProvider") && isPresent("org.slf4j.spi.LocationAwareLogger")) {
                    logApi = LogAdapter.LogApi.SLF4J_LAL;
                } else {
                    logApi = LogAdapter.LogApi.LOG4J;
                }
            } else if (isPresent("org.slf4j.spi.LocationAwareLogger")) {
                logApi = LogAdapter.LogApi.SLF4J_LAL;
            } else if (isPresent("org.slf4j.Logger")) {
                logApi = LogAdapter.LogApi.SLF4J;
            } else {
                logApi = LogAdapter.LogApi.JUL;
            }

        }

        private static class LocationResolvingLogRecord extends LogRecord {
            private static final String FQCN = LogAdapter.JavaUtilLog.class.getName();
            private volatile boolean resolved;

            public LocationResolvingLogRecord(Level level, String msg) {
                super(level, msg);
            }

            public String getSourceClassName() {
                if (!this.resolved) {
                    this.resolve();
                }

                return super.getSourceClassName();
            }

            public void setSourceClassName(String sourceClassName) {
                super.setSourceClassName(sourceClassName);
                this.resolved = true;
            }

            public String getSourceMethodName() {
                if (!this.resolved) {
                    this.resolve();
                }

                return super.getSourceMethodName();
            }

            public void setSourceMethodName(String sourceMethodName) {
                super.setSourceMethodName(sourceMethodName);
                this.resolved = true;
            }

            private void resolve() {
                StackTraceElement[] stack = (new Throwable()).getStackTrace();
                String sourceClassName = null;
                String sourceMethodName = null;
                boolean found = false;
                StackTraceElement[] var5 = stack;
                int var6 = stack.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    StackTraceElement element = var5[var7];
                    String className = element.getClassName();
                    if (FQCN.equals(className)) {
                        found = true;
                    } else if (found) {
                        sourceClassName = className;
                        sourceMethodName = element.getMethodName();
                        break;
                    }
                }

                this.setSourceClassName(sourceClassName);
                this.setSourceMethodName(sourceMethodName);
            }

            protected Object writeReplace() {
                LogRecord serialized = new LogRecord(this.getLevel(), this.getMessage());
                serialized.setLoggerName(this.getLoggerName());
                serialized.setResourceBundle(this.getResourceBundle());
                serialized.setResourceBundleName(this.getResourceBundleName());
                serialized.setSourceClassName(this.getSourceClassName());
                serialized.setSourceMethodName(this.getSourceMethodName());
                serialized.setSequenceNumber(this.getSequenceNumber());
                serialized.setParameters(this.getParameters());
                serialized.setThreadID(this.getThreadID());
                serialized.setMillis(this.getMillis());
                serialized.setThrown(this.getThrown());
                return serialized;
            }
        }

        private static class JavaUtilLog implements Log, Serializable {
            private String name;
            private transient Logger logger;

            public JavaUtilLog(String name) {
                this.name = name;
                this.logger = Logger.getLogger(name);
            }

            public boolean isFatalEnabled() {
                return this.isErrorEnabled();
            }

            public boolean isErrorEnabled() {
                return this.logger.isLoggable(Level.SEVERE);
            }

            public boolean isWarnEnabled() {
                return this.logger.isLoggable(Level.WARNING);
            }

            public boolean isInfoEnabled() {
                return this.logger.isLoggable(Level.INFO);
            }

            public boolean isDebugEnabled() {
                return this.logger.isLoggable(Level.FINE);
            }

            public boolean isTraceEnabled() {
                return this.logger.isLoggable(Level.FINEST);
            }

            public void fatal(Object message) {
                this.error(message);
            }

            public void fatal(Object message, Throwable exception) {
                this.error(message, exception);
            }

            public void error(Object message) {
                this.log(Level.SEVERE, message, (Throwable)null);
            }

            public void error(Object message, Throwable exception) {
                this.log(Level.SEVERE, message, exception);
            }

            public void warn(Object message) {
                this.log(Level.WARNING, message, (Throwable)null);
            }

            public void warn(Object message, Throwable exception) {
                this.log(Level.WARNING, message, exception);
            }

            public void info(Object message) {
                this.log(Level.INFO, message, (Throwable)null);
            }

            public void info(Object message, Throwable exception) {
                this.log(Level.INFO, message, exception);
            }

            public void debug(Object message) {
                this.log(Level.FINE, message, (Throwable)null);
            }

            public void debug(Object message, Throwable exception) {
                this.log(Level.FINE, message, exception);
            }

            public void trace(Object message) {
                this.log(Level.FINEST, message, (Throwable)null);
            }

            public void trace(Object message, Throwable exception) {
                this.log(Level.FINEST, message, exception);
            }

            private void log(Level level, Object message, Throwable exception) {
                if (this.logger.isLoggable(level)) {
                    Object rec;
                    if (message instanceof LogRecord) {
                        rec = (LogRecord)message;
                    } else {
                        rec = new LogAdapter.LocationResolvingLogRecord(level, String.valueOf(message));
                        ((LogRecord)rec).setLoggerName(this.name);
                        ((LogRecord)rec).setResourceBundleName(this.logger.getResourceBundleName());
                        ((LogRecord)rec).setResourceBundle(this.logger.getResourceBundle());
                        ((LogRecord)rec).setThrown(exception);
                    }

                    this.logger.log((LogRecord)rec);
                }

            }

            protected Object readResolve() {
                return new LogAdapter.JavaUtilLog(this.name);
            }
        }

        private static class Slf4jLocationAwareLog extends LogAdapter.Slf4jLog<LocationAwareLogger> implements Serializable {
            private static final String FQCN = LogAdapter.Slf4jLocationAwareLog.class.getName();

            public Slf4jLocationAwareLog(LocationAwareLogger logger) {
                super(logger);
            }

            public void fatal(Object message) {
                this.error(message);
            }

            public void fatal(Object message, Throwable exception) {
                this.error(message, exception);
            }

            public void error(Object message) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isErrorEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 40, String.valueOf(message), (Object[])null, (Throwable)null);
                }

            }

            public void error(Object message, Throwable exception) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isErrorEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 40, String.valueOf(message), (Object[])null, exception);
                }

            }

            public void warn(Object message) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isWarnEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 30, String.valueOf(message), (Object[])null, (Throwable)null);
                }

            }

            public void warn(Object message, Throwable exception) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isWarnEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 30, String.valueOf(message), (Object[])null, exception);
                }

            }

            public void info(Object message) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isInfoEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 20, String.valueOf(message), (Object[])null, (Throwable)null);
                }

            }

            public void info(Object message, Throwable exception) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isInfoEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 20, String.valueOf(message), (Object[])null, exception);
                }

            }

            public void debug(Object message) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isDebugEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 10, String.valueOf(message), (Object[])null, (Throwable)null);
                }

            }

            public void debug(Object message, Throwable exception) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isDebugEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 10, String.valueOf(message), (Object[])null, exception);
                }

            }

            public void trace(Object message) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isTraceEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 0, String.valueOf(message), (Object[])null, (Throwable)null);
                }

            }

            public void trace(Object message, Throwable exception) {
                if (message instanceof String || ((LocationAwareLogger)this.logger).isTraceEnabled()) {
                    ((LocationAwareLogger)this.logger).log((Marker)null, FQCN, 0, String.valueOf(message), (Object[])null, exception);
                }

            }

            protected Object readResolve() {
                return LogAdapter.Slf4jAdapter.createLocationAwareLog(this.name);
            }
        }

        private static class Slf4jLog<T extends org.slf4j.Logger> implements Log, Serializable {
            protected final String name;
            protected transient T logger;

            public Slf4jLog(T logger) {
                this.name = logger.getName();
                this.logger = logger;
            }

            public boolean isFatalEnabled() {
                return this.isErrorEnabled();
            }

            public boolean isErrorEnabled() {
                return this.logger.isErrorEnabled();
            }

            public boolean isWarnEnabled() {
                return this.logger.isWarnEnabled();
            }

            public boolean isInfoEnabled() {
                return this.logger.isInfoEnabled();
            }

            public boolean isDebugEnabled() {
                return this.logger.isDebugEnabled();
            }

            public boolean isTraceEnabled() {
                return this.logger.isTraceEnabled();
            }

            public void fatal(Object message) {
                this.error(message);
            }

            public void fatal(Object message, Throwable exception) {
                this.error(message, exception);
            }

            public void error(Object message) {
                if (message instanceof String || this.logger.isErrorEnabled()) {
                    this.logger.error(String.valueOf(message));
                }

            }

            public void error(Object message, Throwable exception) {
                if (message instanceof String || this.logger.isErrorEnabled()) {
                    this.logger.error(String.valueOf(message), exception);
                }

            }

            public void warn(Object message) {
                if (message instanceof String || this.logger.isWarnEnabled()) {
                    this.logger.warn(String.valueOf(message));
                }

            }

            public void warn(Object message, Throwable exception) {
                if (message instanceof String || this.logger.isWarnEnabled()) {
                    this.logger.warn(String.valueOf(message), exception);
                }

            }

            public void info(Object message) {
                if (message instanceof String || this.logger.isInfoEnabled()) {
                    this.logger.info(String.valueOf(message));
                }

            }

            public void info(Object message, Throwable exception) {
                if (message instanceof String || this.logger.isInfoEnabled()) {
                    this.logger.info(String.valueOf(message), exception);
                }

            }

            public void debug(Object message) {
                if (message instanceof String || this.logger.isDebugEnabled()) {
                    this.logger.debug(String.valueOf(message));
                }

            }

            public void debug(Object message, Throwable exception) {
                if (message instanceof String || this.logger.isDebugEnabled()) {
                    this.logger.debug(String.valueOf(message), exception);
                }

            }

            public void trace(Object message) {
                if (message instanceof String || this.logger.isTraceEnabled()) {
                    this.logger.trace(String.valueOf(message));
                }

            }

            public void trace(Object message, Throwable exception) {
                if (message instanceof String || this.logger.isTraceEnabled()) {
                    this.logger.trace(String.valueOf(message), exception);
                }

            }

            protected Object readResolve() {
                return LogAdapter.Slf4jAdapter.createLog(this.name);
            }
        }

        private static class Log4jLog implements Log, Serializable {
            private static final String FQCN = LogAdapter.Log4jLog.class.getName();
            private static final LoggerContext loggerContext = LogManager.getContext(LogAdapter.Log4jLog.class.getClassLoader(), false);
            private final ExtendedLogger logger;

            public Log4jLog(String name) {
                LoggerContext context = loggerContext;
                if (context == null) {
                    context = LogManager.getContext(LogAdapter.Log4jLog.class.getClassLoader(), false);
                }

                this.logger = context.getLogger(name);
            }

            public boolean isFatalEnabled() {
                return this.logger.isEnabled(org.apache.logging.log4j.Level.FATAL);
            }

            public boolean isErrorEnabled() {
                return this.logger.isEnabled(org.apache.logging.log4j.Level.ERROR);
            }

            public boolean isWarnEnabled() {
                return this.logger.isEnabled(org.apache.logging.log4j.Level.WARN);
            }

            public boolean isInfoEnabled() {
                return this.logger.isEnabled(org.apache.logging.log4j.Level.INFO);
            }

            public boolean isDebugEnabled() {
                return this.logger.isEnabled(org.apache.logging.log4j.Level.DEBUG);
            }

            public boolean isTraceEnabled() {
                return this.logger.isEnabled(org.apache.logging.log4j.Level.TRACE);
            }

            public void fatal(Object message) {
                this.log(org.apache.logging.log4j.Level.FATAL, message, (Throwable)null);
            }

            public void fatal(Object message, Throwable exception) {
                this.log(org.apache.logging.log4j.Level.FATAL, message, exception);
            }

            public void error(Object message) {
                this.log(org.apache.logging.log4j.Level.ERROR, message, (Throwable)null);
            }

            public void error(Object message, Throwable exception) {
                this.log(org.apache.logging.log4j.Level.ERROR, message, exception);
            }

            public void warn(Object message) {
                this.log(org.apache.logging.log4j.Level.WARN, message, (Throwable)null);
            }

            public void warn(Object message, Throwable exception) {
                this.log(org.apache.logging.log4j.Level.WARN, message, exception);
            }

            public void info(Object message) {
                this.log(org.apache.logging.log4j.Level.INFO, message, (Throwable)null);
            }

            public void info(Object message, Throwable exception) {
                this.log(org.apache.logging.log4j.Level.INFO, message, exception);
            }

            public void debug(Object message) {
                this.log(org.apache.logging.log4j.Level.DEBUG, message, (Throwable)null);
            }

            public void debug(Object message, Throwable exception) {
                this.log(org.apache.logging.log4j.Level.DEBUG, message, exception);
            }

            public void trace(Object message) {
                this.log(org.apache.logging.log4j.Level.TRACE, message, (Throwable)null);
            }

            public void trace(Object message, Throwable exception) {
                this.log(org.apache.logging.log4j.Level.TRACE, message, exception);
            }

            private void log(org.apache.logging.log4j.Level level, Object message, Throwable exception) {
                if (message instanceof String) {
                    if (exception != null) {
                        this.logger.logIfEnabled(FQCN, level, (org.apache.logging.log4j.Marker)null, (String)message, exception);
                    } else {
                        this.logger.logIfEnabled(FQCN, level, (org.apache.logging.log4j.Marker)null, (String)message);
                    }
                } else {
                    this.logger.logIfEnabled(FQCN, level, (org.apache.logging.log4j.Marker)null, message, exception);
                }

            }
        }

        private static class JavaUtilAdapter {
            private JavaUtilAdapter() {
            }

            public static Log createLog(String name) {
                return new LogAdapter.JavaUtilLog(name);
            }
        }

        private static class Slf4jAdapter {
            private Slf4jAdapter() {
            }

            public static Log createLocationAwareLog(String name) {
                org.slf4j.Logger logger = LoggerFactory.getLogger(name);
                return (Log)(logger instanceof LocationAwareLogger ? new LogAdapter.Slf4jLocationAwareLog((LocationAwareLogger)logger) : new LogAdapter.Slf4jLog(logger));
            }

            public static Log createLog(String name) {
                return new LogAdapter.Slf4jLog(LoggerFactory.getLogger(name));
            }
        }

        private static class Log4jAdapter {
            private Log4jAdapter() {
            }

            public static Log createLog(String name) {
                return new LogAdapter.Log4jLog(name);
            }
        }

        private static enum LogApi {
            LOG4J,
            SLF4J_LAL,
            SLF4J,
            JUL;

            private LogApi() {
            }
        }
    }

    public interface Log {
        boolean isFatalEnabled();

        boolean isErrorEnabled();

        boolean isWarnEnabled();

        boolean isInfoEnabled();

        boolean isDebugEnabled();

        boolean isTraceEnabled();

        void fatal(Object var1);

        void fatal(Object var1, Throwable var2);

        void error(Object var1);

        void error(Object var1, Throwable var2);

        void warn(Object var1);

        void warn(Object var1, Throwable var2);

        void info(Object var1);

        void info(Object var1, Throwable var2);

        void debug(Object var1);

        void debug(Object var1, Throwable var2);

        void trace(Object var1);

        void trace(Object var1, Throwable var2);
    }
}
