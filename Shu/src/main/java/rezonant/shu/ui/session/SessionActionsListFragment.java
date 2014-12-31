package rezonant.shu.ui.session;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rezonant.shu.state.data.SessionActionCategory;
import rezonant.shu.ui.NavigatedFragment;
import rezonant.shu.ui.data.ActionsListActivity;
import rezonant.shu.ui.data.ActionsListFragment;
import rezonant.shu.Constants;
import rezonant.shu.R;
import rezonant.shu.state.data.Action;
import rezonant.shu.state.data.Session;
import rezonant.shu.state.data.SessionAction;
import rezonant.shu.ui.data.EditActionActivity;
import rezonant.shu.ui.data.EditActionFragment;
import rezonant.shu.ui.data.EditSessionActionActivity;

/**
 * A fragment representing a single Tool detail screen.
 * This fragment is either contained in a {@link rezonant.shu.ui.ToolListActivity}
 * in two-pane mode (on tablets) or a {@link rezonant.shu.ui.ToolDetailActivity}
 * on handsets.
 */
public class SessionActionsListFragment extends NavigatedFragment {
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SessionActionsListFragment() {

	}

	public interface Delegate {
		void sessionActionSelected(SessionAction action);
	}

    @Override
    public String getTitle() {
        if (this.session != null)
            return "Actions - "+this.session.toString();

        return "Actions";
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		sessionActionsRepository = SessionAction.createDao(getActivity());
		sessionsRepository = Session.createDao(getActivity());
		actionsRepository = Action.createDao(getActivity());
        categoryDao = SessionActionCategory.createDao(getActivity());

		if (getArguments().containsKey(Constants.ARG_SESSION_ID)) {
			this.sessionID = getArguments().getLong(Constants.ARG_SESSION_ID);

			Log.i("FYI", "Getting session with ID "+sessionID);

			try {
				session = sessionsRepository.queryForId(sessionID);
			} catch (SQLException e) {
				Log.e("SQLWTF", e.getMessage(), e);
			}
		}
	}

	private Dao<SessionAction,Long> sessionActionsRepository;
	private Dao<Session,Long> sessionsRepository;
	private Dao<Action,Long> actionsRepository;

	private long sessionID;
	private Session session;

	private final int REQUEST_ACTION_TO_ADD = 1;
	private final int REQUEST_NEW_ACTION = 2;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.action_add:
				Intent intent = new Intent(this.getActivity(), ActionsListActivity.class);
				intent.putExtra(ActionsListFragment.ARG_CHOOSER_MODE, true);
				startActivityForResult(intent, REQUEST_ACTION_TO_ADD);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		boolean processResult = false;
        Log.i("FYI", "Receiving result "+resultCode+" from request "+requestCode);

		if (requestCode == REQUEST_NEW_ACTION && resultCode == Activity.RESULT_OK) {
            Log.i("FYI", "Receiving result from NEW ACTION editor");
			processResult = true;
		}

		if (requestCode == REQUEST_ACTION_TO_ADD && resultCode == ActionsListFragment.RESULT_SELECTED) {
            Log.i("FYI", "Receiving result from ACTION CHOOSER");
			processResult = true;
		}


