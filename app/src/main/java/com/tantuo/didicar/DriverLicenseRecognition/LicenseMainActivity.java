package com.tantuo.didicar.DriverLicenseRecognition;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tantuo.didicar.MainActivity;
import com.tantuo.didicar.R;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.tantuo.didicar.utils.LogUtil;

public class LicenseMainActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = "OPenCV-Android";
    private int REQUEST_CAPTURE_IMAGE = 1;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_recognition_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Button takePicBtn = (Button) this.findViewById(R.id.take_picture_btn);
        takePicBtn.setOnClickListener(this);

        Button selectPicBtn = (Button) this.findViewById(R.id.select_picture_btn);
        selectPicBtn.setOnClickListener(this);



        copyFilesFassets(this, "driver_card_sample_tantuo.png", "sdcard/driver_card_sample_tantuo.png");



        //初始化openCV
        iniLoadOpenCV();
    }


    private void iniLoadOpenCV() {
        boolean success = OpenCVLoader.initDebug();
        //首先使用OpenCVLoader.initDebug()检查一下OpenCV库是否成功加载
        if (success) {
            Log.i(TAG, " *****************************************");
            Log.i(TAG, " ***************************openCV load 成功");
        } else {
            Log.i(TAG, " *****************************************");
            Log.i(TAG, " ***************************openCV load 失败");
        }

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.take_picture_btn:
                //使用摄像头对司机驾驶证拍照
                getDriverLicenseFromCamera();
                break;
            case R.id.select_picture_btn:
                //从手机调取要识别的照片
                getDriverLicenseFromPhone();
                //getDriverLicenseFromMysample();
                break;
            default:
                break;
        }
    }

    private void getDriverLicenseFromMysample() {

        //http://139.199.37.235/LBS/didiDriverData/didiDriverProfile/tantuo.png

        Intent intent = new Intent(getApplicationContext(), LicenseOCRActivity.class);

        Uri uri = Uri.fromFile(new File("sdcard/driver_card_sample_tantuo.png"));

 //       Uri imageUri = getImageContentUri(LicenseMainActivity.this, "sdcard/driver_card_sample_tantuo.png");


//        Uri uri = Uri.parse("android.resource://com.tantuo.didicar/assets/driver_card_sample_tantuo.png");
//        intent.putExtra("PICTURE-URL","http://139.199.37.235/LBS/didiDriverData/didiDriverProfile/tantuo.png");
//        intent.putExtra("PICTURE-URL","assets://driver_card_sample_tantuo.png");
        intent.putExtra("PICTURE-URL",uri);

//        Toast.makeText(getApplicationContext(), "android.resource://com.tantuo.didicar/" + R.raw.driver_card_sample_tantuo), Toast.LENGTH_SHORT).show();
//        LogUtil.i("进入类:LicenseMainActivity, 方法:getDriverLicenseFromMysample() uri: " +Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.driver_card_sample_tantuo));
        startActivity(intent);

    }

    private void getDriverLicenseFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = Uri.fromFile(getSaveFilePath());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
    }

    private File getSaveFilePath() {
        //得到SD存储的状态
        String status = Environment.getExternalStorageState();
        //如果没有外部存储设备
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            Log.i(TAG, "SD Card is not mounted...");
            return null;
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String name = df.format(new Date(System.currentTimeMillis())) + ".jpg";
        File filedir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "myOcrImages");
        filedir.mkdirs();
        String fileName = filedir.getAbsolutePath() + File.separator + name;
        File imageFile = new File(fileName);
        return imageFile;
    }

    private void getDriverLicenseFromPhone() {




        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "图像选择..."), REQUEST_CAPTURE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK) {
            if (data == null) {
                Intent intent = new Intent(getApplicationContext(), LicenseOCRActivity.class);
                intent.putExtra("PICTURE-URL", fileUri);
                startActivity(intent);
            } else {
                Uri uri = data.getData();
                Intent intent = new Intent(getApplicationContext(), LicenseOCRActivity.class);
                File f = new File(getRealPath(uri));
                intent.putExtra("PICTURE-URL", Uri.fromFile(f));
                startActivity(intent);
            }
        }
    }

    private String getRealPath(Uri uri) {
        String filePath = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {//4.4及以上
            String wholeID = DocumentsContract.getDocumentId(uri);
            String id = wholeID.split(":")[1];
            String[] column = {MediaStore.Images.Media.DATA};
            String sel = MediaStore.Images.Media._ID + "=?";
            Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column,
                    sel, new String[]{id}, null);
            int columnIndex = cursor.getColumnIndex(column[0]);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        } else {//4.4以下，即4.4以上获取路径的方法
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getApplicationContext().getContentResolver().query(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(column_index);
        }
        Log.i("CV_TAG", "selected image path : " + filePath);
        return filePath;
    }



    /**
     *  从assets目录中复制整个文件夹内容
     *  @param  context  Context 使用CopyFiles类的Activity
     *  @param  oldPath  String  原文件路径  如：/aa
     *  @param  newPath  String  复制后路径  如：xx:/bb/cc
     */
    public void copyFilesFassets(Context context, String oldPath, String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                File file = new File(newPath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFassets(context,oldPath + "/" + fileName,newPath+"/"+fileName);
                }
            } else {//如果是文件
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount=0;
                while((byteCount=is.read(buffer))!=-1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     *
     * @param context
     * @param imageFile
     * @return content Uri
     */
    public static Uri getImageContentUri(Context context, java.io.File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }





}