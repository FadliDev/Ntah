#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cstdlib>

// Kamus mini (ID + EN), urut kira-kira berdasarkan frekuensi.
static const char* W[] = {
 "yang","dan","di","ini","itu","dengan","untuk","tidak","dari","dalam","akan","pada","juga","saya","karena",
 "kamu","kita","mereka","sudah","bisa","ada","apa","tapi","atau","lagi","jika","sama","banget","makasih",
 "terima","kasih","selamat","pagi","siang","malam","gimana","kenapa","dimana","kapan","siapa","udah","belum",
 "nggak","enggak","aja","dong","deh","sih","kok","yuk","ayo","tolong","maaf","besok","hari","nanti","sekarang",
 "the","be","to","of","and","in","that","have","it","for","not","on","with","you","do","at","this","but","from",
 "they","we","say","her","she","will","my","one","all","would","there","their","what","so","out","about","who",
 "get","which","go","when","make","can","like","time","just","know","take","people","into","year","your","good",
 "some","could","them","see","other","than","then","now","look","only","come","think","also","back","after",
 "use","two","how","our","work","first","well","way","even","new","want","because","any","these","give","day",
 "thanks","please","hello","sorry","tomorrow","today","where","why","really","great"
};
static int lev(const std::string& a, const std::string& b) {
    std::vector<int> prev(b.size() + 1), cur(b.size() + 1);
    for (size_t j = 0; j <= b.size(); j++) prev[j] = (int)j;
    for (size_t i = 1; i <= a.size(); i++) {
        cur[0] = (int)i;
        for (size_t j = 1; j <= b.size(); j++)
            cur[j] = std::min({prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (a[i - 1] != b[j - 1])});
        prev = cur;
    }
    return prev[b.size()];
}
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_sally_keyboard_Native_suggest(JNIEnv* env, jobject, jstring p) {
    const char* c = env->GetStringUTFChars(p, nullptr);
    std::string s(c);
    env->ReleaseStringUTFChars(p, c);
    std::vector<std::string> out;
    for (const char* w : W) {            // 1) prediksi prefix
        std::string x(w);
        if (x.size() > s.size() && x.compare(0, s.size(), s) == 0) { out.push_back(x); if (out.size() == 3) break; }
    }
    if (out.size() < 3 && s.size() >= 3) { // 2) koreksi typo (jarak edit <= 1)
        for (const char* w : W) {
            std::string x(w);
            if (x == s || std::abs((int)x.size() - (int)s.size()) > 1 || lev(x, s) > 1) continue;
            if (std::find(out.begin(), out.end(), x) == out.end()) { out.push_back(x); if (out.size() == 3) break; }
        }
    }
    jclass sc = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray((jsize)out.size(), sc, nullptr);
    for (size_t i = 0; i < out.size(); i++)
        env->SetObjectArrayElement(arr, (jsize)i, env->NewStringUTF(out[i].c_str()));
    return arr;
}
