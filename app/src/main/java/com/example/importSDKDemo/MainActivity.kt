package com.example.importSDKDemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    public val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
    private var mProduct: BaseProduct? = null
    private lateinit var mHandler: Handler

    private val REQUIRED_PERMISSION_LIST = arrayOf(
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE
    )

    private val missingPermission = ArrayList<String>()
    private val isRegistrationInProgress = AtomicBoolean(false)
    private val REQUEST_PERMISSION_CODE = 12345

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions()
        }

        setContentView(R.layout.activity_main)

        //Initialize DJI SDK Manager
        mHandler = Handler(Looper.getMainLooper())
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private fun checkAndRequestPermissions() {
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!")
            ActivityCompat.requestPermissions(
                this,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }

    }

    /**
     * Result of runtime permission request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else {
            showToast("Missing permissions!!!")
        }
    }

    // Add the showToast function
    private fun showToast(msg: String) {
        // Implement your toast logic here
    }

    private fun startSDKRegistration() {
        // Implement your SDK registration logic here
    }
}

private fun startSDKRegistration() {
    if (isRegistrationInProgress.compareAndSet(false, true)) {
        // Use a coroutine instead of AsyncTask
        CoroutineScope(Dispatchers.IO).launch {
            showToast("registering, pls wait...")

            // Use Kotlin's lambda syntax for callbacks
            DJISDKManager.getInstance().registerApp(
                applicationContext,
                object : DJISDKManager.SDKManagerCallback {
                    override fun onRegister(djiError: DJIError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            showToast("Register Success")
                            DJISDKManager.getInstance().startConnectionToProduct()
                        } else {
                            showToast("Register sdk fails, please check the bundle id and network connection!")
                        }
                        Log.v(TAG, djiError.description)
                    }

                    override fun onProductDisconnect() {
                        Log.d(TAG, "onProductDisconnect")
                        showToast("Product Disconnected")
                        notifyStatusChange()
                    }

                    override fun onProductConnect(baseProduct: BaseProduct) {
                        Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct))
                        showToast("Product Connected")
                        notifyStatusChange()
                    }

                    override fun onComponentChange(
                        componentKey: BaseProduct.ComponentKey,
                        oldComponent: BaseComponent?,
                        newComponent: BaseComponent?
                    ) {
                        newComponent?.let {
                            it.setComponentListener(object : BaseComponent.ComponentListener {
                                override fun onConnectivityChange(isConnected: Boolean) {
                                    Log.d(TAG, "onComponentConnectivityChanged: $isConnected")
                                    notifyStatusChange()
                                }
                            })
                        }
                        Log.d(
                            TAG,
                            String.format(
                                "onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                componentKey,
                                oldComponent,
                                newComponent
                            )
                        )
                    }

                    override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}

                    override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
                }
            )
        }
    }
}
private fun notifyStatusChange() {
    mHandler.removeCallbacks(updateRunnable)
    mHandler.postDelayed(updateRunnable, 500)
}

private val updateRunnable = Runnable {
    val intent = Intent(FLAG_CONNECTION_CHANGE)
    sendBroadcast(intent)
}

private fun showToast(toastMsg: String) {
    // Use a coroutine to simplify the toast logic
    CoroutineScope(Dispatchers.Main).launch {
        Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show()
    }
}

