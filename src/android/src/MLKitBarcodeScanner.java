package com.intelliacc.MLKitBarcodeScanner;

// ----------------------------------------------------------------------------
// |  Android Imports
// ----------------------------------------------------------------------------
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

// ----------------------------------------------------------------------------
// |  Cordova Imports
// ----------------------------------------------------------------------------
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

// ----------------------------------------------------------------------------
// |  Google Imports
// ----------------------------------------------------------------------------
import org.json.JSONArray;
import org.json.JSONException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.mlkit.vision.barcode.BarcodeScanner;

// ----------------------------------------------------------------------------
// |  Java Imports
// ----------------------------------------------------------------------------
import javax.security.auth.callback.Callback;

// ----------------------------------------------------------------------------
// |  Our Imports
// ----------------------------------------------------------------------------

public class MLKitBarcodeScanner extends CordovaPlugin {
  // ----------------------------------------------------------------------------
  // | Public Properties
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // | Protected Properties
  // ----------------------------------------------------------------------------
  protected CallbackContext CallbackContext;

  // ----------------------------------------------------------------------------
  // | Private Properties
  // ----------------------------------------------------------------------------
  private static final int RC_BARCODE_CAPTURE = 9001;

  // ----------------------------------------------------------------------------
  // |  Public Functions
  // ----------------------------------------------------------------------------
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  /**
    * Check the device to make sure it has the Google Play Services APK. If
    * it doesn't, display a dialog that allows users to download the APK from
    * the Google Play Store or enable it in the device's system settings.
    */
  private boolean checkPlayServices() {
      GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
      int resultCode = apiAvailability.isGooglePlayServicesAvailable(this.cordova.getActivity().getApplicationContext());
      if (resultCode != ConnectionResult.SUCCESS) {
          // if (apiAvailability.isUserResolvableError(resultCode)) {
          //   apiAvailability.getErrorDialog(this, resultCode, 9000).show();
          // }
          return false;
      }
      return true;
  }

  @Override
  public boolean execute(String p_Action, JSONArray p_Args, CallbackContext p_CallbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();
    CallbackContext = p_CallbackContext;

    if (p_Action.equals("scanBarcode")) {
      Thread thread = new Thread(new OneShotTask(context, p_Args));
      thread.start();
      return true;
    } else if (p_Action.equals("checkSupport")) {
      boolean hasSupport = this.checkPlayServices();
      CallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasSupport));
      return true;
    }

    return false;
  }

  @Override
  public void onActivityResult(int p_RequestCode, int p_ResultCode, Intent p_Data) {
    super.onActivityResult(p_RequestCode, p_ResultCode, p_Data);

    if (p_RequestCode == RC_BARCODE_CAPTURE) {
      if (p_ResultCode == CommonStatusCodes.SUCCESS) {
        Intent d = new Intent();
        if (p_Data != null) {
          String barcode = p_Data.getStringExtra(MLKitBarcodeCaptureActivity.BarcodeValue);
          CallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, barcode));
          Log.d("MLKitAndroidScanner", "Barcode read: " + barcode);
        }
      } else {
        String err = p_Data.getParcelableExtra("err");
        CallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, err));
      }
    }
  }

  @Override
  public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
    CallbackContext = callbackContext;
  }

  // ----------------------------------------------------------------------------
  // |  Protected Functions
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // |  Private Functions
  // ----------------------------------------------------------------------------
  private void openNewActivity(Context context, JSONArray args) {
    Intent intent = new Intent(context, MLKitSecondaryActivity.class);
    intent.putExtra("DetectionTypes", args.optInt(2, 1234));
    intent.putExtra("ViewFinderWidth", args.optDouble(1, .5));
    intent.putExtra("ViewFinderHeight", args.optDouble(1, .7));
    intent.putExtra("CameraFacing", args.optInt(0, 1));

    this.cordova.setActivityResultCallback(this);
    this.cordova.startActivityForResult(this, intent, RC_BARCODE_CAPTURE);
  }

  // ----------------------------------------------------------------------------
  // |  Helper classes
  // ----------------------------------------------------------------------------
  private class OneShotTask implements Runnable {
    private Context   _Context;
    private JSONArray _Args   ;

    private OneShotTask(Context p_Context, JSONArray p_TaskArgs) {
      _Context = p_Context;
      _Args = p_TaskArgs;
    }

    public void run() {
      openNewActivity(_Context, _Args);
    }
  }
}
