package rezonant.shu.ui.viewers;



import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.j256.ormlite.dao.Dao;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.ssh.SFTPConnectionThread;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageViewerFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ImageViewerFragment extends Fragment {

    private static final String ARG_FILENAME = "filename";

    private String mFilename;
    private long mConnectionId;
    private SSHConnection mConnection;

    public static ImageViewerFragment newInstance(FilesystemDirectoryFragment.FilesystemEntry entry)
    {
        return newInstance(entry.connection, entry.fullPath);
    }

    public static ImageViewerFragment newInstance(SSHConnection connection, String filename) {
        ImageViewerFragment fragment = new ImageViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILENAME, filename);
        args.putLong(Constants.ARG_CONNECTION_ID, connection.getId());
        fragment.setArguments(args);
        return fragment;
    }

    public ImageViewerFragment() {
        // Required empty public constructor
    }

    Dao<SSHConnection, Long> sshConnectionRepository = null;
    ImageView mImageView;
    ProgressBar mProgressBar;
    Bitmap mBitmap = null;

    @Override
    public void onStop() {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.image_viewer, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
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

    public void refresh()
    {
        Log.i("FYI", "ImageViewerFragment: Refreshing");
        final SSHConnectionThread thread = SSHConnectionManager.instance().getConnection(getActivity(), mConnection);

        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        if (thread != null) {
            thread.ready(new Runnable() {
                @Override
                public void run() {
                    // Ready for SFTP
                    SFTPConnectionThread sftp = thread.getMainSftp();
                    sftp.queueCommand(new SFTPConnectionThread.CommandRunner() {
                        @Override
                        public void run(ChannelSftp sftp) {
                            try {

                                Log.i("FYI", "ImageViewerFragment: Initiating SFTP");
                                final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
                                sftp.get(mFilename, bufferStream);

                                final byte[] bytes = bufferStream.toByteArray();
                                try {
                                    bufferStream.close();
                                } catch (IOException e) {
                                    Log.i("FYI", "IOException while closing buffer stream: "+e.getMessage(), e);
                                }

                                // Send it to the UI thread

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.i("FYI", "ImageViewerFragment: Decoding "+bytes.length+" bytes");
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                        if (bitmap != null) {
                                            Log.i("FYI", "Successfully made the bitmap...");
                                            mBitmap = bitmap;
                                            if (mImageView != null) {
                                                Log.i("FYI", "Setting the bitmap from UI injection job...");
                                                mImageView.setImageBitmap(bitmap);
                                            }

                                            if (mProgressBar != null) {
                                                mProgressBar.setVisibility(View.INVISIBLE);
                                            }
                                        } else {
                                            Log.e("WTF", "ImageViewerFragment: Could not decode the bitmap");

                                            new AlertDialog.Builder(getActivity())
                                                    .setTitle("Error decoding image")
                                                    .setMessage("Could not decode image in file")
                                                    .create()
                                                    .show();
                                        }
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
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_viewer, container, false);

        Log.i("FYI", "Getting image view...");
        mImageView = (ImageView)view.findViewById(R.id.image);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        if (mBitmap != null) {

            Log.i("FYI", "Setting the bitmap from UI initializer...");
            mImageView.setImageBitmap(mBitmap);
        }

        // Scale gesture detector for image

        final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(getActivity(),
                new ScaleGestureDetector.OnScaleGestureListener() {

            private float scale = 1.0f;
            private float startScale = 1.0f;
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                Log.i("FYI", "Scale gesture: "+scale);
                mImageView.setScaleX(scale);
                mImageView.setScaleY(scale);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                Log.i("FYI", "Scale begin: "+detector.getScaleFactor());
                startScale = mImageView.getScaleX();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                Log.i("FYI", "Scale end: "+detector.getScaleFactor());
                startScale = mImageView.getScaleX();
            }
        });

        // onTouch listener to power scale gesture detector and translation

        mImageView.setOnTouchListener(new View.OnTouchListener() {
            private int translatePointer = -1;
            private MotionEvent.PointerCoords startCoords = null;

            @Override
            public boolean onTouch(View v, MotionEvent event) {


                if (translatePointer >= 0) {

                    Log.i("FYI", "Tracking translate....");

                    int pointerIndex = event.findPointerIndex(translatePointer);

                    if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP || event.getActionMasked() == MotionEvent.ACTION_UP) {
                        if (pointerIndex == event.getActionIndex()) {
                            Log.i("FYI", "Translate tracking ended.");
                            translatePointer = -1;
                            pointerIndex = -1;
                            startCoords = null;
                        }
                    }

                    Log.i("FYI", "Pointer index is "+pointerIndex+"....");

                    if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                        if (pointerIndex >= 0) {

                            Log.i("FYI", "Getting coordinates....");
                            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
                            event.getPointerCoords(pointerIndex, coords);

                            if (startCoords != null && mImageView != null && coords != null) {
                                mImageView.setTranslationX(mImageView.getTranslationX() + coords.x - startCoords.x);
                                mImageView.setTranslationY(mImageView.getTranslationY() + coords.y - startCoords.y);

                                Log.i("FYI", "Translation: " + (coords.x - startCoords.x) + ", " + (coords.y - startCoords.y));
                            }

                            return true;
                        } else {
                            translatePointer = -1;
                        }
                    }
                }

                boolean isDownEvent = event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN;

                if (isDownEvent && event.getPointerCount() == 1) {

                    Log.i("FYI", "Start translate, correct number of fingers.");

                    translatePointer = event.getPointerId(event.getActionIndex());
                    Log.i("FYI", "TP: "+translatePointer);
                    MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
                    event.getPointerCoords(event.getActionIndex(), coords);
                    Log.i("FYI", "Coords: "+coords.x+", "+coords.y);
                    startCoords = coords;

                    return true;
                }

                if (isDownEvent && event.getPointerCount() == 2) {
                    translatePointer = -1;
                }

                return scaleDetector.onTouchEvent(event);
            }
        });


        if (mConnection != null) {
            refresh();
        }

        return view;
    }


}
