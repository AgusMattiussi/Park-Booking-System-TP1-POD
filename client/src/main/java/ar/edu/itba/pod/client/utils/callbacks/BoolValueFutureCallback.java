package ar.edu.itba.pod.client.utils.callbacks;

import com.google.protobuf.BoolValue;
import org.slf4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BoolValueFutureCallback extends CustomFutureCallback<BoolValue> {

    private final AtomicInteger added;
    private final AtomicInteger couldNotAdd;

    public BoolValueFutureCallback(Logger logger, CountDownLatch latch, AtomicInteger added, AtomicInteger couldNotAdd) {
        super(logger, latch);
        this.added = added;
        this.couldNotAdd = couldNotAdd;
    }

    @Override
    public void onSuccess(BoolValue boolValue) {
        if (boolValue.getValue())
            added.incrementAndGet();
        else
            couldNotAdd.incrementAndGet();

        getLatch().countDown();
    }
}
