package rezonant.shu.ui.ssh;

import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import rezonant.shu.state.ssh.SSHCommand;
import rezonant.shu.state.ssh.SSHCommandMonitorListener;

/**
 * Created by liam on 7/5/14.
 */
public class BaseActionMonitorFragment extends Fragment implements SSHCommandMonitorListener {

	private List<SSHCommand> monitoredCommands = new ArrayList<SSHCommand>();

	protected void addMonitoredCommand(SSHCommand command)
	{
		if (isActive)
			command.addMonitor(this);
		monitoredCommands.add(command);
	}

	protected void clearMonitoredCommands()
	{
		for (SSHCommand command : monitoredCommands)
			command.removeMonitor(this);
		monitoredCommands.clear();
	}

	protected void removeMonitoredCommand(SSHCommand command)
	{
		monitoredCommands.remove(command);
		command.removeMonitor(this);
	}

	private boolean isActive = false;
	@Override
	public void onResume() {
		for (SSHCommand command : monitoredCommands)
			command.addMonitor(this);
		isActive = true;
		super.onResume();
	}

	@Override
	public void onStop() {
		for (SSHCommand command : monitoredCommands)
			command.removeMonitor(this);
		isActive = false;
		super.onStop();
	}

	@Override
	public void onReceiveCommandData(SSHCommand command, byte[] buffer, int size) {

	}

	@Override
	public void onReceiveCommandErrorData(SSHCommand command, byte[] buffer, int size) {

	}

	@Override
	public void onCommandExited(SSHCommand command, int exitCode) {

	}

	@Override
	public void onCommandStarted(SSHCommand command) {

	}
}
