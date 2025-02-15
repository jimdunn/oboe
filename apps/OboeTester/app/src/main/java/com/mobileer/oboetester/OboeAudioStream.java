/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobileer.oboetester;

import java.io.IOException;

/**
 * Implementation of an AudioStreamBase using Oboe.
 */

abstract class OboeAudioStream extends AudioStreamBase {
    private static final int INVALID_STREAM_INDEX = -1;
    int streamIndex = INVALID_STREAM_INDEX;

    @Override
    public void stopPlayback() throws IOException {
        int result = stopPlaybackNative();
        if (result != 0) {
            throw new IOException("Stop Playback failed! result = " + result);
        }
    }

    public native int stopPlaybackNative();

    @Override
    public void startPlayback() throws IOException {
        int result = startPlaybackNative();
        if (result != 0) {
            throw new IOException("Start Playback failed! result = " + result);
        }
    }

    public native int startPlaybackNative();

    // Write disabled because the synth is in native code.
    @Override
    public int write(float[] buffer, int offset, int length) {
        return 0;
    }

    @Override
    public void open(StreamConfiguration requestedConfiguration,
                     StreamConfiguration actualConfiguration, int bufferSizeInFrames) throws IOException {
        super.open(requestedConfiguration, actualConfiguration, bufferSizeInFrames);
        int result = openNative(requestedConfiguration.getNativeApi(),
                requestedConfiguration.getSampleRate(),
                requestedConfiguration.getChannelCount(),
                requestedConfiguration.getChannelMask(),
                requestedConfiguration.getFormat(),
                requestedConfiguration.getSharingMode(),
                requestedConfiguration.getPerformanceMode(),
                requestedConfiguration.getInputPreset(),
                requestedConfiguration.getUsage(),
                requestedConfiguration.getContentType(),
                requestedConfiguration.getDeviceId(),
                requestedConfiguration.getSessionId(),
                requestedConfiguration.getChannelConversionAllowed(),
                requestedConfiguration.getFormatConversionAllowed(),
                requestedConfiguration.getRateConversionQuality(),
                requestedConfiguration.isMMap(),
                isInput()
        );
        if (result < 0) {
            streamIndex = INVALID_STREAM_INDEX;
            throw new IOException("Open failed! result = " + result);
        } else {
            streamIndex = result;
        }
        actualConfiguration.setNativeApi(getNativeApi());
        actualConfiguration.setSampleRate(getSampleRate());
        actualConfiguration.setSharingMode(getSharingMode());
        actualConfiguration.setPerformanceMode(getPerformanceMode());
        actualConfiguration.setInputPreset(getInputPreset());
        actualConfiguration.setUsage(getUsage());
        actualConfiguration.setContentType(getContentType());
        actualConfiguration.setFramesPerBurst(getFramesPerBurst());
        actualConfiguration.setBufferCapacityInFrames(getBufferCapacityInFrames());
        actualConfiguration.setChannelCount(getChannelCount());
        actualConfiguration.setChannelMask(getChannelMask());
        actualConfiguration.setDeviceId(getDeviceId());
        actualConfiguration.setSessionId(getSessionId());
        actualConfiguration.setFormat(getFormat());
        actualConfiguration.setMMap(isMMap());
        actualConfiguration.setDirection(isInput()
                ? StreamConfiguration.DIRECTION_INPUT
                : StreamConfiguration.DIRECTION_OUTPUT);
    }

    private native int openNative(
            int nativeApi,
            int sampleRate,
            int channelCount,
            int channelMask,
            int format,
            int sharingMode,
            int performanceMode,
            int inputPreset,
            int usage,
            int contentType,
            int deviceId,
            int sessionId,
            boolean channelConversionAllowed,
            boolean formatConversionAllowed,
            int rateConversionQuality,
            boolean isMMap,
            boolean isInput);

    @Override
    public void close() {
        if (streamIndex >= 0) {
            close(streamIndex);
            streamIndex = INVALID_STREAM_INDEX;
        }
    }
    public native void close(int streamIndex);

