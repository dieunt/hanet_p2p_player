package com.connect

import android.app.Activity
import com.connect.module.native_view.CameraMultiFactory
import com.connect.utils.AppConstant
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

class CameraConnectPlugin : FlutterPlugin, ActivityAware {

  private var activity: Activity? = null
  private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    flutterPluginBinding = binding
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    flutterPluginBinding
      .platformViewRegistry
      .registerViewFactory(
        AppConstant.CHANNEL_CAMERA_MULTI,
        CameraMultiFactory(flutterPluginBinding.binaryMessenger, activity!!)
      )
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    // không còn MethodChannel nên không cần xử lý gì
  }
}
