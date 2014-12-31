package rezonant.shu.state.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class SSHSession {

	public SSHSession()
	{
	}

	public SSHSession(Session session, SSHConnection connection)
	{
		this.session = session;
		this.connection = connection;
	}

	@DatabaseField(generatedId = true)
	private int id;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private Session session;

	@DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
	private SSHConnection connection;

	public Session getSession() {
		return session;
	}

	public SSHConnection getConnection() {
		return connection;
	}
}
