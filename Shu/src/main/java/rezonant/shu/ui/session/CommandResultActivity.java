package rezonant.shu.ui.session;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import rezonant.shu.R;

public class CommandResultActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_result);

        if (savedInstanceState == null) {
			Fragment fragment = new CommandResultFragment();
			Bundle args = new Bundle();
			if (getIntent().getExtras() != null)
				args = getIntent().getExtras();

			fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }
}
