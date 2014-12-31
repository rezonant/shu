package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by liam on 6/29/14.
 */
@DatabaseTable
public class SessionAction {
	public SessionAction()
	{

	}

	public SessionAction(Session session, Action action)
	{
		this.session = session;
		this.action = action;
	}

	public static Dao<SessionAction, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, SessionAction.class);
	}

	@Override
	public String toString()
	{
		if (specialLabel != null && !"".equals(specialLabel))
			return specialLabel;

        if (this.title != null)
            return this.title;

		return action.toString();
	}

	private String specialLabel = null;

	public void setSpecialLabel(String label)
	{
		specialLabel = label;
	}

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField
	private String title;

	@DatabaseField(canBeNull = true, foreign = true)
	private SSHConnection connection;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private Action action;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private Session session;

    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    private SessionActionCategory category;

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public SSHConnection getConnection() {
		return connection;
	}

	public void setConnection(SSHConnection connection) {
		this.connection = connection;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

    public SessionActionCategory getCategory() {
        return category;
    }

    public void setCategory(SessionActionCategory category) {
        this.category = category;
    }
}
