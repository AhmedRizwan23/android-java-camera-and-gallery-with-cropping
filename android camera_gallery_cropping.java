package com.jazz.peopleapp.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jazz.peopleapp.R;
import com.jazz.peopleapp.models.UserModel;
import com.jazz.peopleapp.ui.activities.JazzHome;
import com.jazz.peopleapp.utils.ApiConstants;
import com.jazz.peopleapp.utils.MyLog;
import com.jazz.peopleapp.utils.PermissionUtils;
import com.jazz.peopleapp.utils.PickerBuilder;
import com.jazz.peopleapp.utils.SessionManager;
import com.jazz.peopleapp.widgets.CenteringTabLayout;
import com.jazz.peopleapp.widgets.FilesSelect;
import com.jazz.peopleapp.widgets.GPTextViewNoHtml;
import com.jazz.peopleapp.ws.AppJson;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;
import com.vincent.filepicker.Constant;
import com.yalantis.ucrop.UCrop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.media.MediaRecorder.VideoSource.CAMERA;
import static com.jazz.peopleapp.utils.AppConstants.UPLOAD_ATTACHMENT_SIZE;
import static com.vincent.filepicker.Constant.TAKE_PICTURE_REQUEST_CODE;

/**
 * Created by Asif on 10/24/2018.
 */

