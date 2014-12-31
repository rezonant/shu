package rezonant.shu.state.http;

import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import rezonant.httprelay.StreamableResource;
import rezonant.shu.state.ssh.SFTPConnectionThread;
import rezonant.shu.state.ssh.SSHConnectionThread;

/**
 * Uses a private SFTP channel to handle high performance streaming by requesting and
 * delivering only what is asked. Implements StreamableResource for exposing SFTP assets
 * as HTTP-relay streamables.
 *
 * Created by liam on 7/28/14.
 */
public class SftpStreamable implements StreamableResource {

    public SftpStreamable(SSHConnectionThread thread, String filename, String mimetype)
    {
        this.thread = thread;
        this.filename = filename;
        this.mimetype = mimetype;
    }

    SSHConnectionThread thread;
    boolean sftpReady = false;
    String filename;
    String mimetype;

    public String getMimetype()
    {
        return mimetype;
    }
    public long length = -1;

    public long getLength()
    {
        if (length >= 0)
            return length;

        final List<Long> returnValue = new ArrayList<Long>();

        Log.i("FYI", "Starting SFTP channel for SFTP streamable (length)...");
        thread.startSftpChannel(new SSHConnectionThread.SftpChannelStartListener() {
            @Override
            public void start(final ChannelSftp sftp) {
                Log.i("FYI", "Start SFTP channel for SFTP streamable (length) completed.");
                long length = 0;
                try {
                    Log.i("FYI", "[SFTP Streamable] Stat for file length...");
                    SftpATTRS attrs = sftp.lstat(SftpStreamable.this.filename);
                    length = attrs.getSize();
                } catch (SftpException e) {
                    Log.e("WTF", e.getMessage(), e);
                }

                Log.i("FYI", "Returning length "+length+"...");
                synchronized (returnValue) {
                    returnValue.add(length);
                    returnValue.notify();
                }

                sftp.disconnect();
            }
        });

        Log.i("FYI", "Getting length...");

        synchronized (returnValue) {
            while (returnValue.size() == 0) {
                try {
                    returnValue.wait();
                } catch (InterruptedException e) {
                    // No interruption is allowed.
                }
            }
        }

        Log.i("FYI", "Received length "+returnValue.get(0)+"...");
        return length = returnValue.get(0);
    }

    @Override
    public InputStream open(final long offset) {

        // Start a new SFTP channel and pass it back to this thread

        final List<InputStream> result = new ArrayList<InputStream>();

        Log.i("FYI", "Queuing start command for SFTP streamable...");

        thread.startSftpChannel(new SSHConnectionThread.SftpChannelStartListener() {
            @Override
            public void start(final ChannelSftp sftp) {
                synchronized (result) {

                    Log.i("WTF", "Initializing stream with offset " + offset);

                    try {

                        InputStream stream = sftp.get(filename, null, offset);

                        int count = 0;

                        if (stream == null) {
                            Log.e("WTF", "Why?? Stream from SFTP channel is fucking null!?");
                            result.add(null);
                            result.notify();
                        } else {
                            Log.i("FYI", "Good stream received");
                            result.add(stream);
                            result.notify();
                        }
                    } catch (SftpException e) {
                        Log.e("WTF", e.getMessage(), e);

                        if (e.getCause() != null)
                            Log.e("WTF", "Caused by "+e.getCause().getMessage(), e);

                        result.add(null);
                        result.notify();
                        // TODO: better UI here?
                    }
                }
            }
        });

        // Wait patiently for the SFTP channel to arrive from the other thread.

        Log.i("FYI", "Waiting for SFTP stream to start...");
        synchronized (result) {
            int failures = 3;
            while (result.size() == 0 && failures > 0) {
                try {
                    result.wait();
                } catch (InterruptedException e) {
                    // There is no escape, oh wait no there is
                    // isn't there
                    Log.i("FYI", "Interrupted during nap by THIS guy: " + e.getMessage(), e);
                }

                --failures;
            }

            // If result was received:

            if (result.size() > 0) {
                Log.i("FYI", "Received SFTP stream.");
                InputStream stream = result.get(0);

                return stream;

            }

            Log.i("FYI", "No SFTP streamable result received.");
        }

        // Faaaiiiill!
        Log.e("WTF", "I had to return NULL from SftpStreamable.open(). Sorrryyy!!");
        Log.i("FYI", "Filename of failed resource : "+filename);

        return null;
    }
}
