package rezonant.shu.ui.session;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import rezonant.droplog.LogListenerThread;
import rezonant.shu.R;
import com.newrelic.agent.android.NewRelic;

public class SessionsListActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NewRelic.withApplicationToken(
                "AAb80d2b9288e6f93fdae54d435dc8881d64b51cd9"
        ).start(this.getApplication());

        LogListenerThread.initialize(getApplicationContext());

        setContentView(R.layout.activity_sessions_list);

		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			SessionsListFragment fragment = new SessionsListFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}


    }
}
