#include <jni.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <vector>

extern "C" JNIEXPORT jbyteArray JNICALL
Java_dev_tytancraft_TerrainNative_generateHeights(JNIEnv* env, jclass, jint width, jint depth, jlong seed) {
    if (width <= 0 || depth <= 0 || static_cast<std::int64_t>(width) * depth > 16'000'000) {
        return nullptr;
    }

    std::vector<jbyte> heights(static_cast<std::size_t>(width) * depth);
    const double offset = static_cast<double>(seed % 10'000'000LL) * 0.00031;
    for (int z = 0; z < depth; ++z) {
        for (int x = 0; x < width; ++x) {
            const double xd = x + offset;
            const double zd = z - offset * 0.73;
            const double hills = 12.0
                + std::sin(xd * 0.11) * 3.8
                + std::cos(zd * 0.13) * 3.2
                + std::sin((xd + zd) * 0.075) * 2.8
                + std::sin((xd - zd) * 0.033) * 4.0;
            const double ridge = std::max(0.0, 1.0
                - std::abs(std::sin(xd * 0.028) * std::cos(zd * 0.032)) - 0.70) * 18.0;
            const int height = std::max(4, std::min(56, static_cast<int>(std::floor(hills + ridge + 0.5))));
            heights[static_cast<std::size_t>(z) * width + x] = static_cast<jbyte>(height);
        }
    }

    jbyteArray result = env->NewByteArray(static_cast<jsize>(heights.size()));
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(heights.size()), heights.data());
    }
    return result;
}
