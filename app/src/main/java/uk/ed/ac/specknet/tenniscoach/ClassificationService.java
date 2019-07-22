package uk.ed.ac.specknet.tenniscoach;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVReader;

import net.seninp.jmotif.sax.SAXProcessor;
import net.seninp.jmotif.sax.TSProcessor;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ClassificationService extends AsyncTask<String, Void, Integer> {
    private WeakReference<Context> mContext;
    public AsyncResponse classificationDelegate = null;

    private static final Integer NR_PAA_SEGMENTS = 11;
    private static final Integer NR_SAX_SYMBOLS = 10;
    private static final Integer N_THRESHOLD = 0;

    private static final String fServe1 = Environment.getExternalStorageDirectory() + File.separator + "TCSavedData" + File.separator +  "serve1.csv";
    private static final String fServe2 = Environment.getExternalStorageDirectory() + File.separator + "TCSavedData" + File.separator + "serve2.csv";
    private static final String fServe3 = Environment.getExternalStorageDirectory() + File.separator + "TCSavedData" + File.separator + "serve3.csv";
    private static final String fForehand1 = Environment.getExternalStorageDirectory() + File.separator + "TCSavedData" + File.separator + "forehand1.csv";
    private static final String fForehand2 = Environment.getExternalStorageDirectory() + File.separator + "TCSavedData" + File.separator + "forehand2.csv";
    private static final String fForehand3 = Environment.getExternalStorageDirectory() + File.separator + "TCSavedData" + File.separator + "forehand3.csv";

    private double[] accelYServe1;
    private double[] accelYServe2;
    private double[] accelYServe3;
    private double[] accelYForehand1;
    private double[] accelYForehand2;
    private double[] accelYForehand3;

    private String saxAccelYServe1;
    private String saxAccelYServe2;
    private String saxAccelYServe3;
    private String saxAccelYForehand1;
    private String saxAccelYForehand2;
    private String saxAccelYForehand3;

    String[] greenStrokes;

    private NormalAlphabet normalAlphabet = new NormalAlphabet();
    private SAXProcessor saxProcessor = new SAXProcessor();
    private TSProcessor tsProcessor = new TSProcessor();

    public ClassificationService(Context context) {
        this.mContext = new WeakReference<>(context);
        try {
            this.accelYServe1 = readCSVColumn(fServe1, 5);
            this.accelYServe2 = readCSVColumn(fServe2, 5);
            this.accelYServe3 = readCSVColumn(fServe3, 5);
            this.accelYForehand1 = readCSVColumn(fForehand1, 5);
            this.accelYForehand2 = readCSVColumn(fForehand2, 5);
            this.accelYForehand3 = readCSVColumn(fForehand3, 5);
        } catch (Exception e) {
            Log.e("Classification", "Caught Exception: " + e.getMessage());

            this.accelYServe1 = null;
            this.accelYServe2 = null;
            this.accelYServe3 = null;
            this.accelYForehand1 = null;
            this.accelYForehand2 = null;
            this.accelYForehand3 = null;
        }
        try {
            this.saxAccelYServe1 = saxProcessor.ts2saxByChunking(accelYServe1, NR_PAA_SEGMENTS, normalAlphabet.getCuts(NR_SAX_SYMBOLS), N_THRESHOLD).getSAXString("");
            this.saxAccelYServe2 = saxProcessor.ts2saxByChunking(accelYServe2, NR_PAA_SEGMENTS, normalAlphabet.getCuts(NR_SAX_SYMBOLS), N_THRESHOLD).getSAXString("");
            this.saxAccelYServe3 = saxProcessor.ts2saxByChunking(accelYServe3, NR_PAA_SEGMENTS, normalAlphabet.getCuts(NR_SAX_SYMBOLS), N_THRESHOLD).getSAXString("");
            this.saxAccelYForehand1 = saxProcessor.ts2saxByChunking(accelYForehand1, NR_PAA_SEGMENTS, normalAlphabet.getCuts(NR_SAX_SYMBOLS), N_THRESHOLD).getSAXString("");
            this.saxAccelYForehand2 = saxProcessor.ts2saxByChunking(accelYForehand2, NR_PAA_SEGMENTS, normalAlphabet.getCuts(NR_SAX_SYMBOLS), N_THRESHOLD).getSAXString("");
            this.saxAccelYForehand3 = saxProcessor.ts2saxByChunking(accelYForehand3, NR_PAA_SEGMENTS, normalAlphabet.getCuts(NR_SAX_SYMBOLS), N_THRESHOLD).getSAXString("");
        } catch (Exception e) {
            Log.e("Classification", "Caught Exception: " + e.getMessage());

            this.accelYServe1 = null;
            this.accelYServe2 = null;
            this.accelYServe3 = null;
            this.accelYForehand1 = null;
            this.accelYForehand2 = null;
            this.accelYForehand3 = null;
        }
        greenStrokes = new String[]{this.saxAccelYServe1,
                this.saxAccelYServe2,
                this.saxAccelYServe3,
                this.saxAccelYForehand1,
                this.saxAccelYForehand2,
                this.saxAccelYForehand3};
    }

    @Override
    protected Integer doInBackground(String... strings) {
        Double[] newStroke;
        String saxNewStroke;

        try {
            newStroke = ArrayUtils.toObject(readCSVColumn(strings[0], 5));
        } catch (Exception e) {
            Log.e("Classification", "Caught Exception: " + e.getMessage());

            newStroke = null;
        }

        if (newStroke == null || newStroke.length == 0) {
            return -1;
        } else {
            newStroke = normalise(newStroke);
        }

        try {
            saxNewStroke = saxProcessor.ts2saxByChunking(ArrayUtils.toPrimitive(newStroke), NR_PAA_SEGMENTS, normalAlphabet.getCuts(NR_SAX_SYMBOLS), N_THRESHOLD).getSAXString("");
        } catch (Exception e) {
            Log.e("Classification", "Caught Exception: " + e.getMessage());

            saxNewStroke = null;
        }

        if (newStroke == null || newStroke.length == 0) {
            return -1;
        }

        double dist = 0;
        Integer minDist = Integer.MAX_VALUE;
        Integer minDistIdx = 0;


        for (int i = 0; i < greenStrokes.length; i++) {
//            dist = paaDistance(newStroke, ArrayUtils.toObject(greenStrokes[i]));
            try {
                dist = saxProcessor.strDistance(saxNewStroke.toCharArray(), greenStrokes[i].toCharArray());
            } catch (Exception e) {
                Log.e("Classification", "Caught Exception: " + e.getMessage());

                return -1;
            }
            if (dist < minDist) {
                dist = minDist;
                minDistIdx = i;
            }
        }

        if (minDistIdx < 3) {
            return 0; //Serve
        } else {
            return 1; //Forehand
        }
    }

    public static Double[] normalise(Double[] timeSeries) {
        ArrayList<Double> normalisedData = new ArrayList<Double>();
        double sum = 0;

        int window = 3;

        for (int i = 0; i < timeSeries.length; i++) {
            sum = 0;
            if (i < window) {
                for (int j = i; j < i + window; j++) {
                    sum += timeSeries[j];
                }
                normalisedData.add(sum / window);
            } else if (i > (timeSeries.length - window)) {
                for (int j = i - window; j < i; j++) {
                    sum += timeSeries[j];
                }
                normalisedData.add(sum / window);
            } else {
                for (int j = i - window; j < i + window; j++) {
                    sum += timeSeries[j];
                }
                normalisedData.add(sum / window);
            }
        }

        Double[] data = normalisedData.toArray(new Double[normalisedData.size()]);

        return data;
    }

    public static Double paaDistance(Double[] ts1, Double[] ts2) {
        double sum = 0;

        for (int i = 0; i < ts1.length; i++) {
            sum += ts1[i] - ts2[i];
        }

        return Math.sqrt(sum);
    }

    protected double[] readCSVColumn(String fname, int column) {
        try {

            CSVReader reader = new CSVReader(new FileReader(fname));
            ArrayList<Double> data = new ArrayList<>();

            String[] nextLine;

            nextLine = reader.readNext(); //Read file header

            while ((nextLine = reader.readNext()) != null) {
                String entry = nextLine[column];
                data.add(Double.parseDouble(entry));

            }

            return ArrayUtils.toPrimitive(data.toArray(new Double[data.size()]));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        classificationDelegate.classificationFinish(integer);
    }
}
