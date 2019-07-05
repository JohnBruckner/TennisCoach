package uk.ed.ac.specknet.tenniscoach;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String ORIENT_BLE_ADDRESS = "F2:6D:63:1F:17:33"; // test device

    private static final String ORIENT_QUAT_CHARACTERISTIC = "00001526-1212-efde-1523-785feabcd125";
    private static final String ORIENT_RAW_CHARACTERISTIC = "00001527-1212-efde-1523-785feabcd125";

    private Button connect;
    private Button start;
    private Button stop;
    private Button reset;
    private TextView clock;

    private Button[] buttons;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    massSetVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    massSetVisibility(View.INVISIBLE);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        connect = findViewById(R.id.connect_button);
        start = findViewById(R.id.start_button);
        stop = findViewById(R.id.stop_button);
        reset = findViewById(R.id.reset_button);
        clock = findViewById(R.id.TimeText);


    }

    private void massSetVisibility(int visibility) {
        connect = findViewById(R.id.connect_button);
        start = findViewById(R.id.start_button);
        stop = findViewById(R.id.stop_button);
        reset = findViewById(R.id.reset_button);
        clock = findViewById(R.id.TimeText);

        buttons = new Button[]{connect, start, stop, reset};

        for (Button b :
                buttons) {
            b.setVisibility(visibility);
        }
        clock.setVisibility(visibility);
    }

}
