#ifndef OBOE_ENGINE_H
#define OBOE_ENGINE_H

#include <jni.h>
#include <memory>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "OboeAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Spectral Analysis Result for Sound-Reactive UI Animations
 */
struct SpectralAnalysisResult {
    float bassEnergy;      // 20Hz - 250Hz (0.0 - 1.0)
    float midEnergy;       // 250Hz - 2000Hz (0.0 - 1.0)
    float trebleEnergy;    // 2000Hz - 10000Hz (0.0 - 1.0)
    float beatIntensity;   // Instantaneous pulse energy (0.0 - 1.0)
    int calculatedBpm;     // Detected musical tempo (70 - 180 BPM)
    bool isBeatDrop;       // High-energy drop trigger
};

/**
 * Spectral DSP Engine for real-time audio frame processing
 */
class SpectralAnalyzer {
public:
    SpectralAnalyzer(int sampleRate = 44100);
    SpectralAnalysisResult processFrame(const int16_t* pcmBuffer, int numFrames);

private:
    int mSampleRate;
    std::vector<float> mBassHistory;
    size_t mHistoryIndex;
    int64_t mLastBeatMs;
    std::vector<int64_t> mBeatIntervals;
};

#endif // OBOE_ENGINE_H
