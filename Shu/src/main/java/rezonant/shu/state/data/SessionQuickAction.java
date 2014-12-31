package rezonant.shu.state.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by liam on 6/29/14.
 */
@DatabaseTable
public class SessionQuickAction {
	public SessionQuickAction()
	{

	}

	public SessionQuickAction(Session session, Action action)
	{
		this.session = session;
		this.action = action;
	}

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField(canBeNull = false, foreign = true)
	private Action action;

	@DatabaseField(canBeNull = false, foreign = true)
	private Session session;

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
}
