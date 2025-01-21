package app.finwave.backend.service.sessions;

import app.finwave.backend.api.session.SessionManager;
import app.finwave.backend.service.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.concurrent.TimeUnit;

@Singleton
public class SessionsService extends AbstractService {

    protected SessionManager manager;

    @Inject
    public SessionsService(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.deleteOverdueSessions();
    }

    @Override
    public long getRepeatTime() {
        return 24;
    }

    @Override
    public long getInitDelay() {
        return 0;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.HOURS;
    }

    @Override
    public String name() {
        return "Sessions";
    }
}
