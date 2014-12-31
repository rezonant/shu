package rezonant.shu.state.ssh;

/**
* Created by liam on 7/5/14.
*/
public class SSHCommandResult {
	public SSHCommandResult(SSHConnectionThread thread, SSHCommand command) {
		this.thread = thread;
		this.command = command;
	}

	private SSHConnectionThread thread;
	private SSHCommand command;
	private String output;
	private int exitCode;
    private String shuError;
    private Object requestedResource;

    public void setShuError(String error)
    {
        shuError = error;
    }

    public boolean isShuError()
    {
        return shuError != null;
    }


    public String getShuError() { return shuError; }
	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	public SSHCommand getCommand() {
		return command;
	}

	public void setCommand(SSHCommand command) {
		this.command = command;
	}

	public SSHConnectionThread getThread() {
		return thread;
	}

    public Object getRequestedResource() {
        return requestedResource;
    }

    public void setRequestedResource(Object requestedResource) {
        this.requestedResource = requestedResource;
    }
}
