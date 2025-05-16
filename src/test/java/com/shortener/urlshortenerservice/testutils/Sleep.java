package com.shortener.urlshortenerservice.testutils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Sleep {
    public static void sleepMilliSeconds(int delayMilliSeconds){
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(scheduler::shutdown, delayMilliSeconds, TimeUnit.MILLISECONDS);
    }
}