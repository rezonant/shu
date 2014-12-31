package rezonant.shu.ui.session;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import com.j256.ormlite.dao.Dao;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rezonant.httprelay.HttpRelayServer;
import rezonant.shu.state.http.SftpStreamable;
import rezonant.shu.state.http.SftpUrlManager;
import rezonant.shu.ui.ActiveTaskListFragment;
import rezonant.shu.ui.NavigableFragment;
import rezonant.shu.ui.data.ActionExecutionRecordListFragment;
import rezonant.shu.Constants;
import rezonant.shu.ui.data.EditSessionActivity;
import rezonant.shu.ui.viewers.AudioListenerFragment;
import rezonant.shu.ui.viewers.FilesystemDirectoryFragment;
import rezonant.shu.ui.viewers.ImageViewerFragment;
import rezonant.shu.ui.data.MonitorsListFragment;
import rezonant.shu.ui.NavigationController;
import rezonant.shu.R;
import rezonant.shu.ui.viewers.TextFileViewerFragment;
import rezonant.shu.ui.viewers.WebcamViewerFragment;
import rezonant.shu.state.data.Action;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.data.Session;
import rezonant.shu.state.data.SessionAction;
import rezonant.shu.ui.global.SettingsActivity;
import rezonant.shu.ui.listview.ListItem;
import rezonant.shu.state.ssh.SSHCommand;
import rezonant.shu.state.ssh.SSHCommandFinishedListener;
import rezonant.shu.state.ssh.SSHCommandResult;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;
import rezonant.shu.state.ActionExecutionRecord;
import rezonant.shu.state.SessionState;
import rezonant.shu.state.SessionStateManager;

