package rezonant.shu.state.ssh;

/**
* Created by liam on 7/5/14.
*/
public abstract class SSHCommandFinishedListener {
	public abstract void commandCompleted(SSHCommandResult result);
}
