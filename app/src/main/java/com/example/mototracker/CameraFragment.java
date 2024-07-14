package com.example.mototracker;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraFragment extends Fragment {
    private static final String ARG_METADATA_JSON = "metadataJSON";
    private JSONObjectWrapper _addMaintenanceDataJSON;
    private Auth0Authentication _auth0;
    private FragmentSwitcher _fragmentSwitcher;
    private ListenableFuture<ProcessCameraProvider> _cameraProviderFuture;
    private PreviewView _previewView;
    private SeekBar _zoomSeekBar;
    private ImageView _toggleFlashButton;
    private ImageCapture _imageCapture;
    private boolean _cameraAccessPermission;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance(String metadataJSON){
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_METADATA_JSON, metadataJSON);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            _addMaintenanceDataJSON = new JSONObjectWrapper(getArguments().getString(ARG_METADATA_JSON));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        //access the fragment switcher object
        _fragmentSwitcher = FragmentSwitcher.getInstance();

        //check to make sure that we are authenticated
        _auth0 = Auth0Authentication.getInstance(this.getContext());
        if(!_auth0.isAuthenticated()){
            _fragmentSwitcher.switchFragment(new HomeFragment(), getParentFragmentManager());
            return view;
        }

        ImageView pictureButton = view.findViewById(R.id.camera_take_photo);
        _toggleFlashButton = view.findViewById(R.id.camera_flash_toggle);
        _previewView = view.findViewById(R.id.camera_preview_view);
        _zoomSeekBar = view.findViewById(R.id.camera_zoom_seekBar);

        //check and request camera permission
        if(ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            //ask for camera permissions from user
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    _cameraAccessPermission = true;
                    setupCameraX();
                }
                else{
                    _cameraAccessPermission = false;
                }
            }).launch(Manifest.permission.CAMERA);
        }
        else{
            //permission already granted
            _cameraAccessPermission = true;
            setupCameraX();
        }

        pictureButton.setOnClickListener(v -> {
            if(_cameraAccessPermission){
                String task = _addMaintenanceDataJSON.getString("task");
                if(task.isEmpty() || task.equals("Take Photo")){
                    capturePhoto();
                }
                if(task.equals("Parse Text")){
                    capturePhotoParseText();
                }
            }
        });

        _toggleFlashButton.setOnClickListener(v -> {
            if(_cameraAccessPermission && _imageCapture != null){
                if(_imageCapture.getFlashMode() == ImageCapture.FLASH_MODE_ON){
                    _imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);
                    _toggleFlashButton.setImageDrawable(getResources().getDrawable(R.drawable.baseline_flash_off_24));
                }
                else{
                    _imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
                    _toggleFlashButton.setImageDrawable(getResources().getDrawable(R.drawable.baseline_flash_on_24));
                }
            }
        });

        return view;
    }

    private void setupCameraX(){
        _cameraProviderFuture = ProcessCameraProvider.getInstance(this.requireContext());
        _cameraProviderFuture.addListener(() -> {
            try{
                ProcessCameraProvider cameraProvider = _cameraProviderFuture.get();
                startCameraX(cameraProvider);
            }
            catch(ExecutionException | InterruptedException e){
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private Executor getExecutor(){
        return ContextCompat.getMainExecutor(this.requireContext());
    }

    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        //camera selector use case
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        //Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(_previewView.getSurfaceProvider());


        //image capture use case
        _imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, _imageCapture);
        setupZoomControls(camera);

        //set the initial state of the flashMode image
        if(_imageCapture.getFlashMode() == ImageCapture.FLASH_MODE_ON){
            _toggleFlashButton.setImageDrawable(getResources().getDrawable(R.drawable.baseline_flash_on_24));
        }
        else{
            _toggleFlashButton.setImageDrawable(getResources().getDrawable(R.drawable.baseline_flash_off_24));
        }
    }

    private void setupZoomControls(Camera camera) {
        _zoomSeekBar.setMax(100); // Assuming max zoom is 10x, adjust if necessary
        _zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (camera != null) {
                    float zoomRatio = 1f + (progress / 10f);
                    camera.getCameraControl().setZoomRatio(zoomRatio);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void capturePhoto(){
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        _imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        this.requireContext().getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri uri = outputFileResults.getSavedUri();
                        if(uri != null){
                            _addMaintenanceDataJSON.put("photoURI", uri.toString());
                        }
                        parentFragmentSwitcher();
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                }
        );
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void capturePhotoParseText(){
        _imageCapture.takePicture(ContextCompat.getMainExecutor(this.requireContext()), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Image mediaImage = image.getImage();
                if(mediaImage == null){
                    return;
                }
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(inputImage).addOnSuccessListener(text -> {
                    String result = text.getText();
                    _addMaintenanceDataJSON.put("parsedText", result);
                    parentFragmentSwitcher();
                });

                image.close();
            }
        });
    }

    private void parentFragmentSwitcher(){
        String parentFragmentName = _addMaintenanceDataJSON.getString("fragmentName");
        if(parentFragmentName.equals("MaintenanceLog")){
            _fragmentSwitcher.switchFragment(MaintenanceLogFragment.newInstance(_addMaintenanceDataJSON.toString()), getParentFragmentManager());
        }
        if(parentFragmentName.equals("Dashboard")){
            _fragmentSwitcher.switchFragment(DashboardFragment.newInstance(_addMaintenanceDataJSON.toString()), getParentFragmentManager());
        }
    }


}