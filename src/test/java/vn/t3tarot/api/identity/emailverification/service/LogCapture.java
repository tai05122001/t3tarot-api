package vn.t3tarot.api.identity.emailverification.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

final class LogCapture extends AppenderBase<ILoggingEvent> implements AutoCloseable {

    private static final long POLL_TIMEOUT_SECONDS = 10;

    private final Logger logger;
    private final BlockingQueue<ILoggingEvent> events = new LinkedBlockingQueue<>();

    private LogCapture(Logger logger) {
        this.logger = logger;
    }

    static LogCapture of(Class<?> type) {
        LogCapture capture = new LogCapture((Logger) LoggerFactory.getLogger(type));
        capture.setContext(capture.logger.getLoggerContext());
        capture.start();
        capture.logger.addAppender(capture);
        return capture;
    }

    ILoggingEvent next() throws InterruptedException {
        return events.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    boolean isEmpty() {
        return events.isEmpty();
    }

    @Override
    protected void append(ILoggingEvent event) {
        events.add(event);
    }

    @Override
    public void close() {
        logger.detachAppender(this);
        stop();
    }
}
