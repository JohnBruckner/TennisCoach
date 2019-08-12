package uk.ed.ac.specknet.tenniscoach;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.opencsv.CSVWriter;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import net.seninp.jmotif.sax.SAXProcessor;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import io.reactivex.disposables.Disposable;

public class MainActivity extends Activity implements AsyncResponse {
    private static int PERMISSION_REQUEST_LOCATION_COARSE = 0;

    private static final String ORIENT_BLE_ADDRESS = "F2:6D:63:1F:17:33"; // test device
    //    private static final String ORIENT_BLE_ADDRESS = "D4:35:E6:44:FB:AC";
    public static final String ORIENT1_BLE_ADDRESS = "E1:66:70:34:89:72";
    public static final String ORIENT2_BLE_ADDRESS = "E3:CF:82:7B:BF:77";

    private static final String ORIENT_QUAT_CHARACTERISTIC = "00001526-1212-efde-1523-785feabcd125";
    private static final String ORIENT_RAW_CHARACTERISTIC = "00001527-1212-efde-1523-785feabcd125";

    private Context ctx;

    private Button connect;
    private Button start;
    private Button stop;
    private Button reset;
    private TextView clock;
    private TextView strokeType;
    private TextView score;

    private Button[] buttons;

    private Disposable scanSubscription;
    private RxBleClient rxBleClient;
    private ByteBuffer packetData;
    private boolean foundO1 = false;
    private boolean foundO2 = false;
    private boolean receivingO1 = false;
    private boolean receivingO2 = false;

    private AsyncResponse mainActivity;

    private Long capture_started_timestamp = null;
    boolean connected = false;
    private float freq = 0.f;

    private int counter = 0;
    private CSVWriter writer;
    private File path;
    private File file;
    private boolean logging = false;

    private Integer classification = -1;
    private Double rating = 0.;

    private String fname = new String();
    private boolean raw = true; // TODO: Remove
    private boolean multi = false; // TODO: Remove

    private XYPlot plot;

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

        ctx = this;
        Activity a = (Activity) ctx;

        mainActivity = this;

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        connect = findViewById(R.id.connect_button);
        start = findViewById(R.id.start_button);
        stop = findViewById(R.id.stop_button);
        reset = findViewById(R.id.reset_button);
        clock = findViewById(R.id.TimeText);
        strokeType = findViewById(R.id.classification);
        score = findViewById(R.id.score);
        plot = (XYPlot) findViewById(R.id.XYPlot);

//        graphSeries(null, null);

