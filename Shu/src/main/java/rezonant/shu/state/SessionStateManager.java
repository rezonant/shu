package rezonant.shu.state;

import android.util.ArrayMap;

import java.util.HashMap;
import java.util.Map;

import rezonant.shu.state.data.Session;

/**
 * Created by liam on 7/20/14.
 */
public class SessionStateManager {

    private SessionStateManager() {

    }

    private Map<Long,SessionState> sessionStates = new HashMap<Long,SessionState>();
    private static SessionStateManager theInstance = null;

    public static SessionStateManager instance()
    {
        if (theInstance == null) {
            theInstance = new SessionStateManager();
        }

        return theInstance;
    }

    public SessionState getSessionState(Session session)
    {
        if (!sessionStates.containsKey(session.getId())) {
            SessionState newState = new SessionState(session);
            sessionStates.put(session.getId(), newState);
            return newState;
        }

        return sessionStates.get(session.getId());
    }
}
