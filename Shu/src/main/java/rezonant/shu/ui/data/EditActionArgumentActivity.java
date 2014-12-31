package rezonant.shu.ui.data;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import rezonant.shu.R;

public class EditActionArgumentActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_action_argument);

		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.

			this.actionArgumentID = getIntent().getLongExtra(EditActionArgumentFragment.ARG_ACTION_ARGUMENT_ID, 0);

			Bundle arguments = new Bundle();
			arguments.putLong(EditActionArgumentFragment.ARG_ACTION_ARGUMENT_ID, actionArgumentID);

			EditActionArgumentFragment fragment = new EditActionArgumentFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}
	}

	private long actionArgumentID;

	public long getActionArgumentId()
	{
		return actionArgumentID;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.edit_action_argument, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case R.id.action_settings:
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
