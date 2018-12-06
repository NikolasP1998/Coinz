package nikolas.example.com.coinz

import android.os.AsyncTask
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadFileTask(private val caller : DownloadCompleteListener) : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg urls: String): String = try {
        loadFileFromNetwork(urls[0])
    } catch (e: IOException) {
        "Unable to load content. Check your internet connection."
    }

    private fun loadFileFromNetwork(urlString: String): String {
        val stream: InputStream = downloadUrl(urlString)
        // Read input from stream, build result as a string

        val string = stream.use {
            it.reader().use { reader ->
                reader.readText()
            }
        }

        storeJson(string)

        return string
    }

    // Given a string representation of a URL, sets up a connection and gets an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        // Also available: HttpsURLConnection
        conn.readTimeout = 10000 // milliseconds
        conn.connectTimeout = 15000 // milliseconds
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect() // Starts the query

        return conn.inputStream
    }

    private fun storeJson(content: String) {

        val path = "/data/data/nikolas.example.com.coinz/files/coinzmap.geojson"
        File(path).writeText(content)
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)

        caller.downloadComplete(result)
    }
}
