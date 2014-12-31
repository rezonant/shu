package rezonant.shu.ui.session;

import android.app.Activity;
import android.app.ActionBar;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.data.Session;
import rezonant.shu.ui.listview.ItemAdapter;
import rezonant.shu.ui.listview.ListItem;
import rezonant.shu.ui.listview.RealizedListItem;
import rezonant.shu.ui.menus.Menus;
import rezonant.shu.state.ssh.SSHCommand;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;
import rezonant.shu.state.ActionExecutionRecord;
import rezonant.shu.state.SessionState;
import rezonant.shu.state.SessionStateManager;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    private static final String ARG_RUNNING_ACTIONS = "running_actions";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    public NavigationDrawerFragment() {
    }

    private long sessionId;
    private Session session;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dao<Session, Long> sessionRepository = Session.createDao(getActivity());

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARG_SESSION_ID))
                this.sessionId = savedInstanceState.getLong(Constants.ARG_SESSION_ID);
        }

        if (this.getArguments() != null) {
            if (this.getArguments().containsKey(Constants.ARG_SESSION_ID))
                this.sessionId = this.getArguments().getLong(Constants.ARG_SESSION_ID);
        }

        if (this.sessionId != 0) {
            try {
                this.session = sessionRepository.queryForId(this.sessionId);
            } catch (SQLException e) {
                Log.e("WTF", e.getMessage(), e);
                this.session = null;
            }
        }

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition);

        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

	private Object contextItem;

	public static final int CONTEXT_MENU_EDIT = 1;
	public static final int CONTEXT_MENU_REMOVE = 2;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		if (contextItem instanceof RunningActionListItem) {
			menu.setHeaderTitle("Actions");
			menu.add(Menu.NONE, CONTEXT_MENU_REMOVE, Menu.NONE, "Close");

			ListView listView = (ListView)getView().findViewById(R.id.sessions_list);
		}

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		if (item.getItemId() == CONTEXT_MENU_REMOVE) {
			if (contextItem instanceof RunningActionListItem) {
				this.runningActions.remove((RunningActionListItem)contextItem);
				this.refreshMenu();
			}
			return true;
		} else if (item.getItemId() == CONTEXT_MENU_EDIT) {
			// TODO
			//Intent intent = new Intent(getActivity(), EditActionActivity.class);
			//intent.putExtra(Constants.ARG_ACTION_ID, contextMenuAction.getAction().getId());
			//startActivity(intent);
		}

		return false;
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);

		registerForContextMenu(mDrawerListView);

		mDrawerListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
				NavigationDrawerFragment.this.contextItem =
						NavigationDrawerFragment.this.mDrawerListView.getItemAtPosition(i);
				return false;
			}
		});

        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCurrentSelectedPosition = position;
                selectItem(position);
            }
        });

        Dao<SSHConnection,Long> connectionRepository = SSHConnection.createDao(getActivity());

        if (savedInstanceState != null) {

            Parcelable[] items = savedInstanceState.getParcelableArray(ARG_RUNNING_ACTIONS);

            if (items != null) for (Parcelable parcelable : items) {
                RunningActionParcelable action = (RunningActionParcelable) parcelable;
                SSHConnection connection = null;

                try {
                    connection = connectionRepository.queryForId(action.connectionID);
                } catch (SQLException e) {
                    Log.e("WTF", e.getMessage(), e);
                    continue;
                }

                if (connection != null) {
                    SSHConnectionThread thread =
                            SSHConnectionManager.instance().getConnection(getActivity(), connection);
                    SSHCommand command = thread.getCommandById(action.connectionID);

                    if (command != null) {
                        addRunningAction(action.label, command);
                    }
                }
            }
        }

		refreshMenu();

        return mDrawerListView;
    }

	public class RunningActionListItem extends RealizedListItem {
		public RunningActionListItem(String label, SSHCommand command)
		{
			super("action", 0, label);

			this.command = command;
			isSectionHeader = true;
		}

		private SSHCommand command;

		public SSHCommand getCommand() {
			return command;
		}
	}

	private List<RunningActionListItem> runningActions = new ArrayList<RunningActionListItem>();

	public void addRunningAction(String label, SSHCommand command)
	{
		runningActions.add(new RunningActionListItem(label, command));
		refreshMenu();
	}

    public void setSession(Session session)
    {
        this.session = session;
        this.sessionId = session.getId();

        refreshMenu();
    }

	public void refreshMenu()
	{
        if (getActivity() == null) {
            Log.e("WAT", "Cannot refresh drawer, not attached to the activity anymore!");
            return;
        }

        Log.i("FYI", "Drawer is refreshing menu");
		List<ListItem> items = new ArrayList<ListItem>();

		items.add(new ListItem("dashboard", R.string.section_dashboard));
		//items.add(new ListItem("settings", R.string.section_settings));
		items.add(new ListItem("running", R.string.section_running));

        if (session != null) {
            SessionState state = SessionStateManager.instance().getSessionState(session);

            Log.i("FYI", "Drawer has session state, so we're going nuts!");

            List<ActionExecutionRecord> list = state.getActiveActions();
            runningActions.clear();
            for (ActionExecutionRecord record : list) {
                if (record.getSSHCommand() == null)
                    continue; // for now

                Log.i("FYI", "[DRAWER] Adding a list item for the action we have here!");

                runningActions.add(new RunningActionListItem(record.getAction().getName(),
                        record.getSSHCommand()));
            }
        }

		for (RunningActionListItem item : runningActions)
			items.add(item);

        items.add(ListItem.createSectionHeader("actions", R.string.section_start_action));
        items.add(ListItem.createSectionHeader("history", R.string.section_history));

        items.add(ListItem.createSectionHeader("monitors", R.string.section_monitors));
        items.add(ListItem.createSectionHeader("filesystem", R.string.section_filesystem));
        items.add(ListItem.createSectionHeader("webcams", R.string.section_webcams));

		items.add(new ListItem("quick_actions", R.string.section_quick_actions));

		// TODO: insert quick actions here

		items.add(ListItem.createSectionHeader("add_quick_action", R.string.action_add));

		mDrawerListView.setAdapter(new ItemAdapter(
				getActionBar().getThemedContext(),
				Menus.realize(items, getResources())));
		mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
	}

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);

			ListItem item = (ListItem)mDrawerListView.getItemAtPosition(position);

			if (mCallbacks != null) {
				mCallbacks.onNavigationDrawerItemSelected(item);
			}
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private class RunningActionParcelable implements Parcelable {
        public RunningActionParcelable(Parcel parcel) {
            String[] data = new String[3];
            parcel.readStringArray(data);

            this.connectionID = Long.parseLong(data[0]);
            this.commandID = Long.parseLong(data[1]);
            this.label = data[2];
        }

        public RunningActionParcelable(long connectionID, long commandID, String label)
        {
            this.connectionID = connectionID;
            this.commandID = commandID;
            this.label = label;
        }

        public String label;
        public long connectionID;
        public long commandID;


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(new String[] {
               label, connectionID+"", commandID+""
            });
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
        List<RunningActionParcelable> list = new ArrayList<RunningActionParcelable>();

        for (RunningActionListItem item : this.runningActions) {
            list.add(new RunningActionParcelable(item.getCommand().getThread().getId(), item.getCommand().getId(), item.getRealizedLabel()));
        }

        RunningActionParcelable[] array = new RunningActionParcelable[list.size()];
        list.toArray(array);
        outState.putParcelableArray(ARG_RUNNING_ACTIONS, array);

        outState.putLong(Constants.ARG_SESSION_ID, this.sessionId);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.navigation_drawer, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
			case R.id.action_run:
				Toast.makeText(getActivity(), "Start action.", Toast.LENGTH_SHORT).show();
				return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        if (getActivity() == null)
            return null;

        return getActivity().getActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(ListItem item);
    }
}
