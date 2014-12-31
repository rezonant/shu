package rezonant.shu.state.ssh;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.EditText;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import rezonant.shu.threads.MessageThreadRequest;
import rezonant.shu.threads.PasswordThreadRequest;
import rezonant.shu.threads.ThreadRequest;
import rezonant.shu.threads.YesNoThreadRequest;

/**
 * Created by liam on 7/2/14.
 */
public class SSHConnectionThread {

    public SSHConnectionThread(Context context, String user, String host, int port)
	{
		this.context = context;
		this.user = user;
		this.host = host;
		this.port = port;

		commandQueue = new ConcurrentLinkedQueue<SSHCommand>();
		outputQueue = new ConcurrentLinkedQueue<SSHCommandResult>();
	}

	private String user;
	private String host;
	private int port = 22;
	private Thread thread;
	private String password;
    private long id;
    private Date startTime;

	private ConcurrentLinkedQueue<SSHCommand> commandQueue;
	private ConcurrentLinkedQueue<SSHCommandResult> outputQueue;

    public void setContext(Context context)
    {
        context = context;
    }

	@Override
	public String toString()
	{
		return user+"@"+host+":"+port;
	}

	public SSHConnectionThread setPassword(String password)
	{
		this.password = password;
		return this;
	}

	private Handler handler;
    private long nextCommandId = 1;
    private SFTPConnectionThread sftp;

    private Map<Long,WeakReference<SSHCommand>> commandMap =
            new HashMap<Long,WeakReference<SSHCommand>>();

    /**
     * Called when the connection is ready to accept commands and all
     * primary resources have been assigned.
     *
     * @param runnable
     */
    public void ready(final Runnable runnable)
    {
        SSHCommand command = new SSHCommand("Ready?", "@shu::ready");
        queueCommand(command, new SSHCommandFinishedListener() {
            @Override
            public void commandCompleted(SSHCommandResult result) {
                runnable.run();
            }
        });
    }

	public void queueCommand(SSHCommand command)
	{
        if (this.disconnected) {
            Log.e("WTF", "Attempt to queue command on disconnected thread!");
            return;
        }
		command.setThread(this);
        command.setId(nextCommandId++);
        commandMap.put(command.getId(), new WeakReference<SSHCommand>(command));

		synchronized (commandQueue) {
			commandQueue.add(command);
			commandQueue.notify();
		}
	}

    public Date getStartTime() {
        return startTime;
    }

    public void disconnect() {
        SSHCommand disconnect = new SSHCommand("Disconnect", "@shu::disconnect");
        queueCommand(disconnect);
    }

    public static abstract class SftpChannelStartListener {
        public abstract void start(ChannelSftp channel);
    }

    public static abstract class SftpStartListener {
        public abstract void start(SFTPConnectionThread thread);
    }

    private long nextSftpThreadId = 1;
    private Map<Long, SFTPConnectionThread> personalSftpThreads =
            new HashMap<Long, SFTPConnectionThread>();

    /**
     * Get your very own SFTP thread. Has a unique getId() that can be looked up later.
     * Callback is called from the master SSH thread where you should enqueue commands onto the
     * SFTP thread.
     *
     * @param listener
     */
    public void getSftp(final SftpStartListener listener)
    {
        this.queueCommand(new SSHCommand("Start SFTP", "@shu::start-sftp"), new SSHCommandFinishedListener() {
            @Override
            public void commandCompleted(SSHCommandResult result) {
               ChannelSftp sftp = (ChannelSftp)result.getRequestedResource();
               try {
                   sftp.connect();
               } catch (JSchException e) {
                   Log.e("WTF", e.getMessage(), e);
               }

               SFTPConnectionThread thread = new SFTPConnectionThread(SSHConnectionThread.this, sftp);

               thread.setId(nextSftpThreadId++);
               personalSftpThreads.put(thread.getId(), thread);
               thread.start();

               listener.start(thread);
            }
        });
    }

