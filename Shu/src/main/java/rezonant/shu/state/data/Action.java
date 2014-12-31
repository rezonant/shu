package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

import java.util.Arrays;
import java.util.List;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class Action {
	public Action()
	{

	}

	public Action(String type)
	{
		this.type = type;
	}

	public static Dao<Action, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, Action.class);
	}

	public String toString()
	{
		if (this.name == null || "".equals(this.name)) {
			return "Unnamed Action #"+this.getId();
		}
		return this.name;
	}

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField
	private String name;

	@DatabaseField
	private String type;

	@DatabaseField
	private String command;

	@DatabaseField
	private boolean hasOutput;

	@DatabaseField
	private boolean isValue;

	@DatabaseField
	private String unit;

	@ForeignCollectionField(eager = false, foreignFieldName = "action")
	private ForeignCollection<ActionArgument> arguments;

	public ActionArgument[] getArguments()
	{
		ActionArgument[] array = new ActionArgument[arguments.size()];
		arguments.toArray(array);
		return array;
	}

	public void removeArgument(ActionArgument argument)
	{
		arguments.remove(argument);
	}

	public void setArguments(ActionArgument[] arguments)
	{
		setArguments(Arrays.asList(arguments));
	}

	public void clearArguments()
	{
		this.arguments.clear();
	}

	public void addArgument(ActionArgument argument)
	{
		this.arguments.add(argument);
	}

	public void setArguments(List<ActionArgument> arguments)
	{
		this.arguments.clear();
		this.arguments.addAll(arguments);
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public boolean hasOutput() {
		return hasOutput;
	}

	public void setHasOutput(boolean hasOutput) {
		this.hasOutput = hasOutput;
	}

	public boolean isValue() {
		return isValue;
	}

	public void setIsValue(boolean isValue) {
		this.isValue = isValue;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}
