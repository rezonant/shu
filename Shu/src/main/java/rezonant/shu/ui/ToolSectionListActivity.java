package rezonant.shu.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import rezonant.shu.R;
import rezonant.shu.ui.global.AboutShuActivity;
import rezonant.shu.ui.global.SettingsActivity;
import rezonant.shu.ui.session.SessionsListActivity;


/**
 * An activity representing a list of Tools. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ToolDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ToolListFragment} and the item details
 * (if present) is a {@link ToolDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ToolListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ToolSectionListActivity extends FragmentActivity
        implements ToolSectionListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("hrm", "Making ToolSection list!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_section_list);

        if (findViewById(R.id.tool_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ToolSectionListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.tool_section_list))
                    .setActivateOnItemClick(true);
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

    /**
     * Callback method from {@link ToolListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {

        if (id == "settings") {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        if (id == "systemStatus") {
            startActivity(new Intent(this, SessionsListActivity.class));
            return;
        }

        if (id == "about") {
            startActivity(new Intent(this, AboutShuActivity.class));
            return;
        }

        Log.i("Interesting", "The selected ID is " + id);
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ToolListFragment.ARG_MENU_ID, id.toUpperCase());

            ToolListFragment fragment = new ToolListFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.tool_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ToolListActivity.class);
            detailIntent.putExtra(ToolListFragment.ARG_MENU_ID, id);
            startActivity(detailIntent);
        }
    }
}
