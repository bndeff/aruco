package app.aruco;

import static java.lang.Double.max;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends CameraActivity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "ArucoRemote";
    private static final String PREF_KEY = "aruco";
    private static final int defaultDict = Aruco.DICT_6X6_100;
    private CameraBridgeViewBase mCamera;
    private String server = "";
    private int dictionary = defaultDict;
    private String msg = "";
    private Instant msgTime = null;
    private String arucoData = "";
    private int cCols = 0;
    private int cRows = 0;
    private RequestQueue queue;
    Mat rgba;
    Mat rgb;
    Mat gray;
    MatOfInt ids;
    List<Mat> corners;
    Mat bbox;

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mCamera = findViewById(R.id.camera_view);
        mCamera.setVisibility(SurfaceView.VISIBLE);
        mCamera.setCvCameraViewListener(this);
        mCamera.setOnTouchListener(this);
        queue = Volley.newRequestQueue(this);
    }

    void savePreferences() {
        SharedPreferences sp = this.getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("server", server);
        spe.putInt("dictionary", dictionary);
        spe.apply();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mCamera != null)
            mCamera.disableView();
        savePreferences();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found");
        } else {
            Log.i(TAG, "OpenCV loaded successfully");
            mCamera.enableView();
        }
        SharedPreferences sp = this.getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        server = sp.getString("server", "");
        dictionary = sp.getInt("dictionary", defaultDict);
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mCamera);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mCamera != null)
            mCamera.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    private void showMessage(String message) {
        msg = message;
        msgTime = Instant.now().plusSeconds(3);
    }

    private int parseDict(String dict) {
        switch (dict) {
            case "DICT_4X4_50": return Aruco.DICT_4X4_50;
            case "DICT_4X4_100": return Aruco.DICT_4X4_100;
            case "DICT_4X4_250": return Aruco.DICT_4X4_250;
            case "DICT_4X4_1000": return Aruco.DICT_4X4_1000;
            case "DICT_5X5_50": return Aruco.DICT_5X5_50;
            case "DICT_5X5_100": return Aruco.DICT_5X5_100;
            case "DICT_5X5_250": return Aruco.DICT_5X5_250;
            case "DICT_5X5_1000": return Aruco.DICT_5X5_1000;
            case "DICT_6X6_50": return Aruco.DICT_6X6_50;
            case "DICT_6X6_100": return Aruco.DICT_6X6_100;
            case "DICT_6X6_250": return Aruco.DICT_6X6_250;
            case "DICT_6X6_1000": return Aruco.DICT_6X6_1000;
            case "DICT_7X7_50": return Aruco.DICT_7X7_50;
            case "DICT_7X7_100": return Aruco.DICT_7X7_100;
            case "DICT_7X7_250": return Aruco.DICT_7X7_250;
            case "DICT_7X7_1000": return Aruco.DICT_7X7_1000;
            case "DICT_APRILTAG_16h5": return Aruco.DICT_APRILTAG_16h5;
            case "DICT_APRILTAG_25h9": return Aruco.DICT_APRILTAG_25h9;
            case "DICT_APRILTAG_36h10": return Aruco.DICT_APRILTAG_36h10;
            case "DICT_APRILTAG_36h11": return Aruco.DICT_APRILTAG_36h11;
            case "DICT_ARUCO_ORIGINAL": return Aruco.DICT_ARUCO_ORIGINAL;
            default: return -1;
        }
    }

    private void processQR(String qr_data) {
        boolean valid = true;
        String newServer = "";
        String newDictName = "";
        int newDict = -1;
        String [] splits = qr_data.split(" ", 2);
        for(String split: splits) {
            if (split.startsWith("http")) {
                newServer = split;
            } else if (split.startsWith("DICT")) {
                int dv = parseDict(split);
                if (dv != -1) {
                    newDictName = split;
                    newDict = dv;
                } else {
                    valid = false;
                }
            } else {
                valid = false;
            }
        }
        if (!valid || (newServer.isEmpty() && newDict == -1)) {
            showMessage("Cannot parse QR code as server or dictionary");
        } else {
            String qrMsg = "";
            if (!newServer.isEmpty()) {
                server = newServer;
                qrMsg = String.format("Server set to %s", newServer);
            }
            if (newDict != -1) {
                dictionary = newDict;
                if (!qrMsg.isEmpty()) {
                    qrMsg += String.format(", dictionary set to %s", newDictName);
                } else {
                    qrMsg = String.format("Dictionary set to %s", newDictName);
                }
            }
            savePreferences();
            showMessage(qrMsg);
        }
    }

    private void showText(Mat img, String text, Point org, double[] gravity, Scalar color) {
        int font = Imgproc.FONT_HERSHEY_SIMPLEX;
        int scale = 1;
        int thickness = 2;
        int[] baseline = {0};
        Size ts = Imgproc.getTextSize(text, font, scale, thickness, baseline);
        Point pt = new Point(org.x - gravity[0] * ts.width,
                                org.y + gravity[1] * ts.height + gravity[2] * baseline[0]);
        Imgproc.putText(img, text, pt, font, scale, color, thickness, Imgproc.LINE_AA);
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        rgba = inputFrame.rgba();
        cCols = rgba.cols();
        cRows = rgba.rows();

        if (rgb == null) rgb = new Mat();
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);

        if (gray == null) gray = new Mat();
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY);

        if (ids == null) ids = new MatOfInt();
        if (corners == null) corners = new LinkedList<>();
        Dictionary dict = Aruco.getPredefinedDictionary(dictionary);
        DetectorParameters params = DetectorParameters.create();
        Aruco.detectMarkers(gray, dict, corners, ids, params);

        StringBuilder arucoDataBuilder = new StringBuilder();
        arucoDataBuilder.append("[");
        if(!corners.isEmpty()) {
            int n = corners.size();
            for (int i=0; i<n; ++i) {
                if (i > 0) arucoDataBuilder.append(",");
                Mat c = corners.get(i);
                double[] ida = ids.get(new int[]{i, 0});
                int id = (int) ida[0];
                if(c.rows() == 1 && c.cols() == 4) {
                    Point qctr = new Point(0, 0);
                    Point qdir = new Point(0, 0);
                    for (int j=0; j<4; ++j) {
                        Point p1 = new Point(c.get(0, j));
                        Point p2 = new Point(c.get(0, (j+1)%4));
                        Imgproc.line(rgb, p1, p2, new Scalar(0, 255, 0), 2);
                        qctr.x += p1.x;
                        qctr.y += p1.y;
                        qdir.x += (j > 1) ? -p1.x : p1.x;
                        qdir.y += (j > 1) ? -p1.y : p1.y;
                    }
                    Point ctr = new Point(qctr.x / 4, qctr.y / 4);
                    Point dir = new Point(qdir.x / 4, qdir.y / 4);
                    Point trg = new Point(ctr.x + dir.x, ctr.y + dir.y);
                    Imgproc.line(rgb, ctr, trg, new Scalar(0, 255, 0), 2);
                    showText(rgb, Integer.toString(id), ctr, new double[]{0.5,0.5,0}, new Scalar(0, 255, 0));
                    arucoDataBuilder.append("[").append((int) ctr.x).append(",")
                            .append((int) ctr.y).append(",")
                            .append((int) dir.x).append(",")
                            .append((int) dir.y).append(",")
                            .append(id).append("]");
                }
            }
        }
        arucoData = arucoDataBuilder.append(']').toString();
        for (Mat c : corners) c.release();

        if (bbox == null) bbox = new Mat();
        QRCodeDetector qr = new QRCodeDetector();
        String qr_data = qr.detectAndDecode(gray, bbox);
        if(bbox.rows() == 1 && bbox.cols() == 4 && !qr_data.isEmpty()) {
            for(int i=0; i<4; ++i) {
                Point p1 = new Point(bbox.get(0, i));
                Point p2 = new Point(bbox.get(0, (i+1)%4));
                Imgproc.line(rgb, p1, p2, new Scalar(0, 0, 255), 3);
            }
            processQR(qr_data);
        }

        if (msgTime != null && msgTime.compareTo(Instant.now()) > 0) {
            showText(rgb, msg, new Point(64, 64), new double[]{0, 0, 1},
                    new Scalar(255, 255, 255));
        }

        return rgb;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (server.isEmpty()) {
                showMessage("Scan QR code with server address to set up touch-to-send");
            } else {
                int tCols = mCamera.getWidth();
                int tRows = mCamera.getHeight();
                double ratio = max((double) cCols / tCols, (double) cRows / tRows);
                int touchX = (int) ((event.getX() - (double) tCols / 2) * ratio + (double) cCols / 2);
                int touchY = (int) ((event.getY() - (double) tRows / 2) * ratio + (double) cRows / 2);
                byte[] requestData = ("[[" + cCols + "," + cRows + "],[" +
                        touchX + "," + touchY + "]," + arucoData + "]").getBytes(StandardCharsets.UTF_8);
                StringRequest sr = new StringRequest(Request.Method.POST, server, response -> {
                    showMessage("Server reply: " + response);
                }, error -> {
                    showMessage("Error: " + error.getMessage());
                }) {
                    @Override
                    public byte[] getBody() {
                        return requestData;
                    }
                };
                sr.setRetryPolicy(new DefaultRetryPolicy(0,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                queue.add(sr);
                showMessage("Sending request");
            }
        }
        return false;
    }
}
