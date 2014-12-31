package rezonant.shu.ui.session;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.data.Session;
import rezonant.shu.ui.ssh.BaseActionMonitorFragment;
import rezonant.shu.state.ssh.SSHCommand;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;

/**
 * Created by liam on 6/30/14.
 */
public class CommandResultFragment extends BaseActionMonitorFragment {



    /**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public CommandResultFragment() {
	}

	public static final String ARG_RUNTIME = "runtime";
	public static final String ARG_COMMAND_NAME = "command_name";
	public static final String ARG_COMMAND_STRING = "command_string";
	public static final String ARG_EXIT_CODE = "exit_code";
	public static final String ARG_COMMAND_OUTPUT = "command_output";

    private long commandID;
	private SSHCommand command;
    private Dao<SSHConnection, Long> connectionRepository;
    private long connectionID;
    private SSHConnection connection;
    private SSHConnectionThread thread;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		getActivity().getMenuInflater().inflate(R.menu.command_result, menu);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

        TextView output = (TextView)this.getView().findViewById(R.id.output);

        Log.i("FYI", "Options item selected with ID "+item.getItemId());

		switch (item.getItemId()) {

            case R.id.action_increase_font:
                if (output.getTextSize() < 100) {
                    Log.i("FYI", "Increasing font size");
                    output.setTextSize(TypedValue.COMPLEX_UNIT_PX, output.getTextSize() + 5);
                }
                break;
            case R.id.action_decrease_font:
                if (output.getTextSize() > 1) {
                    Log.i("FYI", "Decreasing font size");
                    output.setTextSize(TypedValue.COMPLEX_UNIT_PX, output.getTextSize() - 5);
                }
                break;
            case R.id.action_toggle_word_wrap:
                item.setChecked(!item.isChecked());
                TextView outputView = (TextView)getView().findViewById(R.id.output);

                if (item.isChecked()) {
                    // TODO set word wrap here
                } else {
                    // TODO disable word wrap here

                }
                break;
            case R.id.action_toggle_follow:
                item.setChecked(!item.isChecked());
                mFollowOutput = item.isChecked();
                break;
            case R.id.action_find:
                // TODO
                break;
		}

		return super.onOptionsItemSelected(item);
	}

    private boolean mFollowOutput = true;

	@Override
	public void onReceiveCommandData(SSHCommand command, byte[] buffer, int size) {
		String data = new String(buffer, 0, size);
		TextView outputView = (TextView)getView().findViewById(R.id.output);
		outputView.setText(outputView.getText().toString() + data);

        if (mFollowOutput) {
            ScrollView view = (ScrollView) getView().findViewById(R.id.scrollView);
            view.fullScroll(ScrollView.FOCUS_DOWN);
        }

		super.onReceiveCommandData(command, buffer, size);
	}

	@Override
	public void onReceiveCommandErrorData(SSHCommand command, byte[] buffer, int size) {
		String data = new String(buffer, 0, size);
		TextView outputView = (TextView)getView().findViewById(R.id.output);
		outputView.setText(outputView.getText().toString() + data);

        if (mFollowOutput) {
            ScrollView view = (ScrollView) getView().findViewById(R.id.scrollView);
            view.fullScroll(ScrollView.FOCUS_DOWN);
        }

		super.onReceiveCommandErrorData(command, buffer, size);
	}

	Date executionStarted;

	@Override
	public void onCommandStarted(SSHCommand command) {
		executionStarted = command.getExecutionStarted();
	}

	@Override
	public void onCommandExited(SSHCommand command, int exitCode) {
		//Toast.makeText(getActivity(), "Command has exited with code "+exitCode, 4000);
		TextView exitCodeLabel = (TextView)getView().findViewById(R.id.exitCode);
		exitCodeLabel.setText(exitCode+"");

		setRuntime(command.getRuntime());

		if (command.isFinished()) {
			setStatus("Finished at "+command.getExecutionFinished());
		} else {
			setStatus("Currently running");
		}

		super.onCommandExited(command, exitCode);
	}

	void setStatus(String statusString)
	{
		if (rootView != null) {
			TextView statusLabel = (TextView)rootView.findViewById(R.id.status);
			statusLabel.setText(statusString);
		}
	}

	void setRuntime(long ticks)
	{
		if (rootView == null)
			return;
		double seconds = ticks / 1000.0;

		String runtime = seconds+" seconds";

		if (seconds > 60*60)
			runtime = (Math.round(seconds/60.0/60.0*10)/10.0)+" hours";
		else if (seconds > 60)
			runtime = (Math.round(seconds/60.0*10)/10.0)+" minutes";

		TextView runtimeLabel = (TextView)rootView.findViewById(R.id.runtime);
		runtimeLabel.setText(runtime);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Log.i("FYI", "Initializing fragment!");

        // Initialize repositories
        connectionRepository = SSHConnection.createDao(getActivity());

        // Initialize fragment options
        this.setHasOptionsMenu(true);

        // Process arguments
		this.sessionID = getArguments().getLong(Constants.ARG_SESSION_ID);
		this.sessionRepository = Session.createDao(getActivity());

        this.connectionID = getArguments().getLong(Constants.ARG_CONNECTION_ID);

        Log.i("FYI", "Connection ID for result fragment: "+this.connectionID);

        try {
            this.connection = connectionRepository.queryForId(this.connectionID);
        } catch (SQLException e) {
            Log.e("WTF", e.getMessage(), e);
        }

        if (this.connection != null) {
            this.thread = SSHConnectionManager.instance().getConnection(getActivity(), this.connection);
            if (this.thread != null) {
                this.commandID = getArguments().getLong(Constants.ARG_COMMAND_ID);
                this.command = this.thread.getCommandById(this.commandID);
                this.executionStarted = command.getExecutionStarted();
                this.addMonitoredCommand(command);
            } else {
                Log.i("FYI", "Failed to locate thread for SSH connection "+this.connectionID);
            }
        } else {
            Log.i("FYI", "Failed to locate SSH connection "+this.connectionID);
        }

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

	View rootView;
    Timer updateTimer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.fragment_command_result, container, false);


		if (command.isFinished()) {
			setStatus("Finished at "+command.getExecutionFinished());
		} else {
			setStatus("Currently running");
		}

		setRuntime(command.getRuntime());

		String commandName = command.getCommandName();
		String commandString = command.getCommand();
		String output = command.getCurrentOutput();
		int exitCode = command.getExitCode();

		TextView runtimeLabel = (TextView)rootView.findViewById(R.id.runtime);
		TextView commandNameLabel = (TextView)rootView.findViewById(R.id.commandName);
		TextView commandStringLabel = (TextView)rootView.findViewById(R.id.commandString);
		TextView outputLabel = (TextView)rootView.findViewById(R.id.output);
		TextView exitCodeLabel = (TextView)rootView.findViewById(R.id.exitCode);

		commandNameLabel.setText(commandName);
		commandStringLabel.setText(commandString);
		outputLabel.setText(output);
		exitCodeLabel.setText(""+exitCode);

        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }

        if (!command.isFinished()) {
            updateTimer = new Timer();
            updateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setRuntime(command.getRuntime());
                            if (command.isFinished()) {
                                updateTimer.cancel();
                                updateTimer = null;
                            }
                        }
                    });
                }
            }, 250, 250);
        }

		return rootView;
	}

    @Override
    public void onPause() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
        super.onPause();
    }

    private long sessionID;
	private Dao<Session,Long> sessionRepository;
	private Session session;


	public SSHCommand getCommand() {
		return command;
	}
}
