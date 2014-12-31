package rezonant.shu.state.data;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by liam on 8/9/14.
 */
@DatabaseTable
public class SessionActionCategory {

    public static Dao<SessionActionCategory, Long> createDao(Context context)
    {
        return PersistenceManager.dao(context, SessionActionCategory.class);
    }

    @Override
    public String toString() {
        return title;
    }

    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField
    private String title;

    @ForeignCollectionField
    private ForeignCollection<SessionAction> actions;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ForeignCollection<SessionAction> getActions() {
        return actions;
    }

    public void setActions(ForeignCollection<SessionAction> actions) {
        this.actions = actions;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
