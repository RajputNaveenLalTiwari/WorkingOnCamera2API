package examples.com.workingoncamera2api;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private Size preferredCameraPreviewSize;
    private String myCameraId;
    private TextureView textureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
                {
                    setupCamera(width,height);

                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
                {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
                {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface)
                {

                }
            };

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceStateCallback =
            new CameraDevice.StateCallback()
            {
                @Override
                public void onOpened(CameraDevice camera)
                {
                    cameraDevice = camera;
                    // Toast.makeText(getApplicationContext(),"Camera Opened", Toast.LENGTH_LONG).show();
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(CameraDevice camera)
                {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error)
                {
                    camera.close();
                    cameraDevice = null;
                }
            };

    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;

    private CameraCaptureSession cameraCaptureSession;
    private CameraCaptureSession.CaptureCallback cameraCaptureSessionCallback =
            new CameraCaptureSession.CaptureCallback()
            {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber)
                {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
                {
                    super.onCaptureCompleted(session, request, result);

                    process( result );
                }

                private void process(CaptureResult result)
                {
                    switch ( state )
                    {
                        case STATE_PREVIEW:
                            // Do Nothing
                            break;

                        case STATE_WAIT_LOCK:
                            Integer autoFocusState = result.get( CaptureResult.CONTROL_AF_STATE );

                            if( autoFocusState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED )
                            {
                                /*unLockFocus();
                                Toast.makeText(getApplicationContext(),"Focus Lock Successful",Toast.LENGTH_LONG).show();*/
                                capturingImage();
                            }
                            break;
                    }
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure)
                {
                    super.onCaptureFailed(session, request, failure);
                    Toast.makeText(getApplicationContext(),"Focus Lock Unsuccessful",Toast.LENGTH_LONG).show();
                }
            };

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int state;

    private static File imageFile;

    private ImageReader imageReader;
    private final ImageReader.OnImageAvailableListener onImageAvailableListener =
            new ImageReader.OnImageAvailableListener()
            {
                @Override
                public void onImageAvailable(ImageReader reader)
                {
                    backgroundHandler.post(new ImageSaver(reader.acquireNextImage()));
                }
            };

    private static class ImageSaver implements Runnable
    {
        private final Image image;

        private ImageSaver( Image image )
        {
            this.image = image;
        }

        @Override
        public void run()
        {
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();

            byte[] bytes = new byte[byteBuffer.remaining()];

            byteBuffer.get(bytes);

            FileOutputStream fileOutputStream = null;

            try
            {
                fileOutputStream = new FileOutputStream(imageFile);

                fileOutputStream.write(bytes);

            }
            catch ( Exception e )
            {
                Log.e("FileOutputStream","Error while writing image into file");
                e.printStackTrace();
            }
            finally
            {
                image.close();

                if( fileOutputStream != null )
                {
                    try
                    {
                        fileOutputStream.close();
                    } catch (Exception e)
                    {
                        Log.e("FileOutputStream", "Error while closing a file");
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,270);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = ( TextureView ) findViewById( R.id.textureView );
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        openBackgroundHanderThread();

        if( textureView.isAvailable() )
        {
            setupCamera(textureView.getWidth(),textureView.getHeight());

            openCamera();
        }
        else
        {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause()
    {
        closeCamera();

        closeBackgroundHanderThread();

        super.onPause();
    }


    private void setupCamera(int width, int height)
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try
        {
            for( String cameraId : cameraManager.getCameraIdList())
            {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

                if( cameraCharacteristics.get( CameraCharacteristics.LENS_FACING ) == CameraCharacteristics.LENS_FACING_FRONT )
                {
                    continue;
                }

                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get( CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP );

                Size largestImageSize = Collections.max(
                    Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
                    new Comparator<Size>()
                    {
                        @Override
                        public int compare(Size lhsObject, Size rhsObject)
                        {
                            return Long.signum((lhsObject.getWidth()*lhsObject.getHeight())-(rhsObject.getWidth()*rhsObject.getHeight()));
                        }
                    }
                );

                imageReader = ImageReader.newInstance
                        (
                            largestImageSize.getWidth(),
                            largestImageSize.getHeight(),
                            ImageFormat.JPEG,
                            1
                        );

                imageReader.setOnImageAvailableListener
                        (
                            onImageAvailableListener,
                            backgroundHandler
                        );

                preferredCameraPreviewSize = getPreferredPreviewSize( streamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height );
                    // streamConfigurationMap.getOutputSizes(SurfaceTexture.class) returns array of preview sizes

                myCameraId = cameraId;

                return;
            }
        }
        catch (Exception e)
        {
            Log.e("setupCamera()","Error While camera setup");
            e.printStackTrace();
        }

    }

    private Size getPreferredPreviewSize( Size[] streamConfigurationMapSize, int width, int height )
    {
        List<Size> listOfSizes = new ArrayList<>();

        for ( Size preferredOption : listOfSizes )
        {
            if( width > height )    // LandScape Mode
            {
                if( (preferredOption.getWidth() > width) && (preferredOption.getHeight() > height) )
                {
                    listOfSizes.add( preferredOption );
                }
            }
            else                    // Portrait Mode
            {
                if( (preferredOption.getWidth() > height) && (preferredOption.getHeight() > width) )
                {
                    listOfSizes.add( preferredOption );
                }
            }
        }

        if( listOfSizes.size() > 0 )
        {
            return Collections.min(listOfSizes, new Comparator<Size>()
            {
                @Override
                public int compare(Size lhsObject, Size rhsObject)
                {
                    return Long.signum( (lhsObject.getWidth() * lhsObject.getHeight()) - (rhsObject.getWidth() * rhsObject.getHeight()) );
                }
            });
        }

        return streamConfigurationMapSize[0];
    }

    private void openCamera()
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try
        {
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            cameraManager.openCamera(
                    myCameraId,
                    cameraDeviceStateCallback,
                    backgroundHandler
            );
        }
        catch (Exception e)
        {
            Log.e("openCamera()","Error While opening camera");
            e.printStackTrace();
        }
    }

    private void closeCamera()
    {
        if( cameraCaptureSession != null )
        {
            cameraCaptureSession.close();

            cameraCaptureSession = null;
        }

        if( cameraDevice != null )
        {
            cameraDevice.close();

            cameraDevice = null;
        }

        if( imageReader != null )
        {
            imageReader.close();

            imageReader = null;
        }
    }

    private void createCameraPreviewSession()
    {
        try
        {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();

            surfaceTexture.setDefaultBufferSize(preferredCameraPreviewSize.getWidth(),preferredCameraPreviewSize.getHeight());

            Surface previewSurface = new Surface(surfaceTexture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            captureRequestBuilder.addTarget( previewSurface );

            cameraDevice.createCaptureSession
                    (
                        Arrays.asList(previewSurface, imageReader.getSurface()),
                        new CameraCaptureSession.StateCallback()
                        {
                            @Override
                            public void onConfigured(CameraCaptureSession session)
                            {
                                if( cameraDevice == null )
                                {
                                    return;
                                }

                                try
                                {
                                    captureRequest = captureRequestBuilder.build();

                                    cameraCaptureSession = session;

                                    cameraCaptureSession.setRepeatingRequest
                                            (
                                                captureRequest,
                                                cameraCaptureSessionCallback,
                                                null
                                            );
                                }
                                catch ( Exception e )
                                {
                                    Log.e("createCaptureSession()","Error while creating capture session");
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session)
                            {
                                Toast.makeText(getApplicationContext(),"create camera session failed",Toast.LENGTH_LONG).show();
                            }
                        },
                        backgroundHandler
                    );
        }
        catch ( Exception e )
        {
            Log.e("createCameraPreview","Error while creating camera preview session \n");
            e.printStackTrace();
        }
    }

    private void openBackgroundHanderThread()
    {
        backgroundHandlerThread = new HandlerThread("Camera2 Background Thread");

        backgroundHandlerThread.start();

        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void closeBackgroundHanderThread()
    {
        backgroundHandlerThread.quitSafely();

        try
        {
            backgroundHandlerThread.join();

            backgroundHandlerThread = null;

            backgroundHandler = null;
        }
        catch ( Exception e )
        {
            Log.e("CloseBackgroundThread","Error while closing background thread");
            e.printStackTrace();
        }
    }

    public void takePhoto( View view )
    {
        lockFocus();
    }

    private void lockFocus()
    {
        try
        {
            state = STATE_WAIT_LOCK;

            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);

            cameraCaptureSession.capture(captureRequestBuilder.build(),cameraCaptureSessionCallback,backgroundHandler);
        }
        catch (Exception e)
        {
            Log.e("lockFocus()","Error while focusing lock on image");
            e.printStackTrace();
        }
    }

    private void unLockFocus()
    {
        try
        {
            state = STATE_PREVIEW;

            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

            cameraCaptureSession.capture(captureRequestBuilder.build(),cameraCaptureSessionCallback,backgroundHandler);
        }
        catch (Exception e)
        {
            Log.e("lockFocus()","Error while focusing lock on image");
            e.printStackTrace();
        }
    }

    private Size getLargestImageSize(StreamConfigurationMap streamConfigurationMap)
    {
        Size size = Collections.max(
                Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
                new Comparator<Size>()
                {
                    @Override
                    public int compare(Size lhsObject, Size rhsObject)
                    {
                        return Long.signum((lhsObject.getWidth()*lhsObject.getHeight())-(rhsObject.getWidth()*rhsObject.getHeight()));
                    }
                }
        );
        return size;
    }

    private void capturingImage()
    {
        try
        {
            CaptureRequest.Builder captureRequestBuilderLocal = cameraDevice.createCaptureRequest( CameraDevice.TEMPLATE_STILL_CAPTURE );

            captureRequestBuilderLocal.addTarget( imageReader.getSurface() );

            int displayRotation = getWindowManager().getDefaultDisplay().getRotation();

            captureRequestBuilderLocal.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(displayRotation));

            CameraCaptureSession.CaptureCallback cameraCaptureSessionCallbackLocal =
                    new CameraCaptureSession.CaptureCallback()
                    {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
                        {
                            super.onCaptureCompleted(session, request, result);

                            Toast.makeText(getApplicationContext(),"Image Captured",Toast.LENGTH_LONG).show();

                            unLockFocus();
                        }
                    };

                    cameraCaptureSession.capture
                                (
                                    captureRequestBuilderLocal.build(),
                                    cameraCaptureSessionCallbackLocal,
                                    null
                                );
        }
        catch ( Exception e )
        {
            Log.e("capturingImage()","Error while capturing image");
            e.printStackTrace();
        }
    }
}
