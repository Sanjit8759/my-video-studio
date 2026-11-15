
package com.myvideostudio.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val bg = Color.rgb(14, 18, 29)
    private val panel = Color.rgb(29, 37, 54)
    private val accent = Color.rgb(0, 190, 160)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 30, 22, 20)
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun text(value: String, size: Float, color: Int,
                 bold: Boolean = false): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        fun button(title: String, subtitle: String,
                   action: () -> Unit): LinearLayout {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 17, 18, 17)
                background = GradientDrawable().apply {
                    setColor(panel)
                    cornerRadius = 20f
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }

            card.addView(text(title, 19f, Color.WHITE, true))
            card.addView(text(subtitle, 13f, Color.LTGRAY))
            return card
        }

        fun space(): View {
            return View(this).apply {
                layoutParams = LinearLayout.LayoutParams(1, 24)
            }
        }

        content.addView(
            text("MY VIDEO STUDIO", 27f, accent, true)
        )
        content.addView(
            text("Create videos your way", 15f, Color.LTGRAY)
        )

        content.addView(space())

        content.addView(button(
            "＋ NEW PROJECT",
            "Start editing a new video"
        ) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "video/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, 100)
        })

        content.addView(space())

        content.addView(button(
            "▣ MY PROJECTS",
            "Your saved editing projects"
        ) {
            Toast.makeText(this, "Coming next",
                Toast.LENGTH_SHORT).show()
        })

        content.addView(space())

        content.addView(button(
            "♫ AUDIO STUDIO",
            "Music and voice recording"
        ) {
            Toast.makeText(this, "Coming next",
                Toast.LENGTH_SHORT).show()
        })

        content.addView(space())

        content.addView(button(
            "⚙ SETTINGS",
            "Video quality and preferences"
        ) {
            Toast.makeText(this, "Coming next",
                Toast.LENGTH_SHORT).show()
        })

        content.addView(space())

        content.addView(
            text("EDITING FEATURES", 17f, accent, true)
        )

        content.addView(text(
            "Video • Image • Audio • Text\n" +
            "Layers • Chroma Key • Effects\n" +
            "Export up to 1080p",
            14f, Color.LTGRAY
        ))

        content.addView(space())

        content.addView(text(
            "MY VIDEO STUDIO • Version 1.0",
            12f, Color.GRAY
        ))

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    @Deprecated("Legacy picker callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            Toast.makeText(this, "Video selected",
                Toast.LENGTH_SHORT).show()
        }
    }
}
