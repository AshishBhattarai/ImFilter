package com.test.tear.imfilter.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import com.test.tear.imfilter.MainActivity
import com.test.tear.imfilter.R
import com.test.tear.imfilter.Renderer.EffectMode
import kotlinx.android.synthetic.main.content_main.*

class ToolEdgeFragment : Fragment()  {

    private val seekBarMax = 50.0f

    private lateinit var thresholdSeek: SeekBar
    private lateinit var applyBtn: Button
    private lateinit var clearBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
       val view = inflater.inflate(R.layout.tool_edge_fragment, container, false)

        thresholdSeek = view.findViewById(R.id.thresholdSeekBar)
        applyBtn = view.findViewById(R.id.applyTBtn)
        clearBtn = view.findViewById(R.id.clearTBtn)

        activity?.also { activity ->
            if(activity is MainActivity) {
                applyBtn.setOnClickListener {
                    activity.renderDataManager.loadThreshold(thresholdSeek.progress / seekBarMax)
                }

                clearBtn.setOnClickListener {
                    activity.renderDataManager.removeMode(EffectMode.EDGE)
                }
            }
        }

        return view
    }
}