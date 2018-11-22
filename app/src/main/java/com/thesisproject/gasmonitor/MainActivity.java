package com.thesisproject.gasmonitor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.thesisproject.gasmonitor.api.ApiUtils;
import com.thesisproject.gasmonitor.api.service.OutputService;
import com.thesisproject.gasmonitor.api.service.TokenService;
import com.thesisproject.gasmonitor.model.Coordinate;
import com.thesisproject.gasmonitor.model.Output;
import com.thesisproject.gasmonitor.model.Token;
import com.thesisproject.gasmonitor.modeldata.CoordinatesData;
import com.github.anastr.speedviewlib.PointerSpeedometer;
import com.github.anastr.speedviewlib.SpeedView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 1;
    private CameraManager cameraManager;
    private int cameraFacing;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private String cameraId;
    private Size previewSize;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback stateCallback;
    private TextureView textureView;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private CameraCaptureSession cameraCaptureSession;
    private PointerSpeedometer carbonMonxideMeter;
    private SpeedView carbonDiOxideMeter;
    private LineChart lineChart;
    private Token token;
    Handler handler;
    private Runnable runnable;

    public String TAG = getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getAuthenticationToken();
        handler = new Handler();

        textureView = findViewById(R.id.texture_view);

        lineChart = findViewById(R.id.line_chart);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setUpCamera();
                openCamera();

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                MainActivity.this.cameraDevice = camera;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                camera.close();
                MainActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                camera.close();
                MainActivity.this.cameraDevice = null;
            }
        };

        List<Entry> entries = new ArrayList<>();

        CoordinatesData data = new CoordinatesData();

        for (Coordinate codata : data.getData()) {
            entries.add(new Entry(codata.getX(), codata.getY()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Random Data");
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.parseColor("#FFFFFF"));
        legend.setForm(Legend.LegendForm.CIRCLE);

        final LineData lineData = new LineData(dataSet);
        lineData.setValueTextColor(Color.parseColor("#FFFFFF"));
        lineData.setHighlightEnabled(true);

        lineChart.setData(lineData);
        lineChart.fitScreen();

        XAxis xAxis = lineChart.getXAxis();
        YAxis leftY = lineChart.getAxisLeft();
        YAxis right = lineChart.getAxisRight();

        xAxis.setGridColor(Color.parseColor("#FFFFFF"));
        xAxis.setTextColor(Color.parseColor("#FFFFFF"));

        leftY.setGridColor(Color.parseColor("#FFFFFF"));
        leftY.setTextColor(Color.parseColor("#FFFFFF"));

        right.setGridColor(Color.parseColor("#FFFFFF"));
        right.setTextColor(Color.parseColor("#FFFFFF"));

        lineChart.animateXY(3000, 3000);

        carbonMonxideMeter = findViewById(R.id.sv_co);
        carbonDiOxideMeter = findViewById(R.id.sv_co2);

        carbonMonxideMeter.setMaxSpeed(100f);
//        carbonMonxideMeter.speedTo(200f, 10000);

        // in this speedometer, you can change UnitText Size
        carbonMonxideMeter.setUnitTextSize(15); //def : 5dp
        // change the point color
        carbonMonxideMeter.setPointerColor(Color.RED);
        // change Sweep speedometer color
        carbonMonxideMeter.setSpeedometerColor(Color.GREEN);

        carbonDiOxideMeter.setMaxSpeed(100);
        carbonDiOxideMeter.speedTo(0);

        runOutputService();

    }

    private void runOutputService() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if(token != null)
                    getOutputData();
                else
                    Log.d(TAG, "Token is null");

                handler.postDelayed(this, 1500);
            }
        };

        handler.post(runnable);
    }

    private void getOutputData() {
        String tokenString = String.format("Bearer %s", token.getAccessToken());
        String accept = "application/json, text/plain, */*";
        OutputService outputService = ApiUtils.getOutputService();

        Call<Output> call = outputService.getDeviceOutput(tokenString, accept, "001");

        call.enqueue(new Callback<Output>() {
            @Override
            public void onResponse(Call<Output> call, Response<Output> response) {
                if (response.code() == 200) {
                    Output output = response.body();
                    Log.d(TAG, "Output: " + String.valueOf(output.getOut()));
                    setOutPutToUI(output);
                }
            }

            @Override
            public void onFailure(Call<Output> call, Throwable t) {

            }
        });
    }

    private void setOutPutToUI(Output output) {
        if (output != null)
            carbonMonxideMeter.speedTo(output.getOut(), 1000);
    }

    private void getAuthenticationToken() {

        TokenService tokenService = ApiUtils.getTokenService();
        Call<Token> call = tokenService.getToken("password", "naimKhan",
                "01943781335");

        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.code() == 200) {
                    Token token = response.body();
                    setToken(token);
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {

            }
        });
    }

    private void setToken(Token token) {
        this.token = token;
        runOutputService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void createPreviewSession() {

        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null)
                                return;
                            try {
                                captureRequest = captureRequestBuilder.build();
                                MainActivity.this.cameraCaptureSession = session;
                                MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException cae) {
            cae.printStackTrace();
        }

    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException cae) {
            cae.printStackTrace();
        }
    }

    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException cae) {
            cae.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
}
