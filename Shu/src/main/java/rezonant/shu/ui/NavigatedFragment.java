package rezonant.shu.ui;

import android.support.v4.app.Fragment;

import rezonant.shu.ui.NavigationController;

/**
 * Created by liam on 7/27/14.
 */
public class NavigatedFragment extends Fragment implements NavigableFragment {

    private String title;
    public String getTitle()
    {
        return title;
    }

    @Override
    public void onResume() {

        if (this.getActivity() instanceof NavigationController) {
            NavigationController controller = (NavigationController)this.getActivity();

            controller.fragmentResumed(this);
            controller.setTitle(this.getTitle());
        }

        super.onResume();
    }

    /**
     * In default getTitle() implementation, sets the title for this fragment. May not have any
     * effect if a getTitle() overridden implementation is used.
     *
     * @param title\ The title to set
     */
    protected void setTitle(String title) {
        this.title = title;

        if (this.getActivity() instanceof NavigationController) {
            NavigationController controller = (NavigationController)this.getActivity();
            controller.setTitle(this.title);
        }
    }
}