public class SessionViewActivity
		extends
			FragmentActivity
        implements
        NavigationController,
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        SessionActionsListFragment.Delegate,
        ExecuteActionFragment.Delegate,
        FilesystemDirectoryFragment.Delegate
{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_session_view);

		this.sessionID = getIntent().getLongExtra(Constants.ARG_SESSION_ID, 0);

		Log.i("FYI", "Session View got a session ID of "+this.sessionID);

		sessionRepository = Session.createDao(this);

		try {
			session = sessionRepository.queryForId(this.sessionID);
            if (session == null)
                throw new SQLException("Failed to create session object?");
		} catch (SQLException e) {
            Log.e("WTF", e.getMessage(), e);
			new AlertDialog.Builder(this)
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
		}

        Log.i("FYI", "Got a good session with ID of "+session.getId());

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        if (session != null)
            setTitle(session.getName());

        mTitle = getTitle();

        Log.i("FYI", "Passing session to nav drawer...");

        mNavigationDrawerFragment.setSession(this.session);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        if (savedInstanceState == null) {
            setFragment("dashboard", new DashboardFragment());
        }
    }

	private long sessionID;
	private Session session;
	private Dao<Session,Long> sessionRepository;

    @Override
    public void onNavigationDrawerItemSelected(ListItem item) {
		Fragment fragment = null;
		String fragmentName = null;
		String position = item.getId();

		if (item instanceof NavigationDrawerFragment.RunningActionListItem) {
			NavigationDrawerFragment.RunningActionListItem runningAction =
					(NavigationDrawerFragment.RunningActionListItem) item;

			Fragment newFragment = new CommandResultFragment();
            Bundle args = new Bundle();
            args.putLong(Constants.ARG_CONNECTION_ID, runningAction.getCommand().getThread().getId());
            args.putLong(Constants.ARG_COMMAND_ID, runningAction.getCommand().getId());
			setFragment("execute-"+runningAction.getCommand().getId(), newFragment, args);
			return;
		}

		if (position == "dashboard") {
			// Dashboard
			fragment = new DashboardFragment();
			mTitle = getString(R.string.section_dashboard);
			fragmentName = "dashboard";
		} else if (position == "actions") {
            // Actions
            fragment = new SessionActionsListFragment();
            mTitle = getString(R.string.section_start_action);
            fragmentName = "actions";
        } else if (position == "webcams") {
            // TODO
        } else if (position == "filesystem") {
            // Do it.

            final SSHConnection[] connections = session.getConnections();
            String[] connectionStrings = new String[connections.length];
            int i = 0;
            for (SSHConnection cnx : connections) {
                connectionStrings[i++] = cnx.toString();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Choose connection to browse:")
                    .setItems(connectionStrings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SSHConnection connection = connections[which];

                            setFragment("fs", FilesystemDirectoryFragment.newInstance(connection, "/"));
                            ActionBar actionBar = getActionBar();
                            Log.i("FYI", "Changing title due to setFragment");
                            actionBar.setTitle(mTitle);
                            setTitle(mTitle);
                        }
                    })
                    .create()
                    .show();

        } else if (position == "running") {
            fragment = new ActionExecutionRecordListFragment();
            mTitle = "Active Tasks";
            fragmentName = "history";

            Bundle args = new Bundle();
            args.putString("mode", "active");

            fragment.setArguments(args);
        } else if (position == "history") {
            fragment = new ActionExecutionRecordListFragment();
            mTitle = "History";
            fragmentName = "history";
		} else if (position == "monitors") {
			// Monitors
			fragment = new MonitorsListFragment();
			mTitle = getString(R.string.section_monitors);
			fragmentName = "monitors";
		} else if (position == "quick-actions") {
			// Quick Actions
			//fragment = new QuickActionsListFragment(); // or something
		}

		if (fragment == null)
			return;

		setFragment(fragmentName, fragment);
		ActionBar actionBar = getActionBar();
        Log.i("FYI", "Changing title due to setFragment");
		actionBar.setTitle(mTitle);
		this.setTitle(mTitle);
    }

	private boolean firstFragment = true;
	private Fragment activeFragment = null;
	private String activeFragmentName;

	private void setFragment(String fragmentName, Fragment fragment)
	{
		Bundle args = new Bundle();
        if (fragment.getArguments() != null)
            args = fragment.getArguments();

		setFragment(fragmentName, fragment, args);
	}

	public void setFragment(String fragmentName, Fragment fragment, Bundle args)
	{
        // Fuck this
		//if (fragmentName != null && activeFragmentName != null && activeFragmentName.equals(fragmentName))
		//	return;

		activeFragmentName = fragmentName;

        args.putLong(Constants.ARG_SESSION_ID, this.sessionID);
		fragment.setArguments(args);

		// update the main content by replacing fragments
		FragmentTransaction txn = getSupportFragmentManager().beginTransaction();

		//if (activeFragment != null)
		//	txn.detach(activeFragment);

		txn.replace(R.id.container, fragment);

		if (!firstFragment)
			txn.addToBackStack(fragmentName);

		txn.commit();

		activeFragment = fragment;
        if (fragment instanceof NavigableFragment) {
            String fragTitle = ((NavigableFragment)fragment).getTitle();
            Log.i("FYI", "setFragment(): Fragment is NavigableFragment, setting title to: "+fragTitle);
            setTitle(fragTitle);
        }

		firstFragment = false;
	}

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);

        Log.i("FYI", "Restore action bar, set title to: "+mTitle);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mNavigationDrawerFragment == null) {
            Log.w("FYI", "Nav drawer fragment was null. This probably won't end well.");
            return super.onCreateOptionsMenu(menu);
        }

        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.session_view, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_back_to_sessions:
                this.finish();
                return true;
            case R.id.action_disconnect:
                SSHConnection[] connections = this.session.getConnections();

                Log.i("FYI", "Disconnecting all connections...");
                for (SSHConnection cnx : connections) {
                    if (SSHConnectionManager.instance().isActive(cnx)) {
                        Log.i("FYI", "Disconnecting "+cnx.toString()+"...");

                        SSHConnectionThread thread =
                                SSHConnectionManager.instance().getConnection(this, cnx);

                        thread.disconnect();

                        Log.i("FYI", "Disconnect for "+cnx.toString()+" finished.");
                    }
                }

                Log.i("FYI", "Disconnect: Finishing this activity...");
                finish();

                return true;

            case R.id.action_run:

                Fragment fragment = new SessionActionsListFragment();
                mTitle = getString(R.string.section_start_action);

                setFragment("actions", fragment);

                return true;

        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void sessionActionSelected(SessionAction action) {

        if (action.getId() == 0) {

        }

		Bundle args = new Bundle();

        if (session.getConnections().length == 0) {
            // Can't run anything without connections
            // Give the user an opportunity, otherwise do nothing.

            new AlertDialog.Builder(this)
                    .setTitle("Need a connection")
                    .setMessage("This session does not have any connections yet. Tap Edit session below to create or add your connections.")
                    .setPositiveButton("Edit session", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Intent intent = new Intent(SessionViewActivity.this, EditSessionActivity.class);
                            intent.putExtra(Constants.ARG_SESSION_ID, sessionID);

                            startActivity(intent);

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .create().show();
            return;
        }

		Log.i("FYI", "Executing action with action ID "+action.getAction().getId());

		Fragment fragment = new ExecuteActionFragment();

		args.putLong(Constants.ARG_SESSION_ACTION_ID, action.getId());
		args.putLong(Constants.ARG_SESSION_ID, this.sessionID);
		args.putLong(Constants.ARG_ACTION_ID, action.getAction().getId());
		setFragment("start-execute-"+action.getId(), fragment, args);

		/*
		Intent intent = new Intent(this, ExecuteActionActivity.class);
		intent.putExtra(Constants.ARG_SESSION_ACTION_ID, action.getId());
		intent.putExtra(Constants.ARG_SESSION_ID, this.sessionID);
		intent.putExtra(Constants.ARG_ACTION_ID, action.getAction().getId());
		startActivity(intent);
		*/

	}

	private String replaceVariables(String text, HashMap<Integer,String> replacements)
	{
		Pattern pattern = Pattern.compile("\\$(\\d+)");
		Matcher matcher = pattern.matcher(text);

		StringBuilder builder = new StringBuilder();
		int i = 0;
		while (matcher.find()) {
			int positional;
			try {
				positional = Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException e) {
				positional = 1;
			}

			String replacement = replacements.get(positional);
			builder.append(text.substring(i, matcher.start()));
			if (replacement == null)
				builder.append(matcher.group(0));
			else
				builder.append(replacement);
			i = matcher.end();
		}
		builder.append(text.substring(i, text.length()));
		return builder.toString();
	}

    /**
     * Called when an action is executed from the user interface.
     *
     * @param sessionAction         The session-action being executed
     * @param variables             The variables input by the user
     * @param chosenConnections     The connections chosen by the user
     *
     */
	@Override
	public void actionExecuted(final SessionAction sessionAction, HashMap<Integer, String> variables, List<SSHConnection> chosenConnections) {

        // Get the threads associated with the selected connections.

		final List<SSHConnectionThread> connections = new ArrayList<SSHConnectionThread>();
		for (int i = 0, max = chosenConnections.size(); i < max; ++i) {
			SSHConnection connection = chosenConnections.get(i);

			connections.add(SSHConnectionManager.instance().getConnection(this, connection));
		}

        // Get the action's command definition and apply the
        // user-selected variables to produce a final command string

        Action action = sessionAction.getAction();
		String commandString = action.getCommand();
		commandString = replaceVariables(commandString, variables);

        Log.i("FYI", "Finalized command string: "+commandString);
        Log.i("FYI", "Action type: "+action.getType());
		Log.i("FYI", "Found "+connections.size()+" selected SSH connections");

        if ("ssh".equalsIgnoreCase(action.getType())) {
            // Apply the command to the chosen connections

            for (SSHConnectionThread thread : connections) {
                SSHCommand command = new SSHCommand(sessionAction.getAction().getName(), commandString);
                Fragment fragment = new CommandResultFragment();

                // Record the action execution so that the UI sees it

                final SessionState state = SessionStateManager.instance().getSessionState(this.session);
                final ActionExecutionRecord record = new ActionExecutionRecord(sessionAction);
                record.setSSHCommand(command);
                state.registerAction(record);
                state.nowExecuting(record);

                Log.i("FYI", "STARTED executing record "+record.getId());

                // Actually execute the command

                Log.i("FYI", "Queuing command on connection " + thread);
                thread.queueCommand(command, new SSHCommandFinishedListener() {
                    @Override
                    public void commandCompleted(SSHCommandResult result) {

                        // Handle application errors, which are "hacked" here as special results

                        if (result.isShuError()) {
                            new AlertDialog.Builder(SessionViewActivity.this)
                                    .setTitle("Failed to execute command")
                                    .setMessage(result.getShuError())
                                    .create()
                                    .show();
                        }

                        // Log some things

                        Log.i("FYI", "==============================================================================");
                        Log.i("FYI", "An SSH command has completed, and results have been returned to the UI thread!");
                        Log.i("FYI", "Command run : " + result.getCommand());
                        Log.i("FYI", "Exit code: " + result.getExitCode());
                        Log.i("FYI", "Full output: " + result.getOutput());

                        // Notify the UI state that this execution is completed.

                        Log.i("FYI", "FINISHED executing record "+record.getId());
                        state.finishedExecuting(record);

                        SessionViewActivity.this.mNavigationDrawerFragment.refreshMenu();

                        if (activeFragment != null) {
                            Log.i("FYI", "Currently running fragment: "+activeFragment.getClass().getCanonicalName());
                        }

                        if (activeFragment != null && activeFragment instanceof ActionExecutionRecordListFragment) {
                            Log.i("FYI", "Refreshing active task list fragment");
                            ((ActionExecutionRecordListFragment)activeFragment).refresh();
                        }

                        /*
                        Bundle args = new Bundle();
                        args.putLong(Constants.ARG_SESSION_ID, sessionAction.getSession().getId());
                        args.putLong(Constants.ARG_SESSION_ACTION_ID, sessionAction.getId());
                        args.putString(CommandResultFragment.ARG_COMMAND_NAME, result.getCommand().getCommandName());
                        args.putString(CommandResultFragment.ARG_COMMAND_STRING, result.getCommand().getCommand());
                        args.putString(CommandResultFragment.ARG_COMMAND_OUTPUT, result.getOutput());
                        args.putInt(CommandResultFragment.ARG_EXIT_CODE, result.getExitCode());
                        args.putString(CommandResultFragment.ARG_RUNTIME, "00:11:22");
                        */

                    }
                });

                String time = (new SimpleDateFormat("h:mm a")).format(new Date());
                String navName = String.format("[%s] %s / %s",
                        time,
                        command.getCommandName(),
                        command.getThread());

                Bundle args = new Bundle();
                args.putLong(Constants.ARG_CONNECTION_ID, thread.getId());
                args.putLong(Constants.ARG_COMMAND_ID, command.getId());

                setFragment("execute-"+command.getId(), fragment, args);



                Log.i("FYI", "REFRESHING menu to show newly executed record "+record.getId());
                SessionViewActivity.this.mNavigationDrawerFragment.refreshMenu();
                //SessionViewActivity.this.mNavigationDrawerFragment.addRunningAction(navName, command);
            }
        } else if ("webcam+sftp".equalsIgnoreCase(action.getType())) {
            // Executes a script at ~/.shu/start-webcam if possible, then
            // shows the image at ~/.webcam.jpg every 1 second over SFTP
            // ~/.webcam.jpg should probably be a symbolic link to the real location.

            // Run the start command on all connections and wait for them all to finish.

            Log.i("FYI", "** Preparing to show webcams.");

            class FinishedListener {
                public void finished() {
                    // All start commands are done processing, initialize the user interface
                    Log.i("FYI", " ** Webcam start commands have all finished executing.");
                    Log.i("FYI", " ** Creating Webcam viewer fragment and showing it...");

                    SSHConnectionThread[] threadsArray = new SSHConnectionThread[connections.size()];
                    connections.toArray(threadsArray);
                    WebcamViewerFragment fragment = WebcamViewerFragment.newInstance(threadsArray);
                    setFragment(null, fragment);
                }

                public int remaining;
                public void decrement()
                {
                    this.remaining -= 1;
                    if (this.remaining <= 0) {
                        finished();
                    }
                }
            }

            final FinishedListener finished = new FinishedListener();
            finished.remaining = connections.size();

            Log.i("FYI", "** Queuing startup commands...");

            for (SSHConnectionThread thread : connections) {
                SSHCommand command = new SSHCommand(sessionAction.getAction().getName(),
                        commandString);
                final SSHConnectionThread threadSelf = thread;

                Log.i("FYI", "[Webcam+SFTP] Queuing command on connection " + thread);
                thread.queueCommand(command, new SSHCommandFinishedListener() {
                    @Override
                    public void commandCompleted(SSHCommandResult result) {
                        if (result.isShuError()) {
                            new AlertDialog.Builder(SessionViewActivity.this)
                                    .setTitle("Failed to start webcam on "+threadSelf)
                                    .setMessage("Failed to run webcam start command: "+result.getShuError())
                                    .create()
                                    .show();
                            return;
                        }

                        Log.i("FYI", "** Webcam init command finished for "+threadSelf+" (ec "+result.getExitCode()+")");

                        synchronized (finished) {
                            finished.decrement();
                        }
                    }
                });

            }

            Log.i("FYI", "** Waiting for startup commands to complete...");

            // TODO: Show a spinner here...
        } else {
            Log.e("WTF", "Unsupported action type '"+action.getType()+"'");
        }
	}

    @Override
    public void onOpenDirectory(FilesystemDirectoryFragment.FilesystemEntry entry) {
        setFragment("fs", FilesystemDirectoryFragment.newInstance(entry));
        mTitle = entry.fullPath;
    }

    @Override
    public void onOpenFile(final FilesystemDirectoryFragment.FilesystemEntry entry) {

        final SSHConnectionThread thread = SSHConnectionManager.instance().getConnection(this, entry.connection);

        if (thread != null) {
            SSHCommand command = new SSHCommand("Get File Type", "file -i '"+entry.fullPath.replaceAll("'", "'\"'\"'")+"' | cut -d':' -f2- | cut -d';' -f1");
            thread.queueCommand(command, new SSHCommandFinishedListener() {
                @Override
                public void commandCompleted(SSHCommandResult result) {

                    // TODO Are we on the UI thread here? (see below)

                    // Running on SSH thread

                    final String mimetype = result.getOutput().trim();

                    if (mimetype.indexOf("text/") == 0) {

                        // Throw it up on the text viewer

                        // TODO I think we are already on the UI thread...
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Fragment fragment = TextFileViewerFragment.newInstance(entry);
                                setFragment("text-viewer", fragment);
                            }
                        });
                    } else if (mimetype.indexOf("regular file, no read permission") == 0) {

                        // No permission

                        // TODO I think we are already on the UI thread...
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(SessionViewActivity.this)
                                        .setTitle("Access Denied")
                                        .setMessage("The file '" + entry.fullPath + "' is inaccessible.")
                                        .create()
                                        .show();
                            }
                        });
                    } else if (mimetype.indexOf("image/") == 0) {

                        // Throw it up on the image viewer

                        // TODO I think we are already on the UI thread...
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Fragment fragment = ImageViewerFragment.newInstance(entry);
                                setFragment("image-viewer", fragment);
                            }
                        });
                    } else if (mimetype.indexOf("audio/") == 0) {

                        // Throw it up on the audio viewer
                        HttpRelayServer relayServer = null;

                        try {
                            relayServer = HttpRelayServer.instance();
                        } catch (IOException e) {
                            Log.e("WTF", "Received IOException while starting HTTP relay server: "+e.getMessage(), e);
                        }

                        if (relayServer != null) {


                            String sftpUrl = "sftp://"+thread.toString()+entry.fullPath;


                            Log.i("FYI", "Looking up existing HTTP URL for "+sftpUrl);

                            String httpUrl = SftpUrlManager.instance().getHttpUrl(sftpUrl);

                            if (httpUrl == null) {
                                Log.i("FYI", "Registering SFTP streamable for "+sftpUrl);
                                httpUrl = relayServer.serveStream(new SftpStreamable(thread, entry.fullPath, mimetype));
                                Log.i("FYI", "Registered HTTP URL as "+httpUrl);

                                SftpUrlManager.instance().register(sftpUrl, httpUrl);
                            }

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // blah
                            }

                            if (httpUrl == null) {
                                Log.e("WTF", "Unable to register SFTP url '"+sftpUrl+"' as an HTTP URL.");
                                // TODO: handle this better maybe
                            }

                            // Send over this fucking handy HTTP URL we have now to an audio
                            // player fragment!

                            Log.i("FYI", "Ready to construct audio viewer fragment...");

                            final String finalUrl = httpUrl;
                            final String originalUrl = sftpUrl;
                            final String title = entry.name;

                            // TODO I think we are already on the UI thread...
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Fragment fragment = AudioListenerFragment.newInstance(finalUrl, originalUrl, title, mimetype);
                                    setFragment("audio-viewer", fragment);
                                }
                            });

                        }
                    } else if (mimetype.indexOf("video/") == 0) {

                        // Throw it up on the video viewer
                        HttpRelayServer relayServer = null;

                        try {
                            relayServer = HttpRelayServer.instance();
                        } catch (IOException e) {
                            Log.e("WTF", "Received IOException while starting HTTP relay server: "+e.getMessage(), e);
                        }

                        if (relayServer != null) {
                            String sftpUrl = "sftp://"+thread.toString()+entry.fullPath;
                            String httpUrl = SftpUrlManager.instance().getHttpUrl(sftpUrl);

                            if (httpUrl == null) {
                                httpUrl = relayServer.serveStream(new SftpStreamable(thread, entry.fullPath, mimetype));
                            }

                            // Send over this fucking handy HTTP URL we have now to a video
                            // player fragment!

                            // TODO I think we are already on the UI thread...
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Fragment fragment = VideoViewerFragment.newInstance(entry);
                                    //setFragment("video-viewer", fragment);
                                }
                            });

                        }

                    } else {
                        // TODO I think we are already on the UI thread...
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(SessionViewActivity.this)
                                    .setTitle("Shu cannot proceed")
                                    .setMessage("File Type: "+mimetype+"\n"+
                                                "... cannot open ... for now")
                                    .create()
                                    .show();
                            }
                        });
                    }
                }
            });
        }

    }

    @Override
    public void fragmentResumed(Fragment fragment) {
        Log.i("FYI", "SessionViewActivity fragment resumed.");
        activeFragment = fragment;

        if (fragment instanceof NavigableFragment) {
            Log.i("FYI", "IsNavigable: "+fragment.getClass().getCanonicalName());
            String fragTitle = ((NavigableFragment)fragment).getTitle();
            Log.i("FYI", "Fragment Resumed: Setting title from NavigableFragment: "+fragTitle);
            setTitle(fragTitle);
        }
    }

    @Override
    public void setTitle(String title) {
        Log.i("FYI", "SessionViewActivity setTitle "+title);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(title);
        mTitle = title;

        super.setTitle(title);
    }
}
