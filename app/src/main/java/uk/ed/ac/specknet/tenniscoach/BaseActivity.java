package uk.ed.ac.specknet.tenniscoach;

import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BaseActivity extends AppCompatActivity {
    private TextView mTextMessage;
    private Button connect;
    private Button start;
    private Button stop;
    private Button reset;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            connect = findViewById(R.id.connect_button);
            start = findViewById(R.id.start_button);
            stop = findViewById(R.id.stop_button);
            reset = findViewById(R.id.reset_button);

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.text_record);
                    connect.setVisibility(View.VISIBLE);
                    start.setVisibility(View.VISIBLE);
                    stop.setVisibility(View.VISIBLE);
                    reset.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.text_history);
                    connect.setVisibility(View.INVISIBLE);
                    start.setVisibility(View.INVISIBLE);
                    stop.setVisibility(View.INVISIBLE);
                    reset.setVisibility(View.INVISIBLE);
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
        mTextMessage = findViewById(R.id.message);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

}
