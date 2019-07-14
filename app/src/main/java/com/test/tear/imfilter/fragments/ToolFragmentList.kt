package com.test.tear.imfilter.fragments

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.test.tear.imfilter.R

class ToolFragmentList : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager

    private var toolCallback: (toolPos: Int)->Unit = {}

    private fun setupTabLayout() {

        val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(p0: TabLayout.Tab?) {
                /*Change fragment on pager biased on tab position*/
                p0?.apply { when(position) { 0,1,2,3 -> viewPager.currentItem = position } }
            }
        }

        tabLayout.addOnTabSelectedListener(tabSelectedListener)
    }

    private fun setupViewPager() {

        /* Add tool fragments to the pager*/
        fragmentManager?.also { fm ->
            val adapter = ToolSelectionAdapter(fm)
            /*NOTE: add order matters see ToolConfig object*/
            adapter.addFragment(ToolColorFragment())
            adapter.addFragment(ToolColorPresetFragment())
            adapter.addFragment(ToolEdgeFragment())
            adapter.addFragment(ToolPaintFragment())

            viewPager.adapter = adapter
        }

        /* Change selected tab when scrolled */
        val pageChangeListener = object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {}
            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

            override fun onPageSelected(p0: Int) {
                tabLayout.getTabAt(p0)?.apply { select() }
                toolCallback(p0)
            }
        }

        viewPager.addOnPageChangeListener(pageChangeListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tool_fragment_list, container, false)

        tabLayout = view.findViewById(R.id.tool_tabLayout)
        viewPager = view.findViewById(R.id.tool_container)

        setupTabLayout()
        setupViewPager()

        return view
    }

    fun setOnToolSelected(callback:(toolPos: Int)->Unit) {
        toolCallback = callback
    }
}