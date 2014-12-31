package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by liam on 6/29/14.
 */
@DatabaseTable
public class ActionArgument {

	public ActionArgument()
	{

	}

	public String toString()
	{
		if ("".equals(label))
			return "Unnamed Argument";

		return label;
	}

	public static Dao<ActionArgument, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, ActionArgument.class);
	}

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField(canBeNull = false, foreign = true)
	private Action action;

	@DatabaseField
	private String type = "string";

	@DatabaseField
	private int order;

	@DatabaseField
	private String label;

	@DatabaseField
	private String help;

	@DatabaseField
	private String defaultValue;

	@DatabaseField
	private boolean isRequired;

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
