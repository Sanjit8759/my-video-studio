
package com.myvideostudio.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {

    private var player: ExoPlayer? = null
    private val bg = Color.rgb(14, 18, 29)
    private val accent = Color.rgb(0, 190, 160)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(29, 37, 54))
            setOnClickListener { action() }
        }
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(30), dp(18), dp(18))
            setBackgroundColor(bg)
        }

        val title = TextView(this).apply {
            text = "MY VIDEO STUDIO"
            textSize = 26f
            setTextColor(accent)
        }

        root.addView(title)
        root.addView(makeButton("＋ NEW PROJECT") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, 100)
        })

        root.addView(makeButton("MY PROJECTS") {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        })

        root.addView(makeButton("AUDIO STUDIO") {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        })

        root.addView(makeButton("SETTINGS") {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        })

        setContentView(root)
    }

    private fun openEditor(uri: Uri) {
        player?.release()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(18), dp(12), dp(12))
            setBackgroundColor(bg)
        }

        val heading = TextView(this).apply {
            text = "VIDEO EDITOR"
            textSize = 22f
            setTextColor(accent)
            gravity = Gravity.CENTER
        }
        root.addView(heading)

        root.addView(makeButton("← BACK") {
            player?.release()
            player = null
            showHome()
        })

        val preview = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260)
            )
            useController = true
            setBackgroundColor(Color.BLACK)
        }

        root.addView(preview)

        val timeline = TextView(this).apply {
            text = "VIDEO TIMELINE"
            textSize = 18f
            setTextColor(accent)
            setPadding(0, dp(18), 0, dp(10))
        }
        root.addView(timeline)

        root.addView(TextView(this).apply {
            text = "Selected video clip"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(15), dp(12), dp(15))
            setBackgroundColor(Color.rgb(29, 37, 54))
        })

        root.addView(makeButton("✂ TRIM") {
            Toast.makeText(this, "Trim coming next", Toast.LENGTH_SHORT).show()
        })

        root.addView(makeButton("SPLIT") {
            Toast.makeText(this, "Split coming next", Toast.LENGTH_SHORT).show()
        })

        root.addView(makeButton("＋ ADD LAYER") {
            Toast.makeText(this, "Layers coming next", Toast.LENGTH_SHORT).show()
        })

        setContentView(root)

        player = ExoPlayer.Builder(this).build().also {
            preview.player = it
            it.setMediaItem(MediaItem.fromUri(uri))
            it.prepare()
            it.playWhenReady = false
        }
    }

    @Deprecated("Legacy picker callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                openEditor(uri)
            } else {
                Toast.makeText(this, "Video not selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
