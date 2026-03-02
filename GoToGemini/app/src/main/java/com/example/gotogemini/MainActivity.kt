package com.example.gotogemini

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var goGeminiButton: Button
    private lateinit var promptInput: EditText
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnYouTube: Button
    private lateinit var btnAppControl: Button
    private lateinit var btnStopAll: Button
    private lateinit var logRecyclerView: RecyclerView
    private lateinit var promptSpinner: Spinner

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var currentVideoTask: VideoTask? = null
    private var isRunning = false

    private val logAdapter = LogAdapter()
    private val logs = mutableListOf<LogEntry>()

    // Preset Gemini prompts
    private val presetPrompts = listOf(
        "Hello Gemini, summarize today's news in 3 sentences.",
        "List the top 5 tech trends this month.",
        "Provide a short meditation exercise.",
        "What are the best productivity tips for developers?",
        "Explain quantum computing in simple terms."
    )

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupSpinner()
        setupRecyclerView()
        setupClickListeners()
        requestPermissions()

        addLog("System", "GoToGemini initialized successfully")
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        goGeminiButton = findViewById(R.id.goGeminiButton)
        promptInput = findViewById(R.id.promptInput)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        btnYouTube = findViewById(R.id.btnYouTube)
        btnAppControl = findViewById(R.id.btnAppControl)
        btnStopAll = findViewById(R.id.btnStopAll)
        logRecyclerView = findViewById(R.id.logRecyclerView)
        promptSpinner = findViewById(R.id.promptSpinner)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportMultipleWindows(false)
            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                addLog("Web", "Page loaded: ${url?.take(60)}...")
                currentVideoTask?.let { task ->
                    if (!task.completed) {
                        task.completed = true
                        addLog("Screenshot", "Starting multi-frame capture (${task.frames} frames)")
                        takeMultipleScreenshotsToAI(task.frames, task.delayMs)
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetPrompts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        promptSpinner.adapter = adapter
        promptSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                promptInput.setText(presetPrompts[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        logRecyclerView.layoutManager = LinearLayoutManager(this)
        logRecyclerView.adapter = logAdapter
    }

    private fun setupClickListeners() {
        goGeminiButton.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isNotEmpty()) {
                sendPromptToGemini(prompt)
            } else {
                Toast.makeText(this, "Enter a prompt first", Toast.LENGTH_SHORT).show()
            }
        }

        btnYouTube.setOnClickListener {
            startActivity(Intent(this, YouTubeAutomationActivity::class.java))
        }

        btnAppControl.setOnClickListener {
            startActivity(Intent(this, AppControlActivity::class.java))
        }

        btnStopAll.setOnClickListener {
            stopAllAutomation()
        }

        findViewById<Button>(R.id.btnAutoSequence).setOnClickListener {
            submitPresetSequence()
        }
    }

    // ---------------------------
    // Preset Gemini sequence
    // ---------------------------
    private fun submitPresetSequence() {
        if (isRunning) {
            Toast.makeText(this, "Sequence already running", Toast.LENGTH_SHORT).show()
            return
        }
        isRunning = true
        statusText.text = "Status: Running preset sequence..."
        addLog("Gemini", "Starting preset prompt sequence (${presetPrompts.size} prompts)")

        mainScope.launch {
            for ((index, prompt) in presetPrompts.withIndex()) {
                if (!isRunning) break
                statusText.text = "Status: Sending prompt ${index + 1}/${presetPrompts.size}"
                sendPromptToGemini(prompt)
                delay(7000)
            }
            statusText.text = "Status: Sequence complete"
            isRunning = false
            addLog("Gemini", "Preset sequence completed")
        }
    }

    private fun sendPromptToGemini(prompt: String) {
        val encodedPrompt = Uri.encode(prompt)
        val url = "https://gemini.google.com/app?q=$encodedPrompt"
        addLog("Gemini", "Sending: ${prompt.take(50)}...")
        webView.loadUrl(url)
    }

    // ---------------------------
    // Screenshot task
    // ---------------------------
    data class VideoTask(var frames: Int, var delayMs: Long, var completed: Boolean = false)

    private fun takeMultipleScreenshotsToAI(frames: Int = 3, delayMs: Long = 5000) {
        var count = 0
        val runnable = object : Runnable {
            override fun run() {
                if (count < frames && isRunning) {
                    takeAndSendScreenshotToAI(count + 1, frames)
                    count++
                    handler.postDelayed(this, delayMs)
                }
            }
        }
        handler.post(runnable)
    }

    private fun takeAndSendScreenshotToAI(current: Int, total: Int) {
        addLog("Screenshot", "Captured frame $current/$total")
        statusText.text = "Status: Screenshot $current/$total"

        // Use ScreenshotService for actual capture
        val intent = Intent(this, ScreenshotService::class.java).apply {
            putExtra("frame_number", current)
            putExtra("total_frames", total)
        }
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
            addLog("Error", "Screenshot service failed: ${e.message}")
        }
    }

    // ---------------------------
    // Stop all
    // ---------------------------
    private fun stopAllAutomation() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        currentVideoTask = null
        statusText.text = "Status: Stopped"
        addLog("System", "All automation stopped")
        Toast.makeText(this, "All automation stopped", Toast.LENGTH_SHORT).show()
    }

    // ---------------------------
    // Logging
    // ---------------------------
    private fun addLog(tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message
        )
        logs.add(0, entry)
        if (logs.size > 200) logs.removeAt(logs.size - 1)
        logAdapter.submitList(logs.toList())
        logRecyclerView.scrollToPosition(0)
    }

    // ---------------------------
    // Permissions
    // ---------------------------
    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
