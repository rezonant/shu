package rezonant.shu.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import rezonant.shu.R;
import rezonant.shu.ui.listview.ListItem;
import rezonant.shu.ui.listview.RealizedListItem;
import rezonant.shu.ui.menus.Menus;


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
public class ToolListActivity extends FragmentActivity
        implements ToolListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);

        if (false && findViewById(R.id.tool_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ToolListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.tool_list))
                    .setActivateOnItemClick(true);
        }

        String menuName = getIntent().getStringExtra(ToolListFragment.ARG_MENU_ID);

        Bundle arguments = new Bundle();
        arguments.putString(ToolListFragment.ARG_MENU_ID, menuName);

        ListItem[] menu = Menus.realize(Menus.SECTIONS, getResources());
        RealizedListItem item = (RealizedListItem)Menus.find(menu, menuName);
        setTitle(item.getRealizedLabel());

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            ToolListFragment fragment = new ToolListFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.tool_list, fragment)
                    .commit();
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

    /**
     * Callback method from {@link ToolListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ToolDetailFragment.ARG_ITEM_ID, id);
            ToolDetailFragment fragment = new ToolDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.tool_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ToolDetailActivity.class);
            detailIntent.putExtra(ToolDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
}
