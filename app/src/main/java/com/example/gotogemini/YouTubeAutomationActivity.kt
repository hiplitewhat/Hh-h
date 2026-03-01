package com.example.gotogemini

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class YouTubeAutomationActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchInput: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnPlaylist: Button
    private lateinit var btnCapture: Button
    private lateinit var btnBack: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var framesSpinner: Spinner
    private lateinit var videosSpinner: Spinner

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var isCapturing = false
    private var selectedFrames = 3
    private var selectedVideos = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_youtube)

        initViews()
        setupWebView()
        setupSpinners()
        setupClickListeners()
    }

    private fun initViews() {
        webView = findViewById(R.id.ytWebView)
        searchInput = findViewById(R.id.ytSearchInput)
        btnSearch = findViewById(R.id.btnYtSearch)
        btnPlaylist = findViewById(R.id.btnYtPlaylist)
        btnCapture = findViewById(R.id.btnYtCapture)
        btnBack = findViewById(R.id.btnYtBack)
        statusText = findViewById(R.id.ytStatusText)
        progressBar = findViewById(R.id.ytProgressBar)
        framesSpinner = findViewById(R.id.framesSpinner)
        videosSpinner = findViewById(R.id.videosSpinner)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                statusText.text = "Loaded: ${url?.take(50)}..."
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        webView.loadUrl("https://m.youtube.com")
    }

    private fun setupSpinners() {
        val frameOptions = arrayOf("1 frame", "2 frames", "3 frames", "5 frames", "10 frames")
        framesSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, frameOptions)
        framesSpinner.setSelection(2)
        framesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedFrames = listOf(1, 2, 3, 5, 10)[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val videoOptions = arrayOf("1 video", "2 videos", "3 videos", "5 videos", "10 videos")
        videosSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, videoOptions)
        videosSpinner.setSelection(2)
        videosSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedVideos = listOf(1, 2, 3, 5, 10)[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        btnSearch.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                handleYouTubeSearch(query)
            } else {
                Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show()
            }
        }

        btnPlaylist.setOnClickListener {
            val url = searchInput.text.toString().trim()
            if (url.startsWith("http")) {
                handleYouTubePlaylistAuto(url, selectedFrames, selectedVideos)
            } else {
                Toast.makeText(this, "Enter a playlist URL", Toast.LENGTH_SHORT).show()
            }
        }

        btnCapture.setOnClickListener {
            if (!isCapturing) {
                isCapturing = true
                btnCapture.text = "Stop Capture"
                takeMultipleScreenshots(selectedFrames, 5000)
            } else {
                isCapturing = false
                btnCapture.text = "Start Capture"
                handler.removeCallbacksAndMessages(null)
                statusText.text = "Capture stopped"
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    // ---------------------------
    // YouTube Search
    // ---------------------------
    private fun handleYouTubeSearch(query: String) {
        // Try native YouTube app first
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        if (intent != null) {
            intent.action = Intent.ACTION_SEARCH
            intent.putExtra("query", query)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            statusText.text = "Opened YouTube app: $query"
        } else {
            val url = "https://m.youtube.com/results?search_query=${Uri.encode(query)}"
            webView.loadUrl(url)
            statusText.text = "Searching YouTube: $query"
        }
    }

    // ---------------------------
    // Playlist Mode
    // ---------------------------
    private fun handleYouTubePlaylistAuto(
        playlistUrl: String,
        framesPerVideo: Int = 3,
        videosToCapture: Int = 5
    ) {
        statusText.text = "Loading playlist..."
        mainScope.launch {
            webView.loadUrl(playlistUrl)
            delay(5000) // Wait for playlist to load

            for (i in 1..videosToCapture) {
                statusText.text = "Video $i/$videosToCapture — Capturing $framesPerVideo frames"
                takeMultipleScreenshots(framesPerVideo, 5000)
                delay((framesPerVideo * 5000 + 3000).toLong())

                // Click next video via JavaScript
                webView.evaluateJavascript(
                    """
                    (function() {
                        var nextBtn = document.querySelector('.ytp-next-button');
                        if (nextBtn) { nextBtn.click(); return 'clicked'; }
                        return 'not found';
                    })()
                    """.trimIndent()
                ) { result ->
                    statusText.text = "Next video: $result"
                }
                delay(5000) // Wait for next video to load
            }
            statusText.text = "Playlist capture complete"
        }
    }

    // ---------------------------
    // Screenshot capture
    // ---------------------------
    private fun takeMultipleScreenshots(frames: Int, delayMs: Long) {
        var count = 0
        val runnable = object : Runnable {
            override fun run() {
                if (count < frames && isCapturing) {
                    captureFrame(count + 1, frames)
                    count++
                    handler.postDelayed(this, delayMs)
                }
            }
        }
        handler.post(runnable)
    }

    private fun captureFrame(current: Int, total: Int) {
        statusText.text = "Screenshot $current/$total"

        // Trigger screenshot service
        val intent = Intent(this, ScreenshotService::class.java).apply {
            putExtra("frame_number", current)
            putExtra("total_frames", total)
            putExtra("source", "youtube")
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
