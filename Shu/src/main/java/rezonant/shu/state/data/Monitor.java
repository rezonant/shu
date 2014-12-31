package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class Monitor {
	public Monitor()
	{

	}

	public static Dao<Monitor, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, Monitor.class);
	}

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField
	private String title;

	/**
	 * The amount of time between checking conditions for an alert, in seconds
	 */
	@DatabaseField
	private int pollTime = 60;

	@DatabaseField(canBeNull = false, foreign = true)
	private MonitorConditionGroup monitorGroup;

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getPollTime() {
		return pollTime;
	}

	public void setPollTime(int pollTime) {
		this.pollTime = pollTime;
	}

	public MonitorConditionGroup getMonitorGroup() {
		return monitorGroup;
	}

	public void setMonitorGroup(MonitorConditionGroup monitorGroup) {
		this.monitorGroup = monitorGroup;
	}
}
