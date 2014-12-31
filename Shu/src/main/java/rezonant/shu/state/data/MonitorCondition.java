package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class MonitorCondition {
	public MonitorCondition()
	{
	}


	public static Dao<MonitorCondition, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, MonitorCondition.class);
	}

	@DatabaseField(generatedId = true)
	private int id;

	@DatabaseField(canBeNull = false, foreign = true)
	private Action valueAction;

	@DatabaseField
	private String operator;

	@DatabaseField
	private String value;

	@DatabaseField(canBeNull = false, foreign = true)
	private MonitorConditionGroup group;

	public int getId() {
		return id;
	}

	public Action getValueAction() {
		return valueAction;
	}

	public void setValueAction(Action valueAction) {
		this.valueAction = valueAction;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public MonitorConditionGroup getGroup() {
		return group;
	}

	public void setGroup(MonitorConditionGroup group) {
		this.group = group;
	}
}
