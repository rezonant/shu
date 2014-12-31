package rezonant.shu.state.data;

import android.app.AlertDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by liam on 6/28/14.
 */
public class PersistenceManager {
	public PersistenceManager(final Context context)
	{
		final PersistenceManager self = this;
		final int databaseVersion = 2;

		// Wow don't leave this on unless it's needed (haha)
		//context.deleteDatabase("data");

		SQLiteOpenHelper helper = new SQLiteOpenHelper(context, "data", null, databaseVersion) {
			{
			}

			private SQLiteDatabase db;

			@Override
			public void onOpen(SQLiteDatabase db)
			{
				this.db = db;
				ConnectionSource src = new AndroidConnectionSource(db);
				self.connectionSource = src;
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
				this.db = db;
				Log.i("Info!", "+++++++++++++++++ CREATING DATABASE +++++++++++++++++++++++++");

				ConnectionSource src = new AndroidConnectionSource(db);
				self.connectionSource = src;

				try {
					TableUtils.createTable(src, Action.class);
					TableUtils.createTable(src, ActionArgument.class);
					TableUtils.createTable(src, Readout.class);
					TableUtils.createTable(src, Monitor.class);
					TableUtils.createTable(src, MonitorConditionGroup.class);
					TableUtils.createTable(src, MonitorCondition.class);
					TableUtils.createTable(src, SSHConnection.class);
					TableUtils.createTable(src, SSHSession.class);
					TableUtils.createTable(src, Session.class);
					TableUtils.createTable(src, SessionAction.class);
					TableUtils.createTable(src, SessionQuickAction.class);
                    TableUtils.createTable(src, SessionActionCategory.class);

				} catch (SQLException e) {
					new AlertDialog.Builder(context)
						.setTitle("Failed to create database: "+e.getMessage())
						.setMessage(e.toString())
						.create()
						.show();
				}
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int currentVersion, int newVersion) {
                this.db = db;
                Log.i("IMPORTANT", "DB UPGRADE: "+currentVersion+" => "+newVersion+" +++++++++++++++++++++++++");

                for (int version = currentVersion+1; version <= newVersion; ++version) {
                    Log.i("FYI", "Applying DB upgrade v"+version);
                    applyUpgrade(db, version);
                }
			}

            private void applyUpgrade(SQLiteDatabase db, int version) {
                ConnectionSource src = new AndroidConnectionSource(db);

                db.beginTransaction();

                try {
                    switch (version) {
                        case 1:
                            // Do nothing
                            break;
                        case 2:
                            // Add category to session action
                            Log.i("FYI", "Applying version 2 database upgrades...");

                            try {
                                Log.i("FYI", "Adding category column to SessionAction...");
                                db.execSQL("ALTER TABLE SessionAction ADD COLUMN category_id BIGINT DEFAULT NULL");

                                Log.i("FYI", "Adding category table...");
                                TableUtils.createTable(src, SessionActionCategory.class);

                                Log.i("FYI", "Upgrade to V2 was successul!");
                                db.setTransactionSuccessful();
                            } catch (SQLException e) {
                                Log.e("WTF", "Failed to upgrade database (Roll back will occur): " + e.getMessage(), e);
                            }
                            break;
                    }
                } finally {
                    db.endTransaction();
                }
            }
		};

		SQLiteDatabase db = helper.getWritableDatabase();

        this.db = db;
	}

	private SQLiteDatabase db;

	public void saveDatabase()
	{
	}

	public ConnectionSource getSource()
	{
		return connectionSource;
	}

	public <T> Dao<T,Long> getDao(Class<T> cls)
	{
		try {
			return DaoManager.createDao(connectionSource, cls);
		} catch (SQLException e) {
			Log.e("Shit", "Persistence Manager could not create DAO", e);
			return null;
		}
	}

	public static <T> Dao<T,Long> dao(Context context, Class<T> cls)
	{
		return instance(context).getDao(cls);
	}

	public static ConnectionSource source(Context context)
	{
		return instance(context).getSource();
	}

	private ConnectionSource connectionSource;
	private static PersistenceManager theInstance;
	public static PersistenceManager instance(Context context)
	{
		if (theInstance == null) {
			theInstance = new PersistenceManager(context);
		}

		return theInstance;
	}
}
