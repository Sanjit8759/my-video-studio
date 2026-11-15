
package com.myvideostudio.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.ContentValues
import android.net.Uri
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Composition
import androidx.media3.transformer.Transformer
import java.io.File

class MainActivity : Activity() {

    private var player: ExoPlayer? = null
    private var selectedUri: Uri? = null
    private var duration = 0L

    private var startTime = 0L
    private var endTime = 0L

    private lateinit var startSeek: SeekBar
    private lateinit var endSeek: SeekBar
    private lateinit var startText: TextView
    private lateinit var endText: TextView
    private lateinit var status: TextView

    private val bg = Color.rgb(14, 18, 29)
    private val accent = Color.rgb(0, 190, 160)
    private val panel = Color.rgb(29, 37, 54)

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun button(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(panel)
            setOnClickListener { action() }
        }
    }

    private fun label(text: String, size: Float = 16f): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
        }
    }

    private fun showHome() {
        player?.release()
        player = null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(30), dp(18), dp(18))
            setBackgroundColor(bg)
        }

        root.addView(TextView(this).apply {
            text = "MY VIDEO STUDIO"
            textSize = 27f
            setTextColor(accent)
        })

        root.addView(button("＋ NEW PROJECT") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(intent, 100)
        })

        root.addView(button("MY PROJECTS") {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        })

        root.addView(button("AUDIO STUDIO") {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        })

        root.addView(button("SETTINGS") {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        })

        setContentView(root)
    }

    private fun time(ms: Long): String {
        val seconds = ms / 1000
        return String.format(
            "%02d:%02d",
            seconds / 60,
            seconds % 60
        )
    }

    private fun openEditor(uri: Uri) {
        selectedUri = uri
        player?.release()

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(15), dp(14), dp(25))
            setBackgroundColor(bg)
        }

        scroll.addView(root)

        root.addView(label("MY VIDEO STUDIO", 23f))

        root.addView(button("← BACK") {
            showHome()
        })

        val preview = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(240)
            )
            useController = true
            setBackgroundColor(Color.BLACK)
        }

        root.addView(preview)

        root.addView(label("✂ TRIM VIDEO", 22f))

        startText = label("START: 00:00")
        root.addView(startText)

        startSeek = SeekBar(this).apply {
            max = 1000
            progress = 0
        }
        root.addView(startSeek)

        endText = label("END: 00:00")
        root.addView(endText)

        endSeek = SeekBar(this).apply {
            max = 1000
            progress = 1000
        }
        root.addView(endSeek)

        status = label("Select the start and end points.")
        root.addView(status)

        startSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser && duration > 0) {
                        startTime = duration * progress / 1000

                        if (startTime >= endTime) {
                            startTime = (endTime - 500).coerceAtLeast(0)
                        }

                        startText.text =
                            "START: ${time(startTime)}"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        endSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser && duration > 0) {
                        endTime = duration * progress / 1000

                        if (endTime <= startTime) {
                            endTime =
                                (startTime + 500).coerceAtMost(duration)
                        }

                        endText.text =
                            "END: ${time(endTime)}"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        root.addView(button("RESET TRIM") {
            startTime = 0
            endTime = duration

            startSeek.progress = 0
            endSeek.progress = 1000

            startText.text = "START: 00:00"
            endText.text = "END: ${time(duration)}"

            status.text = "Trim reset."
        })

        root.addView(button("✂ TRIM & EXPORT VIDEO") {
            exportVideo()
        })

        root.addView(label("NEXT: CUT • SPLIT • CROP • MUSIC • TEXT"))

        setContentView(scroll)

        player = ExoPlayer.Builder(this).build().also { exo ->

            preview.player = exo

            exo.setMediaItem(MediaItem.fromUri(uri))

            exo.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {

                        duration = exo.duration.coerceAtLeast(0)

                        startTime = 0
                        endTime = duration

                        startSeek.progress = 0
                        endSeek.progress = 1000

                        startText.text = "START: 00:00"
                        endText.text = "END: ${time(duration)}"

                        status.text = "Video ready. Choose trim points."
                    }
                }
            })

            exo.prepare()
            exo.playWhenReady = false
        }
    }

    private fun exportVideo() {

        val uri = selectedUri

        if (uri == null || duration <= 0) {
            status.text = "Video is not ready. Please wait."
            return
        }

        if (endTime - startTime < 500) {
            status.text = "Select at least 0.5 seconds."
            return
        }

        player?.pause()

        status.text = "Exporting video. Please wait..."

        val output = File(
            cacheDir,
            "trimmed_${System.currentTimeMillis()}.mp4"
        )

        val clipping =
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startTime)
                .setEndPositionMs(endTime)
                .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(clipping)
            .build()

        val editedItem =
            EditedMediaItem.Builder(mediaItem).build()

        val transformer =
            Transformer.Builder(this)
                .addListener(object : Transformer.Listener {

                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {
                        runOnUiThread {
                            try {
                                val saved = saveVideo(output)

                                status.text =
                                    if (saved != null) {
                                        "SUCCESS! Saved in Movies/MY VIDEO STUDIO."
                                    } else {
                                        "Export complete, but saving failed."
                                    }

                                Toast.makeText(
                                    this@MainActivity,
                                    "Video Export Complete",
                                    Toast.LENGTH_LONG
                                ).show()

                            } catch (e: Exception) {
                                status.text = "Save error: ${e.message}"
                            }
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        runOnUiThread {
                            status.text =
                                "Export failed: ${exportException.message}"
                        }
                    }
                })
                .build()

        try {
            transformer.start(editedItem, output.absolutePath)
        } catch (e: Exception) {
            status.text = "Export error: ${e.message}"
        }
    }

    private fun saveVideo(source: File): Uri? {

        if (Build.VERSION.SDK_INT >= 29) {

            val values = ContentValues().apply {
                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    "MY_VIDEO_STUDIO_${System.currentTimeMillis()}.mp4"
                )
                put(
                    MediaStore.Video.Media.MIME_TYPE,
                    "video/mp4"
                )
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES +
                        "/MY VIDEO STUDIO"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val destination = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return null

            contentResolver.openOutputStream(destination)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return null

            val update = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }

            contentResolver.update(destination, update, null, null)

            return destination

        } else {

            val folder = getExternalFilesDir(
                Environment.DIRECTORY_MOVIES
            ) ?: return null

            if (!folder.exists()) folder.mkdirs()

            val destination = File(
                folder,
                "MY_VIDEO_STUDIO_${System.currentTimeMillis()}.mp4"
            )

            source.copyTo(destination, overwrite = true)

            return Uri.fromFile(destination)
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
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                openEditor(uri)
            }
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
