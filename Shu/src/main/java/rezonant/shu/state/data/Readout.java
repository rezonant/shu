package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.*;
import com.j256.ormlite.table.*;

/**
 * Created by liam on 6/28/14.
 */
@DatabaseTable
public class Readout {
	public Readout()
	{

	}

	public static Dao<Readout, Long> createDao(Context context)
	{
		return PersistenceManager.dao(context, Readout.class);
	}

    /************* Fields *************/

	@DatabaseField(generatedId = true)
	private long id;

	@DatabaseField
	private String title;

	@DatabaseField(canBeNull = false, foreign = true)
	private Action valueAction;

    /************* Methods ************/

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Action getValueAction() {
		return valueAction;
	}

	public void setValueAction(Action valueAction) {
		this.valueAction = valueAction;
	}
}
