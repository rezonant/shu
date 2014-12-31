package rezonant.shu.state;

import java.util.Date;

import rezonant.shu.state.data.Action;
import rezonant.shu.state.data.Session;
import rezonant.shu.state.data.SessionAction;
import rezonant.shu.state.ssh.SSHCommand;

/**
 * Created by liam on 7/20/14.
 */
public class ActionExecutionRecord {
    public ActionExecutionRecord(SessionAction sessionAction)
    {
        this.sessionAction = sessionAction;
        this.session = sessionAction.getSession();
        this.action = sessionAction.getAction();
        this.executionTime = new Date();
    }

    private Date executionTime;
    private long id;
    private Session session;
    private Action action;
    private SessionAction sessionAction;
    private SSHCommand sshCommand;

    public String getActionType()
    {
        return this.action.getType();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getExecutionTime() {
        return executionTime;
    }

    public Session getSession() {
        return session;
    }

    public Action getAction() {
        return action;
    }

    public SessionAction getSessionAction() {
        return sessionAction;
    }

    public SSHCommand getSSHCommand() {
        return sshCommand;
    }

    public void setSSHCommand(SSHCommand sshCommand) {
        this.sshCommand = sshCommand;
    }
}
