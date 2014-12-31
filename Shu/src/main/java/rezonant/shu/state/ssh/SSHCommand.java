package rezonant.shu.state.ssh;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
* Created by liam on 7/5/14.
*/
public class SSHCommand {
	public SSHCommand(String commandName, String commandString) {
		this.commandName = commandName;
		this.commandString = commandString;
	}

	private String commandName;
	private String commandString;
	private SSHCommandFinishedListener finishedListener;
	private List<SSHCommandMonitorListener> monitorListeners =
			new ArrayList<SSHCommandMonitorListener>();
	private Date executionStarted;
	private Date executionFinished;
    private long id;
	private SSHConnectionThread thread;

	public String getCommand() {
		return commandString;
	}

	public void setCommand(String command) {
		this.commandString = command;
	}

	public SSHCommandFinishedListener getFinishedListener() {
		return finishedListener;
	}

	public void setFinishedListener(SSHCommandFinishedListener finishedListener) {
		this.finishedListener = finishedListener;
	}

	public String getCommandName() {
		return commandName;
	}

	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}

	public SSHConnectionThread getThread() {
		return thread;
	}

	void setThread(SSHConnectionThread thread) {
		this.thread = thread;
	}

	public void addMonitor(SSHCommandMonitorListener listener)
	{
		monitorListeners.add(listener);
	}

	public void removeMonitor(SSHCommandMonitorListener listener)
	{
		monitorListeners.remove(listener);
	}

	void onData(byte[] buffer, int size)
	{
		for (SSHCommandMonitorListener listener : monitorListeners) {
			listener.onReceiveCommandData(this, buffer, size);
		}
	}

	void onErrorData(byte[] buffer, int size)
	{
		for (SSHCommandMonitorListener listener : monitorListeners) {
			listener.onReceiveCommandErrorData(this, buffer, size);
		}
	}

	void onExited(int exitCode)
	{
		this.exitCode = exitCode;
		this.finished = true;
		this.executionFinished = new Date();

		for (SSHCommandMonitorListener listener : monitorListeners) {
			listener.onCommandExited(this, exitCode);
		}
	}

	public long getRuntime()
	{
		if (this.executionStarted != null && this.executionFinished != null)
			return this.executionFinished.getTime() - this.executionStarted.getTime();

		if (this.executionStarted != null)
			return (new Date()).getTime() - this.executionStarted.getTime();
		return 0;
	}


	void onExecutionStarted()
	{
		this.executionStarted = new Date();
		for (SSHCommandMonitorListener listener : monitorListeners) {
			listener.onCommandStarted(this);
		}
	}

	public Date getExecutionStarted() {
		return executionStarted;
	}

	private String output;
	public String getCurrentOutput()
	{
		return output;
	}

	public void setCurrentOutput(String s) {
		output = s;
	}

	void setExitCode(int exitCode)
	{
		this.exitCode = exitCode;
	}

	public boolean isFinished()
	{
		return finished;
	}

	void setFinished(boolean value)
	{
		finished = value;
	}

	private boolean finished = false;
	private int exitCode = 255;
	public int getExitCode() {
		return exitCode;
	}

	public Date getExecutionFinished() {
		return executionFinished;
	}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
