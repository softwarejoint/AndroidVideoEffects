package com.serenegiant.audiovideosample;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: CameraFragment.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.serenegiant.encoder.MediaAudioEncoder;
import com.serenegiant.encoder.MediaEncoder;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.encoder.MediaVideoEncoder;
import com.serenegiant.fileio.FileHandler;
import com.serenegiant.glutils.GL1977Filter;
import com.serenegiant.glutils.GLArtFilter;
import com.serenegiant.glutils.GLBloomFilter;
import com.serenegiant.glutils.GLGrayscaleFilter;
import com.serenegiant.glutils.GLPosterizeFilter;
import com.serenegiant.glutils.GLToneCurveFilter;
import com.serenegiant.mediaaudiotest.BuildConfig;
import com.serenegiant.mediaaudiotest.R;

import java.io.File;
import java.io.IOException;

public class CameraFragment extends Fragment {

    public final String TAG = "CameraFragment";

    private static final String INTENT_FILEPATH = "com.wafer.picker.image.file";

    public static CameraFragment newInstance() {
        return newInstance(null);
    }

    public static CameraFragment newInstance(@Nullable String mediaPath) {
        Bundle args = new Bundle();
        args.putString(INTENT_FILEPATH, mediaPath);
        CameraFragment fragment = new CameraFragment();
        fragment.setArguments(args);
        return fragment;
    }
    /**
     * for camera preview display
     */
    private CameraGLView mCameraView;
    private String mediaPath;
    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener =
            new MediaEncoder.MediaEncoderListener() {
                @Override
                public void onPrepared(final MediaEncoder encoder) {
                    Log.d(TAG, "onPrepared:encoder=" + encoder);
                    if (encoder instanceof MediaVideoEncoder) {
                        mCameraView.setVideoEncoder((MediaVideoEncoder) encoder);
                    }
                }

                @Override
                public void onStopped(final MediaEncoder encoder) {
                    Log.d(TAG, "onStopped:encoder=" + encoder);
                    if (encoder instanceof MediaVideoEncoder) mCameraView.setVideoEncoder(null);
                }
            };
    /**
     * for scale mode display
     */
    private TextView mScaleModeView;
    /**
     * button for start/stop recording
     */
    private ImageButton mRecordButton;
    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    /**
     * method when touch record button
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.cameraView:
                    final int scale_mode = (mCameraView.getScaleMode() + 1) % 4;
                    mCameraView.setScaleMode(scale_mode);
                    updateScaleModeText();
                    break;
                case R.id.record_button:
                    if (mMuxer == null) {
                        startRecording();
                    } else {
                        stopRecording();
                    }
                    break;
            }
        }
    };

    public CameraFragment() {
        // need default constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //noinspection ConstantConditions
        mediaPath = getArguments().getString(INTENT_FILEPATH);
        if (mediaPath == null) {
            mediaPath = FileHandler.getTempFile(getContext()).getPath();
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mCameraView = rootView.findViewById(R.id.cameraView);
        mCameraView.setVideoSize(1280, 720);
        mCameraView.setOnClickListener(mOnClickListener);
        mScaleModeView = rootView.findViewById(R.id.scalemode_textview);
        updateScaleModeText();
        mRecordButton = rootView.findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(mOnClickListener);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_posterize:
                mCameraView.setDrawer(new GLPosterizeFilter());
                break;
            case R.id.action_grayscale:
                mCameraView.setDrawer(new GLGrayscaleFilter());
                break;
            case R.id.action_art:
                mCameraView.setDrawer(new GLArtFilter());
                break;
            case R.id.action_1977:
                mCameraView.setDrawer(new GL1977Filter());
                break;
            case R.id.action_bloom:
                mCameraView.setDrawer(new GLBloomFilter());
                break;
            case R.id.action_tone_curve:
                GLToneCurveFilter toneCurveFilter = new GLToneCurveFilter();
                toneCurveFilter.setFromCurveFileInputStream(getResources().openRawResource(R.raw.frozen));
                mCameraView.setDrawer(toneCurveFilter);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume:");
        mCameraView.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause:");
        stopRecording();
        mCameraView.onPause();
        super.onPause();
    }

    private void updateScaleModeText() {
        final int scale_mode = mCameraView.getScaleMode();

        String scaleText;
        switch (scale_mode) {
            case 0:
                scaleText = "scale to fit";
                break;
            case 1:
                scaleText = "keep aspect(viewport)";
                break;
            case 2:
                scaleText = "keep aspect(matrix)";
                break;
            case 3:
                scaleText = "keep aspect(crop center)";
                break;
            default:
                scaleText = "";
                break;
        }

        mScaleModeView.setText(scaleText);
    }

    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */
    private void startRecording() {
        Log.d(TAG, "startRecording:");

        try {
            mRecordButton.setColorFilter(0xffff0000);  // turn red
            mMuxer = new MediaMuxerWrapper(mediaPath);  // if you record audio only, ".m4a" is also OK.

            // for video capturing
            new MediaVideoEncoder(mMuxer, mMediaEncoderListener, mCameraView.getVideoWidth(),
                    mCameraView.getVideoHeight(), mCameraView.getDrawer());

            // for audio capturing
            new MediaAudioEncoder(mMuxer, mMediaEncoderListener);

            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
            mRecordButton.setColorFilter(0);
            Log.e(TAG, "startCapture:", e);
        }
    }

    /**
     * request stop recording
     */
    private void stopRecording() {
        Log.d(TAG, "stopRecording:mMuxer=" + mMuxer);

        mRecordButton.setColorFilter(0);  // return to default color

        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            // you should not wait here
        }
    }
}