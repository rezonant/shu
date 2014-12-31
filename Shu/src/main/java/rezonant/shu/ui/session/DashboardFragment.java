package rezonant.shu.ui.session;

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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.data.Session;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;
import rezonant.shu.ui.NavigatedFragment;

/**
 * Created by liam on 6/30/14.
 */
public class DashboardFragment extends NavigatedFragment {

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public DashboardFragment() {

	}

	@Override
	public void onStop() {
		Log.i("FYI", "=========== Dashboard called onStop successfully. ===============");
		super.onStop();
	}

    @Override
    public String getTitle() {
        if (this.session != null)
            return this.session.toString();

        return "Dashboard";
    }

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		getActivity().getMenuInflater().inflate(R.menu.dashboard, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_run:
//				Toast.makeText(getActivity(), "Start action.", Toast.LENGTH_SHORT).show();
//				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setHasOptionsMenu(true);

		this.sessionID = getArguments().getLong(Constants.ARG_SESSION_ID);
		this.sessionRepository = Session.createDao(getActivity());

		try {
			this.session = this.sessionRepository.queryForId(this.sessionID);
		} catch (SQLException e) {
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        TableLayout connectionsList = (TableLayout)rootView.findViewById(R.id.connectionsList);
        connectionsList.removeAllViews();

        for (SSHConnection connection : this.session.getConnections()) {
            boolean active = false;
            Date date = new Date();

            if (SSHConnectionManager.instance().isActive(connection)) {
                SSHConnectionThread thread = SSHConnectionManager.instance().getConnection(getActivity(), connection);
                date = thread.getStartTime();
                active = true;
            }


            TableRow row = new TableRow(getActivity());
            TextView label = new TextView(getActivity());
            TextView value = new TextView(getActivity());

            label.setText(connection.toString()+": ");
            value.setText(active? "Active since "+new SimpleDateFormat("h:mm a").format(date) : "Inactive");

            row.addView(label);
            row.addView(value);

            connectionsList.addView(row);
        }

		return rootView;
	}

	private long sessionID;
	private Dao<Session,Long> sessionRepository;
	private Session session;


}
