package rezonant.shu.state.ssh;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import rezonant.shu.state.data.SSHConnection;

/**
 * Created by liam on 7/4/14.
 */
public class SSHConnectionManager {
	private SSHConnectionManager()
	{

	}

	private static SSHConnectionManager instance;

	public static SSHConnectionManager instance()
	{
		if (instance == null) instance = new SSHConnectionManager();
		return instance;
	}

	private Map<Long, SSHConnectionThread> threads = new HashMap<Long, SSHConnectionThread>();

	public SSHConnectionThread getConnection(Context context, SSHConnection connection)
	{
		if (!threads.containsKey(connection.getId())) {
			SSHConnectionThread newThread = connect(context, connection);
            newThread.setId(connection.getId());
			threads.put(connection.getId(), newThread);
			return newThread;
		}

		return threads.get(connection.getId());
	}

	private SSHConnectionThread connect(Context context, SSHConnection connection)
	{
		SSHConnectionThread thread = new SSHConnectionThread(context, connection.getUser(), connection.getHost(), connection.getPort());
		thread.setPassword(connection.getPassword());
		thread.start();
		return thread;
	}

    public boolean isActive(SSHConnection connection) {
        return this.threads.containsKey(connection.getId());
    }

    public void hasDisconnected(SSHConnectionThread thread) {

        Log.i("FYI", "SSHConnectionManager: Removing SSH thread for #"+thread.getId()+" from registry...");
        Log.i("FYI", this.threads.keySet().size()+" threads currently in registry");

        Log.i("FYI", "Dump of registered SSH threads:");
        for (Long key : this.threads.keySet()) {
            SSHConnectionThread value = this.threads.get(key);

            Log.i("FYI", " - "+key+": "+value);
        }

        this.threads.remove(thread.getId());

        if (this.threads.containsKey(thread.getId())) {

            Log.e("WTF", "Just removed this key, it shouldnt be here anymore: '"+thread.getId()+"'");
        }
        Log.i("FYI", this.threads.keySet().size()+" threads are left in registry");
    }
}
