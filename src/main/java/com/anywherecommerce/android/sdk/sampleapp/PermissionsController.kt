package com.anywherecommerce.android.sdk.sampleapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import java.util.*

object PermissionsController {
    /**
     * Permissions
     */
    @JvmField
    var permissions: Array<String> = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO)
    var grantedPermissions = ArrayList<String>()
    @JvmStatic
    fun verifyAppPermissions(activity: Activity?): Boolean {
        grantedPermissions.clear()
        var hasNecessaryPermissions = true
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && activity != null && permissions != null) {
            for (permission in permissions!!) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    hasNecessaryPermissions = false
                } else {
                    grantedPermissions.add(permission)
                }
            }
        }
        return hasNecessaryPermissions
    }

    fun verifyAppPermission(activity: Activity?, permission: String?): Boolean {
        // Check for device permission
        return if (ActivityCompat.checkSelfPermission(activity!!, permission!!) != PackageManager.PERMISSION_GRANTED) {
            false
        } else true
    }

    fun requestAppPermissions(activity: Activity?, s: String, reqCode: Int) {
        ActivityCompat.requestPermissions(activity!!, arrayOf(s), reqCode)
    }

    @JvmStatic
    fun requestAppPermissions(activity: Activity?, s: Array<String>, reqCode: Int) {
        ActivityCompat.requestPermissions(activity!!, s!!, reqCode)
    }
}