package app.finwave.backend.service;

import java.util.concurrent.TimeUnit;

public abstract class AbstractService {
    public abstract void run();
    public abstract long getRepeatTime();
    public abstract long getInitDelay();
    public abstract TimeUnit getTimeUnit();
    public abstract String name();
}
