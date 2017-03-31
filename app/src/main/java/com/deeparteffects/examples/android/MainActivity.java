package com.deeparteffects.examples.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.bumptech.glide.Glide;
import com.deeparteffects.sdk.android.DeepArtEffectsClient;
import com.deeparteffects.sdk.android.model.Result;
import com.deeparteffects.sdk.android.model.Styles;
import com.deeparteffects.sdk.android.model.UploadRequest;
import com.deeparteffects.sdk.android.model.UploadResponse;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import static com.amazonaws.regions.Regions.EU_WEST_1;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String API_KEY = "--INSERT YOUR API KEY--";
    private static final String ACCESS_KEY = "--INSERT YOUR ACCESS KEY--";
    private static final String SECRET_KEY = "--INSERT YOUR SECRET KEY--";

    private static final int REQUEST_GALLERY = 100;
    private static final int CHECK_RESULT_INTERVAL_IN_MS = 2500;
    private static final int IMAGE_MAX_SIDE_LENGTH = 768;

    private Activity mActivity;
    private Bitmap mImageBitmap;
    private TextView mStatusText;
    private ImageView mImageView;
    private ProgressBar mProgressbarView;
    private boolean isProcessing = false;

    private RecyclerView recyclerView;
    private DeepArtEffectsClient deepArtEffectsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;

        ApiClientFactory factory = new ApiClientFactory()
                .apiKey(API_KEY)
                .credentialsProvider(new AWSCredentialsProvider() {
                    @Override
                    public AWSCredentials getCredentials() {
                        return new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
                    }

                    @Override
                    public void refresh() {
                    }
                }).region(EU_WEST_1.getName());
        deepArtEffectsClient = factory.build(DeepArtEffectsClient.class);

        mStatusText = (TextView) findViewById(R.id.statusText);
        mProgressbarView = (ProgressBar) findViewById(R.id.progressBar);
        mImageView = (ImageView) findViewById(R.id.imageView);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Button btnGallery = (Button) findViewById(R.id.btnGallery);
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, REQUEST_GALLERY);
                }
            }
        });

        loadingStyles();
    }

    private void loadingStyles() {
        mStatusText.setText("Loading styles...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Styles styles = deepArtEffectsClient.stylesGet();
                final StyleAdapter styleAdapter = new StyleAdapter(
                        getApplicationContext(),
                        styles,
                        new StyleAdapter.ClickListener() {
                            @Override
                            public void onClick(String styleId) {
                                if (!isProcessing) {
                                    if (mImageBitmap != null) {
                                        Log.d(TAG, String.format("Style with ID %s clicked.", styleId));
                                        isProcessing = true;
                                        mProgressbarView.setVisibility(View.VISIBLE);
                                        uploadImage(styleId);
                                    } else {
                                        Toast.makeText(mActivity, "Please choose a picture first",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.setAdapter(styleAdapter);
                        mProgressbarView.setVisibility(View.GONE);
                        mStatusText.setText("");
                    }
                });
            }
        }).start();
    }

    private class ImageReadyCheckTimer extends TimerTask {
        private String mSubmissionId;

        public ImageReadyCheckTimer(String submissionId) {
            mSubmissionId = String.valueOf(submissionId);
        }

        @Override
        public void run() {
            try {
                final Result result = deepArtEffectsClient.resultGet(mSubmissionId);
                String submissionStatus = result.getStatus();
                Log.d(TAG, String.format("Submission status is %s", submissionStatus));
                if (submissionStatus.equals(SubmissionStatus.FINISHED)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(mActivity).load(result.getUrl()).centerCrop().crossFade().into(mImageView);
                            mProgressbarView.setVisibility(View.GONE);
                            mImageView.setVisibility(View.VISIBLE);
                            mStatusText.setText("");
                        }
                    });
                    isProcessing = false;
                    cancel();
                }
            } catch (Exception e) {
                cancel();
            }
        }
    }

    private void uploadImage(final String styleId) {
        mStatusText.setText("Uploading picture...");
        Log.d(TAG, String.format("Upload image with style id %s", styleId));
        new Thread(new Runnable() {
            @Override
            public void run() {
                UploadRequest uploadRequest = new UploadRequest();
                uploadRequest.setStyleId(styleId);
                uploadRequest.setImageBase64Encoded(convertBitmapToBase64(mImageBitmap));
                UploadResponse response = deepArtEffectsClient.uploadPost(uploadRequest);
                String submissionId = response.getSubmissionId();
                Log.d(TAG, String.format("Upload complete. Got submissionId %s", response.getSubmissionId()));
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new ImageReadyCheckTimer(submissionId),
                        CHECK_RESULT_INTERVAL_IN_MS, CHECK_RESULT_INTERVAL_IN_MS);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mStatusText.setText("Picture processing...");
                    }
                });
            }
        }).start();
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return Base64.encodeToString(byteArray, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");

        //Handle own activity result
        switch (requestCode) {
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    mImageBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(data.getData(),
                            this.getContentResolver(), IMAGE_MAX_SIDE_LENGTH);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
