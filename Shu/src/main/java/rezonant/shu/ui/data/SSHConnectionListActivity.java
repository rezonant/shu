package rezonant.shu.ui.data;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import rezonant.shu.R;

public class SSHConnectionListActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssh_connection_list);

		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			SSHConnectionsListFragment fragment = new SSHConnectionsListFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}
    }
}
