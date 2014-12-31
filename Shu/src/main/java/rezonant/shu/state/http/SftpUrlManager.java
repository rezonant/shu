package rezonant.shu.state.http;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Just manages the translation between SFTP URLs and HTTP ones when using the builtin HTTP relay
 * server (rezonant.httprelay). This allows us to prevent allocating multiple HTTP streamable
 * resources when the user requests the same file multiple times.
 *
 * SFTP URLs are like:
 *      sftp://user@host[:port]/absolute/path/to/file
 *
 * If port is 22, then the port should not be included, but if it is the penalty is negligible.
 *
 * Created by liam on 8/2/14.
 */
public class SftpUrlManager {

    // Implementation notes:
    // - Applied additional checks and error throws to enforce good memory usage

    private static SftpUrlManager theInstance = null;
    public static SftpUrlManager instance()
    {
        if (theInstance == null) {
            theInstance = new SftpUrlManager();
        }

        return theInstance;
    }

    Map<String,String> urlMap = new HashMap<String,String>();

    public void unregister(String sftpUrl)
    {
        Log.i("FYI", "Unregistering SFTP URL "+sftpUrl);
        if (!urlMap.containsKey(sftpUrl))
            throw new IllegalAccessError("Cannot unregister a URL which is not registered.");

        urlMap.remove(sftpUrl);
    }

    public void register(String sftpUrl, String httpUrl)
    {
        Log.i("FYI", "Registering SFTP URL "+sftpUrl+" as "+httpUrl);
        if (urlMap.containsKey(sftpUrl)) {
            throw new IllegalAccessError("Cannot register this SFTP URL multiple times. If it "+
                    "is appropriate to override this mapping, you must first unregister the "+
                    "previous mapping.");
        }

        urlMap.put(sftpUrl, httpUrl);
    }

    /**
     * Get the HTTP url of a given SFTP url. Returns null if there is no registered
     * HTTP url for the given SFTP url.
     *
     * @param sftpUrl The SFTP URL being requested
     * @return Null if none registered, or the HTTP URL representing the SFTP resource.
     */
    public String getHttpUrl(String sftpUrl)
    {
        if (urlMap.containsKey(sftpUrl)) {

            Log.i("FYI", "Using previously mapped HTTP URL (for SFTP URL "+sftpUrl+"): "+urlMap.get(sftpUrl));

            return urlMap.get(sftpUrl);
        }

        return null;
    }
}
