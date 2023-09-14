package ar.edu.itba.pod.client.utils.callbacks;

import com.google.common.util.concurrent.FutureCallback;

import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;

public abstract class CustomFutureCallback<V> implements FutureCallback<V> {

    private final Logger logger;
    private final CountDownLatch latch;

    protected CustomFutureCallback(Logger logger, CountDownLatch latch) {
        if(logger == null || latch == null)
            throw new IllegalArgumentException("logger and latch must not be null");

        this.logger = logger;
        this.latch = latch;
    }

    @Override
    public void onSuccess(V v) {
        throw new UnsupportedOperationException("onSuccess not implemented");
    }

    @Override
    public void onFailure(Throwable throwable) {
        latch.countDown();
        logger.error(throwable.getMessage());
    }

    public Logger getLogger() {
        return logger;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
