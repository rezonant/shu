package rezonant.shu.ui.data;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.data.Session;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link rezonant.shu.ui.ToolListActivity}
 * in two-pane mode (on tablets) or a {@link rezonant.shu.ui.ToolDetailActivity}
 * on handsets.
 */
public class EditSessionFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	private Dao<SSHConnection, Long> sshConnectionRepository;
	public static final int RESULT_FINISHED = 133709876;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EditSessionFragment() {
	}

	Dao<Session,Long> sessionRepository;

	public void save()
	{
		try {
			if (session.getId() == 0)
				session.setId(sessionId = sessionRepository.create(session));
			else
				sessionRepository.update(session);
		} catch (SQLException e) {
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
			return;
		}
	}

	private class ManageConnectionsButtonListener implements View.OnClickListener {
		public ManageConnectionsButtonListener(EditSessionFragment fragment) {
			this.fragment = fragment;
		}

		private EditSessionFragment fragment;

		@Override
		public void onClick(View view) {
			fragment.onClickManageConnections();
		}
	}

	private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
		public ItemLongClickListener(EditSessionFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private EditSessionFragment fragment;
		private ListView listView;

		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM LONG CLICKED! ****************");

			SSHConnection cnx = (SSHConnection)listView.getItemAtPosition(position);
			fragment.contextMenuConnection = cnx;

			return false;
		}
	}

	private SSHConnection contextMenuConnection;

	@Override
	public void onPause()
	{
		onClickSave();
		super.onPause();
	}

	@Override
	public void onResume()
	{
		refreshUI();
		super.onResume();
	}

	private void onClickSave()
	{
		syncToObject();
		save();
	}

	private void onClickManageConnections()
	{

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sessionRepository = Session.createDao(this.getActivity());
		sshConnectionRepository = SSHConnection.createDao(this.getActivity());

		if (getArguments().containsKey(Constants.ARG_SESSION_ID))
			this.sessionId = getArguments().getLong(Constants.ARG_SESSION_ID);

		Log.i("FYI", "Opening session with ID "+this.sessionId);

		if (this.sessionId == 0) {
			this.session = new Session();
			this.session.setName("");

			try {
				sessionRepository.create(this.session);
				sessionRepository.refresh(this.session);
			} catch (SQLException e) {
				Log.e("WTF", e.getMessage(), e);
			}
		} else {
			try {
				this.session = sessionRepository.queryForId(sessionId);
			} catch (SQLException e) {
				new AlertDialog.Builder(getActivity())
						.setTitle(e.getMessage())
						.setMessage(e.toString())
						.create()
						.show();
			}
		}
	}

	private long sessionId;

	private void refreshUI()
	{
		refreshUI(null, this.session);
	}

	private int getIndexForType(String type)
	{
		String[] actionTypes = getResources().getStringArray(R.array.activity_edit_action_types);

		return Arrays.asList(actionTypes).indexOf(type);
	}

	private String getTypeForIndex(int index)
	{
		String[] actionTypes = getResources().getStringArray(R.array.activity_edit_action_types);

		if (index >= actionTypes.length)
			return "?";

		return actionTypes[index];
	}

	List<SSHConnection> connections;

	private void refreshUI(View view, Session session)
	{
		if (view == null)
			view = this.getView();

		EditText nameField = (EditText)view.findViewById(R.id.name);
		nameField.setText(session.getName());

		ListView connectionsList = (ListView)view.findViewById(R.id.connectionsList);
		connections = new ArrayList<SSHConnection>();

		try {
			connections = sshConnectionRepository.queryForAll();
		} catch (SQLException e) {
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
		}

		List<SSHConnection> menuItems = new ArrayList<SSHConnection>(connections);

		SSHConnection newConnection = new SSHConnection();
		newConnection.setId(0);
		newConnection.setSpecialLabel("Add connection... ");
		menuItems.add(newConnection);

		connectionsList.setAdapter(new ArrayAdapter<SSHConnection>(getActivity(),
				android.R.layout.simple_list_item_multiple_choice, android.R.id.text1, menuItems));

		List<SSHConnection> selectedConnections = Arrays.asList(session.getConnections());
		List<Long> selectedConnectionIDs = new ArrayList<Long>();

		for (SSHConnection cnx : selectedConnections) {
			selectedConnectionIDs.add(cnx.getId());
		}

		int size = connections.size();

		for (int i = 0; i < size; ++i) {
			SSHConnection connection = connections.get(i);
			View cnxView = connectionsList.getChildAt(i);
			connectionsList.setItemChecked(i, selectedConnectionIDs.contains(connection.getId()));
		}
	}

	private void syncToObject()
	{
		View view = getView();

		EditText nameField = (EditText)view.findViewById(R.id.name);
        String name = nameField.getText().toString();

        //if ("".equals(name) || name == null)
        //    session.setName("Unnamed session");
        //else
		session.setName(nameField.getText().toString());

		// Connections

		List<SSHConnection> selectedConnections = new ArrayList<SSHConnection>();

		ListView connectionsList = (ListView)view.findViewById(R.id.connectionsList);
		int size = connections.size();

		for (int i = 0; i < size; ++i) {
			SSHConnection connection = connections.get(i);
			View cnxView = connectionsList.getChildAt(i);

			CheckedTextView itemCheckView = (CheckedTextView)cnxView.findViewById(android.R.id.text1);

			if (itemCheckView.isChecked())
				selectedConnections.add(connection);
		}

		session.setConnections(selectedConnections);
	}

	private Session session;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_edit_session, container, false);

		final ListView connectionsList = (ListView)rootView.findViewById(R.id.connectionsList);
		final EditSessionFragment self = this;

		connectionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				SSHConnection option = (SSHConnection)connectionsList.getItemAtPosition(i);

				if (option == null)
					return;

				if (option.getId() == 0) {

					Intent detailIntent = new Intent(self.getActivity(), EditSSHConnectionActivity.class);
					detailIntent.putExtra(Constants.ARG_SESSION_ID, session.getId());

					startActivity(detailIntent);
				}
			}
		});

		registerForContextMenu(connectionsList);
		connectionsList.setOnItemLongClickListener(new ItemLongClickListener(this, connectionsList));

		refreshUI(rootView, this.session);
		return rootView;
	}



	public static final int CONTEXT_MENU_DELETE = 1;
	public static final int CONTEXT_MENU_EDIT = 2;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.setHeaderTitle("Actions");
		menu.add(Menu.NONE, CONTEXT_MENU_EDIT, Menu.NONE, "Edit");
		menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Delete");

		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		if (item.getItemId() == CONTEXT_MENU_EDIT) {
			// TODO

			Intent intent = new Intent(getActivity(), EditSSHConnectionActivity.class);
			intent.putExtra(Constants.ARG_CONNECTION_ID, contextMenuConnection.getId());
			startActivity(intent);

			return true;
		}

		return false;
	}


}
