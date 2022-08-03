package com.example.dlibmodule;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.dlibmodule.Utils.AspectTextureView;
import com.example.dlibmodule.Utils.ViewHardFilter;
import com.example.dlibmodule.tflite.SimilarityClassifier;
import com.example.dlibmodule.tflite.TFLiteObjectDetectionAPIModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import me.lake.librestreaming.client.RESClient;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.model.RESConfig;


public class LiveActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2,
        Button.OnClickListener, RESConnectionListener, TextureView.SurfaceTextureListener {
    private static final String TAG="MainActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private AspectTextureView camera_preview;

    private ImageView flip_camera;
    private int mCameraId = 0; // 0:후면 카메라 1:전면카메라

    private MediaRecorder recorder;
    private ImageView video_button;
    private int take_video_or_not = 0; // 1:start recording 0:stop recoring

    private CascadeClassifier cascadeClassifier;


    public static List<VisionDetRet> trainRet;    // dlib > 학습
    public static ArrayList<Point> trainLm;      // dlib 랜드마크 > 학습시킬 랜드마크
    public static ArrayList<Double> trainDist;       // dlib 랜드마크 거리 > 학습

    private List<VisionDetRet> liveRet;             // dlib > 실시간
    private ArrayList<Point> liveLm;                    // dlib 랜드마크 > 실시간 랜드마크
    private ArrayList<Double> liveDist;                // dlib 랜드마크 거리 > 실시간

    private FaceDet faceDet2;   // dlib
    private VisionDetRet detRet;    // 일치하는 얼굴(모자이크 제외할 얼굴)

    String distData = ""; // < 이 안에 파이어베이스에 저장될 랜드마크

    //firebase
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = database.getReference();
    //로그인 회원 정보를 다른 Activity에 전달하도록 전역 변수 설정....
    User myUser;

    /////////////////////////
    protected RESClient resClient;  // rtmp library
    private Button stream_button;
    private Button live_button;

    private String stream_key;

    private boolean streaming = false;
    private int stream_count = 0;
    private float left[], top[], right[], bottom[];

    private Bitmap mosaic_bitmap;

    private boolean fabMain_status = false;  // 플로팅버튼 상태
    private FloatingActionButton fab_main;
    private FloatingActionButton fab_mosaic;
    private FloatingActionButton fab_cat;
    private FloatingActionButton fab_sunglasses;
    private FloatingActionButton fab_smile;
    ///////////////////////////

    // tensorflow
    private Bitmap faceBmp;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static int TF_OD_API_INPUT_SIZE = 112;//500;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    public FaceDetector tf_faceDetector;
    public SimilarityClassifier tf_detector;
    private List<org.opencv.core.Rect> mosaicRect;
    private Canvas canvas;
    private float resizeRate_w, resizeRate_h;
    //User myUser;
    String imgpath;
    private String imgName ;
    private String imgUrl;
    InputImage image;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference;
//    private FirebaseDatabase database = FirebaseDatabase.getInstance();
//    private DatabaseReference databaseReference = database.getReference();
    private Bitmap recBmp = null;
    private Bitmap portraitBmp = null;
    private Integer sensorOrientation;
    private Bitmap copyFaceBmp = null;
    private Bitmap copyPortBmp = null;

    private int displayWidth;

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (streaming == false){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("방송을 종료하시겠습니까?");
            builder.setPositiveButton("네", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    LiveActivity.super.onBackPressed();
                    // Intent intent = new Intent(LiveActivity.this, HomeActivity.class);
                    //  startActivity(intent);
                }
            });
            builder.setNegativeButton("아니요", null);
            builder.create().show();
        }

        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("방송을 종료하기 전에 스트림을 종료해주세요");

            builder.setNegativeButton("네", null);
            builder.create().show();
        }
    }

    //dlib
    public double calcDist(double x1, double y1, double x2, double y2) {    // 거리 계산
        double x = Math.pow(x2-x1, 2);
        double y = Math.pow(y2-y1, 2);
        return Math.sqrt(x+y);
    }

    //@WorkerThread
    public void getDist() { // 거리 구하기
        liveDist = new ArrayList<>();
        double[] eye = new double[4];   // x1, y1, x2, y2 순서대로 저장

        for (int j = 0; j < 2; j++) {
            double eyeX = 0;
            double eyeY = 0;
            for (int i = 36; i < 42; i++) {
                eyeX += liveLm.get(i + j * 6).x;
                eyeY += liveLm.get(i + j * 6).y;
            }
            eye[j * 2] = eyeX / 6.0;
            eye[j * 2 + 1] = eyeY / 6.0;
        }
        double x1, y1, x2, y2;
        double dist;

        // 눈끼리의 거리
        x1 = eye[0];    // 왼쪽눈
        y1 = eye[1];
        x2 = eye[2];    // 오른쪽눈
        y2 = eye[3];
        liveDist.add(0, 1.0);
        dist = calcDist(x1, y1, x2, y2);

        // 왼쪽눈과 코와의 거리
        x2 = liveLm.get(30).x;   // 코
        y2 = liveLm.get(30).y;
        liveDist.add(1, calcDist(x1, y1, x2, y2) / dist);

        // 오른쪽눈과 코와의 거리
        x1 = eye[2];    // 오른쪽 눈
        y1 = eye[3];
        liveDist.add(2, calcDist(x1, y1, x2, y2) / dist);

        // 코와 왼쪽 입술의 거리
        x1 = liveLm.get(48).x;   // 왼쪽 입술
        y1 = liveLm.get(48).y;
        liveDist.add(3, calcDist(x1, y1, x2, y2) / dist);

        // 코와 오른쪽 입술의 거리
        x1 = liveLm.get(54).x;   // 오른쪽 입술
        y1 = liveLm.get(54).y;
        liveDist.add(4, calcDist(x1, y1, x2, y2) / dist);
    }

    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    //mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public LiveActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.live);
        myUser = (User)getApplication();

        /*
        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
         */
        // 카메라 전환
        flip_camera = findViewById(R.id.flip_camera);
        // when flip camera button is clicked
        flip_camera.setOnClickListener(this);

        /////////////////////////////////////////////////
        // 영상 스트림
        stream_button = findViewById(R.id.stream_button);
        stream_button.setOnClickListener(this);
        live_button = findViewById(R.id.live);

        // 영상 스트림
        stream_button = findViewById(R.id.stream_button);
        stream_button.setOnClickListener(this);
        live_button = findViewById(R.id.live);

        // 모자이크 비트맵 정하는 메인 버튼
        fab_main = findViewById(R.id.fab_main);
        fab_main.setOnClickListener(this);

        // 모자이크 비트맵 버튼
        fab_mosaic = findViewById(R.id.fab_mosaic);
        fab_mosaic.setOnClickListener(this);

        // 고양이 비트맵 버튼
        fab_cat = findViewById(R.id.fab_cat);
        fab_cat.setOnClickListener(this);

        // 선글라스 비트맵 버튼
        fab_sunglasses = findViewById(R.id.fab_sunglasses);
        fab_sunglasses.setOnClickListener(this);

        // 웃는얼굴 비트맵 버튼
        fab_smile = findViewById(R.id.fab_smile);
        fab_smile.setOnClickListener(this);

        // 모자이크 비트맵 초기
        mosaic_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mosaic);

        // rtmp 연결 카메라
        resClient = new RESClient();
        RESConfig resConfig = RESConfig.obtain();

        resConfig.setFilterMode(RESConfig.FilterMode.HARD);
        resConfig.setTargetVideoSize(new me.lake.librestreaming.model.Size(1080, 1080)); //720, 480));
        resConfig.setBitRate(750 * 1024);
        resConfig.setVideoFPS(20);
        resConfig.setVideoGOP(1);
        resConfig.setRenderingMode(RESConfig.RenderingMode.OpenGLES);//setrender mode in softmode
        resConfig.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);

        int frontDirection, backDirection;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
        frontDirection = cameraInfo.orientation;
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
        backDirection = cameraInfo.orientation;

        resConfig.setFrontCameraDirectionMode((frontDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90) | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
        resConfig.setBackCameraDirectionMode((backDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270));

        //resConfig.setRtmpAddr("rtmp://a.rtmp.youtube.com/live2/feuz-4aq4-a1jv-fd7t-aeq4");
        // firebase에 추가 stream_url과 stream_key

        resConfig.setRtmpAddr("rtmp://a.rtmp.youtube.com/live2/" + myUser.getStreamKey());


        if (!resClient.prepare(resConfig)) {
            resClient = null;
            Toast.makeText(this, "RESClient prepare failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        /////////////////////////////////////////

        /*
        // 얼굴 검출위한 파일 불러오기
        //load the model
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE); //creating a folder
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml"); // creating file on that folder
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            // writing that file from raw folder
            while((byteRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteRead);
            }
            is.close();
            os.close();

            // loading file from cascade folder created above
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            // model is loaded
        }
        catch(IOException e){
            Log.i(TAG, "Cascade file not found");
        }*/

        /*
        //dlib
        // %수정%
        try{
            InputStream is = getResources().openRawResource(R.raw.shape_predictor_68_face_landmarks);
            File landDir = getDir("landmark", Context.MODE_PRIVATE); //creating a folder
            File landFile = new File(landDir, "shape_predictor_68_face_landmarks.dat"); // creating file on that folder
            FileOutputStream os = new FileOutputStream(landFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            // writing that file from raw folder
            while((byteRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteRead);
            }
            is.close();
            os.close();

            // loading file from cascade folder created above
            faceDet2 = new FaceDet(landFile.getAbsolutePath());
        } catch(IOException e){
            Log.i(TAG, "Cascade file not found");
        }
        //faceDet2 = new FaceDet(Constants.getFaceShapeModelPath());
*/

        trainDist = new ArrayList<>();

//        distData = myUser.getLandmark();
//        String[] dists = distData.split("/");
//
//        for(int i=0; i<dists.length; i++) {
//            trainDist.add(i, new Double(dists[i]));
//        }

        camera_preview = (AspectTextureView) findViewById(R.id.frame_Surface);
        camera_preview.setSurfaceTextureListener(this);

        me.lake.librestreaming.model.Size s = resClient.getVideoSize();
        camera_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, (double) s.getWidth() / s.getHeight());

        imgName = myUser.getId()+".jpg"; //이미지 이름
        imgpath = getCacheDir()+"/"+imgName;

        try {
            tf_detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        FaceDetector detector = FaceDetection.getClient(options);

        tf_faceDetector = detector;

        try {
            InputStream is = getResources().openRawResource(R.raw.shape_predictor_68_face_landmarks);
            File landDir = getDir("landmark", Context.MODE_PRIVATE); //creating a folder
            File landFile = new File(landDir, "shape_predictor_68_face_landmarks.dat"); // creating file on that folder
            FileOutputStream os = new FileOutputStream(landFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            // writing that file from raw folder
            while ((byteRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteRead);
            }
            is.close();
            os.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        displayWidth = getWindowManager().getDefaultDisplay().getWidth();

        Bitmap bitmap = BitmapFactory.decodeFile(imgpath);
        copyFaceBmp = BitmapFactory.decodeFile(imgpath);
        Detect(bitmap, true);


        recBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
        //portraitBmp = Bitmap.createBitmap(resClient.getVideoSize().getWidth(), resClient.getVideoSize().getHeight(), Bitmap.Config.ARGB_8888);
        portraitBmp = Bitmap.createBitmap(displayWidth, displayWidth, Bitmap.Config.ARGB_8888);

//        int w, h;
//        w = resClient.getVideoSize().getWidth();
//        h = resClient.getVideoSize().getHeight();

        int rotation = 90;
        if (mCameraId == CameraCharacteristics.LENS_FACING_FRONT) {
            rotation = 270;
        }

        sensorOrientation = rotation - getScreenOrientation();
        mosaicRect = new ArrayList<>();

//        resizeRate_w = resClient.getVideoSize().getWidth() / 112;
//        resizeRate_h = resClient.getVideoSize().getHeight() / 112;

        resizeRate_w = displayWidth / 112;
        resizeRate_h = resizeRate_w;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.stream_button:
                if (stream_button.getText().equals("스트림 시작")) {
                    // start stream
                    stream_button.setText("스트림 종료");
                    live_button.setVisibility(View.VISIBLE);
                    resClient.startStreaming();

                    streaming = true;

                } else {
                    // stop stream
                    stream_button.setText("스트림 시작");
                    live_button.setVisibility(View.INVISIBLE);
                    resClient.stopStreaming();

                    streaming = false;
                    stream_count = 0;
                }

                break;

            case R.id.flip_camera:
                resClient.swapCamera();
                resClient.setHardVideoFilter(null);
                break;

            case R.id.fab_main:
                toggleFab();
                break;

            case R.id.fab_mosaic:
                mosaic_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mosaic);
                resClient.setHardVideoFilter(null);
                break;

            case R.id.fab_cat:
                mosaic_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cat);
                resClient.setHardVideoFilter(null);
                break;

            case R.id.fab_sunglasses:
                mosaic_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sunglasses);
                resClient.setHardVideoFilter(null);
                break;

            case R.id.fab_smile:
                mosaic_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.smile);
                resClient.setHardVideoFilter(null);
                break;
        }
    }

    public void toggleFab() {
        // 플로팅 액션 버튼 닫기
        // 애니메이션 추가
        if(fabMain_status) {
            ObjectAnimator fm_animation = ObjectAnimator.ofFloat(fab_mosaic, "translationX", 0f);
            fm_animation.start();

            ObjectAnimator fc_animation = ObjectAnimator.ofFloat(fab_cat, "translationX", 0f);
            fc_animation.start();

            ObjectAnimator fsu_animation = ObjectAnimator.ofFloat(fab_sunglasses, "translationX", 0f);
            fsu_animation.start();

            ObjectAnimator fsm_animation = ObjectAnimator.ofFloat(fab_smile, "translationX", 0f);
            fsm_animation.start();

            fab_main.setImageResource(R.drawable.ic_baseline_tag_faces_24);
        }
        else {
            // 플로팅 액션 버튼 열기
            ObjectAnimator fm_animation = ObjectAnimator.ofFloat(fab_mosaic, "translationX", 250f);
            fm_animation.start();

            ObjectAnimator fc_animation = ObjectAnimator.ofFloat(fab_cat, "translationX", 450f);
            fc_animation.start();

            ObjectAnimator fsu_animation = ObjectAnimator.ofFloat(fab_sunglasses, "translationX", 650f);
            fsu_animation.start();

            ObjectAnimator fsm_animation = ObjectAnimator.ofFloat(fab_smile, "translationX", 850f);
            fsm_animation.start();

            fab_main.setImageResource(R.drawable.ic_baseline_clear_24);
        }
        // 플로팅 버튼 상태 변경
        fabMain_status = !fabMain_status;
        Log.d("플로팅 상태", fabMain_status+"");
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if(mCameraId == 1) {
            Core.flip(mRgba, mRgba, 1);
            Core.flip(mGray, mGray, -1);
        }

        // in precessing pass mRgba to cascaderec class
       // mRgba = CascadeRec(mRgba);


        return mRgba;
    }

    private Mat CascadeRec(Mat mRgba) { // 얼굴 검출
        // original frame is -90 degree so we have to rotate is to 90 to get proper to face for detection
        Mat a=mRgba.t();
        Core.flip(a,mRgba,1);
        a.release();

        // convert it into RGB
        Mat mRbg = new Mat();
        Imgproc.cvtColor(mRgba, mRbg, Imgproc.COLOR_RGBA2RGB);

        /*
        int height = mRbg.height();
        int width = mRbg.width();

        // minimum size of face in frame
        int absoluteFaceSize = (int)(height * 0.1);

        // face detection using opencv + haarcascade model
        MatOfRect faces = new MatOfRect();

        if(cascadeClassifier != null) {
            // input output
            cascadeClassifier.detectMultiScale(mRbg, faces, 1.1, 2, 2
                    ,new Size(absoluteFaceSize, absoluteFaceSize),  new Size()); //  minimum size of output
        }

        //loop through all faces
        Rect[] facesArray = faces.toArray();
*/

        //dlib test
        Bitmap bmp = Bitmap.createBitmap(mRbg.cols(), mRbg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRbg, bmp);

        liveRet = faceDet2.detect(bmp);
        for(final VisionDetRet ret : liveRet) {
            liveLm = ret.getFaceLandmarks();    // 이 코드가 제대로 작동 안함
            getDist();

            int count=0; // for문 안에서 얼마나 돌았는지 (많이 돌수록 오차율 낮은거) > 많이 돌수록 동일인물일 확률 높아짐
            for(int i=0; i<5; i++) {
                //Log.i("거리 확인 --- ", "train : "+trainDist.get(i));
                //Log.i("거리 확인 --- ", "live : "+liveDist.get(i));
                if(trainDist.get(i)*0.8<liveDist.get(i) && trainDist.get(i)*1.2>liveDist.get(i))
                    count++;
                //Log.i("거리 오차율 확인 -------- " , " "+count);
            }
            if(count==5) {
                detRet = ret;
                break;
            }
        }

        for(final VisionDetRet ret : liveRet) {
            if(ret.equals(detRet))  // 동일인이라 인식된 얼굴이면
                continue;           // 모자이크 안하고 다음 얼굴로 넘어감
            Imgproc.rectangle(mRgba, new org.opencv.core.Point(ret.getLeft(), ret.getTop()),
                    new org.opencv.core.Point(ret.getRight(), ret.getBottom()),
                    new Scalar(0, 255, 0, 255), 2);
            Mat blurMat = mRgba.submat(ret.getTop(), ret.getBottom(), ret.getLeft(), ret.getRight());
            Imgproc.blur(blurMat, blurMat, new Size(99, 99));
        }
/*
        int detIdx = 0;
        for(final VisionDetRet ret : detRets) {
            ArrayList<Point> landmark = ret.getFaceLandmarks();


            for(Point point : landmark) {

                int px1 = point.x;
                int py1 = point.y;

                for(Point p : landmarks) {
                    int px2 = p.x;
                    int py2 = p.y;

                    int rx = px1-px2;
                    int ry = py1-py2;

                    //rx+ry;

                    //test
         /*
                    int sum=0;
                    sum+=Math.pow(Math.abs(rx), 2);
                    //for문에 넣어서 위에 코드 쭉 돌리고

                    double result = Math.sqrt(sum);
                    // 여기까지가 norm인데 rx대신에 뭘 넣어야하는지 좀더 알아봐야할 듯
                    // result값을 기준으로 descs값 정렬
                    // 여기서 descs는 랜드마크같긴한데...
*
                    // 거리의 차의 평균으로 해야하는건지..
                    // 파이썬에서는 랜드마크들 중 제일 짧은 거리를 찾는다고 써있는데..
                    // 이 거리가 뭘 의미하는건지 단순히 점과 점의 거리인지 어떤 거리인지 모르겠음
                    // numpy를 사용할 수 있으면 좀 편할 것 같은데
                    // 이 부분을 어떻게 변환해야할지 모르겠음

                    // 일단 얼굴형 17, 눈썹 각각 5, 콧대 4, 코 밑 5, 눈 각각 6, 윗입술 7, 아랫입술 6, 입 7 로 추정되니
                    // 이것들을 쪼개서 각 점들끼리 거리의 비율 계산해보자
                }

            }

            detIdx += 1;
        }*/
        // photo에서 받아온애랑 실시간으로 받아온애랑 랜드마크 서로 빼서 최소값대로 정렬해야하는 것 같음


/*
        for (int i = 0; i < facesArray.length; i++) {
            // draw face on original frame mRgba
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 2);
                                //VisionDetRet vdr = detRets.get(detIdx);
                                //new Point(vdr.getLeft(), vdr.getTop());
            Mat blurMat = mRgba.submat((int) facesArray[i].tl().y, (int) facesArray[i].br().y, (int) facesArray[i].tl().x, (int) facesArray[i].br().x);
            Imgproc.blur(blurMat, blurMat, new Size(99, 99));

        }*/

        // rotate back original frame to -90 degree
        Mat b=mRgba.t();
        Core.flip(b,mRgba,0);
        b.release();

        mRbg.release(); // 메모리누수.. test
        return mRgba;
    }

    private Bitmap resizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();

        return resizedBitmap;
    }

    protected SurfaceTexture texture;
    protected int sw,sh;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (resClient != null) {
            resClient.startPreview(surface, width, height);
        }
        texture = surface;
        sw = width;
        sh = height;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (resClient != null) {
            resClient.updatePreview(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (resClient != null) {
            resClient.stopPreview(true);
        }
        if(streaming == true) {
            resClient.stopStreaming();
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        final Bitmap faceBP = camera_preview.getBitmap();

        //canvas = new Canvas(faceBP);

        Bitmap resizeFace = resizedBitmap(faceBP, 112, 112);

        //copyFaceBmp = resizedBitmap(camera_preview.getBitmap(), 112, 112);
        copyFaceBmp = resizedBitmap(camera_preview.getBitmap(), displayWidth, displayWidth);

        Detect(resizeFace, false);
        //Bitmap MosaicBitmap = Bitmap.createBitmap(faceBP.getWidth(), faceBP.getHeight(), Bitmap.Config.ARGB_8888);

        //Paint pt = new Paint();
        //pt.setColor(Color.RED);

        //Bitmap mosaicBmp
        if(mosaicRect.size()!=0) { // 얼굴이 인식되면

            left = new float[mosaicRect.size()];
            top = new float[mosaicRect.size()];
            right = new float[mosaicRect.size()];
            bottom = new float[mosaicRect.size()];

            Log.i("mosaicsize: ", mosaicRect.size()+"");

            for(int i=0; i<mosaicRect.size(); i++) { // 모자이크할 인식된 얼굴의 위치
                org.opencv.core.Rect r = mosaicRect.get(i);
                int newX1 = (int)(r.x * resizeRate_w);
                int newY1 = (int)(r.y * resizeRate_h);
                int newX2 = (int)((r.x+r.width)*resizeRate_w);
                int newY2 = (int)((r.y+r.height)*resizeRate_h);
                //canvas.drawRect(new android.graphics.Rect(newX1, newY1, newX2, newY2), pt);
                //Bitmap temp = Bitmap.createScaledBitmap(MosaicBitmap, 50, 50, false);
                //mosaicBmp = Bitmap.createScaledBitmap(temp, MosaicBitmap.getWidth(), MosaicBitmap.getHeight(), false);

                //canvas.drawBitmap(mosaicBmp, (float)mosaicRect.get(i).x, (float)mosaicRect.get(i).y, pt);

                left[i] = newX1;
                top[i] = newY1;
                right[i] = newX2;
                bottom[i] = newY2;

            }

            // bitmap을 이용해서 모자이크 처리
            ViewHardFilter filter = new ViewHardFilter(mosaicRect.size(), left, top, right, bottom, mosaic_bitmap);
            resClient.setHardVideoFilter(filter);

            mosaicRect.clear();

        }
    }

    @Override
    public void onOpenConnectionResult(int result) {
        if (result == 0) {
            Log.e(TAG, "server IP = " + resClient.getServerIpAddr());
        }else {
            Toast.makeText(this, "startfailed", Toast.LENGTH_SHORT).show();
        }
        /**
         * result==0 success
         * result!=0 failed
         */
    }

    @Override
    public void onWriteError(int errno) {
        if (errno == 9) {
            resClient.stopStreaming();
            resClient.startStreaming();
            Toast.makeText(this, "errno==9,restarting", Toast.LENGTH_SHORT).show();
        }
        /**
         * failed to write data,maybe restart.
         */
    }

    @Override
    public void onCloseConnectionResult(int i) {
        /**
         * result==0 success
         * result!=0 failed
         */
    }


    public void Detect(Bitmap bmp, Boolean isTrain) {
        if(bmp != null) {
            faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
            faceBmp = bmp;
            //image = InputImage.fromBitmap(resizedBitmap(faceBmp, TF_OD_API_INPUT_SIZE / 2, TF_OD_API_INPUT_SIZE / 2), 0);
            image = InputImage.fromBitmap(resizedBitmap(faceBmp, displayWidth / 2, displayWidth / 2), 0);

            tf_faceDetector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                @Override
                public void onSuccess(List<com.google.mlkit.vision.face.Face> faces) {


                    final List<SimilarityClassifier.Recognition> mappedRecognitions =
                            new LinkedList<SimilarityClassifier.Recognition>();

                    if (faces.size() == 0) {
                        //얼굴검출X
                        //resClient.setHardVideoFilter(null);
                        mosaicRect.clear(); // 화면에 얼굴 없으면 모자이크 범위 없애기
                        return;
                    }
/*
                    int sourceW = resClient.getVideoSize().getWidth();
                    int sourceH = resClient.getVideoSize().getHeight();
                    int targetW = portraitBmp.getWidth();
                    int targetH = portraitBmp.getHeight();

                    Matrix transform = createTransform(
                            sourceW,
                            sourceH,
                            targetW,
                            targetH,
                            sensorOrientation);
 */

                    int i=0;
                    for (com.google.mlkit.vision.face.Face face : faces) {
                        final RectF boundingBox = new RectF(face.getBoundingBox());

                        if (boundingBox != null) {
                            RectF faceBB = new RectF(boundingBox);

                            if(!isTrain) {
                                if (faceBB.left < 0)
                                    faceBB.left = 0;
                                if (faceBB.top < 0)
                                    faceBB.top = 0;
                            }
                            //copyPortBmp = Bitmap.createBitmap(copyFaceBmp, (int)faceBB.left, (int)faceBB.top, (int)faceBB.width(), (int)faceBB.height());
                            recBmp = resizedBitmap(Bitmap.createBitmap(copyFaceBmp), 112, 112);//, 0, 0, 112, 112);

                            String label = "";
                            float confidence = -1f;
                            Object extra = null;
                            Bitmap crop = Bitmap.createBitmap(portraitBmp,
                                    (int) faceBB.left,
                                    (int) faceBB.top,
                                    (int) faceBB.width(),
                                    (int) faceBB.height());

                            final List<SimilarityClassifier.Recognition> resultsAux = tf_detector.recognizeImage(recBmp, true);

                            if (resultsAux.size() > 0) {
                                SimilarityClassifier.Recognition result = resultsAux.get(0);

                                extra = result.getExtra();

                                float conf = result.getDistance();
                                Log.i("거리 값 >> ", conf + " / " + (i++));

                                if (conf < 1.0f) {
                                    confidence = conf;
                                    label = result.getTitle();
                                }
                            }

                            final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                                    "0", label, confidence, boundingBox);

                            result.setLocation(boundingBox);
                            result.setExtra(extra);
                            result.setCrop(crop);

                            mappedRecognitions.add(result);

                            SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);//0);

                            if (isTrain) {
                                tf_detector.register("user", rec);

                            } else {
                                float conf = rec.getDistance();

                                if (conf == -1) {  // 동일인물아니면..?
                                    mosaicRect.add(new org.opencv.core.Rect((int) boundingBox.left, (int) boundingBox.top,
                                            (int) boundingBox.right, (int) boundingBox.bottom));
                                }
                            }
                        }
                    }
                }
            });
        }
    }
/*
    private Matrix createTransform(     // \\수정\\ > 안쓰면 삭제
                                        final int srcWidth,
                                        final int srcHeight,
                                        final int dstWidth,
                                        final int dstHeight,
                                        final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                //LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }
        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }
 */
    protected int getScreenOrientation() {  // \\수정\\ >> 안쓰면 삭제
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }
}