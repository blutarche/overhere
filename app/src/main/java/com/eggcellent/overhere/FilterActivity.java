package com.eggcellent.overhere;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class FilterActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private SeekBar timeSeekbar;
    private SeekBar distanceSeekbar;
    private EditText hashtagEditText;
    private EditText friendsEditText;

    private TextView timeText;
    private TextView distanceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        mToolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(mToolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        timeSeekbar = (SeekBar) findViewById(R.id.timeSeekbar);
        distanceSeekbar = (SeekBar) findViewById(R.id.distanceSeekbar);
        hashtagEditText = (EditText) findViewById(R.id.hashtagEditText);
        friendsEditText = (EditText) findViewById(R.id.friendsEditText);

        timeText = (TextView) findViewById(R.id.timeValue);
        distanceText = (TextView) findViewById(R.id.distanceValue);

        Intent data = getIntent();
        timeSeekbar.setProgress(data.getIntExtra("Time", 100));
        timeTextChange(timeSeekbar.getProgress());
        distanceSeekbar.setProgress(data.getIntExtra("Distance", 100));
        distanceTextChange(distanceSeekbar.getProgress());
        hashtagEditText.setText(data.getStringExtra("Hashtag"));
        friendsEditText.setText(data.getStringExtra("Friends"));

        setSeekbarChangeListener();
    }

    private void setSeekbarChangeListener() {
        timeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeTextChange(progress);
            }
        });
        distanceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distanceTextChange(progress);
            }
        });
    }

    private void timeTextChange (int progress) {
        if (progress == 100) {
            timeText.setText("1 year");
        } else if (95 <= progress && progress < 100) {
            timeText.setText("11 months");
        } else if (90 <= progress && progress < 95) {
            timeText.setText("10 months");
        } else if (85 <= progress && progress < 90) {
            timeText.setText("9 months");
        } else if (80 <= progress && progress < 85) {
            timeText.setText("8 months");
        } else if (75 <= progress && progress < 80) {
            timeText.setText("7 months");
        } else if (70 <= progress && progress < 75) {
            timeText.setText("6 months");
        } else if (65 <= progress && progress < 70) {
            timeText.setText("5 months");
        } else if (60 <= progress && progress < 65) {
            timeText.setText("4 months");
        } else if (55 <= progress && progress < 60) {
            timeText.setText("3 months");
        } else if (50 <= progress && progress < 55) {
            timeText.setText("2 months");
        } else if (45 <= progress && progress < 50) {
            timeText.setText("1 month");
        } else if (40 <= progress && progress < 45) {
            timeText.setText("3 weeks");
        } else if (35 <= progress && progress < 40) {
            timeText.setText("2 weeks");
        } else if (30 <= progress && progress < 35) {
            timeText.setText("1 week");
        } else if (25 <= progress && progress < 30) {
            timeText.setText("3 days");
        } else if (2 <= progress && progress < 25) {
            timeText.setText((progress) + " hours");
        } else if (progress == 1) {
            timeText.setText("1 hour");
        } else if (progress == 0) {
            timeText.setText("30 mins");
        }
    }

    private void distanceTextChange (int progress) {
        if (progress == 100) {
            distanceText.setText("Global");
        } else if (80 <= progress && progress < 100) {
            int temp = progress - 50;
            distanceText.setText((temp * 100) + " km");
        } else if (30 <= progress && progress < 80) {
            int temp = progress - 29;
            distanceText.setText((temp * 50) + " km");
        } else {
            distanceText.setText((progress + 1) + " km");
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_ok) {
            applyActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applyActivity() {
        Intent intent = new Intent();
        intent.putExtra("Time", timeSeekbar.getProgress());
        intent.putExtra("Distance", distanceSeekbar.getProgress());
        intent.putExtra("Hashtag", hashtagEditText.getText().toString());
        intent.putExtra("Friends", friendsEditText.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }
}
