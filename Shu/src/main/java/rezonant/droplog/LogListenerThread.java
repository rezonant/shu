package rezonant.droplog;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.logentries.android.AndroidLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by liam on 8/6/14.
 */
public class LogListenerThread {
    private LogListenerThread() {

    }

    private Thread thread = null;
    private static LogListenerThread theInstance;

    public static void initialize(Context application)
    {
        LogListenerThread listener = instance();
        listener.start(application);
    }

    public static LogListenerThread instance()
    {
        if (theInstance == null)
            theInstance = new LogListenerThread();
        return theInstance;
    }

    public void start(final Context application)
    {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("rezonant.droplog", "Thread start.");

                while (true) {
                    try {
                        Log.i("rezonant.droplog", "Starting logcat monitor.");
                        Process process = Runtime.getRuntime().exec("logcat");
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));

                        AndroidLogger logger = AndroidLogger.getLogger(application, "611677ad-32ca-4352-ba73-9af01714184d", true);
                        StringBuilder log = new StringBuilder();
                        String line = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            if ("".equals(line.trim()))
                                continue;



                            /*
                            line = line.replaceAll("  ", " ");  // normalize spaces    <-- of this?
                            String[] fields = line.split(" ",5);  // TODO: " +" should work instead

                            String date = fields[0];
                            String time = fields[1];
                            String appString = fields[2];
                            String tag = fields[3];
                            String message = "";

                            if (fields.length > 4)
                                message = fields[4];

                            String[] appParts = appString.split("/", 2);
                            String appName = appParts.length > 1? appParts[1] : appString;

                            message = appName+"::"+tag+" "+message;

                            if (tag == null) {
                                continue;
                            }

                            if ("I".equals(tag.charAt(0))) {
                                // Info
                                //logger.info(message);
                            } else if ("W".equals(tag.charAt(0))) {
                                // Info
                                //logger.warn(message);
                            } else if ("E".equals(tag.charAt(0))) {
                                // Info
                                //logger.error(message);
                            }
                            */
                        }

                        Log.w("rezonant.droplog", "Logcat ended. Restarting...");

                        // do stuff with the new line
                    } catch (IOException e) {
                        Log.w("rezonant.droplog", "IOException during logcat", e);
                    }
                }
            }
        });
        thread.start();
    }

    public void stop()
    {
    }
}
