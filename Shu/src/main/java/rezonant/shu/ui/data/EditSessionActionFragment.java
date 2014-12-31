package rezonant.shu.ui.data;



import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.SessionAction;
import rezonant.shu.state.data.SessionActionCategory;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditSessionActionFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class EditSessionActionFragment extends Fragment {

    // TODO: Rename and change types of parameters
    private long mSessionActionId;
    private SessionAction mSessionAction;
    private Dao<SessionAction, Long> sessionActionRepository;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param action The session-action to edit
     * @return A new instance of fragment EditSessionActionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EditSessionActionFragment newInstance(SessionAction action) {

        EditSessionActionFragment fragment = new EditSessionActionFragment();

        Bundle args = new Bundle();
        args.putLong(Constants.ARG_SESSION_ACTION_ID, action.getId());
        fragment.setArguments(args);
        return fragment;
    }
    public EditSessionActionFragment() {
        // Required empty public constructor
    }

    private void syncObject()
    {
        TextView name = (TextView)getView().findViewById(R.id.name);
        mSessionAction.setTitle(name.getText().toString());

        Spinner categorySpinner = (Spinner)getView().findViewById(R.id.category);

        if (categorySpinner.getSelectedItem() != null) {
            Object item = categorySpinner.getSelectedItem();

            if (item instanceof SessionActionCategory) {
                SessionActionCategory category = (SessionActionCategory)item;
                mSessionAction.setCategory(category);
            } else {
                mSessionAction.setCategory(null);
            }
        } else {
            mSessionAction.setCategory(null);
        }

        try {
            sessionActionRepository.update(mSessionAction);
        } catch (SQLException e) {
            Log.e("WTF", e.getMessage(), e);
        }
    }

    @Override
    public void onPause() {
        syncObject();
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionActionRepository = SessionAction.createDao(getActivity());

        if (getArguments() != null) {
            mSessionActionId = getArguments().getLong(Constants.ARG_SESSION_ACTION_ID);
            try {
                mSessionAction = sessionActionRepository.queryForId(mSessionActionId);
            } catch (SQLException e) {
                Log.e("WTF", e.getMessage(), e);
            }
        }

        Log.i("FYI", "Got mSessionAction: "+mSessionAction);
        if (mSessionAction == null) {
            Log.e("WTF", "Shit, it's null Jim (ID: "+mSessionActionId+")");
            new AlertDialog.Builder(getActivity())
                    .setTitle("Failed to retrieve Session Action with ID "+mSessionActionId)
                    .setMessage("Eh")
                    .create()
                    .show();
        }
    }

    private class SpecialItem {
        public static final int NEW_CATEGORY = 0;
        public static final int NO_CATEGORY = 1;

        public SpecialItem(String title, int meaning)
        {
            this.meaning = meaning;
            this.title = title;
        }

        public int meaning;
        public String title;

        @Override
        public String toString() {
            return title;
        }
    }

    Dao<SessionActionCategory,Long> categoryDao;
    Spinner categorySpinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View master = inflater.inflate(R.layout.fragment_edit_session_action, container, false);

        TextView name = (TextView)master.findViewById(R.id.name);

        if (mSessionAction != null) {
            name.setText(mSessionAction.getTitle());
        }

        categoryDao = SessionActionCategory.createDao(getActivity());

        categorySpinner = (Spinner)master.findViewById(R.id.category);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getAdapter().getItem(position);

                if (item instanceof SpecialItem) {
                    SpecialItem specialItem = (SpecialItem)item;

                    if (specialItem.meaning == SpecialItem.NEW_CATEGORY) {
                        // Create new category

                        final EditText input = new EditText(getActivity());
                        input.setInputType(InputType.TYPE_CLASS_TEXT);

                        new AlertDialog.Builder(getActivity())
                                .setTitle("Enter name for new category:")
                                .setView(input)

                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Create new category
                                        SessionActionCategory category = new SessionActionCategory();
                                        category.setTitle(input.getText().toString());
                                        try {
                                            categoryDao.create(category);
                                        } catch (SQLException e) {
                                            Log.e("FYI", e.getMessage(), e);
                                        }

                                        mSessionAction.setCategory(category);

                                        refresh();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Do nothing
                                    }
                                })
                                .create()
                                .show();
                    }

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        refreshView(master);

        return master;
    }

    private void refreshView(View master) {

        List<Object> items = new ArrayList<Object>();
        int selectedPosition = 0;

        try {
            items.add(new SpecialItem("No Category", SpecialItem.NO_CATEGORY));

            for (SessionActionCategory category : categoryDao.queryForAll()) {
                items.add(category);

                if (mSessionAction.getCategory() != null && category.getId() == mSessionAction.getCategory().getId()) {
                    selectedPosition = items.size() - 1;
                }

            }

            items.add(new SpecialItem("Add category...", SpecialItem.NEW_CATEGORY));

        } catch (SQLException e) {
            Log.e("WTF", e.getMessage(), e);
        }

        categorySpinner.setAdapter(new ArrayAdapter<Object>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, items));
        categorySpinner.setSelection(selectedPosition);

    }

    public void refresh()
    {
        refreshView(getView());
    }


}
