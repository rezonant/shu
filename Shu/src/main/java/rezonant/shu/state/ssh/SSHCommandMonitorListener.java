package rezonant.shu.state.ssh;

/**
* Created by liam on 7/5/14.
*/
public interface SSHCommandMonitorListener {
	public void onReceiveCommandData(SSHCommand command, byte[] buffer, int size);
	public void onReceiveCommandErrorData(SSHCommand command, byte[] buffer, int size);
	public void onCommandExited(SSHCommand command, int exitCode);
	public void onCommandStarted(SSHCommand command);
}