    @Override
    public int getBufferCapacityInFrames() {
        return getBufferCapacityInFrames(streamIndex);
    }
    private native int getBufferCapacityInFrames(int streamIndex);

    @Override
    public int getBufferSizeInFrames() {
        return getBufferSizeInFrames(streamIndex);
    }
    private native int getBufferSizeInFrames(int streamIndex);

    @Override
    public boolean isThresholdSupported() {
        return true;
    }

    @Override
    public int setBufferSizeInFrames(int thresholdFrames) {
        return setBufferSizeInFrames(streamIndex, thresholdFrames);
    }
    private native int setBufferSizeInFrames(int streamIndex, int thresholdFrames);

    public int getNativeApi() {
        return getNativeApi(streamIndex);
    }
    private native int getNativeApi(int streamIndex);

    @Override
    public int getFramesPerBurst() {
        return getFramesPerBurst(streamIndex);
    }
    private native int getFramesPerBurst(int streamIndex);

    public int getSharingMode() {
        return getSharingMode(streamIndex);
    }
    private native int getSharingMode(int streamIndex);

    public int getPerformanceMode() {
        return getPerformanceMode(streamIndex);
    }
    private native int getPerformanceMode(int streamIndex);

    public int getInputPreset() {
        return getInputPreset(streamIndex);
    }
    private native int getInputPreset(int streamIndex);

    public int getSampleRate() {
        return getSampleRate(streamIndex);
    }
    private native int getSampleRate(int streamIndex);

    public int getFormat() {
        return getFormat(streamIndex);
    }
    private native int getFormat(int streamIndex);

    public int getUsage() {
        return getUsage(streamIndex);
    }
    private native int getUsage(int streamIndex);

    public int getContentType() {
        return getContentType(streamIndex);
    }
    private native int getContentType(int streamIndex);

    public int getChannelCount() {
        return getChannelCount(streamIndex);
    }
    private native int getChannelCount(int streamIndex);

    public int getChannelMask() {
        return getChannelMask(streamIndex);
    }
    private native int getChannelMask(int streamIndex);

    public int getDeviceId() {
        return getDeviceId(streamIndex);
    }
    private native int getDeviceId(int streamIndex);

    public int getSessionId() {
        return getSessionId(streamIndex);
    }
    private native int getSessionId(int streamIndex);

    public boolean isMMap() {
        return isMMap(streamIndex);
    }
    private native boolean isMMap(int streamIndex);

    @Override
    public native long getCallbackCount(); // TODO Move to another class?

    @Override
    public int getLastErrorCallbackResult() {
        return getLastErrorCallbackResult(streamIndex);
    }
    private native int getLastErrorCallbackResult(int streamIndex);

    @Override
    public long getFramesWritten() {
        return getFramesWritten(streamIndex);
    }
    private native long getFramesWritten(int streamIndex);

    @Override
    public long getFramesRead() {
        return getFramesRead(streamIndex);
    }
    private native long getFramesRead(int streamIndex);

    @Override
    public int getXRunCount() {
        return getXRunCount(streamIndex);
    }
    private native int getXRunCount(int streamIndex);

    @Override
    public double getLatency() {
        return getTimestampLatency(streamIndex);
    }
    private native double getTimestampLatency(int streamIndex);

    @Override
    public double getCpuLoad() {
        return getCpuLoad(streamIndex);
    }
    private native double getCpuLoad(int streamIndex);

    @Override
    public String getCallbackTimeStr() {
        return getCallbackTimeString();
    }
    public native String getCallbackTimeString();

    @Override
    public native void setWorkload(double workload);

    @Override
    public int getState() {
        return getState(streamIndex);
    }
    private native int getState(int streamIndex);

    public static native void setCallbackReturnStop(boolean b);

    public static native void setUseCallback(boolean checked);

    public static native void setCallbackSize(int callbackSize);

    public static native int getOboeVersionNumber();

}
