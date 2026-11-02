package com.kycbypass.app

import android.os.Build
import android.os.Bundle

import com.facebook.react.ReactActivity
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.ReactHost
import com.facebook.react.ReactInstanceManager
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

import expo.modules.ReactActivityDelegateWrapper

import com.kycbypass.app.CameraInject.CameraInjectPackage

class MainActivity : ReactActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.AppTheme);
    super.onCreate(null)
  }

  override fun getMainComponentName(): String = "main"

  override fun createReactActivityDelegate(): ReactActivityDelegate {
    return ReactActivityDelegateWrapper(
        this,
        BuildConfig.IS_NEW_ARCHITECTURE_ENABLED,
        object : DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled) {
            override fun createReactNativeHost(host: ReactNativeHost): ReactNativeHost {
                return object : ReactNativeHost(host.application) {
                    override fun getUseDeveloperSupport(): Boolean = host.useDeveloperSupport
                    override fun getPackages(): List<ReactPackage> {
                        val packages = host.packages
                        packages.add(CameraInjectPackage())
                        return packages
                    }
                    override fun getJSMainModuleName(): String = host.jsMainModuleName
                    override fun getJSBundleName(): String = host.jsBundleName
                    override fun getBundleName(): String? = host.bundleName
                    override fun getMinNumMeasures(): Int = host.minNumMeasures
                }
            }
        }
    )
  }

  override fun invokeDefaultOnBackPressed() {
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
          if (!moveTaskToBack(false)) {
              super.invokeDefaultOnBackPressed()
          }
          return
      }
      super.invokeDefaultOnBackPressed()
  }
}