    /**
     * Starts the SFTP channel and then runs the given listener on the main SSH thread.
     * You should jump off this thread as soon as you can to prevent hogging it.
     *
     * It's done this way since startSftpChannel() may be synch'ed to the UI thread, which
     * would cause a threadlock if the handler message is sent.
     *
     * @param listener
     */
    public void startSftpChannel(final SftpChannelStartListener listener)
    {
        Log.i("FYI", "Queueing start SFTP channel command...");
        this.queueCommand(new SSHCommand("Start private SFTP Channel", "@shu::start-sftp-inline"), new SSHCommandFinishedListener() {
            @Override
            public void commandCompleted(SSHCommandResult result) {
                Log.i("FYI", "Start SFTP channel commandCompleted()");

                ChannelSftp sftp = (ChannelSftp)result.getRequestedResource();
                try {
                    Log.i("FYI", "startSftpChannel(inner): sftp.connect()");
                    sftp.connect();
                } catch (JSchException e) {
                    Log.e("WTF", e.getMessage(), e);
                }

                Log.i("FYI", "Calling the listener for startSftpChannel...");
                listener.start(sftp);
            }
        });

    }

    /**
     * Look up your thread again later
     *
     * @param existingThread
     * @return
     */
    public SFTPConnectionThread getSftp(long existingThread)
    {
        if (!personalSftpThreads.containsKey(existingThread))
            return null;

        return personalSftpThreads.get(existingThread);
    }


    /**
     * Retrieve a command by ID. Used for fragments/etc to look up a command by ID as needed.
     *
     * @param id
     * @return
     */
    public SSHCommand getCommandById(long id)
    {
        if (!commandMap.containsKey(id))
            return null;

        WeakReference<SSHCommand> ref = commandMap.get(id);
        SSHCommand command = ref.get();

        if (command == null) {
            commandMap.remove(id);
            return null;
        }

        return command;
    }

	public void queueCommand(SSHCommand command, SSHCommandFinishedListener listener)
	{
		command.setFinishedListener(listener);
		queueCommand(command);
	}

	public static final int SSH_COMMAND_RESULT = 1;
	public static final int SSH_REQUEST_PASSWORD = 2;
	public static final int SSH_REQUEST_ADD_TO_HOST = 3;
	public static final int SSH_YES_NO = 4;
	public static final int SSH_REQUEST_PASSPHRASE = 5;
	public static final int SSH_RECEIVE_DATA = 6;
	public static final int SSH_COMMAND_STARTED = 7;
	public static final int SSH_RECEIVE_ERROR_DATA = 8;
	public static final int SSH_SUDO_PASSWORD = 9;
    public static final int SSH_MESSAGE = 10;

	private Context context;
    private String sudoPassword = null;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    class ReceivedDataPacket {
		public ReceivedDataPacket(SSHCommand command, String data)
		{
			this.command = command;
			this.data = data;
		}

		SSHCommand command;
		String data;
	}

	OutputStream stdin;
    private boolean disconnected = false;

    public SFTPConnectionThread getMainSftp()
    {
        return this.sftp;
    }

