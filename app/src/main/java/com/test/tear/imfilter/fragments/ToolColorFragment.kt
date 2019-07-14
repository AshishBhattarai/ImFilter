package com.test.tear.imfilter.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsSeekBar
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.test.tear.imfilter.MainActivity
import com.test.tear.imfilter.R
import com.test.tear.imfilter.Renderer.EffectMode
import com.test.tear.imfilter.Renderer.ImSurfaceView
import kotlinx.android.synthetic.main.content_main.*

class ToolColorFragment : Fragment() {

    private val seekBarMax = 100.0f

    private lateinit var applyBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var redSeekBar: SeekBar
    private lateinit var greenSeekBar: SeekBar
    private lateinit var blueSeekBar: SeekBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tool_color_fragment, container, false)

        applyBtn = view.findViewById(R.id.colorBtn)
        clearBtn = view.findViewById(R.id.clearColorBtn)
        redSeekBar = view.findViewById(R.id.redSeekBar)
        greenSeekBar = view.findViewById(R.id.greenSeekBar)
        blueSeekBar = view.findViewById(R.id.blueSeekBar)

        // TODO : Use render manager to pass color data

        activity?.also { activity ->
            if(activity is MainActivity) {
                applyBtn.setOnClickListener { applyPressed(activity) }
                clearBtn.setOnClickListener { activity.renderDataManager.removeMode(EffectMode.COLOR) }
            }
        }

        return view
    }

    private fun applyPressed(activity: MainActivity) {
        // load color values to the shader
        val r = redSeekBar.progress/seekBarMax
        val g = greenSeekBar.progress/seekBarMax
        val b = blueSeekBar.progress/seekBarMax

        activity.renderDataManager.loadColor(r, g ,b)
    }

}