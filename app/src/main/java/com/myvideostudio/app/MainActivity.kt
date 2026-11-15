
package com.myvideostudio.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Composition
import androidx.media3.transformer.Transformer
import java.io.File

@OptIn(UnstableApi::class)
class MainActivity : Activity() {

    data class Clip(val start: Long, val end: Long)

    private var player: ExoPlayer? = null
    private var selectedUri: Uri? = null
    private var duration = 0L

    private val clips = mutableListOf<Clip>()
    private var selectedClip = -1

    private lateinit var timeline: SeekBar
    private lateinit var positionText: TextView
    private lateinit var clipList: LinearLayout
    private lateinit var status: TextView
    private lateinit var deleteButton: Button

    private val handler = Handler(Looper.getMainLooper())

    private val bg = Color.rgb(14, 18, 29)
    private val accent = Color.rgb(0, 190, 160)
    private val panel = Color.rgb(29, 37, 54)

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun time(ms: Long): String {
        val s = ms.coerceAtLeast(0) / 1000
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    private fun label(t: String, size: Float = 17f) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
        }

    private fun button(t: String, action: () -> Unit) =
        Button(this).apply {
            text = t
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(panel)
            setOnClickListener { action() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        player?.release()
        player = null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(30), dp(18), dp(18))
            setBackgroundColor(bg)
        }

        root.addView(label("MY VIDEO STUDIO", 27f))

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

    private val updateTimeline = object : Runnable {
        override fun run() {
            val p = player
            if (p != null && duration > 0) {
                val pos = p.currentPosition.coerceIn(0, duration)
                timeline.progress = (pos * 10000 / duration).toInt()
                positionText.text =
                    "POSITION: ${time(pos)} / ${time(duration)}"
            }
            handler.postDelayed(this, 300)
        }
    }

