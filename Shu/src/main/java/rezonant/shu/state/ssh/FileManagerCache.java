package rezonant.shu.state.ssh;

import android.util.ArrayMap;

import java.util.HashMap;
import java.util.Map;

import rezonant.shu.ui.viewers.FilesystemDirectoryFragment;

/**
 * Created by liam on 7/28/14.
 */
public class FileManagerCache {

    private FileManagerCache()
    {
    }

    public static FileManagerCache instance()
    {
        if (theInstance == null) {
            theInstance = new FileManagerCache();
        }

        return theInstance;
    }

    private static FileManagerCache theInstance;

    private Map<String,FilesystemDirectoryFragment.FilesystemEntry> entries =
            new HashMap<String, FilesystemDirectoryFragment.FilesystemEntry>();

    public FilesystemDirectoryFragment.FilesystemEntry getEntry(long connectionId, String filesystemPath)
    {
        String key = connectionId+":"+filesystemPath;
        if (entries.containsKey(key))
            return entries.get(key);

        return null;
    }


    public void registerEntry(FilesystemDirectoryFragment.FilesystemEntry entry)
    {
        entries.put(entry.connection.getId()+":"+entry.fullPath, entry);
    }
}
