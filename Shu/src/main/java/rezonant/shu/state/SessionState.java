package rezonant.shu.state;

import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rezonant.shu.state.data.Session;

/**
 * Manages state for a single session. This allows the user interface to present a unified view
 * of session state controlled by the "active" portion of the application.
 *
 * All operations on SessionState objects are synchronized, which is important because it is
 * accessed from many threads.
 *
 * You can retrieve a SessionState object by first retrieving the SessionStateManager singleton
 * instance and then passing a rezonant.jarvis.data.Session object.
 *
 * Created by liam on 7/20/14.
 */
public class SessionState {
    public SessionState(Session session)
    {
        this.session = session;
    }

    private Session session;
    private List<ActionExecutionRecord> actionsExecuted
            = new ArrayList<ActionExecutionRecord>();
    private Map<Long,ActionExecutionRecord> actionsExecutedMap
            = new HashMap<Long,ActionExecutionRecord>();
    private long freeId = 1;
    private List<ActionExecutionRecord> actionsExecuting
            = new ArrayList<ActionExecutionRecord>();
    private Date startTime;

    public boolean isActive(ActionExecutionRecord record) {
        return this.actionsExecuting.contains(record);
    }

    public static class Stats {
        public Stats()
        {

        }

        public long actionsExecuted;
        public long secondsActive;
    }

    /**
     * Get some statistics about the session related to it's state within the context of
     * this instance of the application.
     *
     * @return
     */
    public synchronized Stats getStats()
    {
        Stats stats = new Stats();
        stats.actionsExecuted = this.freeId;
        stats.secondsActive = new Date().getTime() - startTime.getTime();

        return stats;
    }

    /**
     * Registers an action which is about to be executed by the user.
     * You should probably call nowExecuting() after this, when the
     * command begins executing.
     *
     * @param record
     */
    public synchronized void registerAction(ActionExecutionRecord record)
    {
        record.setId(freeId++);
        actionsExecuted.add(record);
        actionsExecutedMap.put(record.getId(), record);
    }

    /**
     * Add the given execution record to the currently-running list.
     * You should then call finishedExecuting(ActionExecutionRecord)
     * when this execution has completed.
     *
     * NOTE: You should call registerAction() before calling this.
     *
     * @param record
     */
    public synchronized void nowExecuting(ActionExecutionRecord record)
    {
       this.actionsExecuting.add(record);
    }

    /**
     * Call this when the execution is finished, and should be removed from
     * the currently-running list.
     *
     * @param record
     */
    public synchronized void finishedExecuting(ActionExecutionRecord record)
    {
        this.actionsExecuting.remove(record);
    }

    /**
     * Retrieve an action execution event by ID.
     *
     * @param id
     * @return
     */
    public synchronized ActionExecutionRecord getByID(long id)
    {
        if (actionsExecutedMap.containsKey(id)) {
            return actionsExecutedMap.get(id);
        }

        return null;
    }

    /**
     * Get all active actions. These are the actions which have called nowExecuting()
     * but have not yet called finishedExecuting().
     *
     * @return
     */
    public synchronized List<ActionExecutionRecord> getActiveActions()
    {
        return new ArrayList<ActionExecutionRecord>(actionsExecuting);
    }

    public synchronized List<ActionExecutionRecord> getAllActionsExecuted()
    {
        return new ArrayList<ActionExecutionRecord>(actionsExecuted);
    }

    public Session getSession() {
        return session;
    }
}
