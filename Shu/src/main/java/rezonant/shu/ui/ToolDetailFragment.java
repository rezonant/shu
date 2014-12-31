package rezonant.shu.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import rezonant.shu.R;
import rezonant.shu.ui.listview.RealizedListItem;
import rezonant.shu.ui.menus.Menus;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link ToolListActivity}
 * in two-pane mode (on tablets) or a {@link ToolDetailActivity}
 * on handsets.
 */
public class ToolDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_MENU_ID = "menu_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private RealizedListItem mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ToolDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String menuId = null;
        String itemId = null;

        if (getArguments().containsKey(ARG_ITEM_ID))
            itemId = getArguments().getString(ARG_ITEM_ID);

        if (getArguments().containsKey(ARG_MENU_ID))
            menuId = getArguments().getString(ARG_MENU_ID);

        // Load the dummy content specified by the fragment
        // arguments. In a real-world scenario, use a Loader
        // to load content from a content provider.
        mItem = (RealizedListItem)Menus.find(menuId, itemId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tool_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.tool_detail)).setText(mItem.getRealizedLabel());
        }

        return rootView;
    }
}
