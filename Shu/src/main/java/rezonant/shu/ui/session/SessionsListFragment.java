package rezonant.shu.ui.session;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.ui.data.ActionsListActivity;
import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.ui.data.SSHConnectionListActivity;
import rezonant.shu.state.data.Session;
import rezonant.shu.ui.data.EditSessionActivity;
import rezonant.shu.ui.global.AboutShuActivity;
import rezonant.shu.ui.global.SettingsActivity;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link rezonant.shu.ui.ToolListActivity}
 * in two-pane mode (on tablets) or a {@link rezonant.shu.ui.ToolDetailActivity}
 * on handsets.
 */
public class SessionsListFragment extends Fragment {

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SessionsListFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		sessionsRepository = Session.createDao(getActivity());
	}

	private Dao<Session,Long> sessionsRepository;


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.sessions_list, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.action_add:

				Intent detailIntent = new Intent(getActivity(), EditSessionActivity.class);
				startActivity(detailIntent);

				return true;
			case R.id.action_actions:
				startActivity(new Intent(getActivity(), ActionsListActivity.class));
				return true;

			case R.id.action_ssh_connections:
				startActivity(new Intent(getActivity(), SSHConnectionListActivity.class));
				return true;

            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;

            case R.id.action_about:
                startActivity(new Intent(getActivity(), AboutShuActivity.class));
                return true;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume()
	{
		refreshView(getView());
		super.onResume();
	}

    private int currentTip = -1;

	private void refreshView(View view)
	{

		try {
			final ListView list = (ListView)view.findViewById(R.id.sessions_list);
			final List<Session> data = sessionsRepository.queryForAll();

			if (data.size() < 10) {
				Session newSession = new Session();
				newSession.setId(0);

                if (data.size() == 0) {
                    newSession.setName(getResources().getString(R.string.action_add_first_session));
                } else if (data.size() == 1) {
                    newSession.setName(getResources().getString(R.string.action_add_second_session));
                } else {
                    newSession.setName(getResources().getString(R.string.action_tap_to_add));
                }

				data.add(newSession);
			}

            final BaseAdapter adapter = new BaseAdapter() {
                @Override
                public int getCount() {
                    return data.size();
                }

                @Override
                public Object getItem(int position) {
                    return data.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = getLayoutInflater(null).inflate(R.layout.list_session, parent, false);
                    }

                    Session item = data.get(position);

                    TextView nameLabel = (TextView)convertView.findViewById(R.id.name);
                    final TextView statusLabel = (TextView)convertView.findViewById(R.id.statusLabel);

                    nameLabel.setText(item.toString());

                    if (item.getId() != 0) {
                        int count = 0;
                        for (SSHConnection connection : item.getConnections()) {
                            boolean active = SSHConnectionManager.instance().isActive(connection);
                            if (active) {
                                count += 1;
                            }
                        }

                        if (count == 1)
                            statusLabel.setText(count + " active connection");
                        else
                            statusLabel.setText(count + " active connections");

                        nameLabel.setTextColor(Color.WHITE);
                        statusLabel.setTextColor(Color.GRAY);

                    } else {
                        if (data.size() == 1)
                            statusLabel.setText("Make a new session to get started.");
                        else if (data.size() < 5)
                            statusLabel.setText("Add another session! You can make as many as you need.");
                        else
                            statusLabel.setText("Power user!");

                        nameLabel.setTextColor(Color.GRAY);
                        statusLabel.setTextColor(Color.GRAY);
                    }

                    return convertView;
                }
            };
            list.setAdapter(adapter);

//			list.setAdapter(new ArrayAdapter<Session>(getActivity(),
//					android.R.layout.simple_list_item_1,
//					android.R.id.text1, data));
		} catch (SQLException e) {
			Log.e("Database", "SQL exception", e);
		}

	}

    private class ItemClickListener implements AdapterView.OnItemClickListener {
		public ItemClickListener(SessionsListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private SessionsListFragment fragment;
		private ListView listView;

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM CLICKED (pos "+position+")! ****************");

			Session session = (Session)listView.getItemAtPosition(position);
			fragment.itemClicked(session);
		}
	}

	private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
		public ItemLongClickListener(SessionsListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private SessionsListFragment fragment;
		private ListView listView;

		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM LONG CLICKED! ****************");

			Session session = (Session)listView.getItemAtPosition(position);
			fragment.contextMenuSession = session;

			return false;
		}
	}

	private void itemClicked(Session session)
	{
		launchView(session);
	}

	private void launchEdit(Session session)
	{
		Intent detailIntent = new Intent(this.getActivity(), EditSessionActivity.class);
		detailIntent.putExtra(Constants.ARG_SESSION_ID, session.getId());
		startActivity(detailIntent);
	}

	private void launchView(Session session)
	{
		if (session.getId() == 0) {
			launchEdit(session);
			return;
		}

		Log.i("WTF", "User clicked session with ID "+session.getId());

		Intent detailIntent = new Intent(this.getActivity(), SessionViewActivity.class);
		detailIntent.putExtra(Constants.ARG_SESSION_ID, session.getId());
		startActivity(detailIntent);
	}

	private void itemLongClicked(Session session, View view)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);
	}

	public static final int CONTEXT_MENU_EDIT = 1;
	public static final int CONTEXT_MENU_DELETE = 2;
	public static final int CONTEXT_MENU_SHARE = 3;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		boolean isNew = false;

		if (contextMenuSession != null) {
			menu.setHeaderTitle("Actions ("+contextMenuSession.toString()+")");
			if (contextMenuSession.getId() == 0)
				isNew = true;
		} else {
			menu.setHeaderTitle("Actions");
		}

		menu.add(Menu.NONE, CONTEXT_MENU_EDIT, Menu.NONE, "Edit");

		if (!isNew) {
			menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Delete");
			menu.add(Menu.NONE, CONTEXT_MENU_SHARE, Menu.NONE, "Share");
		}

		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	private Session contextMenuSession;

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		if (item.getItemId() == CONTEXT_MENU_DELETE) {
			Log.i("FYI", "DELETING item with id "+contextMenuSession.getId());
			try {
				sessionsRepository.delete(contextMenuSession);
				contextMenuSession = null;
				this.refreshView(getView());
			} catch (SQLException e) {
				Log.e("Database", "SQL exception", e);
			}
			return true;
		} else if (item.getItemId() == CONTEXT_MENU_EDIT) {
			launchEdit(contextMenuSession);
		}

		return false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sessions_list, container, false);

		ListView listView = (ListView)rootView.findViewById(R.id.sessions_list);
		registerForContextMenu(listView);
		listView.setOnItemLongClickListener(new ItemLongClickListener(this, listView));
		listView.setOnItemClickListener(new ItemClickListener(this, listView));

		refreshView(rootView);

		return rootView;
	}
}
