#include <jni.h>
#include <string>
#include <cctype>

// Fuzzy score: prefix > substring > subsequence. 0 = no match.
static int fuzzy(const std::string& q, const std::string& t) {
    if (q.empty()) return 1;
    if (t.compare(0, q.size(), q) == 0) return 1000 - (int)t.size();
    size_t pos = t.find(q);
    if (pos != std::string::npos) return 600 - (int)pos;
    size_t ti = 0; int gaps = 0;
    for (char c : q) {
        size_t f = t.find(c, ti);
        if (f == std::string::npos) return 0;
        gaps += (int)(f - ti);
        ti = f + 1;
    }
    return 300 - gaps;
}
static std::string lower(JNIEnv* env, jstring s) {
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string r(c);
    env->ReleaseStringUTFChars(s, c);
    for (auto& ch : r) ch = (char)tolower((unsigned char)ch);
    return r;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_sally_home_Native_score(JNIEnv* env, jobject, jstring q, jstring t) {
    return fuzzy(lower(env, q), lower(env, t));
}
