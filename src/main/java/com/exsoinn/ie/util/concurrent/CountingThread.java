package com.exsoinn.ie.util.concurrent;

import org.apache.log4j.Logger;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by QuijadaJ on 8/14/2017.
 */
public class CountingThread extends Thread {
    private static final Logger _LOGGER = Logger.getLogger(CountingThread.class);
    private static final AtomicInteger alive = new AtomicInteger();

    public CountingThread(ThreadGroup pGrp, Runnable pTarget, String pName, long pStackSize) {
        super(pGrp, pTarget, pName, pStackSize);
    }

    public static int runningThreads() {
        return alive.get();
    }


    @Override
    public void run() {
        _LOGGER.debug("Thread " + getName() + " executing, " + alive.get() + " threads currently running.");
        try {
            alive.incrementAndGet();
            super.run();
        } finally {
            alive.decrementAndGet();
            _LOGGER.debug("Thread " + getName() + " completed, " + alive.get() + " threads currently running.");
        }
    }
}
