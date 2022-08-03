package com.example.dlibmodule;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.dlibmodule.tflite.SimilarityClassifier;
import com.example.dlibmodule.tflite.TFLiteObjectDetectionAPIModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.example.dlibmodule.LiveActivity.trainDist;
import static com.example.dlibmodule.LiveActivity.trainLm;
import static com.example.dlibmodule.LiveActivity.trainRet;


//사진화면
public class Fragment2 extends Fragment {
    private View view;
    private Button selectBtn;
    private Button startBtn;
    ImageView facePhoto, helpBtn;

    CropImage.ActivityResult result;    // dlib test

    private Uri resultUri;

    //로그인 회원 정보를 다른 Activity에 전달하도록 전역 변수 설정....
    User myUser;
    String imgpath;

    //firebase
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = database.getReference();
    //storage
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference;

    private String imgName ; //한나:imageView 내부저장소에 저장...
    private String imgUrl; //한나:크롭이미지 저장할 곳...

    //progress dialog
    ProgressDialog progressDialog;

    // tensorflow
    private SimilarityClassifier detector;
    private FaceDetector faceDetector;
    private boolean isSuc; // 등록한 사진에 얼굴이 있는지 판단

    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static int TF_OD_API_INPUT_SIZE = 112;//500;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.photo1, container, false);

        facePhoto = view.findViewById(R.id.facePhoto);
        startBtn = view.findViewById(R.id.startBtn);
        selectBtn = view.findViewById(R.id.selectBtn);
        helpBtn = view.findViewById(R.id.helpBtn);

        myUser = (User)getActivity().getApplication();
        imgName = myUser.getId()+".jpg"; //이미지 이름
        imgpath = getActivity().getCacheDir()+"/"+imgName;
        imgUrl = "gs://haniumproject-88a20.appspot.com/images/" + imgName; //storage에 이미지가 저장될 url. realtime database에 저장할 url 주소.