        NormalAlphabet na = new NormalAlphabet();
        SAXProcessor sp = new SAXProcessor();

        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "TCSavedData");

        boolean success = true;

        if (!folder.exists()) {
            success = folder.mkdirs();
        }

        path = folder;

        if (ContextCompat.checkSelfPermission(a,
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(a,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_LOCATION_COARSE);
        }

        packetData = ByteBuffer.allocate(180);
        packetData.order(ByteOrder.LITTLE_ENDIAN);

        rxBleClient = RxBleClient.create(this);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                start.setEnabled(false);
                reset.setEnabled(true);

                // make a new filename based on the start timestamp
                String file_ts = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());

                String[] entries = null;
                if (raw) {
                    file = new File(path, "Orient_raw_" + file_ts + ".csv");
                    entries = "device#timestamp#packet_seq#sample_seq#accel_x#accel_y#accel_z#gyro_x#gyro_y#gyro_z#mag_x#mag_y#mag_z".split("#");
                } else {
                    file = new File(path, "Orient_quat_" + file_ts + ".csv");
                    entries = "device#timestamp#packet_seq#sample_seq#quat_w#quat_x#quat_y#quat_z".split("#");
                }

                fname = file.getAbsolutePath();


                try {
                    writer = new CSVWriter(new FileWriter(file), ',');
                } catch (IOException e) {
                    Log.e("MainActivity", "Caught IOException: " + e.getMessage());
                }

                writer.writeNext(entries);

                logging = true;
                capture_started_timestamp = System.currentTimeMillis();
                counter = 0;
                Toast.makeText(ctx, "Start logging", Toast.LENGTH_SHORT).show();
                stop.setEnabled(true);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logging = false;
                stop.setEnabled(false);
                try {
                    writer.flush();
                    writer.close();
                    Toast.makeText(ctx, "Recording saved", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e("MainActivity", "Caught IOException: " + e.getMessage());
                }

                ClassificationService classificationService = new ClassificationService(ctx);
                classificationService.classificationDelegate = mainActivity;
                classificationService.execute(fname);

                start.setEnabled(true);
                reset.setEnabled(false);
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logging = false;
                File file = new File(folder, fname);
                stop.setEnabled(false);
                try {
                    writer.flush();
                    writer.close();
                    boolean deleted = file.delete();
                    Toast.makeText(ctx, "Recording deleted", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e("MainActivity", "Caught IOException: " + e.getMessage());
                }
                start.setEnabled(true);
                reset.setEnabled(false
                );
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanSubscription = rxBleClient.scanBleDevices(
                        new ScanSettings.Builder()
                                // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                                // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                                .build()
                        // add filters if needed
                )
                        .subscribe(
                                scanResult -> {
                                    Log.i("OrientAndroid", "FOUND: " + scanResult.getBleDevice().getName() + ", " +
                                            scanResult.getBleDevice().getMacAddress());
                                    // Process scan result here.
                                    if (!multi) {
                                        if (scanResult.getBleDevice().getMacAddress().equals(ORIENT_BLE_ADDRESS)) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(ctx, "Found " + scanResult.getBleDevice().getName() + ", " +
                                                                scanResult.getBleDevice().getMacAddress(),
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                            connectToOrient(ORIENT_BLE_ADDRESS, 0);
                                            scanSubscription.dispose();
                                        }
                                    } else {
                                        if (!foundO1) {
                                            if (scanResult.getBleDevice().getMacAddress().equals(ORIENT1_BLE_ADDRESS)) {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(ctx, "Found " + scanResult.getBleDevice().getName() + ", " +
                                                                    scanResult.getBleDevice().getMacAddress(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                                foundO1 = true;
                                                connectToOrient(ORIENT1_BLE_ADDRESS, 1);
                                            }
                                        } else if (!foundO2) {
                                            if (scanResult.getBleDevice().getMacAddress().equals(ORIENT2_BLE_ADDRESS)) {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(ctx, "Found " + scanResult.getBleDevice().getName() + ", " +
                                                                    scanResult.getBleDevice().getMacAddress(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                                foundO2 = true;
                                                connectToOrient(ORIENT2_BLE_ADDRESS, 2);


                                            }
                                        }
                                        if (foundO1 && foundO2)
                                            scanSubscription.dispose();
                                    }
                                },
                                throwable -> {
                                    // Handle an error here.
                                    runOnUiThread(() -> {
                                        Toast.makeText(ctx, "BLE scanning error",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                        );

            }
        });
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

    private void connectToOrient(String addr, int n) {
        RxBleDevice dev;
        dev = rxBleClient.getBleDevice(addr);
        String characteristic;
        if (raw) characteristic = ORIENT_RAW_CHARACTERISTIC;
        else characteristic = ORIENT_QUAT_CHARACTERISTIC;

        boolean ac = false;
        if (n == 2) ac = true;

        dev.establishConnection(ac)
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        bytes -> {
                            //n += 1;
                            // Given characteristic has been changes, here is the value.

                            //Log.i("OrientAndroid", "Received " + bytes.length + " bytes");
                            if (!connected) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ctx, "Receiving sensor data from " + Integer.toString(n),
                                            Toast.LENGTH_SHORT).show();
                                    if (n == 0) {
                                        connected = true;
                                        start.setEnabled(true);
                                    } else if (n == 1)
                                        receivingO1 = true;
                                    else if (n == 2)
                                        receivingO2 = true;
                                    if (receivingO1 && receivingO2) {
                                        connected = true;
                                        start.setEnabled(true);
                                    }


                                });
                            }
                            if (multi) {
                                handleMultiRawPacket(bytes, n);
                            } else {
                                if (raw) handleRawPacket(bytes);
                                else handleQuatPacket(bytes);
                            }
                        },
                        throwable -> {
                            // Handle an error here.
                            Log.e("OrientAndroid", "Error: " + throwable.toString());
                        }
                );
    }

    private void handleQuatPacket(final byte[] bytes) {
        long ts = System.currentTimeMillis();
        packetData.clear();
        packetData.put(bytes);
        packetData.position(0);

        int w = packetData.getInt();
        int x = packetData.getInt();
        int y = packetData.getInt();
        int z = packetData.getInt();

        double dw = w / 1073741824.0;  // 2^30
        double dx = x / 1073741824.0;
        double dy = y / 1073741824.0;
        double dz = z / 1073741824.0;

        //Log.i("OrientAndroid", "QuatInt: (w=" + w + ", x=" + x + ", y=" + y + ", z=" + z + ")");
        //Log.i("OrientAndroid", "QuatDbl: (w=" + dw + ", x=" + dx + ", y=" + dy + ", z=" + dz + ")");

        if (logging) {
            //String[] entries = "first#second#third".split("#");
            String[] entries = {Integer.toString(0),
                    Long.toString(ts),
                    Integer.toString(counter),
                    Integer.toString(0),
                    Double.toString(dw),
                    Double.toString(dx),
                    Double.toString(dy),
                    Double.toString(dz),
            };

            writer.writeNext(entries);

            if (counter % 1 == 0) {
                long elapsed_time = System.currentTimeMillis() - capture_started_timestamp;
                int total_secs = (int) elapsed_time / 1000;
                int s = total_secs % 60;
                int m = total_secs / 60;

                String m_str = Integer.toString(m);
                if (m_str.length() < 2) {
                    m_str = "0" + m_str;
                }

                String s_str = Integer.toString(s);
                if (s_str.length() < 2) {
                    s_str = "0" + s_str;
                }


                Long elapsed_capture_time = System.currentTimeMillis() - capture_started_timestamp;
                float connected_secs = elapsed_capture_time / 1000.f;
                freq = counter / connected_secs;
//                Log.i("OrientAndroid", "Packet count: " + Integer.toString(n) + ", Freq: " + Float.toString(freq));

                String time_str = m_str + ":" + s_str;

                String accel_str = "Quat: (" + dw + ", " + dx + ", " + dy + ", " + dz + ")";
                String freq_str = "Freq: " + freq;

                runOnUiThread(() -> {
                    clock.setText(time_str);
                });
            }

            counter += 1;
        }
    }

    private void handleRawPacket(final byte[] bytes) {
        long ts = System.currentTimeMillis();
        packetData.clear();
        packetData.put(bytes);
        packetData.position(0);

        // 180 bytes in a packet, 18 bytes per reading.
        // There are 10 readings in a a packet
        for (int i = 0; i < 10; i++) {
            float accel_x = packetData.getShort() / 1024.f;  // integer part: 6 bits, fractional part 10 bits, so div by 2^10
            float accel_y = packetData.getShort() / 1024.f;
            float accel_z = packetData.getShort() / 1024.f;

            float gyro_x = packetData.getShort() / 32.f;  // integer part: 11 bits, fractional part 5 bits, so div by 2^5
            float gyro_y = packetData.getShort() / 32.f;
            float gyro_z = packetData.getShort() / 32.f;

            float mag_x = packetData.getShort() / 16.f;  // integer part: 12 bits, fractional part 4 bits, so div by 2^4
            float mag_y = packetData.getShort() / 16.f;
            float mag_z = packetData.getShort() / 16.f;

            //Log.i("OrientAndroid", "Accel:(" + accel_x + ", " + accel_y + ", " + accel_z + ")");
            //Log.i("OrientAndroid", "Gyro:(" + gyro_x + ", " + gyro_y + ", " + gyro_z + ")");
            //if (mag_x != 0f || mag_y != 0f || mag_z != 0f)
            //Log.i("OrientAndroid", "Mag:(" + mag_x + ", " + mag_y + ", " + mag_z + ")");

            if (logging) {
                //String[] entries = "first#second#third".split("#");
                String[] entries = {Integer.toString(0),
                        Long.toString(ts),
                        Integer.toString(counter),
                        Integer.toString(0),
                        Float.toString(accel_x),
                        Float.toString(accel_y),
                        Float.toString(accel_z),
                        Float.toString(gyro_x),
                        Float.toString(gyro_y),
                        Float.toString(gyro_z),
                        Float.toString(mag_x),
                        Float.toString(mag_y),
                        Float.toString(mag_z),
                };
                writer.writeNext(entries);

                if (counter % 12 == 0) {
                    long elapsed_time = System.currentTimeMillis() - capture_started_timestamp;
                    int total_secs = (int) elapsed_time / 1000;
                    int s = total_secs % 60;
                    int m = total_secs / 60;

                    String m_str = Integer.toString(m);
                    if (m_str.length() < 2) {
                        m_str = "0" + m_str;
                    }

                    String s_str = Integer.toString(s);
                    if (s_str.length() < 2) {
                        s_str = "0" + s_str;
                    }

                    Long elapsed_capture_time = System.currentTimeMillis() - capture_started_timestamp;
                    float connected_secs = elapsed_capture_time / 1000.f;
                    freq = counter / connected_secs;
                    //Log.i("OrientAndroid", "Packet count: " + Integer.toString(n) + ", Freq: " + Float.toString(freq));

                    String time_str = m_str + ":" + s_str;

                    String accel_str = "Accel: (" + accel_x + ", " + accel_y + ", " + accel_z + ")";
                    String gyro_str = "Gyro: (" + gyro_x + ", " + gyro_y + ", " + gyro_z + ")";
                    String freq_str = "Freq: " + freq;

                    runOnUiThread(() -> {
                        clock.setText(time_str);
                    });
                }

                counter += 1;
            }
        }

    }

    private void handleMultiRawPacket(final byte[] bytes, int n) {
        long ts = System.currentTimeMillis();
        packetData.clear();
        packetData.put(bytes);
        packetData.position(0);

        for (int i = 0; i < 10; i++) {

            float accel_x = packetData.getShort() / 1024.f;  // integer part: 6 bits, fractional part 10 bits, so div by 2^10
            float accel_y = packetData.getShort() / 1024.f;
            float accel_z = packetData.getShort() / 1024.f;

            float gyro_x = packetData.getShort() / 32.f;  // integer part: 11 bits, fractional part 5 bits, so div by 2^5
            float gyro_y = packetData.getShort() / 32.f;
            float gyro_z = packetData.getShort() / 32.f;

            float mag_x = packetData.getShort() / 16.f;  // integer part: 12 bits, fractional part 4 bits, so div by 2^4
            float mag_y = packetData.getShort() / 16.f;
            float mag_z = packetData.getShort() / 16.f;

            //Log.i("OrientAndroid", "Accel:(" + accel_x + ", " + accel_y + ", " + accel_z + ")");
            //Log.i("OrientAndroid", "Gyro:(" + gyro_x + ", " + gyro_y + ", " + gyro_z + ")");
            //if (mag_x != 0f || mag_y != 0f || mag_z != 0f)
            //Log.i("OrientAndroid", "Mag:(" + mag_x + ", " + mag_y + ", " + mag_z + ")");

            if (logging) {
                //String[] entries = "first#second#third".split("#");
                String[] entries = {Integer.toString(n),
                        Long.toString(ts),
                        Integer.toString(counter),
                        Integer.toString(i),
                        Float.toString(accel_x),
                        Float.toString(accel_y),
                        Float.toString(accel_z),
                        Float.toString(gyro_x),
                        Float.toString(gyro_y),
                        Float.toString(gyro_z),
                        Float.toString(mag_x),
                        Float.toString(mag_y),
                        Float.toString(mag_z),
                };
                writer.writeNext(entries);

                if (counter % 12 == 0) {
                    long elapsed_time = System.currentTimeMillis() - capture_started_timestamp;
                    int total_secs = (int) elapsed_time / 1000;
                    int s = total_secs % 60;
                    int m = total_secs / 60;

                    String m_str = Integer.toString(m);
                    if (m_str.length() < 2) {
                        m_str = "0" + m_str;
                    }

                    String s_str = Integer.toString(s);
                    if (s_str.length() < 2) {
                        s_str = "0" + s_str;
                    }


                    Long elapsed_capture_time = System.currentTimeMillis() - capture_started_timestamp;
                    float connected_secs = elapsed_capture_time / 1000.f;
                    freq = counter / connected_secs;
                    //Log.i("OrientAndroid", "Packet count: " + Integer.toString(n) + ", Freq: " + Float.toString(freq));

                    String time_str = m_str + ":" + s_str;

                    String accel_str = "Accel: (" + accel_x + ", " + accel_y + ", " + accel_z + ")";
                    String gyro_str = "Gyro: (" + gyro_x + ", " + gyro_y + ", " + gyro_z + ")";
                    String freq_str = "Freq: " + freq;

                    runOnUiThread(() -> {
                        clock.setText(time_str);
                    });
                }

            }
        }
        counter += 1;
    }

    @Override
    public void classificationFinish(Integer classification) {
        this.classification = classification;
        RatingService ratingService = new RatingService(ctx, classification);
        ratingService.ratingDelegate = this;
        ratingService.execute(fname);
    }

    @Override
    public void ratingFinish(Double result) {
        this.rating = result;
        if (classification == 0) {
            strokeType.setText("Serve");
        } else {
            strokeType.setText("Forehand");
        }
        score.setText(rating.toString());
    }

    @Override
    public void graphSeries(Double[] stroke, Double[] reference) {
        final Number[] domainLabels = {1, 2, 3, 6, 7, 8, 9, 10, 13, 14};
        Number[] series1Numbers = {1, 4, 2, 8, 4, 16, 8, 32, 16, 64};
        Number[] series2Numbers = {5, 2, 10, 5, 20, 10, 40, 20, 80, 40};

        XYSeries newStroke = new SimpleXYSeries(
                Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "New Stroke");
        XYSeries referenceStroke = new SimpleXYSeries(
                Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Reference"
        );

        LineAndPointFormatter series1Format =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);

        LineAndPointFormatter series2Format =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels_2);

        series2Format.getLinePaint().setPathEffect(new DashPathEffect(new float[]{

                // always use DP when specifying pixel sizes, to keep things consistent across devices:
                PixelUtils.dpToPix(20),
                PixelUtils.dpToPix(15)}, 0));

        series2Format.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        plot.addSeries(newStroke, series1Format);
        plot.addSeries(referenceStroke, series2Format);

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                int i = Math.round(((Number) obj).floatValue());
                return toAppendTo.append(domainLabels[i]);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });
    }
}
