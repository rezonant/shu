package rezonant.shu.ui.global;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import rezonant.shu.R;

public class AboutShuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_shu);


        TextView versionLabel = (TextView)getWindow().getDecorView().findViewById(R.id.versionLabel);
        TextView detailsLabel = (TextView)getWindow().getDecorView().findViewById(R.id.detailedInformationLabel);

        versionLabel.setText("v0.4.0");
        detailsLabel.setText(
            "(C) 2014 rezonant. All rights reserved for now. (Expect source soon enough!)\n"+
            "This application uses JSCH.\n"+
            "For awesome custom apps, visit 906 Technologies\n" +
            " at http://906tech.com/"
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about_shu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
