package rezonant.shu.state.ssh;

import android.util.Log;

import com.jcraft.jsch.ChannelSftp;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by liam on 7/19/14.
 */
public class SFTPConnectionThread {
    public SFTPConnectionThread(SSHConnectionThread masterThread, ChannelSftp sftpChannel)
    {
        this.masterThread = masterThread;
        this.sftpChannel = sftpChannel;
        this.commandQueue = new ConcurrentLinkedQueue<CommandRunner>();
    }

    private SSHConnectionThread masterThread;
    private ConcurrentLinkedQueue<CommandRunner> commandQueue;
    private ChannelSftp sftpChannel;
    private long id;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static abstract class CommandRunner {
        public abstract void run(ChannelSftp sftp);
    }

    public void queueCommand(CommandRunner runner)
    {
        synchronized (commandQueue) {
            commandQueue.add(runner);
            commandQueue.notify();
        }
    }

    private int referenceCount = 0;
    private boolean disposed = false;

    public SFTPConnectionThread reference()
    {
        referenceCount++;
        return this;
    }

    public void dispose()
    {
        if (disposed)
            return;

        referenceCount--;
        if (referenceCount <= 0) {
            disposed = true;

            // TODO Other things probably
        }
    }


    public void start()
    {
        Thread thread = new Thread() {
            @Override
            public void run() {

                Log.i("FYI", "[SFTP Thread] Ready to go for " + SFTPConnectionThread.this.masterThread);

                SFTPConnectionThread self = SFTPConnectionThread.this;

                while (true) {

                    // End when necessary

                    boolean wasDisposed = false;

                    synchronized (self) {
                        wasDisposed = self.disposed;
                    }

                    if (wasDisposed)
                        break;

                    // Grab a command to run

                    CommandRunner runner = null;

                    synchronized (commandQueue) {
                        try {
                            if (commandQueue.size() == 0) {
                                Log.i("FYI", "[SFTP Thread] Waiting for SFTP command ("+SFTPConnectionThread.this.masterThread+")");
                                commandQueue.wait();
                            }
                        } catch (InterruptedException e) {
                            // No supported interruptions
                        }

                        if (commandQueue.size() > 0)
                            runner = commandQueue.poll();
                    }

                    // If we got one, do it

                    if (runner != null) {
                        Log.i("FYI", "[SFTP Thread] Recvd command from queue ("+SFTPConnectionThread.this.masterThread+")");
                        runner.run(sftpChannel);
                    }
                }
            }
        };

        thread.start();
    }
}
