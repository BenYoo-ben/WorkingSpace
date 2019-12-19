package com.example.parking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;


// Get video data from Camera = CameraPreview
class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {

    private final String TAG = "CameraPreview";

    private int mCameraID;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mDisplayOrientation;
    private List<Size> mSupportedPreviewSizes;
    private Size mPreviewSize;
    private boolean isPreview = false;

    private AppCompatActivity mActivity;
    private String resultString = "";
   private MLKit mlk;
    protected void getMLK(MLKit mlk)
    {
        this.mlk = mlk;
    }

    private int focusing=0;

    private int scaledX=400, scaledY=400;
    private long imagecode;

    private Context mContext;

    private ImageView IV;
    private ImageView CI;

    public void getmContext(Context c){this.mContext=c;}
    public void getImageView(ImageView iv)
    {
        this.IV = iv;
    }
    int ThreadCount =0;

    public CameraPreview(Context context, AppCompatActivity activity, int cameraID, SurfaceView surfaceView, ImageView CI) {
        super(context);


        Log.d("@@@", "Preview");

        this.CI = CI;

        mActivity = activity;
        mCameraID = cameraID;
        mSurfaceView = surfaceView;


        mSurfaceView.setVisibility(View.VISIBLE);


        // submit SurfaceHolder.Callback to get timing of surface's life cycle
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

    }





    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }



    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }



    // Letting know where to show preview in the surface.
    public void surfaceCreated(SurfaceHolder holder) {

        // Open an instance of the camera
        try {
            mCamera = Camera.open(mCameraID); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Camera " + mCameraID + " is not available: " + e.getMessage());
        }


        // retrieve camera's info.
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, cameraInfo);

        mCameraInfo = cameraInfo;
        mDisplayOrientation = mActivity.getWindowManager().getDefaultDisplay().getRotation();

        int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
        mCamera.setDisplayOrientation(orientation);



        mSupportedPreviewSizes =  mCamera.getParameters().getSupportedPreviewSizes();
        requestLayout();

        // get Camera parameters
        Camera.Parameters params = mCamera.getParameters();

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            // set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            // set Camera parameters
            mCamera.setParameters(params);
        }


        try {

            mCamera.setPreviewDisplay(holder);


            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            mCamera.startPreview();
            isPreview = true;
            Log.d(TAG, "Camera preview started.");





        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }



    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Release the camera for other applications.
        if (mCamera != null) {
            if (isPreview)
                mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            isPreview = false;
        }

    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }



    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "Preview surface does not exist");
            return;
        }


        // stop preview before making changes
        try {
           // mCamera.stopPreview();
            /******************************
             * Solution to Camera Pause ? ?  ? ? ?
             */
            Log.d(TAG, "Preview stopped.");
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
        mCamera.setDisplayOrientation(orientation);

        try {
            mCamera.setPreviewDisplay(mHolder);
            /******************************
             * Solution to Camera Pause ? ?  ? ? ?
             */
         //   mCamera.startPreview();
            Log.d(TAG, "Camera preview started.");
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }



//to adjust to camera's rotate state.
    public static int calculatePreviewOrientation(Camera.CameraInfo info, int rotation) {
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }


    public void Focused() {
        if (focusing == 0) {
            focusing=1;
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    focusing=0;
                }
            });
        }
    }
    public void takePicture(){


        mCamera.autoFocus (new Camera.AutoFocusCallback() {

            public void onAutoFocus(boolean success, Camera camera) {

                Log.d("TakePic","Focusing...");
                if(success){

                    Log.d("TakePic","Success!");
                TakePic tp = new TakePic(mCamera);
                tp.start();

                }
                else
                {
                    Log.d("TakePic","Failure");
                }

            }

        });
    }


    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {

        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };







    class TakePic extends Thread implements Runnable
    {

        private Camera mCamera;
        private Activity CM;

        TakePic(Camera mCamera)
        {
            this.mCamera = mCamera;
            Log.d("TakePic","ThreadBorn"+ThreadCount);
            ThreadCount++;
        }


        @Override
        public void destroy() {
            super.destroy();
            Log.d("TakePic","ThreadDying"+ThreadCount);
            ThreadCount--;
        }

        Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(final byte[] data, final Camera camera) {

                new ProcessImageTask(data).execute();


            }
        };
        @Override
        public void run()
        {


            mActivity.runOnUiThread(new Runnable()
            {

                @Override
                public void run() {
                    Log.d("TakePic","Setting Loading IV..");
                    IV.setVisibility(View.GONE);
                    CI.setVisibility(View.VISIBLE);
                }
            }
            );


            this.mCamera.takePicture(null, null, this.jpegCallback);


            Log.d("TakePic","After TakePhoto");

            try {
                this.sleep(1000);
                mCamera.stopPreview();
                mCamera.startPreview();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        };

        private class SaveImageTask extends AsyncTask<byte[], Void, Void> {



            @Override
            protected Void doInBackground(byte[]... data) {
                FileOutputStream outStream = null;


                try {

                    File path = new File (mContext.getFilesDir().getAbsolutePath());
                    if (!path.exists()) {
                        path.mkdirs();
                    }
                    Log.d("PATH",path.toString());

                    String fileName =imagecode+".jpg";
                    File outputFile = new File(path, fileName);
                    File outputText = new File(path,"ResultText.txt");



                    outStream = new FileOutputStream(outputFile);
                    outStream.write(data[0]);
                    outStream.flush();

                    outStream = new FileOutputStream(outputText);
                    if(resultString!=null) {
                        byte[] tmpdata = resultString.getBytes();
                        outStream.write(tmpdata);
                    }
                    outStream.close();

                    Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to "
                            + outputFile.getAbsolutePath());





                    // Add to gallery
                    Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(outputFile));
                    getContext().sendBroadcast(mediaScanIntent);



                    try {
                        mCamera.setPreviewDisplay(mHolder);
                        mCamera.startPreview();
                        Log.d(TAG, "Camera preview started.");
                    } catch (Exception e) {
                        Log.d(TAG, "Error starting camera preview: " + e.getMessage());
                    }

                    //imagecoe to mlkit
                    mlk.setImageCode(imagecode);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

        }

        private class ProcessImageTask extends AsyncTask<byte[], Void, Void> {

            byte[] data;
            byte[] currentData;

            ProcessImageTask(final byte[] data){
                this.data = data;
            }
            @Override
            protected Void doInBackground(byte[]... datab) {
                Log.d("TakePic","HERE?");
                //이미지의 너비와 높이 결정

                int w = mCamera.getParameters().getPictureSize().width;
                int h = mCamera.getParameters().getPictureSize().height;
                int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);


                //byte array를 bitmap으로 변환
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeByteArray( data, 0, data.length, options);


                //이미지를 디바이스 방향으로 회전
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                bitmap =  Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
                Calendar c = Calendar.getInstance();

                imagecode = c.getTimeInMillis();

                MLKit mlkOnThread = new MLKit(mlk,mContext);
                mlkOnThread.runTextRecognition(bitmap);
                mlkOnThread.setImageCode(c.getTimeInMillis());

                // mlk.runTextRecognition(bitmap);




                final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,scaledX, scaledY, true);
                //bitmap을 byte array로 변환
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                currentData = stream.toByteArray();

                //파일로 저장





                mActivity.runOnUiThread(new Runnable()
                {

                    @Override
                    public void run() {
                        IV.setVisibility(View.VISIBLE);
                        IV.setImageBitmap(scaledBitmap);
                        CI.setVisibility(View.GONE);
                    }
                });


                Log.d("TakePic","ThreadInterrupt"+ThreadCount);
                interrupt();

                return null;
            }


            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);


                new SaveImageTask().execute(currentData);
                Log.d("TakePic","ImageSaveStart");
            }




        }
    }

}