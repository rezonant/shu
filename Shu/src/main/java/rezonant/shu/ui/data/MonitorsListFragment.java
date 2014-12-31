package rezonant.shu.ui.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import rezonant.shu.R;
import rezonant.shu.state.data.Action;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link rezonant.shu.ui.ToolListActivity}
 * in two-pane mode (on tablets) or a {@link rezonant.shu.ui.ToolDetailActivity}
 * on handsets.
 */
public class MonitorsListFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ACTION_ID = "action_id";
	public static final String ARG_CHOOSER_MODE = "chooser_mode";

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public MonitorsListFragment() {

	}

	private boolean isChooserMode = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		if (getArguments().containsKey(ARG_CHOOSER_MODE)) {
			// Enter chooser mode, returning the selected item
			// instead of entering the edit activity for it
			isChooserMode = true;
		}

		actionsRepository = Action.createDao(getActivity());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add("Phew!");
		super.onCreateOptionsMenu(menu, inflater);
	}

	private Dao<Action,Long> actionsRepository;

	@Override
	public void onResume()
	{
		refreshView(getView());
		super.onResume();
	}

	private void refreshView(View view)
	{

		try {
			ListView list = (ListView)view.findViewById(R.id.actions_list);
			List<Action> data = actionsRepository.queryForAll();

			Action newAction = new Action();
			newAction.setId(0);
			newAction.setName("Add...");
			data.add(newAction);

			Action[] items = new Action[data.size()];
			data.toArray(items);

			Log.i("FYI", "XXXXXXXXXXXXXXXXXXXXXX There were "+data.size()+" items in the actions repo");



			list.setAdapter(new ArrayAdapter<Action>(getActivity(),
					android.R.layout.simple_list_item_1,
					android.R.id.text1, items));
		} catch (SQLException e) {
			Log.e("Database", "SQL exception", e);
		}

	}

	private class ItemClickListener implements AdapterView.OnItemClickListener {
		public ItemClickListener(MonitorsListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private MonitorsListFragment fragment;
		private ListView listView;

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM CLICKED! **************** "+position+" ID: "+id);


			Action action = (Action)listView.getItemAtPosition(position);

			if (action == null) {
				Log.e("WTF", "Action was not found!!");
				return;
			}

			Log.i("FYI", "PICKED ######### "+action.toString());
			fragment.itemClicked(action);
		}
	}

	public static final int CONTEXT_MENU_DELETE = 1;
	public static final int CONTEXT_MENU_SHARE = 1;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.setHeaderTitle("Actions");
		menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Delete");
		menu.add(Menu.NONE, CONTEXT_MENU_SHARE, Menu.NONE, "Share");

		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
		public ItemLongClickListener(MonitorsListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private MonitorsListFragment fragment;
		private ListView listView;

		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM LONG CLICKED! ****************");

			Action action = (Action)listView.getItemAtPosition(position);
			fragment.contextMenuAction = action;

			return false;
		}
	}

	private Action contextMenuAction;

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		if (item.getItemId() == CONTEXT_MENU_DELETE) {
			Log.i("FYI", "DELETING action with id "+contextMenuAction.getId());
			try {
				actionsRepository.delete(contextMenuAction);
				contextMenuAction = null;
				this.refreshView(getView());
			} catch (SQLException e) {
				Log.e("Database", "SQL exception", e);
			}
			return true;
		}

		return false;
	}

	private void itemClicked(Action action)
	{
		if (isChooserMode) {

			Intent resultIntent = new Intent();

			resultIntent.getExtras().putLong(ARG_ACTION_ID, action.getId());
			getActivity().setResult(Activity.RESULT_OK, resultIntent);
			getActivity().finish();

			return;
		}

		Intent detailIntent = new Intent(this.getActivity(), EditActionActivity.class);
		detailIntent.putExtra(EditActionFragment.ARG_ACTION_ID, action.getId());

		startActivity(detailIntent);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {


		View rootView = inflater.inflate(R.layout.fragment_actions_list, container, false);

		ListView listView = (ListView)rootView.findViewById(R.id.actions_list);

		registerForContextMenu(listView);

		listView.setOnItemClickListener(new ItemClickListener(this, listView));
		listView.setOnItemLongClickListener(new ItemLongClickListener(this, listView));

		refreshView(rootView);

		return rootView;
	}
}
