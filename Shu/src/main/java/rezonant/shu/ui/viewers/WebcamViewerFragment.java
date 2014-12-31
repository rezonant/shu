package rezonant.shu.ui.viewers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.ssh.SFTPConnectionThread;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WebcamViewerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WebcamViewerFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class WebcamViewerFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_CAMERAS = "cameras";

    // TODO: Rename and change types of parameters
    private long[] connectionIDs;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WebcamViewerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WebcamViewerFragment newInstance(SSHConnectionThread[] cameras) {
        List<SSHWebcamConnectionParcelable> list = new ArrayList<SSHWebcamConnectionParcelable>();

        for (SSHConnectionThread thread : cameras) {
            list.add(new SSHWebcamConnectionParcelable(thread.getId()));
        }

        SSHWebcamConnectionParcelable[] parcelables = new SSHWebcamConnectionParcelable[list.size()];
        list.toArray(parcelables);

        WebcamViewerFragment fragment = new WebcamViewerFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(ARG_CAMERAS, parcelables);
        fragment.setArguments(args);
        return fragment;
    }

    public static class SSHWebcamConnectionParcelable implements Parcelable {
        public SSHWebcamConnectionParcelable(Parcel in) {
            String[] array = new String[1];
            in.readStringArray(array);
            this.connectionID = Long.parseLong(array[0]);
        }

        public SSHWebcamConnectionParcelable (long connectionID) {
            this.connectionID = connectionID;
        }

        public long connectionID;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            String[] array = new String[1];
            array[0] = connectionID+"";
            dest.writeStringArray(array);
        }
    }

    @Override
    public void onStop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        super.onStop();
    }

    @Override
    public void onResume() {

        // Schedule it in the future

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                WebcamViewerFragment.this.downloadNewImages();
            }
        }, 500, 500);

        super.onResume();
    }

    @Override
    public void onPause() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        super.onPause();
    }

    public WebcamViewerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Parcelable[] parcelables = getArguments().getParcelableArray(ARG_CAMERAS);
            long[] ids = new long[parcelables.length];
            int current = 0;
            for (Parcelable parcelable : parcelables) {
                SSHWebcamConnectionParcelable webcamConnection =
                        (SSHWebcamConnectionParcelable)parcelable;

                ids[current++] = webcamConnection.connectionID;
            }

            connectionIDs = ids;
        }
    }

    private class WebcamUI {

        private Bitmap lastImage;

        public WebcamUI(View rootView, TextView label, ImageView image)
        {
            this.label = label;
            this.rootView = rootView;
            this.image = image;
        }

        private TextView label;
        private ImageView image;
        private View rootView;
        private boolean commandQueued;
        private SSHConnection connection;
        private SSHConnectionThread ssh;
        private SFTPConnectionThread sftp;

        public SSHConnection getConnection() {
            return connection;
        }

        public void setConnection(SSHConnection connection) {
            this.connection = connection;
        }

        public SSHConnectionThread getSsh() {
            return ssh;
        }

        public void setSsh(SSHConnectionThread ssh) {
            this.ssh = ssh;
        }

        public SFTPConnectionThread getSftp() {
            return sftp;
        }

        public void setSftp(SFTPConnectionThread sftp) {
            this.sftp = sftp;
        }
    }

    private List<WebcamUI> webcamUIs = new ArrayList<WebcamUI>();
    private void onImageReceived(int index, byte[] data)
    {
        //Log.i("FYI", "Fragment received new webcam image for index "+index);
        //Log.i("FYI", "Payload is "+data.length+" bytes");

        WebcamUI ui = webcamUIs.get(index);

        //Log.i("FYI", "Decoding bitmap for webcam "+index);
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        data = null;

        if (bitmap == null) {
            Log.e("WTF", "Failed to decode bitmap for index "+index);
            return;
        }

        //Log.i("FYI", "Bitmap successful for webcam "+index);
        ui.image.setImageBitmap(bitmap);

        if (ui.lastImage != null) {
            ui.lastImage.recycle();
            ui.lastImage = null;
        }

        ui.lastImage = bitmap;
    }

    public final int NEW_WEBCAM_IMAGE = 1;
    private Handler handler;
    private ProgressBar mProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_webcam_viewer, container, false);
        LinearLayout backbone = (LinearLayout)view.findViewById(R.id.backbone);

        mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        // Prepare a handler for receiving pings from the main SFTP threads

        this.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                if (msg.arg1 == NEW_WEBCAM_IMAGE) {

                    if (!firstImage && mProgressBar != null)
                        mProgressBar.setVisibility(View.INVISIBLE);

                    Log.i("FYI", "Handler received new webcam image");
                    // New webcam image received

                    WebcamViewerFragment fragment = WebcamViewerFragment.this;
                    byte[] data = (byte[])msg.obj;
                    fragment.onImageReceived(msg.arg2, data);

                    msg.obj = null;
                    return;
                }

                super.handleMessage(msg);
            }
        };

        // Create UIs

        Log.i("FYI", "Creating UIs for "+this.connectionIDs.length+" webcams...");

        for (final long connectionID : this.connectionIDs) {
            Log.i("FYI", "Constructing UI for: " + connectionID);

            final View webcamView = inflater.inflate(R.layout.webcam_view, container, false);
            final ImageView image = (ImageView) webcamView.findViewById(R.id.image);
            final TextView label = (TextView) webcamView.findViewById(R.id.label);

            webcamView.setId(View.generateViewId());
            image.setId(View.generateViewId());
            label.setId(View.generateViewId());


            // Scale gesture detector for image

            final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(getActivity(),
                    new ScaleGestureDetector.OnScaleGestureListener() {

                private float scale = 1.0f;
                private float startScale = 1.0f;
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    scale *= detector.getScaleFactor();
                    Log.i("FYI", "Scale gesture: "+scale);
                    image.setScaleX(scale);
                    image.setScaleY(scale);
                    return true;
                }

                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    Log.i("FYI", "Scale begin: "+detector.getScaleFactor());
                    startScale = image.getScaleX();
                    return true;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {
                    Log.i("FYI", "Scale end: "+detector.getScaleFactor());
                    startScale = image.getScaleX();
                }
            });

            // onTouch listener to power scale gesture detector and translation

            image.setOnTouchListener(new View.OnTouchListener() {
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

                                if (startCoords != null && image != null && coords != null) {
                                    image.setTranslationX(image.getTranslationX() + coords.x - startCoords.x);
                                    image.setTranslationY(image.getTranslationY() + coords.y - startCoords.y);

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


            WebcamUI ui = new WebcamUI(webcamView, label, image);

            final int index = WebcamViewerFragment.this.webcamUIs.size();
            WebcamViewerFragment.this.webcamUIs.add(ui);

            // Get the SFTP connection thread

            Dao<SSHConnection, Long> connectionRepository = SSHConnection.createDao(getActivity());

            SSHConnection connection = null;

            try {
                connection = connectionRepository.queryForId(connectionID);
            } catch (SQLException e) {
                Log.e("WTF", e.getMessage(), e);
                continue;
            }

            SSHConnectionThread thread = SSHConnectionManager.instance().getConnection(getActivity(), connection);

            final SFTPConnectionThread sftp = thread.getMainSftp();

            ui.setConnection(connection);
            ui.setSsh(thread);
            ui.setSftp(sftp);

            backbone.addView(webcamView);
        }

        // Queue up image downloading commands...

        downloadNewImages();

        return view;
    }

    private Timer timer;
    private boolean firstImage = true;

    private void downloadNewImages()
    {
        // Before we start, clean up some memory
        System.gc();

        for (int i = 0, max = webcamUIs.size(); i < max; ++i) {
            final int index = i;
            final WebcamUI ui = webcamUIs.get(i);

            if (ui.commandQueued)
                continue; // Don't queue more than we can execute.

            //Log.i("FYI", " ** Queuing download command for connection "+ui.connection);

            ui.commandQueued = true;
            ui.getSftp().queueCommand(new SFTPConnectionThread.CommandRunner() {
                @Override
                public void run(ChannelSftp sftp) {

                    //Log.i("FYI", " ** Webcam command for "+ui.connection+" is now running.");
                    // Transfer an image
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    boolean good = false;

                    try {
                        //Log.i("FYI", "[Webcam] "+ui.connection+": GET");
                        sftp.get("/home/liam/.webcam.jpg", stream);
                        //Log.i("FYI", "[Webcam] " + ui.connection + ": GET finished");

                        firstImage = false;

                        good = true;
                    } catch (SftpException e) {
                        Log.e("WTF", e.getMessage(), e);
                    }

                    if (good) {
                        //Log.i("FYI", "[Webcam] "+ui.connection+": Good result, sending handler update");
                        byte[] bytes = stream.toByteArray();
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // Do nothing because we already have a good result.
                            Log.w("ERMOK", "Got IOException during stream.close(): "+e.getMessage(),
                                    e);
                        }

                        Message msg = new Message();
                        msg.setTarget(handler);
                        msg.arg1 = NEW_WEBCAM_IMAGE;
                        msg.arg2 = index;
                        msg.obj = bytes;
                        msg.sendToTarget();
                    }
                    ui.commandQueued = false;
                }
            });
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            //throw new ClassCastException(activity.toString()
            //        + " must implement Delegate");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
