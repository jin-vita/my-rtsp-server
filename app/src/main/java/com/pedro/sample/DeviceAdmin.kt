package com.pedro.sample

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class DeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)

        Log.d("DeviceAdmin", "DeviceAdmin Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)

        Log.d("DeviceAdmin", "DeviceAdmin Disabled")
    }

}