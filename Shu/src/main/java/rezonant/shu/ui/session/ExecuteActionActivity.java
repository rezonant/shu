package rezonant.shu.ui.session;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.util.HashMap;
import java.util.List;

import rezonant.shu.R;
import rezonant.shu.state.data.SSHConnection;
import rezonant.shu.state.data.SessionAction;

public class ExecuteActionActivity extends FragmentActivity implements ExecuteActionFragment.Delegate {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_actions_list);

		if (savedInstanceState == null) {
			ExecuteActionFragment fragment = new ExecuteActionFragment();

			Bundle args = getIntent().getExtras();
			if (args == null)
				args = new Bundle();

			fragment.setArguments(args);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}
	}

	public void actionExecuted(SessionAction session, HashMap<Integer, String> variables, List<SSHConnection> connections)
	{

	}
}