	public void start()
	{
        if (this.disconnected) {
            Log.e("WTF", "Attempt to recycle thread after disconnecting. This will not end well.");
            return;
        }

        this.startTime = new Date();
		final SSHConnectionThread self = this;

		this.handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				if (msg.arg1 == SSH_REQUEST_ADD_TO_HOST) {

                    Log.i("FYI", "(ADD TO HOST) Param: "+msg.arg2+", Obj: "+msg.obj);

				} else if (msg.arg1 == SSH_REQUEST_PASSWORD) {

					final PasswordThreadRequest request = (PasswordThreadRequest)msg.obj;
					final AlertDialog.Builder alert = new AlertDialog.Builder(context);
					final EditText input = new EditText(context);

					input.setInputType(InputType.TYPE_CLASS_TEXT| InputType.TYPE_TEXT_VARIATION_PASSWORD);

					alert.setView(input);    //edit text added to alert
					alert.setTitle("Authentication Required");   //title set
                    alert.setMessage(request.getMessage());        //msg setz
					alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							String password = input.getText().toString();
							request.respond(password);
						}
					});
					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							request.respond(null);
						}
					});

					AlertDialog dialog = alert.create();
					dialog.show();


				} else if (msg.arg1 == SSH_REQUEST_PASSPHRASE) {
					final AlertDialog.Builder alert = new AlertDialog.Builder(context);
					final EditText input = new EditText(context);
					final ThreadRequest request = (ThreadRequest)msg.obj;

					input.setInputType(InputType.TYPE_CLASS_TEXT| InputType.TYPE_TEXT_VARIATION_PASSWORD);

					alert.setView(input);    //edit text added to alert
					alert.setTitle("Enter key passphrase");   //title setted
					alert.setPositiveButton("Elevate Privileges", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							String password = input.getText().toString();
							request.respond(password);
						}
					});
					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							request.respond(null);
						}
					});
					AlertDialog dialog = alert.create();
					dialog.show();
				} else if (msg.arg1 == SSH_RECEIVE_DATA) {
					ReceivedDataPacket data = (ReceivedDataPacket)msg.obj;
					byte[] bytes = data.data.getBytes();
					data.command.onData(bytes, bytes.length);
				} else if (msg.arg1 == SSH_RECEIVE_ERROR_DATA) {
					ReceivedDataPacket data = (ReceivedDataPacket)msg.obj;
					byte[] bytes = data.data.getBytes();
					data.command.onErrorData(bytes, bytes.length);
				} else if (msg.arg1 == SSH_COMMAND_STARTED) {
					((SSHCommand)msg.obj).onExecutionStarted();
				} else if (msg.arg1 == SSH_SUDO_PASSWORD) {

                    if (SSHConnectionThread.this.sudoPassword != null) {
                        try {
                            // TODO/FIXME: Have this expire properly
                            // TODO/FIXME: Potential attack, non-sudo command sends password prompt
                            // TODO/FIXME:      after Shu has cached the password.

                            stdin.write((SSHConnectionThread.this.sudoPassword+"\n").getBytes());
                            stdin.flush();
                        } catch (IOException e) {
                            Log.e("WTF", e.getMessage(), e);
                        }
                        return;
                    }

					final AlertDialog.Builder alert = new AlertDialog.Builder(context);
					final EditText input = new EditText(context);

					input.setInputType(InputType.TYPE_CLASS_TEXT| InputType.TYPE_TEXT_VARIATION_PASSWORD);

					alert.setView(input);    //edit text added to alert
					alert.setTitle("Enter sudo password");   //title setted
					alert.setPositiveButton("Elevate Privileges", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							String password = input.getText().toString();
							try {
                                SSHConnectionThread.this.sudoPassword = password;
								stdin.write((password+"\n").getBytes());
								stdin.flush();
							} catch (IOException e) {
								Log.e("WTF", "Error while sending entered password back to host-side agent: "+e.getMessage(), e);
							}
						}
					});

					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							try {
								stdin.write("\n".getBytes());
								stdin.flush();
							} catch (IOException e) {
								Log.e("WTF", "Error while cancelling password: "+e.getMessage(), e);
							}
						}
					});

					AlertDialog dialog = alert.create();
					dialog.show();
					String password = input.getText().toString();

				} else if (msg.arg1 == SSH_COMMAND_RESULT) {
					Log.i("FYI", "Handler has received command result");

					SSHCommandResult result = (SSHCommandResult)msg.obj;

                    if (result.getCommand() == null) {

                        Log.i("FYI", "Received result for null command");
                        Log.i("FYI", "* Shu Error: "+result.getShuError());

                        String error = result.getShuError();

                        if (error != null) {

                            new AlertDialog.Builder(context)
                                    .setTitle("Connection Error")
                                    .setMessage(error)
                                    .create()
                                    .show();
                        }
                        return;
                    }

					result.getCommand().onExited(result.getExitCode());
					SSHCommandFinishedListener listener = result.getCommand().getFinishedListener();

					if (listener != null)
						listener.commandCompleted(result);
				} else if (msg.arg1 == SSH_YES_NO) {
					final YesNoThreadRequest request = (YesNoThreadRequest)msg.obj;

					new AlertDialog.Builder(self.context)
							.setMessage(request.getPrompt())
							.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									Log.i("FYI", "Responding YES");
									request.respond(true);
								}
							})
							.setNegativeButton("No", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									Log.i("FYI", "Responding NO");
									request.respond(false);
								}
							})
							.create()
							.show();
				} else if (msg.arg1 == SSH_MESSAGE) {
                    final MessageThreadRequest request = (MessageThreadRequest)msg.obj;

                    new AlertDialog.Builder(self.context)
                            .setTitle("Information")
                            .setMessage(request.getMessage())
                            .create()
                            .show();
                }
			}
		};

		this.thread = new Thread()
		{
			private void sendMessage(int type, Object arg)
			{
				Message msg = new Message();
				msg.setTarget(handler);
				msg.arg1 = type;
				msg.arg2 = 0;
				msg.obj = arg;
				msg.sendToTarget();
			}

			private void establishSession()
			{
				try {
					Log.i("FYI", "Setting up jsch");
					session = jsch.getSession(user, host, port);

					if (self.password != null && !self.password.equals(""))
						session.setPassword(self.password);

					Log.i("FYI", "SSH: Adding UI integration...");
					session.setUserInfo(new UserInfo() {
						@Override
						public String getPassphrase() {
							Log.w("SSH", "=========== Passphrase dialog invoked");

                            PasswordThreadRequest request = new PasswordThreadRequest(thread, "Enter passphrase:");
                            sendMessage(SSH_REQUEST_PASSWORD, request);
                            return (String)request.waitForResult();
						}

						@Override
						public String getPassword() {
							Log.w("SSH", "=========== Password dialog invoked");
							PasswordThreadRequest request = new PasswordThreadRequest(thread, "Enter password:");
							sendMessage(SSH_REQUEST_PASSWORD, request);
							return (String)request.waitForResult();
						}

						@Override
						public boolean promptPassword(String s) {
							Log.w("SSH", "=========== Prompt Password invoked: "+s);
							return promptYesNo(s);
						}

						@Override
						public boolean promptPassphrase(String s) {
							Log.w("OHSHIT", "Passphrase is not implemented yet oh noes! "+s);
                            return promptYesNo(s);
						}

						@Override
						public boolean promptYesNo(String s) {
							Log.w("SSH", "=========== Yes/No dialog invoked: "+s);

							YesNoThreadRequest request = new YesNoThreadRequest(thread, s);
							sendMessage(SSH_YES_NO, request);
							return (Boolean)request.waitForResult();
						}

						@Override
						public void showMessage(String s) {

                            YesNoThreadRequest request = new YesNoThreadRequest(thread, s);
                            sendMessage(SSH_MESSAGE, request);
                            request.waitForResult();
						}
					});

					Log.i("FYI", "SSH: Connecting to host...");
					session.connect();
					Log.i("FYI", "SSH: Success!");

                    Log.i("FYI", "SFTP: Opening channel...");
                    ChannelSftp sftp = (ChannelSftp)session.openChannel("sftp");
                    sftp.connect();

                    if (sftp != null) {
                        Log.i("FYI", "SFTP: Ready to go!");
                        SSHConnectionThread.this.sftp = new SFTPConnectionThread(SSHConnectionThread.this, sftp);
                        SSHConnectionThread.this.sftp.start();
                    }
				} catch (JSchException e) {


                    SSHCommandResult result = new SSHCommandResult(SSHConnectionThread.this, null);
                    result.setShuError(e.getMessage());
                    result.setExitCode(1000);
                    result.setOutput("");
                    sendMessage(SSH_COMMAND_RESULT, result);

					Log.i("WTF", e.getMessage(), e);
				}

			}

			JSch jsch;
			private Session session = null;

			@Override
			public void run() {
				final Thread thread = this;

				jsch = new JSch();

				this.establishSession();

                if (this.session == null || !this.session.isConnected()) {
                    // Abort
                    Log.e("WTF", "Detected failure to connect, aborting start of SSH connection thread...");
                    SSHConnectionManager.instance().hasDisconnected(SSHConnectionThread.this);
                    return;
                }

				// Wait for commands and run them

				while (true) {
					try {
						synchronized (commandQueue) {
							if (commandQueue.size() == 0)
								commandQueue.wait();
						}
					} catch (InterruptedException e) {
						// No interrupts are specially handled.
					}

					SSHCommand command = commandQueue.poll();
					if (command == null)
						continue;

                    Log.i("FYI", "Processing command: "+command.getCommand());

                    // Handle special disconnect command

                    if ("@shu::disconnect".equalsIgnoreCase(command.getCommand())) {
                        if (session.isConnected()) {
                            session.disconnect();
                        }

                        Log.i("FYI", "SSHConnectionThread disconnect received...");
                        session = null;
                        SSHConnectionThread.this.disconnected = true;
                        Log.i("FYI", "SSHConnectionManager.hasDisconnected(thread)...");
                        SSHConnectionManager.instance().hasDisconnected(SSHConnectionThread.this);
                        break;
                    }

                    // Check if connected, if not reconnect

                    if (!session.isConnected()) {
                        this.establishSession();
                    }

                    // Handle other special commands

                    if ("@shu::ready".equalsIgnoreCase(command.getCommand())) {
                        // No-op. Used to ensure that the connection is fully ready for making
                        // use of the primary channels, such as sftp.

                        SSHCommandResult result = new SSHCommandResult(SSHConnectionThread.this, command);
                        result.setExitCode(1000);
                        result.setOutput("");
                        sendMessage(SSH_COMMAND_RESULT, result);
                        continue;
                    }

                    if ("@shu::start-sftp".equalsIgnoreCase(command.getCommand())) {
                        // Start an SFTP channel and pass it back to result as a requested
                        // resource.

                        Log.i("FYI", "Processing start SFTP channel command...");

                        ChannelSftp sftp;
                        try {
                            sftp = (ChannelSftp) session.openChannel("sftp");
                        } catch (Exception e) {
                            sftp = null;
                        }

                        Log.i("FYI", "Passing result for  start-SFTP-channel command...");
                        SSHCommandResult result = new SSHCommandResult(SSHConnectionThread.this, command);
                        result.setRequestedResource(sftp);
                        result.setExitCode(1000);
                        result.setOutput("");
                        sendMessage(SSH_COMMAND_RESULT, result);
                        continue;
                    }

                    if ("@shu::start-sftp-inline".equalsIgnoreCase(command.getCommand())) {
                        // Start an SFTP channel and pass it back to result as a requested
                        // resource.

                        Log.i("FYI", "Processing start SFTP channel command...");

                        ChannelSftp sftp;
                        try {
                            sftp = (ChannelSftp) session.openChannel("sftp");
                        } catch (Exception e) {
                            sftp = null;
                        }

                        // In this version we call the listener directly.

                        Log.i("FYI", "Passing result for  start-SFTP-channel command...");

                        SSHCommandResult result = new SSHCommandResult(SSHConnectionThread.this, command);
                        result.setRequestedResource(sftp);
                        result.setExitCode(1000);
                        result.setOutput("");
                        result.getCommand().onExited(result.getExitCode());

                        SSHCommandFinishedListener listener = result.getCommand().getFinishedListener();
                        if (listener != null)
                            listener.commandCompleted(result);

                        continue;
                    }

                    // Run a standard SSH command

					try {
						Log.i("FYI", "SSH: Opening shell channel...");
						ChannelExec exec = null;
                        int reattempts = 0;

                        while (true) {

                            try {
                                exec = (ChannelExec) session.openChannel("exec");
                            } catch (JSchException e) {
                                if (e.getMessage() == "session is down") {
                                    if (reattempts > 3) {
                                        exec = null;
                                        break;
                                    }

                                    this.establishSession();
                                    ++reattempts;
                                    continue;
                                }
                            }

                            break;
                        }

						if (exec == null) {
							Log.e("WTF", "Failed to create an SSH channel, aborting command.");
                            SSHCommandResult result = new SSHCommandResult(SSHConnectionThread.this, command);
                            result.setShuError("Failed to create an SSH channel, aborting command.");
                            result.setExitCode(1000);
                            result.setOutput("");
                            sendMessage(SSH_COMMAND_RESULT, result);

							continue;
						}

						String commandString = command.getCommand();

						commandString = "bash -c '. \"$HOME/.shurc\"; SUDO_ASKPASS=\"$HOME/.shu-askpass\" "+commandString.replace("'", "'\"'\"'")+"'";

						Log.i("FYI", "Running this command nao: ["+commandString+"]");

						exec.setCommand(commandString);

						stdin = exec.getOutputStream();
						exec.setInputStream(null);
						exec.setErrStream(null);
						InputStream in;
						InputStream errs;

						try {
							in = exec.getInputStream();
							errs = exec.getErrStream();
						} catch (IOException e) {
							Log.e("WTF", "Failed to retrieve streams for SSH command output/errors. Aborting.");
							Log.e("WTF", e.getMessage(), e);
							continue;
						}

						Log.i("FYI", "SSH: Running command...");
						sendMessage(SSH_COMMAND_STARTED, command);
						exec.connect();

						byte[] tmp = new byte[1024];
						StringBuilder output = new StringBuilder();
						while(true) {
							while (in.available() > 0) {
								int i = in.read(tmp, 0, 1024);
								if (i < 0)
									break;

								String str = new String(tmp, 0, i);
								output.append(str);
								command.setCurrentOutput(output.toString());

								sendMessage(SSH_RECEIVE_DATA, new ReceivedDataPacket(command, str));
							}

							while (errs.available() > 0) {
								int i = errs.read(tmp, 0, 1024);
								if (i < 0)
									break;

								String str = new String(tmp, 0, i);
								String esc = ((char)27)+"P";

								if (str.contains(esc)) {
									str = str.replace(esc, "");
									sendMessage(SSH_SUDO_PASSWORD, null);
								}

								output.append(str);
								sendMessage(SSH_RECEIVE_ERROR_DATA, new ReceivedDataPacket(command, str));
							}

							if (exec.isClosed()) {
								if(in.available() > 0)
									continue;
								break;
							}

							try {
								Thread.sleep(50);
							} catch (Exception ee) { }
						}

						Log.i("FYI", "SSH: Exit status: " + exec.getExitStatus());
						exec.disconnect();

						// Construct the return message

						SSHCommandResult result = new SSHCommandResult(SSHConnectionThread.this, command);
						result.setExitCode(exec.getExitStatus());
						result.setOutput(output.toString());

						// Send the return message

						sendMessage(SSH_COMMAND_RESULT, result);


						outputQueue.add(result);

					} catch (JSchException e) {
						Log.e("WTF", "JSchException occurred while running command "+command.getCommand());
						Log.e("WTF", e.getMessage(), e);
					} catch (IOException e) {
						Log.e("WTF", "IOException occurred while running command "+command.getCommand());
						Log.e("WTF", e.getMessage(), e);

					}
				}
			}
		};

		this.thread.start();
	}
}
