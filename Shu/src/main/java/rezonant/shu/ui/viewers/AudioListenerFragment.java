package rezonant.shu.ui.viewers;



import android.app.AlertDialog;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.dao.Dao;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.ssh.SFTPConnectionThread;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;


public class AudioListenerFragment extends Fragment {

    private static final String ARG_URL = "url";
    private static final String ARG_TITLE = "title";
    private static final String ARG_ORIGINAL_URL = "original_url";
    private static final String ARG_MIMETYPE = "mimetype";

    private String mUrl;
    private String mOriginalUrl;
    private String mTitle;
    private String mMimetype;

    public static AudioListenerFragment newInstance(String url, String originalUrl, String title, String mimetype) {
        AudioListenerFragment fragment = new AudioListenerFragment();
        Bundle args = new Bundle();

        args.putString(ARG_URL, url);
        args.putString(ARG_ORIGINAL_URL, originalUrl);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MIMETYPE, mimetype);

        fragment.setArguments(args);
        return fragment;
    }

    public AudioListenerFragment() {
        // Required empty public constructor
    }

    Dao<SSHConnection, Long> sshConnectionRepository = null;
    ImageView mImageView;

    @Override
    public void onStop() {
        // TODO recycle here
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
            //case R.id.action_play_again:
            //    start();
            //    return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        sshConnectionRepository = SSHConnection.createDao(getActivity());

        if (getArguments() != null) {
            mUrl = getArguments().getString(ARG_URL);
            mOriginalUrl = getArguments().getString(ARG_ORIGINAL_URL);
            mTitle = getArguments().getString(ARG_TITLE);
            mMimetype = getArguments().getString(ARG_MIMETYPE);
        }
    }

    private static MediaPlayer mediaPlayer;

    @Override
    public void onPause() {

        // Stop playing and clean up our media player

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Cancel our periodic UI updates

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        super.onPause();
    }

    private MediaPlayer.TrackInfo[] trackInfo = null;

    public void start()
    {
        Log.i("FYI", "AudioListenerFragment: Setting up UI for playback");
        titleLabel.setText(mTitle);
        originalUrlLabel.setText(mOriginalUrl);
        httpUrlLabel.setText(mUrl);
        seekBar.setProgress(0);
        seekBar.setMax(100);

        Log.i("FYI", "AudioListenerFragment: Starting playback on "+mUrl+"...");

        if (mediaPlayer == null) {
            MediaPlayer player = new MediaPlayer();
            trackInfo = null;

            try {
                player.setDataSource(getActivity(), Uri.parse(mUrl));
            } catch (IOException e) {

                Log.e("WTF", "While setting data source for audio: " + e.getMessage(), e);
                new AlertDialog.Builder(getActivity())
                        .setTitle("Failed to set data source for audio: " + e.getMessage())
                        .setMessage(e.toString())
                        .create()
                        .show();
            }

            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //trackInfo = mediaPlayer.getTrackInfo();
                }
            });

            Log.i("FYI", "Preparing...");
            try {
                player.prepare();
            } catch (IOException e) {

                Log.e("WTF", "While setting data source for audio: " + e.getMessage(), e);
                new AlertDialog.Builder(getActivity())
                        .setTitle("Failed to set data source for audio: " + e.getMessage())
                        .setMessage(e.toString())
                        .create()
                        .show();
            }
            Log.i("FYI", "Playing...");
            player.start();

            mediaPlayer = player;
        }

        Log.i("FYI", "AudioListenerFragment: Setting listeners on MediaPlayer...");

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("WTF", "MediaPlayer error " + what + "," + extra);
                return false;
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (repeat) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                }
            }
        });

        Log.i("FYI", "AudioListenerFragment: ...");

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer == null) {
                            Log.e("WTF", "Mediaplayer was null when timer went off.");
                            return;
                        }
                        int pos = mediaPlayer.getCurrentPosition();
                        int max = mediaPlayer.getDuration();

                        seekBar.setMax(max/1000);
                        seekBar.setProgress(pos/1000);

                        progressLabel.setText(timespanToString(pos) + " of " + timespanToString(max));
                        mimetypeLabel.setText(mMimetype);

                        /*
                        MediaPlayer.TrackInfo[] infos = mediaPlayer.getTrackInfo();

                        if (infos != null && infos.length > 0) {
                            MediaPlayer.TrackInfo info = infos[0];

                            if (info.getFormat() != null) {
                                MediaFormat format = info.getFormat();
                                String mimeType = format.getString(MediaFormat.KEY_MIME);
                                int bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);

                                String description = "";
                                if (bitrate > 0) {
                                    String formattedBitrate = bitrate+"bps";

                                    if (bitrate > 1000000) { // Mbps
                                        formattedBitrate = Math.round(bitrate/100000.0)/10.0+"Mbps";
                                    } else if (bitrate > 1000) { // Kbps
                                        formattedBitrate = Math.round(bitrate/100.0)/10.0+"Kbps";
                                    }

                                    description += "("+formattedBitrate+") ";
                                }

                                if (mimeType != null) {
                                    description += infos.length + "x " + mimeType;
                                }

                                mimetypeLabel.setText(description);
                            }
                        }
                        */
                    }
                });
            }
        }, 0, 500);
    }

    private String timespanToString(int totalMilliseconds)
    {
        int totalSeconds = totalMilliseconds / 1000;
        int hours;
        int minutes;
        int seconds;

        hours = totalSeconds/3600;
        totalSeconds -= hours*3600;

        minutes = totalSeconds/60;
        totalSeconds -= minutes*60;

        seconds = totalSeconds;

        return zeropad(hours)+":"+zeropad(minutes)+":"+zeropad(seconds);
    }

    private String zeropad(int number)
    {
        if (number < 10)
            return "0"+number;

        return number+"";
    }

    private Timer timer;
    private SeekBar seekBar;
    private TextView titleLabel;
    private TextView progressLabel;
    private TextView mimetypeLabel;
    private TextView originalUrlLabel;
    private TextView httpUrlLabel;
    private ImageButton playBtn;
    private ImageButton repeatBtn;
    private ImageButton muteBtn;
    private boolean muted = false;
    private boolean repeat = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_audio_listener, container, false);

        seekBar = (SeekBar)view.findViewById(R.id.seek);
        titleLabel = (TextView)view.findViewById(R.id.title);
        progressLabel = (TextView)view.findViewById(R.id.progress);
        mimetypeLabel = (TextView)view.findViewById(R.id.mimetype);
        originalUrlLabel = (TextView)view.findViewById(R.id.originalUrl);
        httpUrlLabel = (TextView)view.findViewById(R.id.httpUrl);
        playBtn = (ImageButton)view.findViewById(R.id.playBtn);
        repeatBtn = (ImageButton)view.findViewById(R.id.repeatBtn);
        muteBtn = (ImageButton)view.findViewById(R.id.muteBtn);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer == null)
                    return;

                if (mediaPlayer.isPlaying())
                    mediaPlayer.pause();
                else
                    mediaPlayer.start();
            }
        });

        muteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (muted)
                    mediaPlayer.setVolume(1, 1);
                else
                    mediaPlayer.setVolume(0, 0);

                muted = !muted;
            }
        });

        repeatBtn.setOnClickListener(new View.OnClickListener() {

            Toast lastToast = null;

            @Override
            public void onClick(View v) {
                repeat = !repeat;

                if (lastToast != null)
                    lastToast.cancel();

                if (repeat) {
                    (lastToast = Toast.makeText(getActivity(), "Repeat is on", Toast.LENGTH_SHORT)).show();
                } else {
                    (lastToast = Toast.makeText(getActivity(), "Repeat is off", Toast.LENGTH_SHORT)).show();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser)
                    return;

                if (mediaPlayer == null)
                    return;

                mediaPlayer.seekTo(progress*1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (mUrl != null) {
            start();
        }

        return view;
    }


}