public class MyProfile extends Fragment implements AppJson.AppJSONDelegate {
    private SessionManager sessionManager;
    private AppJson appJson;
    private CircleImageView profileImage;
    private GPTextViewNoHtml name, grade, empNo, department, officialEmail, officialNo, approvingAutority;
    ImageView editProfilePic;
    private String encodedPic;
    String path = "";
    imagecallback imagecallback;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
    ImageView imgQR;
    private Uri currentPhotoUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_myprofile, container, false);
        sessionManager = new SessionManager(getContext());
        appJson = new AppJson(this, getContext());

        profileImage = v.findViewById(R.id.profile);
        name = v.findViewById(R.id.name);
        grade = v.findViewById(R.id.grade);
        empNo = v.findViewById(R.id.employeeNo);
        department = v.findViewById(R.id.department);
        officialEmail = v.findViewById(R.id.officialEmail);
        officialNo = v.findViewById(R.id.officialNo);
        approvingAutority = v.findViewById(R.id.approvingAuthority);
        editProfilePic = v.findViewById(R.id.editProfilePic);
        imgQR = v.findViewById(R.id.imgQR);

        setProfileDetails();

        editProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPictureDialog();
            }
        });

        imgQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQrImageDialog();
            }
        });

        return v;
    }

    private void showQrImageDialog() {
        Dialog attachDialog = new Dialog(getContext());
        attachDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        attachDialog.setContentView(R.layout.dialog_qr_image);

        attachDialog.setCanceledOnTouchOutside(false);
        WindowManager.LayoutParams lp = attachDialog.getWindow().getAttributes();
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.60);
        attachDialog.getWindow().setLayout(width, height);
        lp.dimAmount = 0.7f;

        ImageView imageViewQR = attachDialog.findViewById(R.id.imageViewQR);
        ImageView imgCancel = attachDialog.findViewById(R.id.imgCancel);

        String base = sessionManager.getStringValue("imageQr");
        byte[] imageAsBytes = Base64.decode(base.getBytes(), Base64.DEFAULT);
        imageViewQR.setImageBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));

        imgCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attachDialog.dismiss();
            }
        });

        attachDialog.show();
    }

    private void setProfileDetails() {
        Gson gson = new Gson();
        Type type = new TypeToken<UserModel>() {
        }.getType();
        UserModel userobject = gson.fromJson(sessionManager.getStringValue("userobject"), type);
        name.setText(userobject.getName());
        grade.setText(userobject.getGradename());
        empNo.setText(userobject.getEmployeeid());
        department.setText(userobject.getDeptname());
        officialEmail.setText(userobject.getOfficialemail());
        officialNo.setText(userobject.getMobilenumber());
        approvingAutority.setText(userobject.getManagername());
        if (!userobject.getProfileimage().equals("")) {
            Picasso.get().load(userobject.getProfileimage()).placeholder(R.drawable.graycamera).into(profileImage);
        }
    }

    private void setupImageService() {
        RequestParams params = new RequestParams();
        Gson gson = new Gson();
        Type type = new TypeToken<UserModel>() {
        }.getType();
        UserModel userobject = gson.fromJson(sessionManager.getStringValue("userobject"), type);
        params.put("Key", userobject.getLoginkey());
        params.put("username", sessionManager.getStringValue("uname"));
        appJson.appJSONCallWithCallName(AppJson.JSONCallName.PROFILEIMAGEURL, params, true, true);
    }

    @Override
    public void appJSONReceivedResponse(String response, AppJson.JSONCallName jsonCallName) {
        switch (jsonCallName) {
            case PROFILEIMAGEURL:

                try {
                    if (response.trim().charAt(0) == '{') {
                        JSONObject jsonObject = new JSONObject(response);
                        if (!(jsonObject.getString("ImageURL").equals(""))) {
                            Picasso.get().load(jsonObject.getString("ImageURL")).placeholder(R.drawable.graycamera).into(profileImage);
                        }

                        CenteringTabLayout tabLayout = (CenteringTabLayout) getActivity().findViewById(R.id.tabs);
                        CircleImageView mLogo = tabLayout.getTabAt(2).getCustomView().findViewById(R.id.nav_icon);
                        if (!(jsonObject.getString("ImageURL")).equals("")) {
                            Picasso.get().load(jsonObject.getString("ImageURL")).placeholder(R.drawable.graycamera).into(mLogo);
                        }

                        imagecallback = (imagecallback) ((JazzHome) getActivity()).setting;
                        imagecallback.uriparameters(jsonObject.getString("ImageURL"));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void appJSONReceivedFailure(byte[] response, String e, AppJson.JSONCallName jsonCallName) {
    }

    public void showPictureDialog() {
        final AlertDialog.Builder pictureDialog = new AlertDialog.Builder(getContext());
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Choose File",
                "Camera"
        };
        //to set items in dialogbox
        pictureDialog.setItems(pictureDialogItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                switch (which) {
                    case 0:
                        dialogInterface.dismiss();
                        choosePhotoFromGallery();
                        break;
                    case 1:
                        dialogInterface.dismiss();
                        checkPermissions();
                        break;
                }
            }
        });
        //to show dialog box
        pictureDialog.show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.e("android_check", "Android 13 or greater");
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(getActivity(), new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                }, TAKE_PICTURE_REQUEST_CODE);
            } else {
                FromCamera();  // Camera permission is granted, open camera
            }
        } else {
            Log.e("android_check", "Android less than 13");
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(getActivity(), new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, TAKE_PICTURE_REQUEST_CODE);
            } else {
                takePhotoFromCamera();  // Permissions are granted, open camera
            }
        }
    }
    private void FromCamera() {

        Log.e("Testing", "Test 3");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Constant.REQUEST_CODE_TAKE_IMAGE);
        } else {
            Log.e("Camera", "No camera app found to handle intent");
        }
    }






    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Constant.REQUEST_CODE_TAKE_IMAGE:
                    if (data != null && data.getExtras() != null) {
                        Bundle extras = data.getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        if (imageBitmap != null) {
                            // Save the captured image to a file and get the URI
                            Uri imageUri = getImageUri(getContext(), imageBitmap);
                            if (imageUri != null) {
                                Log.e("ImageSave", "Image saved to: " + imageUri.getPath());

                                // Start UCrop for cropping and rotating the image
                                startCropActivity(imageUri);
                            } else {
                                Log.e("ImageSave", "Failed to save the image.");
                            }
                        } else {
                            Log.d("camera", "Failed to retrieve image data.");
                        }
                    } else {
                        Log.d("camera", "Failed to load Image/File. Please try again.");
                    }
                    break;

                case Constant.REQUEST_CODE_PICK_IMAGE:
                    // Handle image selection from the gallery
                    if (data != null && data.getData() != null) {
                        Uri selectedImageUri = data.getData();

                        if (selectedImageUri != null) {
                            // Start UCrop for cropping and rotating the image
                            startCropActivity(selectedImageUri);
                        } else {
                            Log.d("gallery", "No image data found in the intent.");
                        }
                    } else {
                        Log.d("gallery", "Failed to load Image/File. Please try again.");
                    }
                    break;

                case UCrop.REQUEST_CROP:
                    // Handle the cropped image result
                    handleCropResult(data);
                    break;

                case UCrop.RESULT_ERROR:
                    // Handle the crop error
                    handleCropError(data);
                    break;
            }
        } else {
            Log.d("camera_", "Failed to load Image/File. Please try again.");
        }
    }
    private void handleCropError(@Nullable Intent result) {
        final Throwable cropError = UCrop.getError(result);
        if (cropError != null) {
            Log.e("UCropError", "Crop error: " + cropError.getMessage());
        }
    }

    private void handleCropResult(@Nullable Intent result) {
        if (result == null) return;

        final Uri resultUri = UCrop.getOutput(result);
        if (resultUri != null) {
            // Set the cropped and rotated image to the ImageView
            profileImage.setImageURI(resultUri);

            // You can upload the cropped image here
            String imagePath = getRealPathFrom_URI(getActivity(), resultUri);
            new AsyncProfilePicUpload(imagePath).execute();
        } else {
            Log.e("UCrop", "Crop failed, URI is null.");
        }
    }

    private String getRealPathFrom_URI(Context context, Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = null;
        String realPath = "";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 and above: Use ContentResolver and InputStream to get the file path
                File file = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(file);

                if (inputStream != null) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    inputStream.close();
                    outputStream.close();
                    realPath = file.getAbsolutePath();
                }

            } else {
                // Below Android 10: Use MediaStore to get the file path directly
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    realPath = cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return realPath;
    }


    private void startCropActivity(@NonNull Uri uri) {
        // Use getActivity().getCacheDir() to resolve the getCacheDir() method
        Uri destinationUri = Uri.fromFile(new File(getActivity().getCacheDir(), "cropped_image.jpg"));
        UCrop uCrop = UCrop.of(uri, destinationUri);

        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(90);
        options.setToolbarTitle("Edit Photo");
        options.setFreeStyleCropEnabled(true);

        uCrop.withOptions(options);
        uCrop.start(getActivity(), this);
    }

    public void choosePhotoFromGallery() {
        // Check for READ_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        // Check for CAMERA and WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getActivity(),
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA);
        }

        // Handle Android versions differently
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle Android 13 (API level 33) and above
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, Constant.REQUEST_CODE_PICK_IMAGE);
        } else if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Handle permissions for Android 6.0 (Marshmallow) to Android 12 (API level 32)
            return;
        } else {
            // For Android 12 and below, use PickerBuilder as before
            new PickerBuilder(getActivity(), PickerBuilder.SELECT_FROM_GALLERY)
                    .setOnImageReceivedListener(new PickerBuilder.onImageReceivedListener() {
                        @Override
                        public void onImageReceived(Uri imageUri) {
                            Log.e("***AAA: ", String.valueOf(imageUri));
                            processSelectedImage(imageUri);
                        }
                    })
                    .setCropScreenColor((Color.parseColor("#000000")))
                    .setOnPermissionRefusedListener(new PickerBuilder.onPermissionRefusedListener() {
                        @Override
                        public void onPermissionRefused() {
                            Log.e("Permission", "Permission was refused by the user.");
                        }
                    })
                    .start();
        }
    }

    // A separate method to process the selected image
    private void processSelectedImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);

            Log.d("ImageProcessing", "Selected image URI: " + imageUri);
            final int maxSize = 960;
            int outWidth;
            int outHeight;
            int inWidth = bitmap.getWidth();
            int inHeight = bitmap.getHeight();

            if (inWidth > inHeight) {
                outWidth = maxSize;
                outHeight = (inHeight * maxSize) / inWidth;
            } else {
                outHeight = maxSize;
                outWidth = (inWidth * maxSize) / inHeight;
            }

            // Resize the bitmap
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);

            // Rotate the bitmap if necessary
            Bitmap bitmapRotated = ((JazzHome) getContext()).modifyOrientation(resizedBitmap,
                    ((JazzHome) getContext()).getFileNameByUri(getContext(), imageUri));

            // Get the URI and file path of the processed image
            Uri uri = getImageUri(getContext(), bitmapRotated);
            String path = getRealPathFromURI(getActivity(), uri);

            MyLog.d("people-profilepath", path);

            // Upload the processed image
            new AsyncProfilePicUpload(path).execute();

            // Update the profile image view
            profileImage.setImageBitmap(bitmapRotated);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ImageProcessing", "Failed to process the selected image.");
        }
    }
    private void processSelectedImage__android13_and_above(Uri selectedImageUri) {
        try {
            // Get the bitmap from the selected image URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImageUri);






            // Get the URI and file path of the processed image
            Uri uri = getImageUri(getContext(), bitmap);
            String path = getRealPathFromURI(getActivity(), uri);

            MyLog.d("people-profilepath", path);

            // Upload the processed image
            new AsyncProfilePicUpload(path).execute();

            // Update the profile image view
            profileImage.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ImageProcessing", "Failed to process the selected image on Android 13+.");
        }
    }




    private void takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getActivity(),
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA);
        }
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            new PickerBuilder(getActivity(), PickerBuilder.SELECT_FROM_CAMERA)
                    .setOnImageReceivedListener(new PickerBuilder.onImageReceivedListener() {
                        @Override
                        public void onImageReceived(Uri imageUri) {
                            Log.e("***AAA: ", String.valueOf(imageUri));
//                        Toast.makeText(getApplicationContext(), "Got image - " + imageUri, Toast.LENGTH_LONG).show();
                            Bitmap bitmap = null;
                            try {
                                //  bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), imageUri);
                                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);

//                            bitmap = BitmapFactory.decodeStream(getApplicationContext().getContentResolver()
//                                    .openInputStream(Uri.parse(String.valueOf(imageUri))));

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

//                            camera.setImageURI(imageUri);
                            Log.e("***AAA2: ", String.valueOf(imageUri));
                            final int maxSize = 960;
                            int outWidth;
                            int outHeight;
                            int inWidth = bitmap.getWidth();
                            int inHeight = bitmap.getHeight();

                            if (inWidth > inHeight) {
                                outWidth = maxSize;
                                outHeight = (inHeight * maxSize) / inWidth;
                            } else {
                                outHeight = maxSize;
                                outWidth = (inWidth * maxSize) / inHeight;
                            }

                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
                            Uri uri = getImageUri(getContext(), resizedBitmap);
                            path = getRealPathFromURI(getActivity(), uri);
                            //UploadPic();
                            //File sourceFile = new File(path);
                            String path2 = "";
                            Bitmap bitmapRotated = null;
                            try {
                                bitmapRotated = ((JazzHome) getContext()).modifyOrientation(resizedBitmap,
                                        ((JazzHome) getContext()).getFileNameByUri(getContext(), imageUri));
                                Uri uri2 = getImageUri(getContext(), bitmapRotated);
                                path2 = getRealPathFromURI(getActivity(), uri2);
                                MyLog.d("people-profilepath2", path);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            AsyncProfilePicUpload upload = new AsyncProfilePicUpload(path2);
                            upload.execute();
//                            attachment.setText(imageUri.getLastPathSegment());
                            profileImage.setImageBitmap(bitmapRotated);


                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                            byte[] byteArray = byteArrayOutputStream.toByteArray();
                            encodedPic = Base64.encodeToString(byteArray, Base64.DEFAULT);


                        }
                    })
//                    .setImageName("image")
//                .setImageFolderName("Konnectrix")
                    .withTimeStamp(false)
//                .setCropScreenColor(Color.CYAN)
                    .setCropScreenColor((Color.parseColor("#000000")))
                    .setOnPermissionRefusedListener(new PickerBuilder.onPermissionRefusedListener() {
                        @Override
                        public void onPermissionRefused() {
//                        Toast.makeText(getApplicationContext(), "NO", Toast.LENGTH_SHORT).show();

                        }
                    })
                    .start();


//        if (Build.VERSION.SDK_INT >= 23 &&
//                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            toast("Enable permission for Storage");
//            return;
//        }
//        if (Build.VERSION.SDK_INT >= 23 &&
//                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            toast("Enable permission for Storage");
//
//            return;
//
//        }
        }
    }

    private class AsyncProfilePicUpload extends AsyncTask<Void, Void, Void> {

        /**
         * The Path.
         */
        String path;
        /**
         * The Is uploaded.
         */
        boolean isUploaded = false;

        /**
         * Instantiates a new Async profile pic upload.
         *
         * @param path the path
         */
        public AsyncProfilePicUpload(String path) {
            this.path = path;
        }

        @Override
        protected Void doInBackground(Void... params) {
            File sourceFile = new File(path);
            final MediaType MEDIA_TYPE = path.endsWith("png") ? MediaType.parse("image/png") : MediaType.parse("image/jpeg");

            //image/png

            Gson gson = new Gson();
            Type type = new TypeToken<UserModel>() {
            }.getType();
            UserModel userobject = gson.fromJson(sessionManager.getStringValue("userobject"), type);

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("username", sessionManager.getStringValue("uname"))
                    .addFormDataPart("ip", "0.0.0.0")
                    .addFormDataPart("image", "profile_image.png", RequestBody.create(MEDIA_TYPE, sourceFile))
                    .addFormDataPart("Key", userobject.getLoginkey())
                    .addFormDataPart("EmployeeID", userobject.getEmployeeid())
                    .addFormDataPart("EmployeeName", userobject.getName())
                    .build();
            Request request = new Request.Builder()
                    .url(ApiConstants.BASEURL + "UploadProfilePic")
                    .post(requestBody)
                    .build();
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
            Response response = null;
            try {

                response = client.newCall(request).execute();
                MyLog.d("profile_picture", "Response :: " + response.body().string());
                isUploaded = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //progressBar.setVisibility(View.GONE);
            profileImage.setVisibility(View.VISIBLE);
            if (isUploaded) {
                downloadProfileImageUrl(true);
            }
        }
    }

    public void downloadProfileImageUrl(boolean isForecefullUpdate) {
        if (isForecefullUpdate) {
            setupImageService();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "IMG_" + Calendar.getInstance().getTime(), null);
        return Uri.parse(path);
    }

    public static String getRealPathFromURI(Activity activity, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    interface imagecallback {
        void uriparameters(String url);
    }
}
