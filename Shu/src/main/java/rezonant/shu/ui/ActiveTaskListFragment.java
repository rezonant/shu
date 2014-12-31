package rezonant.shu.ui;

import android.content.Intent;
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
import android.widget.ListView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.ui.data.EditSSHConnectionActivity;
import rezonant.shu.ui.data.EditSSHConnectionFragment;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link rezonant.shu.ui.ToolListActivity}
 * in two-pane mode (on tablets) or a {@link rezonant.shu.ui.ToolDetailActivity}
 * on handsets.
 */
public class ActiveTaskListFragment extends Fragment {
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ActiveTaskListFragment() {

	}

	public interface Delegate {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		sshConnectionsRepository = SSHConnection.createDao(getActivity());
	}

	private Dao<SSHConnection,Long> sshConnectionsRepository;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.sshconnection_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case R.id.action_add:

				Intent detailIntent = new Intent(getActivity(), EditSSHConnectionActivity.class);
				detailIntent.putExtra(EditSSHConnectionFragment.ARG_CONNECTION_ID, 0);
				startActivity(detailIntent);

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

    public void refresh()
    {
        refreshView(getView());
    }

	private void refreshView(View view)
	{

		try {
			ListView list = (ListView)view.findViewById(R.id.actions_list);
			List<SSHConnection> data = sshConnectionsRepository.queryForAll();

			if (data.size() == 0) {
				SSHConnection newConnection = new SSHConnection();
				newConnection.setId(0);
				newConnection.setSpecialLabel(getResources().getString(R.string.action_tap_to_add));
				data.add(newConnection);
			}

			list.setAdapter(new ArrayAdapter<SSHConnection>(getActivity(),
					android.R.layout.simple_list_item_1,
					android.R.id.text1, data));
		} catch (SQLException e) {
			Log.e("Database", "SQL exception", e);
		}

	}

	private class ItemClickListener implements AdapterView.OnItemClickListener {
		public ItemClickListener(ActiveTaskListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private ActiveTaskListFragment fragment;
		private ListView listView;

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM CLICKED! ****************");


			SSHConnection cnx = (SSHConnection)listView.getItemAtPosition(position);

			fragment.itemClicked(cnx);
		}
	}

	public static final int CONTEXT_MENU_DELETE = 1;
	public static final int CONTEXT_MENU_SHARE = 1;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.setHeaderTitle("Actions");
		menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Delete");
		menu.add(Menu.NONE, CONTEXT_MENU_SHARE, Menu.NONE, "Share");

		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
		public ItemLongClickListener(ActiveTaskListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private ActiveTaskListFragment fragment;
		private ListView listView;

		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM LONG CLICKED! ****************");

			SSHConnection connection = (SSHConnection)listView.getItemAtPosition(position);
			fragment.contextMenuConnection = connection;

			return false;
		}
	}

	private SSHConnection contextMenuConnection;

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		if (item.getItemId() == CONTEXT_MENU_DELETE) {
			Log.i("FYI", "DELETING action with id "+contextMenuConnection.getId());
			try {
				sshConnectionsRepository.delete(contextMenuConnection);
				contextMenuConnection = null;
				this.refreshView(getView());
			} catch (SQLException e) {
				Log.e("Database", "SQL exception", e);
			}
			return true;
		}

		return false;
	}

	private void itemClicked(SSHConnection cnx)
	{
		Intent detailIntent = new Intent(this.getActivity(), EditSSHConnectionActivity.class);
		detailIntent.putExtra(EditSSHConnectionFragment.ARG_CONNECTION_ID, cnx.getId());
		startActivity(detailIntent);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_actions_list, container, false);

		ListView listView = (ListView)rootView.findViewById(R.id.actions_list);

		registerForContextMenu(listView);

		listView.setOnItemClickListener(new ItemClickListener(this, listView));
		listView.setOnItemLongClickListener(new ItemLongClickListener(this, listView));

		refreshView(rootView);

		return rootView;
	}
}