		if (processResult) {
			long actionID = data.getLongExtra(Constants.ARG_ACTION_ID, 0);
			Log.i("FYI", "The result has landed! Got action ID of "+actionID);

			if (actionID == 0) {
                Log.i("FYI", "Launching editor for NEW action");
				// Launch editor for a new action
				startActivityForResult(new Intent(getActivity(), EditActionActivity.class), REQUEST_NEW_ACTION);
				return;
			}

			Action action;
			try {
				action = actionsRepository.queryForId(actionID);
			} catch (SQLException e) {
				new AlertDialog.Builder(getActivity())
						.setTitle(e.getMessage())
						.setMessage(e.toString())
						.create()
						.show();

				Log.e("Database", "SQL exception", e);
				return;
			}

            Log.i("FYI", "Adding action #"+action.getId()+" to this session as a new SessionAction");
			SessionAction sessionAction = new SessionAction();
			sessionAction.setAction(action);
			sessionAction.setSession(session);
			sessionAction.setTitle(action.getName());

			this.session.addAction(sessionAction);
            Log.i("FYI", "Session action has ID " + sessionAction.getId());

            Log.i("FYI", "Refreshing view...");
			refreshView(getView());
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		getActivity().getMenuInflater().inflate(R.menu.session_actions_list, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onResume()
	{
		refreshView(getView());
		super.onResume();
	}

    private class SectionHeader {
        public SectionHeader(String title, long id)
        {
            this.title = title;
            this.id = id;
        }

        @Override
        public String toString() {
            return title;
        }

        public String title;
        public long id;
    }

	private void refreshView(View view)
	{
		try {
			sessionsRepository.refresh(session);
		} catch (SQLException e) {
			new AlertDialog.Builder(getActivity())
					.setTitle(e.getMessage())
					.setMessage(e.toString())
					.create()
					.show();

			Log.e("Database", "SQL exception", e);
		}

		ListView list = (ListView)view.findViewById(R.id.actions_list);
		List<Object> items = new ArrayList<Object>();
        List<SessionAction> data = new ArrayList<SessionAction>(Arrays.asList(session.getActions()));

        EditText searchBox = (EditText)view.findViewById(R.id.search);
        String searchTerm = searchBox.getText().toString().toLowerCase();

        // Only show search if there is more than a few entries...

        if (data.size() >= 5) {
            if (data.size() == 5) {
                searchBox.setHint(getResources().getString(R.string.action_tap_to_search));
            } else {
                searchBox.setHint(getResources().getString(R.string.action_tap_to_search_attach_above));
            }

            searchBox.setVisibility(View.VISIBLE);
        } else {
            searchBox.setVisibility(View.GONE);
        }

        // Filter

        if (searchTerm != null && !"".equals(searchTerm)) {

            for (int i = 0, max = data.size(); i < max; ++i) {
                SessionAction item = data.get(i);
                String command = "";
                if (item.getAction() != null && item.getAction().getCommand() != null) {
                    command = item.getAction().getCommand();
                }

                boolean keep = false;

                if (item.getTitle().toLowerCase().indexOf(searchTerm) >= 0 || command.toLowerCase().indexOf(searchTerm) >= 0) {
                    keep = true;
                }

                if (item.getCategory() != null && item.getCategory().getTitle().toLowerCase().indexOf(searchTerm) >= 0) {
                    keep = true;
                }

                if (!keep) {
                    data.remove(item);
                    --max;
                    --i;
                }
            }
        }

        // Sort the data by category
        Collections.sort(data, new Comparator<SessionAction>() {
            @Override
            public int compare(SessionAction lhs, SessionAction rhs) {
                if (lhs.getCategory() != null && rhs.getCategory() == null)
                    return -1;

                if (lhs.getCategory() == null && rhs.getCategory() != null)
                    return 1;

                if (lhs.getCategory() != null && lhs.getCategory().getId() != rhs.getCategory().getId()) {
                    return lhs.getCategory().getTitle().compareTo(rhs.getCategory().getTitle());
                }

                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        SessionActionCategory lastCategory = null;
        long lastCategoryId = -1;

        if ("".equals(searchTerm) && data.size() < 5) {
            SessionAction attachOne = new SessionAction();

            if (data.size() == 0) {
                attachOne.setTitle(getResources().getString(R.string.action_add_first_session_action));
            } else if (data.size() == 1) {
                attachOne.setTitle(getResources().getString(R.string.action_add_second_session_action));
            } else {
                attachOne.setTitle(getResources().getString(R.string.action_tap_to_add));
            }

            attachOne.setId(0);

            items.add(attachOne);
        }

        List<Long> categoriesUsed = new ArrayList<Long>();

        for (SessionAction action : data) {

            boolean categoryChanged = false;
            long categoryId = 0;
            if (action.getCategory() != null)
                categoryId = action.getCategory().getId();

            if (lastCategoryId != categoryId) {

                categoriesUsed.add(categoryId);

                if (lastCategoryId > 0) {
                    SessionAction attachOne = new SessionAction();
                    attachOne.setTitle(getResources().getString(R.string.action_tap_to_add));
                    attachOne.setId(0);
                    attachOne.setCategory(lastCategory);
                    items.add(attachOne);
                }

                String label = "Uncategorized";
                lastCategory = action.getCategory();
                lastCategoryId = categoryId;
                if (lastCategory != null) {
                    label = lastCategory.getTitle();
                }

                items.add(new SectionHeader(label, categoryId));
            }

            items.add(action);
        }

        if (lastCategoryId >= 0) {
            SessionAction attachOne = new SessionAction();
            attachOne.setTitle(getResources().getString(R.string.action_tap_to_add));
            attachOne.setId(0);
            attachOne.setCategory(lastCategory);
            items.add(attachOne);
        }

        // If we have any "always show when empty" categories, do so now

        for (Long id : displayEvenWhenEmpty) {

            // Look up the category

            SessionActionCategory category = null;
            try {
                category = categoryDao.queryForId(id);
            } catch (SQLException e) {
                Log.e("WTF", "While looking up 'Always show' category with ID "+id+": "
                        +e.getMessage(), e);
                continue;
            }

            if (categoriesUsed.contains(category.getId()))
                continue;

            items.add(new SectionHeader(category.getTitle(), category.getId()));

            SessionAction fakeAction = new SessionAction();
            fakeAction.setTitle(getResources().getString(R.string.action_tap_to_add));
            fakeAction.setId(0);
            fakeAction.setCategory(category);

            items.add(fakeAction);
        }

        SectionHeader addHeader = new SectionHeader("Add category...", 0);
        items.add(addHeader);

		final Object[] finalItems = new Object[items.size()];
        items.toArray(finalItems);

        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return finalItems.length;
            }

            @Override
            public Object getItem(int position) {
                return finalItems[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean isEnabled(int position) {
                Object item = getItem(position);

                if (item instanceof SessionAction)
                    return true;

                if (item instanceof SectionHeader) {
                    SectionHeader header = (SectionHeader)item;
                    if (header.id == 0)
                        return true;
                    return false;
                }

                return super.isEnabled(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                Object item = finalItems[position];
                String neededTag = "unknown";

                if (item instanceof SessionAction) {
                    SessionAction sessionAction = ((SessionAction) item);

                    if (sessionAction.getId() == 0 && sessionAction.getCategory() == null) {
                        neededTag = "blankSessionAction";

                        // If we can't reuse the view, make sure to replace it first...
                        if (convertView == null || !neededTag.equals(convertView.getTag())) {
                            convertView = getLayoutInflater(null).inflate(android.R.layout.simple_list_item_1, parent, false);
                        }

                        TextView titleView = (TextView) convertView.findViewById(android.R.id.text1);
                        titleView.setText(item.toString());

                    } else {
                        neededTag = "sessionAction";

                        // If we can't reuse the view, make sure to replace it first...
                        if (convertView == null || !neededTag.equals(convertView.getTag())) {
                            convertView = getLayoutInflater(null).inflate(R.layout.list_session_action, parent, false);
                        }

                        TextView titleView = (TextView) convertView.findViewById(R.id.title);
                        TextView detailView = (TextView) convertView.findViewById(R.id.details);

                        titleView.setText(item.toString());
                        if (sessionAction.getAction() != null) {
                            detailView.setText(sessionAction.getAction().getCommand());

                            titleView.setTextColor(Color.WHITE);
                            detailView.setTextColor(Color.GRAY);

                        } else {
                            if (sessionAction.getCategory() != null)
                                detailView.setText("Add action to category "+sessionAction.getCategory().getTitle());
                            else
                                detailView.setText("...");

                            titleView.setTextColor(Color.GRAY);
                            detailView.setTextColor(Color.GRAY);
                        }
                    }

                } else if (item instanceof SectionHeader) {
                    SectionHeader sectionHeader  = ((SectionHeader) item);
                    neededTag = "sectionHeader";

                    // If we can't reuse the view, make sure to replace it first...
                    if (convertView == null || !neededTag.equals(convertView.getTag())) {
                        convertView = getLayoutInflater(null).inflate(R.layout.list_section_header_2, parent, false);
                    }

                    TextView titleView = (TextView) convertView.findViewById(R.id.title);
                    titleView.setText(item.toString());
                }

                convertView.setTag(neededTag);
                return convertView;
            }
        });

		//list.setAdapter(new ArrayAdapter<Object>(getActivity(),
		//		android.R.layout.simple_list_item_1,
		//		android.R.id.text1, items));

	}

    private List<Long> displayEvenWhenEmpty = new ArrayList<Long>();
    private Dao<SessionActionCategory, Long> categoryDao;

    @Override
    public void onSaveInstanceState(Bundle outState) {

        long[] array = new long[displayEvenWhenEmpty.size()];
        for (int i = 0, max = displayEvenWhenEmpty.size(); i < max; ++i)
            array[i] = displayEvenWhenEmpty.get(i);

        outState.putLongArray("displayEvenWhenEmpty", array);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {

        if (savedInstanceState != null) {

            if (savedInstanceState.containsKey("displayEvenWhenEmpty")) {
                long[] ids = savedInstanceState.getLongArray("displayEvenWhenEmpty");

                displayEvenWhenEmpty.clear();
                for (long id : ids)
                    displayEvenWhenEmpty.add(id);
            }
        }

        super.onViewStateRestored(savedInstanceState);
    }

    private class ItemClickListener implements AdapterView.OnItemClickListener {
		public ItemClickListener(SessionActionsListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private SessionActionsListFragment fragment;
		private ListView listView;

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM CLICKED! **************** "+position+" ID: "+id);

            Object item = listView.getItemAtPosition(position);

            if (item instanceof SessionAction) {
                SessionAction action = (SessionAction) listView.getItemAtPosition(position);

                if (action == null) {
                    Log.e("WTF", "Action was not found!!");
                    return;
                }

                Log.i("FYI", "PICKED ######### " + action.toString());
                fragment.itemClicked(action);
            } else if (item instanceof SectionHeader) {
                SectionHeader header = (SectionHeader)item;

                if (header.id == 0) {
                    // Add new category

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

                                    displayEvenWhenEmpty.add(category.getId());
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
	}

    public void refresh()
    {
        refreshView(getView());
    }
	public static final int CONTEXT_MENU_DELETE = 232;
	public static final int CONTEXT_MENU_EDIT = 233;
	public static final int CONTEXT_MENU_DEFINE = 234;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.setHeaderTitle("Actions");
		menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Remove from Session");
		menu.add(Menu.NONE, CONTEXT_MENU_EDIT, Menu.NONE, "Edit Action");
		menu.add(Menu.NONE, CONTEXT_MENU_DEFINE, Menu.NONE, "Edit Definition");

		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
		public ItemLongClickListener(SessionActionsListFragment fragment, ListView listView) {
			this.fragment = fragment;
			this.listView = listView;
		}

		private SessionActionsListFragment fragment;
		private ListView listView;

		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			Log.i("FYI", "************ ITEM LONG CLICKED! ****************");

            Object item = listView.getItemAtPosition(position);

            if (item instanceof SessionAction) {
                SessionAction action = (SessionAction) item;
                fragment.contextMenuAction = action;
            } else {
                fragment.contextMenuAction = null;
            }

			return false;
		}
	}

	private SessionAction contextMenuAction;

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ListView listView = (ListView)getView().findViewById(R.id.sessions_list);

        Log.i("FYI", "Action clicked w: "+item.getItemId());

        if (contextMenuAction == null)
            return false;

		if (item.getItemId() == CONTEXT_MENU_DELETE) {
			Log.i("FYI", "DELETING SessionAction with id "+contextMenuAction.getId());
			try {
				sessionActionsRepository.delete(contextMenuAction);
				contextMenuAction = null;
				this.refreshView(getView());
			} catch (SQLException e) {

				new AlertDialog.Builder(getActivity())
						.setTitle(e.getMessage())
						.setMessage(e.toString())
						.create()
						.show();

				Log.e("Database", "SQL exception", e);
			}
			return true;
		} else if (item.getItemId() == CONTEXT_MENU_DEFINE) {
            Log.i("FYI", "Define action clicked");
			Intent intent = new Intent(getActivity(), EditActionActivity.class);
			intent.putExtra(Constants.ARG_ACTION_ID, contextMenuAction.getAction().getId());
			startActivity(intent);

		} else if (item.getItemId() == CONTEXT_MENU_EDIT) {
            Log.i("FYI", "Edit session action clicked");
			Intent intent = new Intent(getActivity(), EditSessionActionActivity.class);
			intent.putExtra(Constants.ARG_SESSION_ACTION_ID, contextMenuAction.getId());
			startActivity(intent);
		}

		return false;
	}

	private void itemClicked(SessionAction action)
	{
        if (action.getId() == 0) {
            Intent intent = new Intent(this.getActivity(), ActionsListActivity.class);
            intent.putExtra(ActionsListFragment.ARG_CHOOSER_MODE, true);
            startActivityForResult(intent, REQUEST_ACTION_TO_ADD);
            return;
        }

		if (getActivity() instanceof Delegate) {
			Delegate del = (Delegate)getActivity();
			del.sessionActionSelected(action);
			return;
		}
	}

    Timer searchTimeout = null;

    @Override
    public void onPause() {
        if (searchTimeout != null) {
            searchTimeout.cancel();
            searchTimeout = null;
        }

        super.onPause();
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {


		View rootView = inflater.inflate(R.layout.fragment_actions_list, container, false);

		ListView listView = (ListView)rootView.findViewById(R.id.actions_list);

		registerForContextMenu(listView);

		listView.setOnItemClickListener(new ItemClickListener(this, listView));
		listView.setOnItemLongClickListener(new ItemLongClickListener(this, listView));

        EditText searchBox = (EditText)rootView.findViewById(R.id.search);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchTimeout != null) {
                    searchTimeout.cancel();
                    searchTimeout = null;
                }

                searchTimeout = new Timer();
                searchTimeout.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refresh();
                            }
                        });
                    }
                }, 250);

            }
        });

		refreshView(rootView);

		return rootView;
	}
}
