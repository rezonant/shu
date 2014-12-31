package rezonant.shu.ui.data;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import rezonant.shu.Constants;
import rezonant.shu.R;

public class EditSSHConnectionActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ssh_connection);

		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.

			this.connectionID = getIntent().getLongExtra(Constants.ARG_CONNECTION_ID, 0);
			this.addToSessionID = getIntent().getLongExtra(Constants.ARG_SESSION_ID, 0);

			Bundle arguments = new Bundle();
			arguments.putLong(Constants.ARG_CONNECTION_ID, connectionID);
			arguments.putLong(Constants.ARG_SESSION_ID, addToSessionID);

			EditSSHConnectionFragment fragment = new EditSSHConnectionFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}
    }

	private long connectionID;
	private long addToSessionID;

}
