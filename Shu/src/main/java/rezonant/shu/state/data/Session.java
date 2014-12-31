package rezonant.shu.state.data;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class Session {
	public Session()
	{
	}

	public Session(String name, String type)
	{
		this.name = name;
		this.type = type;
	}

	public static Dao<Session, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, Session.class);
	}

	@Override
	public String toString()
	{
		if ("".equals(this.name))
			return "Unnamed Session";
		return this.name;
	}

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField
	private String name = "New Session";

	@DatabaseField
	private String type = "default";

	@DatabaseField
	private Date lastUsed = null;

	@DatabaseField
	private Date created = new Date();

	@DatabaseField
	private boolean starred = false;

	@DatabaseField
	private String authorName = "";

	@DatabaseField
	private String importSourceType = "";

	@DatabaseField
	private String importSource = "";

	@ForeignCollectionField
	ForeignCollection<SSHSession> connections;

	@ForeignCollectionField
	ForeignCollection<SessionAction> actions;

	@ForeignCollectionField
	ForeignCollection<SessionQuickAction> quickActions;

	public SessionAction[] getActions()
	{
		if (actions == null)
			return new SessionAction[0];

		SessionAction[] array = new SessionAction[actions.size()];
		actions.toArray(array);
		return array;
	}

	public Action[] getQuickActions()
	{
		Action[] array = new Action[quickActions.size()];
		int i = 0;
		for (SessionQuickAction action : quickActions) {
			array[i] = action.getAction();
			++i;
		}
		return array;
	}

	public void setActions(List<Action> actions)
	{
		Map<Long,SessionAction> existingActions = new HashMap<Long,SessionAction>();

		for (SessionAction sessionAction : this.actions) {
			existingActions.put(sessionAction.getAction().getId(), sessionAction);
		}

		this.actions.clear();
		for (Action action : actions) {
			if (existingActions.containsKey(action.getId()))
				this.actions.add(existingActions.get(action.getId()));
			else
				this.addAction(action);
		}
	}

	public void setQuickActions(List<Action> actions)
	{
		Map<Long,SessionQuickAction> existingActions = new HashMap<Long,SessionQuickAction>();

		for (SessionQuickAction sessionAction : this.quickActions) {
			existingActions.put(sessionAction.getAction().getId(), sessionAction);
		}

		this.quickActions.clear();
		for (Action action : actions) {
			if (existingActions.containsKey(action.getId()))
				this.quickActions.add(existingActions.get(action.getId()));
			else
				this.addQuickAction(action);
		}
	}

	public SSHConnection[] getConnections()
	{
        if (connections == null) {
            Log.e("WTF", "Connections is null, unexpectedly. Trying to recover by returning empty list.");
            return new SSHConnection[0];
        }
		SSHConnection[] array = new SSHConnection[connections.size()];
		int i = 0;
		for (SSHSession session : connections) {
			array[i] = session.getConnection();
			++i;
		}
		return array;
	}

	public void addConnection(SSHConnection connection)
	{
		connections.add(new SSHSession(this, connection));
	}

	public void addAction(Action action)
	{
		actions.add(new SessionAction(this, action));
	}

	public void addAction(SessionAction action)
	{
		action.setSession(this);
		actions.add(action);
	}

	public void addQuickAction(Action action)
	{
		quickActions.add(new SessionQuickAction(this, action));
	}

	public void removeConnection(SSHConnection connection) {
		List<SSHSession> list = new ArrayList<SSHSession>(connections);

		for (SSHSession cnx : list) {
			if (cnx.getConnection().getId() == connection.getId()) {
				connections.remove(cnx);
			}
		}
	}

	public void setConnections(List<SSHConnection> connections) {

		Map<Long,SSHSession> existingSessions = new HashMap<Long,SSHSession>();

		for (SSHSession cnxSession : this.connections) {
			existingSessions.put(cnxSession.getConnection().getId(), cnxSession);
		}

		this.connections.clear();
		for (SSHConnection cnx : connections) {
			if (existingSessions.containsKey(cnx.getId()))
				this.connections.add(existingSessions.get(cnx.getId()));
			else
				this.addConnection(cnx);
		}
	}

	/**
	 * Get a string describing the source of the import. The format of the string
	 * depends heavily on the import source type (see getImportSourceType()).
	 * @return The import source string (maybe a URL or other identifier)
	 */
	public String getImportSource() {
		return importSource;
	}

	/**
	 * Get the type of import source that was used to receive this activity when it was created.
	 *
	 * @return
	 */
	public String getImportSourceType() {
		return importSourceType;
	}

	/**
	 * Get the author's name (usually blank unless imported)
	 * @return
	 */
	public String getAuthorName() {
		return authorName;
	}

	/**
	 * Was this activity starred by the user
	 * @return
	 */
	public boolean isStarred() {
		return starred;
	}

	/**
	 * Set the starred status for this item.
	 * @param starred
	 */
	public void setStarred(boolean starred) {
		this.starred = starred;
	}

	/**
	 * When the item was created.
	 * @return
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * When the item was last used.
	 * @return
	 */
	public Date getLastUsed() {
		return lastUsed;
	}

	/**
	 * Set the last used time.
	 */
	public void wasUsed() {
		this.lastUsed = new Date();
	}

	/**
	 * Retrieve the type of activity this is
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type of activity
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the name of the activity
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the activity
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the numeric ID of this activity
	 * @return
	 */
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
