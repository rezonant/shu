package rezonant.shu.ui.viewers;



import android.app.AlertDialog;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.ssh.SFTPConnectionThread;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;

public class TextFileViewerFragment extends Fragment {
    private static final String ARG_FILENAME = "filename";

    // TODO: Rename and change types of parameters
    private String mFilename;
    private long mConnectionId;
    private SSHConnection mConnection;

    public static TextFileViewerFragment newInstance(SSHConnection connection, String filename) {
        TextFileViewerFragment fragment = new TextFileViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILENAME, filename);
        args.putLong(Constants.ARG_CONNECTION_ID, connection.getId());
        fragment.setArguments(args);
        return fragment;
    }

    public static TextFileViewerFragment newInstance(FilesystemDirectoryFragment.FilesystemEntry entry) {
        return newInstance(entry.connection, entry.fullPath);
    }

    public TextFileViewerFragment() {
        // Required empty public constructor
    }

    private Dao<SSHConnection,Long> sshConnectionRepository;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.text_file_viewer, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_increase_font:
                if (mTextView.getTextSize() < 100) {
                    Log.i("FYI", "Increasing font size");
                    mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextView.getTextSize() + 5);
                }
                break;
            case R.id.action_decrease_font:
                if (mTextView.getTextSize() > 1) {
                    Log.i("FYI", "Decreasing font size");
                    mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextView.getTextSize() - 5);
                }
                break;

            case R.id.action_refresh:
                refresh();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);
        sshConnectionRepository = SSHConnection.createDao(getActivity());

        if (getArguments() != null) {
            mFilename = getArguments().getString(ARG_FILENAME);
            mConnectionId = getArguments().getLong(Constants.ARG_CONNECTION_ID);

            try {
                mConnection = sshConnectionRepository.queryForId(mConnectionId);
            } catch (SQLException e) {
                Log.e("WTF", e.getMessage(), e);
            }
        }
    }

    private TextView mTextView;
    private ProgressBar mProgressBar;

    public void refresh()
    {
        if (mProgressBar != null)
            mProgressBar.setVisibility(View.VISIBLE);

        final SSHConnectionThread thread =
                SSHConnectionManager.instance().getConnection(getActivity(), this.mConnection);

        if (thread == null)
            return;

        thread.ready(new Runnable() {
            @Override
            public void run() {
                // Ready for SFTP
                SFTPConnectionThread sftp = thread.getMainSftp();
                sftp.queueCommand(new SFTPConnectionThread.CommandRunner() {
                    @Override
                    public void run(ChannelSftp sftp) {
                        try {

                            final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
                            sftp.get(mFilename, bufferStream);

                            final byte[] bytes = bufferStream.toByteArray();
                            byte[] subset = bytes;
                            final int maxSize = 1024*500;

                            if (bytes.length > maxSize) {
                                subset = Arrays.copyOfRange(bytes, 0, maxSize);

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(getActivity())
                                                .setTitle("File view is truncated")
                                                .setMessage("Currently limited to "+FilesystemDirectoryFragment.formatBytes(maxSize)+" files. Your view will be limited to the first 1MB of this "+FilesystemDirectoryFragment.formatBytes(bytes.length)+" file")
                                                .create()
                                                .show();
                                    }
                                });
                            }

                            try {
                                bufferStream.close();
                            } catch (IOException e) {
                                Log.i("FYI", "IOException while closing buffer stream: "+e.getMessage(), e);
                            }

                            final String content = new String(subset);

                            // Send it to the UI thread

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextView.setText(content);
                                    if (mProgressBar != null)
                                        mProgressBar.setVisibility(View.INVISIBLE);
                                }
                            });

                        } catch (final SftpException e) {
                            Log.i("WTF", "Received exception while reading file: "+e.getMessage(), e);

                            // Report to user
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(getActivity())
                                        .setTitle("Error retrieving file")
                                        .setMessage("Could not retrieve file: "+e.getMessage())
                                        .create()
                                        .show();
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_text_file_viewer, container, false);

        this.mTextView = (TextView)view.findViewById(R.id.textView);
        this.mTextView.setHorizontallyScrolling(true);

        this.mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        this.refresh();

        final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(getActivity(), new ScaleGestureDetector.OnScaleGestureListener() {

            private float initialFontSize;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Log.i("FYI", "Whoo, scale factor is: "+detector.getScaleFactor());

                mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, initialFontSize*detector.getScaleFactor());
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                Log.i("FYI", "Whoo, scale began at: " + detector.getScaleFactor());

                initialFontSize = mTextView.getTextSize();
                return false;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

                Log.i("FYI", "Whoo, scale ended at: " + detector.getScaleFactor());
            }
        });

        mTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i("FYI", "onTouch on the text view: Here we are");
                scaleDetector.onTouchEvent(event);
                return false;
            }
        });
        return view;
    }
}