        storageReference = storage.getReferenceFromUrl("gs://haniumproject-88a20.appspot.com/").child("images/"+imgName);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Image Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal);

        progressDialog.show();
        firstSetImageView();

        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteJpeg(); // 사용하던 이미지가 바뀌면 이전에 저장된 이미지는 삭제하도록 하기 위해서 selectPicture() 이전에 delete 함수 넣음
                SelectPicture();
                Toast.makeText(getActivity(), "변경사항을 저장해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start(v);
                startBtn.setEnabled(false);
            }
        });

        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("1. 방송을 시작하려면 사용자의 사진을 먼저 등록해야 합니다.\n" +
                        "2. '얼굴 선택' 버튼을 클릭하여 사용자의 얼굴이 나오도록 사진을 선택하고 " +
                        "'변경사항 저장' 버튼을 클릭하여 저장해주세요. 등록할 사진은 한 개만 등록됩니다. " +
                        "등록한 사진을 변경하고 싶다면 위와 같은 방법으로 변경 가능합니다.\n" +
                        "3. 등록할 사진의 이미지는 정면 사진이어야 하고 입은 다물어야 하며 웃거나 찡그리지 않은" +
                        "자연스러운 표정이어야 합니다. ");
                builder.setPositiveButton("알겠습니다.", null);
                builder.create().show();
            }
        });

        try{
            detector = TFLiteObjectDetectionAPIModel.create(
                    getAssets(),
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);
        } catch(IOException e) {}

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        FaceDetector d = FaceDetection.getClient(options);

        faceDetector = d;

        return view;
    }

    public void firstSetImageView(){ //처음 PhotoActivity에 들어오면 DB에 url 정보가 저장되어있는지 확인 후
        if(myUser.getPhotourl() != null){//저장되어있으면 이미지뷰에 storage 이미지를 set...
            storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){ //Glide 이용하여 이미지뷰에 로딩
                        Glide.with(getActivity())
                                .load(task.getResult())
                                .into(facePhoto);
                    }else{ //URL을 가져오지 못하면 토스트 메세지
                        Toast.makeText(getActivity(),"url 불러오기 실패", Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                }
            });
        } else progressDialog.dismiss();    // 새로추가
        //저장되어있지 않으면 기본 검정 화면...
    }

    public void SelectPicture() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setRequestedSize(500, 500)
                .setAspectRatio(1, 1)
                .start(getContext(), this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { // 해당 함수 작동 안됨... 작대기 없애도록 수정...
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            //CropImage.ActivityResult result = CropImage.getActivityResult(data);      // dlib test
            result = CropImage.getActivityResult(data);     // dlib test
            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();
                //facePhoto.setImageURI(resultUri);
                ContentResolver resolver = getActivity().getContentResolver();
                try{
                    InputStream inputStream = resolver.openInputStream(resultUri);
                    Bitmap imgBitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    saveBitmapToJpeg(imgBitmap); //
                    Bitmap bm = BitmapFactory.decodeFile(imgpath); //아이디.jpg가 저장됨...
                    facePhoto.setImageBitmap(bm); //크롭이미지를 imageView에 set...
                    startBtn.setEnabled(true);
                }catch (Exception e){
                    Toast.makeText(getActivity(), "파일 저장 실패", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public void saveBitmapToJpeg(Bitmap bitmap){ //이미지 내부 저장소에 저장...
        File tempFile = new File(getActivity().getCacheDir(), imgName);

        try{
            tempFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream); //cropped(숫자).jpg가 저장됨...
            outputStream.close();
        }catch (Exception e){
            Toast.makeText(getActivity(), "saveBitmapToJpeg 실패", Toast.LENGTH_SHORT).show();
        }
    }

    public void start(View v) { // dlib 랜드마크 저장
        /*
        Bitmap bitmap = BitmapFactory.decodeFile(imgpath);

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                isSuc = false;
                if (faces.size() == 0 || faces.size() > 1) // 얼굴 검출X or 얼굴 2개 이상 검출
                    return;
                else
                    isSuc = true;
            }
        });

        if(isSuc) { // 얼굴 하나만 검출되면
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle("저장되었습니다")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            alert.create();
            alert.show();
        }else { // 얼굴 0개 or 여러개 검출되면
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            // alert 텍스트 사진을 다시 선택해주세요(?) 같은 텍스트 넣고 사진 설정 관련 도움말이 필요하면 우측 상단의 도움말 버튼 누르라고 알려주는것도?
            alert.setTitle("저장 실패")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            alert.create();
            alert.show();
        }

         */

        uploadJpeg();
    }

    public void deleteJpeg(){ //이미지 내부저장소에서 삭제...
        try{
            File file = getActivity().getCacheDir();
            File[] fileList = file.listFiles();
            for(int i =0; i<fileList.length; i++){
                if(fileList[i].getName().equals(imgName) || fileList[i].getName().contains("cropped")){
                    fileList[i].delete();
                }
            }
        }catch (Exception e){
            Toast.makeText(getActivity(), "파일 삭제 실패", Toast.LENGTH_SHORT).show();
        }
    }

    public void uploadJpeg(){ //내부저장소에 저장된 이미지 firebase storage에 업로드...
        //업로드할 파일이 있으면 수행
        if(resultUri != null){
            //업로드
            storageReference.putFile(resultUri);
            Toast.makeText(getActivity(), "동기화 완료", Toast.LENGTH_SHORT).show();

            //업로드하면서 realtime database에 이미지 정보 추가...
            myUser.setPhotourl(imgUrl);
            databaseReference.child("User").child(myUser.getId()).child("profile").setValue(myUser.getPhotourl());
        }
        else{
            Toast.makeText(getActivity(), "동기화 실패", Toast.LENGTH_SHORT).show();
        }
    }

    public AssetManager getAssets() {
        return getResources().getAssets();
    }
}