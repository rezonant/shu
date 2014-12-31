package rezonant.shu.state;

import rezonant.shu.state.data.SessionAction;
import rezonant.shu.state.ssh.SSHCommand;
import rezonant.shu.state.ssh.SSHConnectionThread;

/**
 * Created by liam on 7/5/14.
 */
public class ActiveTask {
	public ActiveTask(SessionAction sourceAction, SSHCommand command)
	{
		this.sourceAction = sourceAction;
	}

	private SessionAction sourceAction;
	private SSHCommand command;

	public SessionAction getSourceAction() {
		return sourceAction;
	}

	public void setSourceAction(SessionAction sourceAction) {
		this.sourceAction = sourceAction;
	}

	public SSHConnectionThread getThread() {
		return this.command.getThread();
	}

	public SSHCommand getCommand() {
		return command;
	}

	public void setCommand(SSHCommand command) {
		this.command = command;
	}
}
