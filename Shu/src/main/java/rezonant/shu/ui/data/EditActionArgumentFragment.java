package rezonant.shu.ui.data;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Arrays;

import rezonant.shu.R;
import rezonant.shu.state.data.ActionArgument;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link rezonant.shu.ui.ToolListActivity}
 * in two-pane mode (on tablets) or a {@link rezonant.shu.ui.ToolDetailActivity}
 * on handsets.
 */
public class EditActionArgumentFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ACTION_ARGUMENT_ID = "action_argument_id";

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EditActionArgumentFragment() {
	}

	Dao<ActionArgument,Long> actionArgumentRepository;

	public void save()
	{
		try {
			if (actionArgument.getId() == 0) {
				actionArgumentRepository.create(actionArgument);
				actionArgumentRepository.refresh(actionArgument);
			} else
				actionArgumentRepository.update(actionArgument);
		} catch (SQLException e) {
			Log.e("WTF", e.getMessage(), e);
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
			return;
		}

		Log.i("Ok", "Saved action "+actionArgument.getId()+" successfully");

		getActivity().finish();
	}

	private class SelectCommandButtonListener implements View.OnClickListener {
		public SelectCommandButtonListener(EditActionArgumentFragment fragment) {
			this.fragment = fragment;
		}

		private EditActionArgumentFragment fragment;

		@Override
		public void onClick(View view) {
			fragment.onClickSelectCommand();
		}
	}

	@Override
	public void onPause()
	{
		onClickSave();
		super.onPause();
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

		actionArgumentRepository = ActionArgument.createDao(this.getActivity());

		long actionArgumentId = 0;

		if (getArguments().containsKey(ARG_ACTION_ARGUMENT_ID))
			actionArgumentId = getArguments().getLong(ARG_ACTION_ARGUMENT_ID);

		Log.i("FYI", "##################### Edit Action Argument starting with ID: "+actionArgumentId);

		if (actionArgumentId == 0) {
			this.actionArgument = new ActionArgument();
		} else {
			try {
				this.actionArgument = actionArgumentRepository.queryForId(actionArgumentId);
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
		refreshUI(null, this.actionArgument);
	}

	private int getIndexForType(String type)
	{
		String[] actionTypes = getResources().getStringArray(R.array.activity_edit_action_argument_types);

		return Arrays.asList(actionTypes).indexOf(type);
	}

	private String getTypeForIndex(int index)
	{
		String[] actionTypes = getResources().getStringArray(R.array.activity_edit_action_argument_types);

		if (index >= actionTypes.length)
			return "?";

		return actionTypes[index];
	}

	private void refreshUI(View view, ActionArgument actionArgument)
	{
		if (view == null)
			view = this.getView();

		EditText nameInput = (EditText)view.findViewById(R.id.label);
		nameInput.setText(actionArgument.getLabel());

		EditText helpInput = (EditText)view.findViewById(R.id.help);
		helpInput.setText(actionArgument.getHelp());

        EditText defaultInput = (EditText)view.findViewById(R.id.help);
        defaultInput.setText(actionArgument.getDefaultValue());

		Spinner typeSpinner = (Spinner)view.findViewById(R.id.type);
		int spinnerSel = getIndexForType(actionArgument.getType());
		typeSpinner.setSelection(spinnerSel);

		CheckBox isRequiredCheckbox = (CheckBox)view.findViewById(R.id.isRequired);
		isRequiredCheckbox.setChecked(actionArgument.isRequired());


	}

	private void syncToObject()
	{
		View view = getView();

		EditText nameInput = (EditText)view.findViewById(R.id.label);
		actionArgument.setLabel(nameInput.getText().toString());

        EditText helpInput = (EditText)view.findViewById(R.id.help);
        actionArgument.setHelp(helpInput.getText().toString());

        EditText defaultInput = (EditText)view.findViewById(R.id.defaultValue);
        actionArgument.setDefaultValue(defaultInput.getText().toString());


		Spinner typeSpinner = (Spinner)view.findViewById(R.id.type);
		actionArgument.setType(getTypeForIndex(typeSpinner.getSelectedItemPosition()));

		CheckBox isRequiredCheckbox = (CheckBox)view.findViewById(R.id.isRequired);

		actionArgument.setRequired(isRequiredCheckbox.isChecked());
	}

	private ActionArgument actionArgument;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_edit_action_argument, container, false);

		refreshUI(rootView, this.actionArgument);
		return rootView;
	}
}
