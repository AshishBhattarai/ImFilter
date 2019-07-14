package com.test.tear.imfilter.fragments

import android.media.effect.EffectFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.test.tear.imfilter.MainActivity
import com.test.tear.imfilter.R
class ToolColorPresetFragment : Fragment()  {

    private lateinit var greyscaleBtn: Button
    private lateinit var sepiaBtn: Button
    private lateinit var lomoBtn: Button
    private lateinit var negativeBtn: Button
    private lateinit var clearEffectBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tool_color_preset_fragment, container, false)

        greyscaleBtn = view.findViewById(R.id.greyScaleBtn)
        sepiaBtn = view.findViewById(R.id.sepiaBtn)
        lomoBtn = view.findViewById(R.id.lomoBtn)
        negativeBtn = view.findViewById(R.id.negativeBtn)
        clearEffectBtn = view.findViewById(R.id.clearEffectBtn)


        activity?.also {activity ->
            if(activity is MainActivity) {
                greyscaleBtn.setOnClickListener { setEffect(EffectFactory.EFFECT_GRAYSCALE, activity) }
                sepiaBtn.setOnClickListener { setEffect(EffectFactory.EFFECT_SEPIA, activity) }
                lomoBtn.setOnClickListener { setEffect(EffectFactory.EFFECT_LOMOISH, activity) }
                negativeBtn.setOnClickListener { setEffect(EffectFactory.EFFECT_NEGATIVE, activity) }
                clearEffectBtn.setOnClickListener { setEffect("", activity) }
            }
        }

        return view
    }

    private fun setEffect(effectName: String, activity: MainActivity) {
        activity.renderDataManager.loadPreset(effectName)
    }


}