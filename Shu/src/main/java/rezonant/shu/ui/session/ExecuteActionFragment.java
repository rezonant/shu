package rezonant.shu.ui.session;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.Action;
import rezonant.shu.state.data.ActionArgument;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.data.Session;
import rezonant.shu.state.data.SessionAction;
import rezonant.shu.ui.NavigatedFragment;

/**
 * Created by liam on 6/30/14.
 */
public class ExecuteActionFragment extends NavigatedFragment {


    @Override
    public String getTitle() {
        if (this.sessionAction != null)
            return this.sessionAction.getTitle()+" - "+this.sessionAction.getSession().getName();

        return "Execute Action";
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		this.setHasOptionsMenu(true);

		this.sessionActionID = getArguments().getLong(Constants.ARG_SESSION_ACTION_ID);
		this.actionRepository = Action.createDao(getActivity());
		this.sessionActionRepository = SessionAction.createDao(getActivity());

		Log.i("FYI", "The Eagle Has Landed (ExecuteActionFragment) with action ID "+sessionActionID);
		try {
			this.sessionAction = this.sessionActionRepository.queryForId(this.sessionActionID);
		} catch (SQLException e) {
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();
		}
	}

	public interface Delegate {
		public void actionExecuted(SessionAction action, HashMap<Integer,String> variables, List<SSHConnection> connections);
	}

	private long sessionActionID = 0;
	private Dao<Action,Long> actionRepository;
	private Dao<SessionAction,Long> sessionActionRepository;
	private SessionAction sessionAction;
	private int idBase = 1337;
	private HashMap<Integer, EditText> argumentViews;

	private void addArgumentView(int position, View rootView, ActionArgument argument)
	{
		Log.i("FYI", "Building argument view for "+argument.getLabel());
		LinearLayout layout = (LinearLayout)rootView.findViewById(R.id.layout1);
		LayoutInflater inflater = getActivity().getLayoutInflater();

		View view = inflater.inflate(R.layout.fragment_argument, layout, false);

		TextView labelField = (TextView)view.findViewById(R.id.label);
		EditText valueField = (EditText)view.findViewById(R.id.value);

		labelField.setId(idBase+position+labelField.getId());
		valueField.setId(idBase + position + valueField.getId());

		labelField.setText("($"+(position+1)+") "+argument.getLabel()+": ");
		valueField.setHint(argument.getDefaultValue()+" - "+argument.getHelp());

		argumentViews.put(position, valueField);

		Log.i("FYI", "Attaching argument view for " + argument.getLabel());
		layout.addView(view);
	}

	private void refreshUI(View rootView)
	{

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		Log.i("FYI", "ExecuteActionFragment onCreateView");
		View rootView = inflater.inflate(R.layout.fragment_execute_action, container, false);

		LinearLayout layout = (LinearLayout)rootView.findViewById(R.id.layout1);
		layout.setLayoutParams(new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));



		if (this.sessionAction != null) {
			Log.i("FYI", "Expanding arguments of solid action...");
			argumentViews = new HashMap<Integer,EditText>();
			int position = 0;
			for (ActionArgument arg : this.sessionAction.getAction().getArguments()) {
				addArgumentView(position, rootView, arg);
				++position;
			}

			getActivity().setTitle(sessionAction.getTitle());

			TextView commandLabel = (TextView)rootView.findViewById(R.id.command);
			commandLabel.setText(sessionAction.getAction().getCommand());

			TextView helpLabel = (TextView)rootView.findViewById(R.id.help);
            helpLabel.setText(sessionAction.getAction().getName());

			ListView connectionsListView = (ListView)rootView.findViewById(R.id.connectionsList);
			Session session = this.sessionAction.getSession();
			this.connectionsList = Arrays.asList(session.getConnections());

			Log.i("FYI", "Found "+this.connectionsList.size()+" connections from session");

			connectionsListView.setAdapter(new ArrayAdapter<SSHConnection>(
					getActivity(), android.R.layout.simple_list_item_multiple_choice,
					android.R.id.text1, this.connectionsList));

			Button executeButton = (Button)rootView.findViewById(R.id.execute);

			final ExecuteActionFragment self = this;
			executeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					self.onExecute();
				}
			});
		}

		return rootView;
	}

	List<SSHConnection> connectionsList;

	private void onExecute()
	{
		ListView connectionsListView = (ListView)getView().findViewById(R.id.connectionsList);

        Action action = sessionAction.getAction();

        if (getActivity() instanceof Delegate) {
            List<SSHConnection> connections = new ArrayList<SSHConnection>();

            for (int i = 0, max = connectionsList.size(); i < max; ++i) {
                SSHConnection connection = connectionsList.get(i);
                if (connectionsListView.isItemChecked(i))
                    connections.add(connection);
                continue;
            }

            String commandString = sessionAction.getAction().getCommand();
            HashMap<Integer, String> variableMap = new HashMap<Integer, String>();
            ActionArgument[] args = sessionAction.getAction().getArguments();

            for (int i = 0, max = args.length; i < max; ++i) {
                EditText valueView = argumentViews.get(i);
                String content = valueView.getText().toString();
                ActionArgument arg = args[i];

                if (content == null || "".equals(content)) {
                    // Use default
                    content = arg.getDefaultValue();
                }

                variableMap.put((i + 1), content);
            }

            ((Delegate) getActivity()).actionExecuted(sessionAction, variableMap, connections);

            return;
        }

		Log.w("WTF", "The activity is not a delegate of ExecuteActionFragment");
	}

}
