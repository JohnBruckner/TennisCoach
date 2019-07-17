package uk.ed.ac.specknet.tenniscoach;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import net.seninp.jmotif.sax.SAXProcessor;
import net.seninp.jmotif.sax.TSProcessor;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RatingService extends AsyncTask<String, Void, Double> {

    private WeakReference<Context> mContext;
    public AsyncResponse delegate = null;
    private Integer classification;

    private static final Integer nrPAASegments = 11;
    private static final Integer nrSAXSymbols = 10;

    private static final String FServe1 = Environment.getExternalStorageState() + File.separator + "serve1.csv";
    private static final String FServe2 = Environment.getExternalStorageState() + File.separator + "serve2.csv";
    private static final String FServe3 = Environment.getExternalStorageState() + File.separator + "serve3.csv";
    private static final String FForehand1 = Environment.getExternalStorageState() + File.separator + "forehand1.csv";
    private static final String FForehand2 = Environment.getExternalStorageState() + File.separator + "forehand2.csv";
    private static final String FForehand3 = Environment.getExternalStorageState() + File.separator + "forehand3.csv";

    private double[] AccelYServe1;
    private double[] AccelYServe2;
    private double[] AccelYServe3;
    private double[] AccelYForehand1;
    private double[] AccelYForehand2;
    private double[] AccelYForehand3;

    double[][] greenStrokes;

    private NormalAlphabet normalAlphabet = new NormalAlphabet();
    private SAXProcessor saxProcessor = new SAXProcessor();
    private TSProcessor tsProcessor = new TSProcessor();

    public RatingService(Context context, Integer classification) {
        this.mContext = new WeakReference<>(context);

        if (classification == 0) {
            try {
                this.AccelYServe1 = TSProcessor.readFileColumn(FServe1, 5, 0);
                this.AccelYServe2 = TSProcessor.readFileColumn(FServe2, 5, 0);
                this.AccelYServe3 = TSProcessor.readFileColumn(FServe3, 5, 0);
            }
            catch (Exception e) {
                Log.e("Classification", "Caught Exception: " + e.getMessage());

                this.AccelYServe1 = null;
                this.AccelYServe2 = null;
                this.AccelYServe3 = null;
                this.AccelYForehand1 = null;
                this.AccelYForehand2 = null;
                this.AccelYForehand3 = null;
            }
            try {
                this.AccelYServe1 = TSProcessor.readFileColumn(FServe1, 5, 0);
                this.AccelYServe2 = TSProcessor.readFileColumn(FServe2, 5, 0);
                this.AccelYServe3 = TSProcessor.readFileColumn(FServe3, 5, 0);
            } catch (Exception e) {
                Log.e("Classification", "Caught Exception: " + e.getMessage());

                this.AccelYServe1 = null;
                this.AccelYServe2 = null;
                this.AccelYServe3 = null;
                this.AccelYForehand1 = null;
                this.AccelYForehand2 = null;
                this.AccelYForehand3 = null;
            }
            greenStrokes = new double[][]{this.AccelYServe1,
                                          this.AccelYServe2,
                                          this.AccelYServe3};
        } else {
            try {
                this.AccelYForehand1 = TSProcessor.readFileColumn(FForehand1, 5, 0);
                this.AccelYForehand2 = TSProcessor.readFileColumn(FForehand2, 5, 0);
                this.AccelYForehand3 = TSProcessor.readFileColumn(FForehand3, 5, 0);
            } catch (Exception e) {
                Log.e("Classification", "Caught Exception: " + e.getMessage());

                this.AccelYServe1 = null;
                this.AccelYServe2 = null;
                this.AccelYServe3 = null;
                this.AccelYForehand1 = null;
                this.AccelYForehand2 = null;
                this.AccelYForehand3 = null;
            }
            try {
                this.AccelYServe3 = TSProcessor.readFileColumn(FServe3, 5, 0);
                this.AccelYForehand1 = TSProcessor.readFileColumn(FForehand1, 5, 0);
                this.AccelYForehand2 = TSProcessor.readFileColumn(FForehand2, 5, 0);
                this.AccelYForehand3 = TSProcessor.readFileColumn(FForehand3, 5, 0);
            } catch (Exception e) {
                Log.e("Classification", "Caught Exception: " + e.getMessage());

                this.AccelYServe1 = null;
                this.AccelYServe2 = null;
                this.AccelYServe3 = null;
                this.AccelYForehand1 = null;
                this.AccelYForehand2 = null;
                this.AccelYForehand3 = null;
            }
            greenStrokes = new double[][]{this.AccelYForehand1,
                                          this.AccelYForehand2,
                                          this.AccelYForehand3};
        }
    }

    @Override
    protected Double doInBackground(String... strings) {
        Double[] newStroke;
        DTW dtw = new DTW();
        DTW.Result result;

        try {
            newStroke = ArrayUtils.toObject(TSProcessor.readFileColumn(strings[0], 6, 0));
        } catch (Exception e) {
            Log.e("Classification", "Caught Exception: " + e.getMessage());

            newStroke = null;
        }

        if (newStroke == null || newStroke.length == 0) {
            return -1.;
        } else {
            newStroke = normalise(newStroke);
        }

        try {
            newStroke = ArrayUtils.toObject(tsProcessor.paa(ArrayUtils.toPrimitive(newStroke), nrPAASegments));
        } catch (Exception e) {
            Log.e("Classification", "Caught Exception: " + e.getMessage());

            newStroke = null;
        }

        if (newStroke == null || newStroke.length == 0) {
            return -1.;
        }

        if (classification == 0) {
            result = dtw.compute(toFloatArray(ArrayUtils.toPrimitive(newStroke)), toFloatArray(AccelYServe1));
        } else {
            result = dtw.compute(toFloatArray(ArrayUtils.toPrimitive(newStroke)), toFloatArray(AccelYForehand1));
        }

        return result.getDistance();
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

    float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float)arr[i];
        }
        return ret;
    }
}
