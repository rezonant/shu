package rezonant.shu.ui.data;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Arrays;

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
public class EditSSHConnectionFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_CONNECTION_ID = "connection_id";

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EditSSHConnectionFragment() {
	}

	Dao<SSHConnection,Long> sshConnectionRepository;

	public void save()
	{
		try {
			if (sshConnection.getId() == 0) {
				sshConnectionRepository.create(sshConnection);
				sshConnectionRepository.refresh(sshConnection);
			} else
				sshConnectionRepository.update(sshConnection);

			if (this.addToSessionId > 0) {
				Dao<Session,Long> sessionRepository = Session.createDao(this.getActivity());
				Session session = sessionRepository.queryForId(this.addToSessionId);

				if (session != null) {
					session.addConnection(sshConnection);
					sessionRepository.update(session);
				}
			}

		} catch (SQLException e) {
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
			return;
		}

		Log.i("Ok", "Saved action "+sshConnection.getId()+" successfully");

		getActivity().finish();
	}

	private class SelectCommandButtonListener implements View.OnClickListener {
		public SelectCommandButtonListener(EditSSHConnectionFragment fragment) {
			this.fragment = fragment;
		}

		private EditSSHConnectionFragment fragment;

		@Override
		public void onClick(View view) {
			fragment.onClickSelectCommand();
		}
	}

	private class SaveButtonListener implements View.OnClickListener {
		public SaveButtonListener(EditSSHConnectionFragment fragment) {
			this.fragment = fragment;
		}

		private EditSSHConnectionFragment fragment;

		@Override
		public void onClick(View view) {
			fragment.onClickSave();
		}
	}

	@Override
	public void onPause()
	{
		onClickSave();
		super.onPause();
	}

	private void onClickSelectCommand()
	{
		Log.i("Ok", "We are at da edit command shit");
		new AlertDialog.Builder(getActivity())
				.setTitle("Oh noes!")
				.setMessage("Whoot")
				.create()
				.show();
	}

	private void onClickSave()
	{
		syncToObject();
		save();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sshConnectionRepository = SSHConnection.createDao(this.getActivity());

		this.connectionId = 0;
		this.addToSessionId = 0;

		if (getArguments().containsKey(Constants.ARG_CONNECTION_ID))
			connectionId = getArguments().getLong(Constants.ARG_CONNECTION_ID);

		if (getArguments().containsKey(Constants.ARG_SESSION_ID))
			addToSessionId = getArguments().getLong(Constants.ARG_SESSION_ID);

		if (connectionId == 0) {
			this.sshConnection = new SSHConnection();

		} else {
			try {
				this.sshConnection = sshConnectionRepository.queryForId(connectionId);
			} catch (SQLException e) {
				new AlertDialog.Builder(getActivity())
						.setTitle(e.getMessage())
						.setMessage(e.toString())
						.create()
						.show();
			}
		}
	}

	private long connectionId = 0;
	private long addToSessionId = 0;

	private void refreshUI()
	{
		refreshUI(null, this.sshConnection);
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

	private void refreshUI(View view, SSHConnection sshConnection)
	{
		if (view == null)
			view = this.getView();

		EditText userField = (EditText)view.findViewById(R.id.user);
		EditText hostField = (EditText)view.findViewById(R.id.host);
		EditText portField = (EditText)view.findViewById(R.id.port);
		EditText passwordField = (EditText)view.findViewById(R.id.password);
		CheckBox rememberPasswordCheckbox = (CheckBox)view.findViewById(R.id.savePassword);

		portField.setText(sshConnection.getPort()+"");
		userField.setText(sshConnection.getUser());
		hostField.setText(sshConnection.getHost());
		passwordField.setText(sshConnection.getPassword());
		rememberPasswordCheckbox.setChecked("".equals(sshConnection.getPassword()));
	}

	private void syncToObject()
	{
		View view = getView();

		EditText userField = (EditText)view.findViewById(R.id.user);
		EditText hostField = (EditText)view.findViewById(R.id.host);
		EditText portField = (EditText)view.findViewById(R.id.port);
		EditText passwordField = (EditText)view.findViewById(R.id.password);
		CheckBox rememberPasswordCheckbox = (CheckBox)view.findViewById(R.id.savePassword);

		sshConnection.setHost(hostField.getText().toString());
		sshConnection.setUser(userField.getText().toString());
		sshConnection.setPassword(passwordField.getText().toString());

		int port = 22;

		try {
			port = Integer.parseInt(portField.getText().toString());
		} catch (NumberFormatException e) {
			port = 22;
		}

		sshConnection.setPort(port);

		//rememberPasswordCheckbox.setChecked("".equals(sshConnection.getPassword()));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		inflater.inflate(R.menu.edit_sshconnection, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private SSHConnection sshConnection;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_edit_ssh_connection, container, false);

		refreshUI(rootView, this.sshConnection);
		return rootView;
	}
}
