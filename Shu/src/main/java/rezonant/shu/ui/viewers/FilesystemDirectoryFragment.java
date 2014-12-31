package rezonant.shu.ui.viewers;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.j256.ormlite.dao.Dao;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.ssh.FileManagerCache;
import rezonant.shu.state.ssh.SFTPConnectionThread;
import rezonant.shu.state.ssh.SSHConnectionManager;
import rezonant.shu.state.ssh.SSHConnectionThread;
import rezonant.shu.ui.NavigatedFragment;

/**
 * A fragment representing a filesystem directory listing.
 */
public class FilesystemDirectoryFragment
        extends
        NavigatedFragment
        implements
            AbsListView.OnItemClickListener
{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_DIRECTORY = "directory";

    private SSHConnection mConnection;
    private long mConnectionId;
    private String mDirectory;

    private Delegate mListener;

    private AdapterView<ListAdapter> mAdapterView;
    private ProgressBar mProgressBar;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private BaseAdapter mAdapter;
    private FilesystemEntry mContextItem;

    public static FilesystemDirectoryFragment newInstance(SSHConnection connection, String directory) {
        FilesystemDirectoryFragment fragment = new FilesystemDirectoryFragment();
        Bundle args = new Bundle();

        args.putString(ARG_DIRECTORY, directory);
        args.putLong(Constants.ARG_CONNECTION_ID, connection.getId());

        fragment.setArguments(args);
        return fragment;
    }

    public static FilesystemDirectoryFragment newInstance(FilesystemEntry entry) {

        String directory = entry.fullPath;
        SSHConnection connection = entry.connection;

        FilesystemDirectoryFragment fragment = new FilesystemDirectoryFragment();
        Bundle args = new Bundle();

        args.putString(ARG_DIRECTORY, directory);
        args.putLong(Constants.ARG_CONNECTION_ID, connection.getId());

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilesystemDirectoryFragment() {
    }

    Dao<SSHConnection, Long> sshConnectionRepository = null;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.filesystem_directory, menu);

        MenuItem item = menu.findItem(R.id.action_toggle_hidden_files);

        if (mShowHiddenFiles) {
            item.setTitle("Hide hidden files");
        } else {
            item.setTitle("Show hidden files");
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    public final int CONTEXT_MENU_OPEN = 1;
    public final int CONTEXT_MENU_OPEN_WITH = 2;
    public final int CONTEXT_MENU_INFO = 3;
    public final int CONTEXT_MENU_DELETE = 4;
    public final int CONTEXT_MENU_SHELL = 5;
    public final int CONTEXT_MENU_DOWNLOAD = 5;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        menu.add(Menu.NONE, CONTEXT_MENU_OPEN, Menu.FIRST, "Open");
        menu.add(Menu.NONE, CONTEXT_MENU_OPEN_WITH, Menu.FIRST, "Open With");
        menu.add(Menu.NONE, CONTEXT_MENU_DOWNLOAD, Menu.FIRST, "Download");
        menu.add(Menu.NONE, CONTEXT_MENU_INFO, Menu.FIRST, "Info");
        menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.FIRST, "Delete");

        if (mContextItem != null) {
            if (mContextItem.isDirectory) {
                menu.add(Menu.NONE, CONTEXT_MENU_SHELL, Menu.FIRST, "Open Shell");
            }
        }

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private Toast lastToast = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refresh();
                break;
            case R.id.action_toggle_hidden_files:
                mShowHiddenFiles = !mShowHiddenFiles;

                if (lastToast != null)
                    lastToast.cancel();

                if (mShowHiddenFiles) {
                    (lastToast = Toast.makeText(getActivity(), "Displaying hidden files", Toast.LENGTH_SHORT)).show();
                    item.setTitle("Hide hidden files");
                } else {
                    (lastToast = Toast.makeText(getActivity(), "Not displaying hidden files", Toast.LENGTH_SHORT)).show();
                    item.setTitle("Show hidden files");
                }

                refresh();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        sshConnectionRepository = SSHConnection.createDao(this.getActivity());

        if (getArguments() != null) {
            mDirectory = getArguments().getString(ARG_DIRECTORY);
            mDirectory = mDirectory.replaceAll("//", "/");
            setTitle(this.mDirectory);

            mConnectionId = getArguments().getLong(Constants.ARG_CONNECTION_ID);

            try {
                mConnection = sshConnectionRepository.queryForId(mConnectionId);
            } catch (SQLException e) {
                Log.e("WTF", e.getMessage(), e);
            }
        }
    }

    public static double tenths(double value)
    {
        return Math.round(value * 10)/10.0;
    }

    public static String formatBytes(long bytes)
    {
        if (bytes < 1024)
            return bytes+" bytes";
        if (bytes < 1024*1024)
            return tenths(bytes/1024.0) + " KB";
        if (bytes < 1024*1024*1024)
            return tenths(bytes/1024.0/1024.0) + " MB";
        if (bytes < 1024*1024*1024*1024)
            return tenths(bytes/1024.0/1024.0/1024.0) + " GB";
        if (bytes < 1024*1024*1024*1024*1024)
            return tenths(bytes/1024.0/1024.0/1024.0/1024.0) + " TB";
        if (bytes < 1024*1024*1024*1024*1024*1024)
            return tenths(bytes/1024.0/1024.0/1024.0/1024.0/1024.0) + " PB";

        return "Epic";
    }

    private boolean mShowHiddenFiles = false;

    private FilesystemEntry newEntry(String filename, ChannelSftp.LsEntry lsEntry,
                                     ChannelSftp sftp, SFTPConnectionThread sftpThread,
                                     List<FilesystemEntry> directoriesToCheck)
    {
        SftpATTRS attrs = lsEntry.getAttrs();
        final FilesystemEntry candidateEntry = new FilesystemEntry(mConnection, filename);
        FileManagerCache.instance().registerEntry(candidateEntry);

        if (attrs.isLink()) {
            // Follow the symlink
            try {
                String destination = sftp.readlink(filename);

                if (destination != null) {
                    SftpATTRS linkDest = sftp.lstat(destination);
                    attrs = linkDest;
                    candidateEntry.symLinkDestination = destination;
                }

            } catch (SftpException e) {
                Log.e("WTF", e.getMessage(), e);
            }

            candidateEntry.isSymLink = true;
        }

        candidateEntry.isDirectory = attrs.isDir();

        if (candidateEntry.isDirectory) {
            candidateEntry.mimetype = "application/x-directory";

            if (false && candidateEntry.subDirectoryCount == -1 && candidateEntry.subFileCount == -1) {

                candidateEntry.subDirectoryCount = 0;
                candidateEntry.subFileCount = 0;

                // Determine what the subdir counts are, but do it out of band
                // to not block the directory listing

                sftpThread.queueCommand(new SFTPConnectionThread.CommandRunner() {
                    @Override
                    public void run(ChannelSftp sftp) {
                        Vector results = null;
                        try {
                            results = sftp.ls(candidateEntry.fullPath);
                        } catch (SftpException e) {
                            Log.e("WTF", e.getMessage(), e);
                            return;
                        }

                        int dirs = 0, files = 0;
                        for (Object o : results) {
                            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) o;

                            if (entry.getAttrs().isDir())
                                ++dirs;
                            else
                                ++files;
                        }

                        candidateEntry.subFileCount = files;
                        candidateEntry.subDirectoryCount = dirs;

                        Activity activity = getActivity();

                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (!FilesystemDirectoryFragment.this.isVisible())
                                        return;

                                    mAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                });
            }

        } else {

            if (candidateEntry.mimetype == null) {
                candidateEntry.mimetype = "application/x-unknown";

                directoriesToCheck.add(candidateEntry);
            }
        }

        candidateEntry.fileSize = attrs.getSize();
        candidateEntry.permissions = attrs.getPermissionsString();

        return candidateEntry;
    }

    private void doRefresh(final SSHConnectionThread thread)
    {
        final SFTPConnectionThread sftpThread = thread.getMainSftp();

        if (sftpThread == null)
            return;

        sftpThread.queueCommand(new SFTPConnectionThread.CommandRunner() {
            @Override
            public void run(ChannelSftp sftp) {
                final FilesystemDirectoryFragment fragment = FilesystemDirectoryFragment.this;
                String directory = fragment.mDirectory;
                List<FilesystemEntry> entries = new ArrayList<FilesystemEntry>();
                final List<FilesystemEntry> directoriesToCheck = new ArrayList<FilesystemEntry>();

                try {

                    sftp.cd(fragment.mDirectory);
                    Vector v = sftp.ls(".");

                    Log.i("FYI", "Iterating directory at "+fragment.mDirectory);


                    for (Object o : v) {
                        ChannelSftp.LsEntry sftpEntry = (ChannelSftp.LsEntry)o;

                        if (".".equals(sftpEntry.getFilename()) || "..".equals(sftpEntry.getFilename())) {
                            continue;
                        }

                        if (!fragment.mShowHiddenFiles && sftpEntry.getFilename().indexOf(".") == 0) {
                            continue;
                        }

                        SftpATTRS attrs = sftpEntry.getAttrs();

                        String filename = (mDirectory+"/"+sftpEntry.getFilename()).replaceAll("//", "/");

                        FilesystemEntry entry =
                                FileManagerCache.instance().getEntry(mConnection.getId(), filename);

                        if (entry == null) {
                            entry = newEntry(filename, sftpEntry, sftp, sftpThread, directoriesToCheck);
                        }

                        entries.add(entry);
                    }

                } catch (SftpException e) {
                    Log.e("WTF", e.getMessage(), e);
                }

                // Sort everything

                Collections.sort(entries, new Comparator<FilesystemEntry>() {
                    @Override
                    public int compare(FilesystemEntry lhs, FilesystemEntry rhs) {
                        if (lhs.isDirectory && !rhs.isDirectory)
                            return -1;
                        else if (!lhs.isDirectory && rhs.isDirectory)
                            return 1;
                        else {
                            return lhs.name.compareTo(rhs.name);
                        }
                    }
                });

                // Finalize the entries

                final FilesystemEntry[] finalEntries = new FilesystemEntry[entries.size()];
                entries.toArray(finalEntries);
                entries = null;

                // Do it now when we are loading anyway
                System.gc();

                // The return volley back to the UI thread, bitch!

                fragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (fragment.mProgressBar != null) {
                            Log.i("WTF", "Clearing throbber");
                            fragment.mProgressBar.setProgress(100);
                            fragment.mProgressBar.setMax(100);
                            mProgressBar.setVisibility(View.INVISIBLE);
                        } else {
                            Log.e("WTF", "No progress bar to speak of");
                        }

                        fragment.mAdapter = new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return finalEntries.length;
                            }

                            @Override
                            public Object getItem(int position) {
                                return finalEntries[position];
                            }

                            @Override
                            public long getItemId(int position) {
                                return position;
                            }

                            class ViewBag {
                                public TextView nameView;
                                public ImageView iconView;
                                public TextView detailsView;
                                public TextView moreDetailsView;
                                public TextView inlineDetailsView;
                            }

                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                if (convertView == null) {
                                    LayoutInflater inflater = getActivity().getLayoutInflater();
                                    View view = inflater.inflate(R.layout.list_filesystem_entry, parent, false);
                                    ViewBag bag = new ViewBag();

                                    bag.nameView = (TextView)view.findViewById(R.id.name);
                                    bag.iconView = (ImageView)view.findViewById(R.id.image);
                                    bag.detailsView = (TextView)view.findViewById(R.id.details);
                                    bag.moreDetailsView = (TextView)view.findViewById(R.id.moreDetails);
                                    bag.inlineDetailsView = (TextView)view.findViewById(R.id.inline_details);

                                    view.setTag(bag);

                                    convertView = view;
                                }

                                FilesystemEntry entry = finalEntries[position];
                                ViewBag bag = (ViewBag)convertView.getTag();

                                bag.inlineDetailsView.setText("");

                                if (entry.isDirectory) {

                                    bag.iconView.setImageResource(R.drawable.folder);
                                    bag.nameView.setText(entry.name+"/");

                                    if (entry.subFileCount >= 0 && entry.subDirectoryCount >= 0) {
                                        bag.inlineDetailsView.setText(entry.subDirectoryCount+" dir(s) • "+entry.subFileCount+" file(s)");
                                    }
                                } else {
                                    bag.iconView.setImageResource(R.drawable.mime_text_plain);
                                    bag.nameView.setText(entry.name);
                                }

                                if (entry.isDirectory) {
                                    bag.detailsView.setText("Directory");
                                } else {
                                    bag.detailsView.setText(formatBytes(entry.fileSize) + " • " + entry.mimetype);
                                }

                                if (entry.isSymLink) {
                                    bag.moreDetailsView.setText("=> "+entry.symLinkDestination);
                                } else {
                                    bag.moreDetailsView.setText(entry.permissions);
                                }


                                return convertView;
                            }
                        };

                        /*
                        fragment.mAdapter = new ArrayAdapter<FilesystemEntry>(
                                fragment.getActivity(),
                                android.R.layout.simple_list_item_1,
                                android.R.id.text1,
                                finalEntries);
                        */

                        // In the case that we don't have mAdapterView yet, the onCreate does not
                        // blow away this adapter, so we're good.

                        if (mAdapterView != null) {
                            mAdapterView.setAdapter(mAdapter);
                        }
                    }
                });

                // Hey, we're back on our own thread again!
                // Let's throw on some extra cool stuff

                for (FilesystemEntry entry : directoriesToCheck) {
                    String filename = entry.fullPath;
                    // Enqueue an SSH command to determine mimetype of this file
                    Log.i("FYI", "Starting mimetype check on " + filename);
                    try {
                        ChannelExec exec = (ChannelExec) sftp.getSession().openChannel("exec");
                        exec.setCommand("file -Li '" + filename.replaceAll("'", "'\"'\"'") + "' | cut -d: -f2- | cut -d';' -f1");

                        exec.setInputStream(null);
                        exec.setErrStream(null);


                        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
                        exec.setOutputStream(bufferStream);
                        exec.connect();

                        Log.i("FYI", "Waiting for the file command to return...");
                        while (!exec.isClosed()) {
                            try {
                                Thread.sleep(0);
                            } catch (InterruptedException e) {
                                continue;
                            }
                        }

                        String data = new String(bufferStream.toByteArray());

                        Log.i("FYI", "Mimetype for " + filename + ": " + data);
                        entry.mimetype = data.replaceAll("\n", "").trim();

                    } catch (JSchException e) {
                        Log.e("WTF", e.getMessage(), e);
                    }

                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        });
    }


    public void refresh()
    {
        if (mConnection == null)
            return;

        if (mProgressBar != null) {

            Log.i("FYI", "Setting throbber");
            mProgressBar.setProgress(0);
            mProgressBar.setMax(100);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            Log.e("WTF", "No progress bar to be found");
        }

        final SSHConnectionThread thread = SSHConnectionManager.instance().getConnection(getActivity(), mConnection);
        if (thread != null) {

            // Make sure that the main SFTP thread has been initialized. This also allows for
            // user interface prompting in the mean time if the connection has not been established
            // yet.

            thread.ready(new Runnable() {
                @Override
                public void run() {
                    doRefresh(thread);
                }
            });
        }


    }

    public class FilesystemEntry {



        public FilesystemEntry(SSHConnection connection, String fullPath)
        {
            this.connection = connection;
            this.fullPath = fullPath;

            if (fullPath.indexOf("/") >= 0) {
                this.name = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            } else {
                this.name = this.fullPath;
            }

        }

        public SSHConnection connection;

        @Override
        public String toString() {
            if (this.isDirectory)
                return name+"/";

            return name;
        }

        public String name;
        public String mimetype;
        public String fullPath;

        public boolean isDirectory;
        public int subDirectoryCount = -1;
        public int subFileCount = -1;

        public long fileSize;
        public String permissions;
        public boolean isSymLink = false;
        public String symLinkDestination = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_filesystemdirectory, container, false);
        mAdapterView = (AdapterView<ListAdapter>)view.findViewById(android.R.id.list);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        registerForContextMenu(mAdapterView);

        // Probably not loaded yet, but you never know.
        // And if it is, there's no other chance to show it
        // since we *just* made the mAdapterView, so this is
        // *important*

        if (mAdapterView != null && mAdapter != null) {
            mAdapterView.setAdapter(mAdapter);
        }

        // Set OnItemClickListener so we can be notified on item clicks
        mAdapterView.setOnItemClickListener(this);
        mAdapterView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mContextItem = (FilesystemEntry)mAdapterView.getItemAtPosition(position);
                return false;
            }
        });

        if (mConnection != null) {
            this.refresh();
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (Delegate) activity;
        } catch (ClassCastException e) {
            //throw new ClassCastException(activity.toString()
            //    + " must implement Delegate");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {

            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.VISIBLE);
            }

            FilesystemEntry entry = (FilesystemEntry)mAdapterView.getItemAtPosition(position);
            if (entry.isDirectory)
                mListener.onOpenDirectory(entry);
            else
                mListener.onOpenFile(entry);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mAdapterView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    public interface Delegate {
        public void onOpenDirectory(FilesystemEntry entry);
        public void onOpenFile(FilesystemEntry entry);
    }
}
