package com.test.tear.imfilter

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import com.test.tear.imfilter.Renderer.EffectMode
import com.test.tear.imfilter.fragments.ToolFragmentList
import com.test.tear.imfilter.fragments.ToolPos
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

object ActivityCode {
    const val STORAGE_PERMISSION    =     0
    const val PICK_IMAGE            =     1
    const val CAMERA_PERMISSION     =     2
}

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val TAG = "MainActivity"

    private lateinit var toolFragmentList: ToolFragmentList
    lateinit var renderDataManager: RenderDataManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nav_view.setNavigationItemSelectedListener(this)

        toolFragmentList = (toolFragment as ToolFragmentList)
        renderDataManager = RenderDataManager(this)

        /* Called this before .setOnToolSelected */
        renderDataManager.onRestoreInstanceState(savedInstanceState)

        /* touch effect on buttons
         *
         * TODO: Do this on xml
         *
         * */
        val onTouchListener = { v:View, e:MotionEvent ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    /* updated version resources.getColor(id, theme?) - requires api lvl 23+*/
                    v.background.setColorFilter(resources.getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP)
                    v.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    v.background.clearColorFilter()
                    v.invalidate()
                }
            }
            false
        }

        setupToolBtn(onTouchListener)
        setUpNavBtn(onTouchListener)
        saveBtn.setOnTouchListener(onTouchListener)

        Log.d(TAG, "MainActivity created.")

        /* check permission */
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            /* No Permission so request */
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), ActivityCode.STORAGE_PERMISSION)
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), ActivityCode.CAMERA_PERMISSION)
        }
    }

    override fun onResume() {
        renderDataManager.onResume()
        imSurfaceView.onResume()
        super.onResume()
    }

    override fun onPause() {
        imSurfaceView.onPause()
        super.onPause()
    }

    /* Confirm Exit */
    override fun onBackPressed() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.app_name)
            setIcon(R.mipmap.ic_launcher)
            setMessage("Unsaved changes will be lost on exit.")
            setCancelable(false)
            setPositiveButton("Yes") { _: DialogInterface, _: Int -> finish() }
            setNegativeButton("No") {dialog : DialogInterface, _: Int -> dialog.cancel() }
            create()?.apply { show() }
        }
    }

    // called when activity is saved
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        renderDataManager.onSaveInstanceState(outState)
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when(p0.itemId) {
            R.id.nav_camera -> {
                renderDataManager.enableCamera(true)
            }
            R.id.nav_gallery -> {
                renderDataManager.enableCamera(false)
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), ActivityCode.PICK_IMAGE)
            }
            R.id.nav_manage -> {
                renderDataManager.enableCamera(false)
            }
            R.id.nav_share -> {

            }
            R.id.nav_exit -> {
               onBackPressed()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setUpNavBtn(onTouchListener:(View, MotionEvent)->Boolean) {
        /* navBtn events */
        navBtn.setOnTouchListener(onTouchListener)

        navBtn.setOnClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupToolBtn(onTouchListener: (View, MotionEvent) -> Boolean) {
        /* tooBtn event */
        toolBtn.setOnTouchListener(onTouchListener)

        toolBtn.setOnClickListener {
            if(toolFragmentList.isHidden) {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_up_anim, R.anim.exit_slide_up_anim)
                    .show(toolFragmentList).commit()
                val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_left_anim)
                toolBtn.startAnimation(animation)
            }
            else {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_down_anim, R.anim.exit_slide_down_anim)
                    .hide(toolFragmentList).commit()
                val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_anim)
                toolBtn.startAnimation(animation)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == ActivityCode.PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri -> renderDataManager.loadImage(uri) }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // if permission denied exit
        if(requestCode == ActivityCode.STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            finish()
        }
        if(requestCode == ActivityCode.CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            finish()
        }

        // TODO: Warning message
    }
}
