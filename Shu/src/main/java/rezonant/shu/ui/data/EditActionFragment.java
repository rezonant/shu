package rezonant.shu.ui.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Arrays;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.Action;
import rezonant.shu.state.data.ActionArgument;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link rezonant.shu.ui.ToolListActivity}
 * in two-pane mode (on tablets) or a {@link rezonant.shu.ui.ToolDetailActivity}
 * on handsets.
 */
public class EditActionFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ACTION_ID = "action_id";
	public static final int RESULT_FINISHED = 133701234;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EditActionFragment() {
	}

	Dao<Action,Long> actionRepository;
	Dao<ActionArgument,Long> actionArgumentRepository;

	public void save()
	{
		try {
			if (action.getId() == 0) {
				actionRepository.create(action);
				actionRepository.refresh(action);
			} else
				actionRepository.update(action);
		} catch (SQLException e) {
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
			return;
		}

		Log.i("Ok", "Saved action "+action.getId()+" successfully");
	}

	private class SelectCommandButtonListener implements View.OnClickListener {
		public SelectCommandButtonListener(EditActionFragment fragment) {
			this.fragment = fragment;
		}

		private EditActionFragment fragment;

		@Override
		public void onClick(View view) {
			fragment.onClickSelectCommand();
		}
	}

	@Override
	public void onPause()
	{
		onClickSave();
        finish();

		super.onPause();
	}

    public void finish()
    {
        Intent intent = new Intent();
        intent.putExtra(Constants.ARG_ACTION_ID, action.getId());
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

	@Override
	public void onResume()
	{
		refreshUI(getView(), action);
		super.onResume();
	}

	private void onClickSelectCommand()
	{
		Log.i("Ok", "We are at da edit command shit");
		new AlertDialog.Builder(getActivity())
				.setTitle("Oh noes!")
				.setMessage("Whoot")
				.create()
				.show();
	}


	private void onClickSave()
	{
		syncToObject();
		save();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		actionRepository = Action.createDao(this.getActivity());
		actionArgumentRepository = ActionArgument.createDao(this.getActivity());

		long actionId = 0;

		if (getArguments().containsKey(ARG_ACTION_ID))
			actionId = getArguments().getLong(ARG_ACTION_ID);

		if (actionId == 0) {
			this.action = new Action();
			this.action.setName("");
			this.action.setType("ssh");
			this.action.setUnit("");
			this.action.setCommand("");
			this.action.setIsValue(false);
			this.action.setHasOutput(false);

			try {
				this.actionRepository.create(this.action);
				this.actionRepository.refresh(this.action);
			} catch (SQLException e) {
				new AlertDialog.Builder(getActivity())
						.setTitle(e.getMessage())
						.setMessage(e.toString())
						.create()
						.show();
			}

		} else {
			try {
				this.action = actionRepository.queryForId(actionId);
			} catch (SQLException e) {
				new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
			}
		}
	}

	private void refreshUI()
	{
		refreshUI(null, this.action);
	}

	private int getIndexForType(String type)
	{
		String[] actionTypes = getResources().getStringArray(R.array.activity_edit_action_types);

		return Arrays.asList(actionTypes).indexOf(type);
	}

	private String getTypeForIndex(int index)
	{
		String[] actionTypes = getResources().getStringArray(R.array.activity_edit_action_types);

		if (index >= actionTypes.length)
			return "?";

		return actionTypes[index];
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

	private class ItemClickListener implements AdapterView.OnItemClickListener {
		public ItemClickListener(EditActionFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private EditActionFragment fragment;
		private ListView listView;

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

			ActionArgument actionArgument = (ActionArgument)listView.getItemAtPosition(position);
			fragment.onItemClicked(actionArgument);
		}
	}

	private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
		public ItemLongClickListener(EditActionFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private EditActionFragment fragment;
		private ListView listView;

		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM LONG CLICKED! ****************");

			ActionArgument actionArgument = (ActionArgument)listView.getItemAtPosition(position);
			fragment.contextMenuArgument = actionArgument;

			return false;
		}
	}

	private void onItemClicked(ActionArgument argument)
	{
		Intent detailIntent = new Intent(this.getActivity(), EditActionArgumentActivity.class);
		detailIntent.putExtra(EditActionArgumentFragment.ARG_ACTION_ARGUMENT_ID, argument.getId());
		startActivity(detailIntent);
	}

	private Action contextMenuAction;

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		if (item.getItemId() == CONTEXT_MENU_DELETE) {
            if (contextMenuArgument == null) {
                Log.e("WTF", "No context menu action argument! Cannot proceed with delete operation.");
                return false;
            }

			Log.i("FYI", "DELETING action argument with id "+contextMenuArgument.getId());
			try {
				this.action.removeArgument(contextMenuArgument);
				actionRepository.update(this.action);
				actionRepository.refresh(this.action);

				actionArgumentRepository.delete(contextMenuArgument);
				contextMenuArgument = null;
				this.refreshUI(getView(), action);
			} catch (SQLException e) {

				Log.e("Database", "SQL exception", e);

				new AlertDialog.Builder(getActivity())
						.setTitle(e.getMessage())
						.setMessage(e.toString())
						.create()
						.show();
			}
			return true;
		}

		return false;
	}

	private void refreshUI(View view, Action action)
	{
		if (view == null)
			view = this.getView();


		Log.i("FYI", "Refreshing Action's arguments list now");

		try {
			actionRepository.refresh(this.action);
		} catch (SQLException e) {

			Log.e("Database", "SQL exception", e);

			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
		}

		ListView argumentsListView = (ListView)view.findViewById(R.id.arguments_list);
		for (ActionArgument arg : action.getArguments()) {
			Log.i("FYI", arg.getId()+" : "+arg.getLabel());
		}

		argumentsListView.setAdapter(new ArrayAdapter<ActionArgument>(getActivity(),
				android.R.layout.simple_list_item_1, android.R.id.text1, action.getArguments()));

		EditText nameInput = (EditText)view.findViewById(R.id.name);
		nameInput.setText(action.getName());

		Spinner typeSpinner = (Spinner)view.findViewById(R.id.type);
		int spinnerSel = getIndexForType(action.getType());
        Log.i("FYI", "getIndexForType: "+action.getType()+" = "+spinnerSel);
		typeSpinner.setSelection(spinnerSel);

		CheckBox hasOutputCheckbox = (CheckBox)view.findViewById(R.id.hasOutput);
		CheckBox isValueCheckbox = (CheckBox)view.findViewById(R.id.isValue);

		hasOutputCheckbox.setChecked(action.hasOutput());
		isValueCheckbox.setChecked(action.isValue());

		EditText commandField = (EditText)view.findViewById(R.id.command);
		commandField.setText(action.getCommand());

		EditText unitField = (EditText)view.findViewById(R.id.unit);
		unitField.setText(action.getUnit());
	}

	private void syncToObject()
	{
		View view = getView();

		EditText nameInput = (EditText)view.findViewById(R.id.name);
        String name = nameInput.getText().toString();


        if ("".equals(name) || name == null)
            action.setName("Unnamed Action");
        else
		    action.setName(name);

		Spinner typeSpinner = (Spinner)view.findViewById(R.id.type);
        String type = getTypeForIndex(typeSpinner.getSelectedItemPosition());
        type = type.toLowerCase();
		action.setType(type);

		CheckBox hasOutputCheckbox = (CheckBox)view.findViewById(R.id.hasOutput);
		CheckBox isValueCheckbox = (CheckBox)view.findViewById(R.id.isValue);

		action.setHasOutput(hasOutputCheckbox.isChecked());
		action.setIsValue(isValueCheckbox.isChecked());

		EditText commandField = (EditText)view.findViewById(R.id.command);
		action.setCommand(commandField.getText().toString());

		EditText unitField = (EditText)view.findViewById(R.id.unit);
		action.setUnit(unitField.getText().toString());
	}

	private ActionArgument contextMenuArgument;

	private Action action;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_edit_action, container, false);

		Button selectCmdBtn = (Button)rootView.findViewById(R.id.selectCmdBtn);
		selectCmdBtn.setOnClickListener(new SelectCommandButtonListener(this));

		ListView argumentsListView = (ListView)rootView.findViewById(R.id.arguments_list);
		registerForContextMenu(argumentsListView);

		argumentsListView.setOnItemClickListener(new ItemClickListener(this, argumentsListView));
        argumentsListView.setOnItemLongClickListener(new ItemLongClickListener(this, argumentsListView));

		ImageButton addArgumentBtn = (ImageButton)rootView.findViewById(R.id.addArgumentBtn);
		addArgumentBtn.setOnClickListener(new AddArgumentButtonListener(this));

		refreshUI(rootView, this.action);
		return rootView;
	}

	private class AddArgumentButtonListener implements View.OnClickListener {
		public AddArgumentButtonListener(EditActionFragment editActionFragment) {
			fragment = editActionFragment;
		}

		private EditActionFragment fragment;


		@Override
		public void onClick(View view) {
			fragment.onAddArgumentClick();
		}
	}

	private void onAddArgumentClick()
	{
		ActionArgument newArgument = new ActionArgument();

		newArgument.setAction(this.action);

		try {
			actionArgumentRepository.create(newArgument);
			actionRepository.refresh(action);
		} catch (SQLException e) {

			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
			return;
		}

		Log.i("FYI", "+++++++++ Action argument being added is "+newArgument.getId());

		Intent detailIntent = new Intent(this.getActivity(), EditActionArgumentActivity.class);
		detailIntent.putExtra(EditActionArgumentFragment.ARG_ACTION_ARGUMENT_ID, newArgument.getId());
		startActivity(detailIntent);
	}
}
