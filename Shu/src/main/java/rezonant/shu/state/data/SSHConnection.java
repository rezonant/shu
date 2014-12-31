package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class SSHConnection {
	public SSHConnection()
	{

	}

	public SSHConnection(String host, String user, String pass)
	{
		this.host = host;
		this.user = user;
		this.password = pass;
	}


	public static Dao<SSHConnection, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, SSHConnection.class);
	}

	private String specialLabel = null;

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField
	private String host;

	@DatabaseField
	private String user;

	@DatabaseField
	private int port = 22;

	@DatabaseField
	private String password;

	@DatabaseField
	private Date lastUsed = null;

	@DatabaseField
	private Date created = new Date();

	@ForeignCollectionField
	private ForeignCollection<SSHSession> sessions;

	@Override
	public String toString()
	{
		if (this.specialLabel != null)
			return this.specialLabel;
		if (port != 22) {
			return user+"@"+host+":"+port;
		}

		return user+"@"+host;
	}

	public void setSpecialLabel(String label)
	{
		this.specialLabel = label;
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
	public String getUser() {
		return user;
	}

	/**
	 * Set the type of activity
	 * @param user
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Get the name of the activity
	 * @return
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the name of the activity
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Get the numeric ID of this activity
	 * @return
	 */
	public long getId() {
		return id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Session[] getSessions() {
		List<Session> sessions = new ArrayList<Session>();

		for (SSHSession session : this.sessions) {
			sessions.add(session.getSession());
		}

		Session[] array = new Session[sessions.size()];
		sessions.toArray(array);
		return array;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
