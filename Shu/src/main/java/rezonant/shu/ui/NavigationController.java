package rezonant.shu.ui;

import android.support.v4.app.Fragment;

/**
 * Created by liam on 7/27/14.
 */
public interface NavigationController {
    public void fragmentResumed(Fragment fragment);
    public void setTitle(String title);
}
