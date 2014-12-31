package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class MonitorConditionGroup {
	private MonitorConditionGroup()
	{
	}


	public static Dao<MonitorConditionGroup, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, MonitorConditionGroup.class);
	}

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField
	private boolean ifAny = false;

	@DatabaseField(canBeNull = true, foreign = true)
	private MonitorConditionGroup parent = null;

	@ForeignCollectionField
	private ForeignCollection<MonitorConditionGroup> subgroups;

	@ForeignCollectionField
	private ForeignCollection<MonitorCondition> conditions;

	public boolean isIfAny() {
		return ifAny;
	}

	public MonitorConditionGroup setAny() {
		this.ifAny = true;
		return this;
	}

	public MonitorConditionGroup setAll() {
		this.ifAny = false;
		return this;
	}

	public boolean isAll() {
		return !this.ifAny;
	}

	public boolean isAny() {
		return this.ifAny;
	}

	public MonitorCondition[] getConditions()
	{
		MonitorCondition[] array = new MonitorCondition[conditions.size()];
		conditions.toArray(array);
		return array;
	}

	public MonitorConditionGroup[] getSubgroups()
	{
		MonitorConditionGroup[] array = new MonitorConditionGroup[subgroups.size()];
		subgroups.toArray(array);
		return array;
	}

	public MonitorConditionGroup addCondition(MonitorCondition condition) {
		this.conditions.add(condition);
		return this;
	}

	public MonitorConditionGroup removeCondition(MonitorCondition condition) {
		List<MonitorCondition> list = new ArrayList<MonitorCondition>(conditions);

		for (MonitorCondition cond : list) {
			if (cond.getId() == condition.getId())
				conditions.remove(cond);
		}

		return this;
	}

	public MonitorConditionGroup addSubgroup(MonitorConditionGroup group) {
		this.subgroups.add(group);
		return this;
	}

	public long getId() {
		return id;
	}

	public MonitorConditionGroup getParent() {
		return parent;
	}

	public void setParent(MonitorConditionGroup parent) {
		this.parent = parent;
	}
}