    private fun openEditor(uri: Uri) {
        selectedUri = uri
        clips.clear()
        selectedClip = -1

        player?.release()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(15), dp(14), dp(25))
            setBackgroundColor(bg)
        }

        scroll.addView(root)

        root.addView(label("MY VIDEO STUDIO", 24f))
        root.addView(button("← BACK") { showHome() })

        val preview = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                -1, dp(250)
            )
            useController = true
            setBackgroundColor(Color.BLACK)
        }

        root.addView(preview)

        root.addView(label("VIDEO TIMELINE", 21f))

        positionText = label("POSITION: 00:00 / 00:00")
        root.addView(positionText)

        timeline = SeekBar(this).apply {
            max = 10000
            progress = 0
        }

        root.addView(timeline)

        timeline.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser && duration > 0) {
                        val pos = duration * progress / 10000
                        positionText.text =
                            "POSITION: ${time(pos)} / ${time(duration)}"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (duration > 0) {
                        player?.seekTo(
                            duration * (seekBar?.progress ?: 0) / 10000
                        )
                    }
                }
            }
        )

        root.addView(label("✂ CUT / SPLIT VIDEO", 21f))

        root.addView(label(
            "1. Play the video\n" +
            "2. Pause where you want to cut\n" +
            "3. Press SPLIT\n" +
            "4. Select unwanted clip and DELETE"
        ))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        actions.addView(
            button("✂ SPLIT") { splitClip() },
            LinearLayout.LayoutParams(0, dp(60), 1f)
        )

        deleteButton = button("DELETE CLIP") {
            deleteClip()
        }

        actions.addView(
            deleteButton,
            LinearLayout.LayoutParams(0, dp(60), 1f)
        )

        root.addView(actions)

        root.addView(label("YOUR VIDEO CLIPS", 20f))

        clipList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(clipList)

        status = label("Loading video...")
        root.addView(status)

        root.addView(button("RESET ALL CUTS") {
            if (duration > 0) {
                clips.clear()
                clips.add(Clip(0, duration))
                selectedClip = 0
                refreshClips()
                status.text = "All cuts reset."
            }
        })

        root.addView(button("EXPORT FINAL VIDEO") {
            exportVideo()
        })

        setContentView(scroll)

        player = ExoPlayer.Builder(this).build().also { exo ->

            preview.player = exo
            exo.setMediaItem(MediaItem.fromUri(uri))

            exo.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        duration = exo.duration.coerceAtLeast(0)

                        clips.clear()

                        if (duration > 0) {
                            clips.add(Clip(0, duration))
                            selectedClip = 0
                        }

                        refreshClips()
                        status.text = "Video ready. Play and pause to split."
                    }
                }
            })

            exo.prepare()
            exo.playWhenReady = false
        }

        handler.removeCallbacks(updateTimeline)
        handler.post(updateTimeline)
    }

    private fun splitClip() {
        val pos = player?.currentPosition ?: 0L

        val index = clips.indexOfFirst {
            pos > it.start + 100 &&
            pos < it.end - 100
        }

        if (index < 0) {
            status.text = "Pause inside a clip before splitting."
            return
        }

        val old = clips[index]

        clips[index] = Clip(old.start, pos)
        clips.add(index + 1, Clip(pos, old.end))

        selectedClip = index + 1

        refreshClips()
        status.text = "Split successful! Two clips created."
    }

    private fun deleteClip() {
        if (selectedClip !in clips.indices) {
            status.text = "Select a clip first."
            return
        }

        if (clips.size <= 1) {
            status.text = "At least one clip must remain."
            return
        }

        clips.removeAt(selectedClip)

        selectedClip = selectedClip.coerceAtMost(clips.lastIndex)

        refreshClips()
        status.text = "Selected clip deleted."
    }

    private fun refreshClips() {
        clipList.removeAllViews()

        clips.forEachIndexed { index, clip ->

            val item = Button(this).apply {
                text =
                    "${if (selectedClip == index) "▶ " else ""}" +
                    "CLIP ${index + 1}   " +
                    "${time(clip.start)} - ${time(clip.end)}"

                setTextColor(Color.WHITE)

                setBackgroundColor(
                    if (selectedClip == index)
                        Color.rgb(0, 115, 100)
                    else panel
                )

                setOnClickListener {
                    selectedClip = index

                    player?.seekTo(clip.start)

                    if (duration > 0) {
                        timeline.progress =
                            (clip.start * 10000 / duration).toInt()
                    }

                    refreshClips()

                    status.text = "Clip ${index + 1} selected."
                }
            }

            clipList.addView(item)
        }

        deleteButton.isEnabled =
            clips.size > 1 && selectedClip in clips.indices
    }

    private fun exportVideo() {
        val uri = selectedUri

        if (uri == null || clips.isEmpty()) {
            status.text = "No clips available."
            return
        }

        player?.pause()

        status.text = "Exporting final video..."

        val output = File(
            cacheDir,
            "final_${System.currentTimeMillis()}.mp4"
        )

        val items = clips.map { clip ->

            val clipping =
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.start)
                    .setEndPositionMs(clip.end)
                    .build()

            val media = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(clipping)
                .build()

            EditedMediaItem.Builder(media).build()
        }

        try {
            val sequence =
                EditedMediaItemSequence.withAudioAndVideoFrom(items)

            val composition =
                Composition.Builder(sequence).build()

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
                                        if (saved != null)
                                            "SUCCESS! Video saved in Movies/MY VIDEO STUDIO."
                                        else
                                            "Export completed but save failed."

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

            transformer.start(composition, output.absolutePath)

        } catch (e: Exception) {
            status.text = "Export error: ${e.message}"
        }
    }

    private fun saveVideo(source: File): Uri? {
        if (!source.exists()) return null

        if (Build.VERSION.SDK_INT >= 29) {

            val values = ContentValues().apply {
                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    "MY_VIDEO_STUDIO_${System.currentTimeMillis()}.mp4"
                )
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
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
        handler.removeCallbacks(updateTimeline)
        player?.release()
        player = null
        super.onDestroy()
    }
}
