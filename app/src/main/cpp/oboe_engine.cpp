#include "oboe_engine.h"
#include <chrono>
#include <numeric>
#include <algorithm>

static SpectralAnalyzer* gAnalyzer = nullptr;
static JavaVM* gJavaVM = nullptr;
static jobject gCallbackObject = nullptr;
static jmethodID gOnAudioIntensityMethod = nullptr;

SpectralAnalyzer::SpectralAnalyzer(int sampleRate)
    : mSampleRate(sampleRate),
      mBassHistory(16, 0.0f),
      mHistoryIndex(0),
      mLastBeatMs(0) {}

SpectralAnalysisResult SpectralAnalyzer::processFrame(const int16_t* pcmBuffer, int numFrames) {
    SpectralAnalysisResult result;
    if (numFrames <= 0 || !pcmBuffer) {
        return result;
    }

    double sumSquares = 0.0;
    int16_t peakValue = 0;

    // Goertzel frequency band filters for Low (100Hz), Mid (1000Hz), High (4000Hz)
    const double kBass = 2.0 * cos(2.0 * M_PI * 100.0 / mSampleRate);
    const double kMid = 2.0 * cos(2.0 * M_PI * 1000.0 / mSampleRate);
    const double kTreble = 2.0 * cos(2.0 * M_PI * 4000.0 / mSampleRate);

    double sPrevBass1 = 0.0, sPrevBass2 = 0.0;
    double sPrevMid1 = 0.0, sPrevMid2 = 0.0;
    double sPrevTreble1 = 0.0, sPrevTreble2 = 0.0;

    for (int i = 0; i < numFrames; ++i) {
        double sample = static_cast<double>(pcmBuffer[i]) / 32768.0;
        sumSquares += sample * sample;
        if (std::abs(pcmBuffer[i]) > peakValue) {
            peakValue = std::abs(pcmBuffer[i]);
        }

        // Goertzel recurrences
        double sBass = sample + kBass * sPrevBass1 - sPrevBass2;
        sPrevBass2 = sPrevBass1;
        sPrevBass1 = sBass;

        double sMid = sample + kMid * sPrevMid1 - sPrevMid2;
        sPrevMid2 = sPrevMid1;
        sPrevMid1 = sMid;

        double sTreble = sample + kTreble * sPrevTreble1 - sPrevTreble2;
        sPrevTreble2 = sPrevTreble1;
        sPrevTreble1 = sTreble;
    }

    double rms = std::sqrt(sumSquares / numFrames);
    float normalizedEnergy = std::min(1.0, std::max(0.0, rms));

    double bassPower = std::sqrt(sPrevBass1 * sPrevBass1 + sPrevBass2 * sPrevBass2 - kBass * sPrevBass1 * sPrevBass2) / numFrames;
    double midPower = std::sqrt(sPrevMid1 * sPrevMid1 + sPrevMid2 * sPrevMid2 - kMid * sPrevMid1 * sPrevMid2) / numFrames;
    double treblePower = std::sqrt(sPrevTreble1 * sPrevTreble1 + sPrevTreble2 * sPrevTreble2 - kTreble * sPrevTreble1 * sPrevTreble2) / numFrames;

    result.bassEnergy = static_cast<float>(std::min(1.0, (bassPower * 12.0) + (normalizedEnergy * 1.2)));
    result.midEnergy = static_cast<float>(std::min(1.0, (midPower * 18.0) + (normalizedEnergy * 0.8)));
    result.trebleEnergy = static_cast<float>(std::min(1.0, (treblePower * 25.0) + (normalizedEnergy * 0.5)));

    // Beat detection logic
    mBassHistory[mHistoryIndex] = result.bassEnergy;
    mHistoryIndex = (mHistoryIndex + 1) % mBassHistory.size();

    float avgBass = std::accumulate(mBassHistory.begin(), mBassHistory.end(), 0.0f) / mBassHistory.size();

    auto nowMs = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();

    result.beatIntensity = std::min(1.0f, std::max(0.0f, result.bassEnergy * 0.7f + normalizedEnergy * 0.3f));
    result.isBeatDrop = false;

    if (result.bassEnergy > (avgBass * 1.5f) && result.bassEnergy > 0.40f) {
        result.beatIntensity = 1.0f;
        if (nowMs - mLastBeatMs > 320) {
            int64_t interval = nowMs - mLastBeatMs;
            mLastBeatMs = nowMs;
            if (interval >= 300 && interval <= 1000) {
                mBeatIntervals.push_back(interval);
                if (mBeatIntervals.size() > 8) {
                    mBeatIntervals.erase(mBeatIntervals.begin());
                }
            }

            if (avgBass < 0.35f && result.bassEnergy > 0.70f) {
                result.isBeatDrop = true;
            }
        }
    }

    if (!mBeatIntervals.empty()) {
        double avgInterval = std::accumulate(mBeatIntervals.begin(), mBeatIntervals.end(), 0.0) / mBeatIntervals.size();
        result.calculatedBpm = (avgInterval > 0) ? std::min(180, std::max(70, static_cast<int>(60000.0 / avgInterval))) : 128;
    } else {
        result.calculatedBpm = 128;
    }

    return result;
}

// JNI Implementation Bridge Functions
extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    gJavaVM = vm;
    LOGI("Oboe C++/JNI library loaded successfully.");
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_com_example_feature_camera_OboeNativeEngine_nativeInitStream(
        JNIEnv* env, jobject thiz, jint sampleRate, jint framesPerBuffer) {
    if (gAnalyzer) {
        delete gAnalyzer;
    }
    gAnalyzer = new SpectralAnalyzer(sampleRate);
    LOGI("Oboe native spectral analyzer initialized with sample rate: %d", sampleRate);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_feature_camera_OboeNativeEngine_nativeStartStream(
        JNIEnv* env, jobject thiz) {
    LOGI("Oboe audio stream started.");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_example_feature_camera_OboeNativeEngine_nativeStopStream(
        JNIEnv* env, jobject thiz) {
    if (gAnalyzer) {
        delete gAnalyzer;
        gAnalyzer = nullptr;
    }
    LOGI("Oboe audio stream stopped.");
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_feature_camera_OboeNativeEngine_nativeProcessAudioFrame(
        JNIEnv* env, jobject thiz, jshortArray buffer, jint size) {
    if (!gAnalyzer || !buffer || size <= 0) {
        return nullptr;
    }

    jshort* pcmData = env->GetShortArrayElements(buffer, nullptr);
    SpectralAnalysisResult res = gAnalyzer->processFrame(pcmData, size);
    env->ReleaseShortArrayElements(buffer, pcmData, JNI_ABORT);

    jfloatArray resultArray = env->NewFloatArray(6);
    if (resultArray != nullptr) {
        jfloat values[6];
        values[0] = res.bassEnergy;
        values[1] = res.midEnergy;
        values[2] = res.trebleEnergy;
        values[3] = res.beatIntensity;
        values[4] = static_cast<jfloat>(res.calculatedBpm);
        values[5] = res.isBeatDrop ? 1.0f : 0.0f;
        env->SetFloatArrayRegion(resultArray, 0, 6, values);
    }

    return resultArray;
}

} // extern "C"
