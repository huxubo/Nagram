package tw.nekomimi.nekogram.transtale.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.transtale.Translator
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*

object MicrosoftTranslator : Translator {

    private val targetLanguages = Arrays.asList(
            "ar", "as", "bn", "bs", "bg", "yue", "ca", "zh", "zh-Hans", "zh-Hant",
            "hr", "cs", "da", "prs", "nl", "en", "et", "fj", "fil", "fi",
            "fr", "de", "el", "gu", "ht", "he", "hi", "mww", "hu", "is",
            "id", "ga", "it", "ja", "kn", "kk", "tlh", "ko", "ku", "kmr",
            "lv", "lt", "mg", "ms", "ml", "mt", "mi", "mr", "nb", "or", "ps",
            "fa", "pl", "pt", "pa", "otq", "ro", "ru", "sm", "sr", "sk", "sl",
            "es", "sw", "sv", "ty", "ta", "te", "th", "to", "tr", "uk", "ur",
            "vi", "cy", "yua")
    private var useCN = false

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        if (to !in targetLanguages) {
            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))
        }

        return withContext(Dispatchers.IO) {
            val param = "fromLang=auto-detect&text=" + URLEncoder.encode(query, "UTF-8") +
                    "&to=" + to
            val response = request(param)
            val jsonObject = JSONArray(response).getJSONObject(0)
            if (!jsonObject.has("translations")) {
                throw IOException(response)
            }
            val array = jsonObject.getJSONArray("translations")
            array.getJSONObject(0).getString("text")
        }
    }

    private fun request(param: String): String {
        val httpConnectionStream: InputStream
        val downloadUrl = URL("https://" + (if (useCN) "cn" else "www") + ".bing.com/ttranslatev3")
        val httpConnection = downloadUrl.openConnection() as HttpURLConnection
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1")
        httpConnection.connectTimeout = 1000
        //httpConnection.setReadTimeout(2000);
        httpConnection.requestMethod = "POST"
        httpConnection.doOutput = true
        httpConnection.instanceFollowRedirects = false
        val dataOutputStream = DataOutputStream(httpConnection.outputStream)
        val t = param.toByteArray(Charset.defaultCharset())
        dataOutputStream.write(t)
        dataOutputStream.flush()
        dataOutputStream.close()
        httpConnection.connect()
        if (httpConnection.responseCode != HttpURLConnection.HTTP_OK) {
            if (httpConnection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                useCN = !useCN
                FileLog.e("Move to " + if (useCN) "cn" else "www")
                return request(param)
            }
            httpConnectionStream = httpConnection.errorStream
        } else {
            httpConnectionStream = httpConnection.inputStream
        }
        val outbuf = ByteArrayOutputStream()
        val data = ByteArray(1024 * 32)
        while (true) {
            val read = httpConnectionStream.read(data)
            if (read > 0) {
                outbuf.write(data, 0, read)
            } else if (read == -1) {
                break
            } else {
                break
            }
        }
        val result = String(outbuf.toByteArray())
        httpConnectionStream.close()
        outbuf.close()
        return result
    }

}