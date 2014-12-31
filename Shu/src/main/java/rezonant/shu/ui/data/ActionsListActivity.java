package rezonant.shu.ui.data;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import rezonant.shu.R;

public class ActionsListActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actions_list);

		if (savedInstanceState == null) {
			ActionsListFragment fragment = new ActionsListFragment();

			Bundle args = getIntent().getExtras();
			if (args == null)
				args = new Bundle();

			fragment.setArguments(args);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, fragment)
					.commit();
		}
    }
}
