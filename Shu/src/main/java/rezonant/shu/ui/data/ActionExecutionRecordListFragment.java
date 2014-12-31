package rezonant.shu.ui.data;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;


import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.Session;
import rezonant.shu.ui.dummy.DummyContent;
import rezonant.shu.ui.session.CommandResultFragment;
import rezonant.shu.ui.session.SessionViewActivity;
import rezonant.shu.state.ssh.SSHCommand;
import rezonant.shu.state.ActionExecutionRecord;
import rezonant.shu.state.SessionState;
import rezonant.shu.state.SessionStateManager;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ActionExecutionRecordListFragment extends Fragment implements AbsListView.OnItemClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    // TODO: Rename and change types of parameters
    public static ActionExecutionRecordListFragment newInstance(String param1, String param2) {
        ActionExecutionRecordListFragment fragment = new ActionExecutionRecordListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ActionExecutionRecordListFragment() {
    }

    private long sessionId = 0;
    private Session session;
    private String mode = "all";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            if (getArguments().containsKey(Constants.ARG_SESSION_ID))
                this.sessionId = getArguments().getLong(Constants.ARG_SESSION_ID);

            if (getArguments().containsKey("mode")) {
                this.mode = getArguments().getString("mode");
            }
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARG_SESSION_ID))
                this.sessionId = savedInstanceState.getLong(Constants.ARG_SESSION_ID);
        }

        if (this.sessionId != 0) {
            Dao<Session,Long> sessionRepository = Session.createDao(getActivity());
            try {
                this.session = sessionRepository.queryForId(this.sessionId);
            } catch (SQLException e) {
                Log.e("WTF", e.getMessage(), e);
                return;
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_actionexecutionrecord, container, false);

        // Set OnItemClickListener so we can be notified on item clicks

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);

        // Refresh

        refresh();

        return view;
    }

    public void refresh()
    {

        if (this.session == null)
            return;

        // Set the adapter

        View view = getView();
        SessionState state = SessionStateManager.instance().getSessionState(this.session);
        List<ActionExecutionRecord> records = state.getAllActionsExecuted();
        List<Item> items = new ArrayList<Item>();

        for (ActionExecutionRecord record : records) {
            if ("active".equals(this.mode) && !state.isActive(record))
                continue;

            items.add(new Item(record));
        }

        Item[] itemsArray = new Item[items.size()];
        items.toArray(itemsArray);

        mAdapter = new ArrayAdapter<ActionExecutionRecordListFragment.Item>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, itemsArray);

        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
                // Android, this isn't always needed! Sick of commenting this out already.

//            throw new ClassCastException(activity.toString()
//                + " must implement Delegate");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
            return;
        }

        Item item = (Item)this.mAdapter.getItem(position);
        ActionExecutionRecord record = item.getRecord();

        SSHCommand command = record.getSSHCommand();

        if (command != null) {
            Fragment newFragment = new CommandResultFragment();
            Bundle args = new Bundle();
            args.putLong(Constants.ARG_CONNECTION_ID, command.getThread().getId());
            args.putLong(Constants.ARG_COMMAND_ID, command.getId());

            if (getActivity() instanceof SessionViewActivity) {
                ((SessionViewActivity)getActivity()).setFragment("execute-"+command.getId(), newFragment, args);
            }
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
    * This interface must be implemented by activities that contain this
    * fragment to allow an interaction in this fragment to be communicated
    * to the activity and potentially other fragments contained in that
    * activity.
    * <p>
    * See the Android Training lesson <a href=
    * "http://developer.android.com/training/basics/fragments/communicating.html"
    * >Communicating with Other Fragments</a> for more information.
    */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

    public class Item {
        public Item(ActionExecutionRecord record)
        {
            this.record = record;
        }

        @Override
        public String toString() {
            return "["+new SimpleDateFormat("h:mm a").format(this.record.getExecutionTime())+"] "+this.record.getAction().getName();
        }

        private ActionExecutionRecord record;

        public ActionExecutionRecord getRecord() {
            return record;
        }

        public void setRecord(ActionExecutionRecord record) {
            this.record = record;
        }
    }
}
