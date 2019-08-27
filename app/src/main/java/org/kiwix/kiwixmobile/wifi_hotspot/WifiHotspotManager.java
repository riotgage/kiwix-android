package org.kiwix.kiwixmobile.wifi_hotspot;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * WifiHotstopManager class makes use of the Android's WifiManager and WifiConfiguration class
 * to implement the wifi hotspot feature.
 * Created by Adeel Zafar on 28/5/2019.
 */

public class WifiHotspotManager {
  private final WifiManager wifiManager;
  private WifiManager.LocalOnlyHotspotReservation hotspotReservation;
  private HotspotStateListener hotspotStateListener;
  private static final String TAG = "WifiHotspotManager";

  public WifiHotspotManager(@NonNull Context context) {
    wifiManager =
        (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
  }

  //Workaround to turn on hotspot for Oreo versions
  @RequiresApi(api = Build.VERSION_CODES.O)
  public void turnOnHotspot() {
    wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

      @Override
      public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
        super.onStarted(reservation);
        hotspotReservation = reservation;
        WifiConfiguration currentConfig = hotspotReservation.getWifiConfiguration();

        printCurrentConfig(currentConfig);
        hotspotStateListener.onHotspotTurnedOn(currentConfig);
        Log.v(TAG, "Local Hotspot Started");
      }

      @Override
      public void onStopped() {
        super.onStopped();
        hotspotStateListener.onHotspotStopped();
        Log.v(TAG, "Local Hotspot Stopped");
      }

      @Override
      public void onFailed(int reason) {
        super.onFailed(reason);
        hotspotStateListener.onHotspotFailedToStart();
        Log.v(TAG, "Local Hotspot failed to start");
      }
    }, new Handler());
  }

  //Workaround to turn off hotspot for Oreo versions
  @RequiresApi(api = Build.VERSION_CODES.O)
  public void turnOffHotspot() {
    if (hotspotReservation != null) {
      hotspotReservation.close();
      hotspotReservation = null;
      hotspotStateListener.onHotspotStopped();
      Log.v(TAG, "Turned off hotspot");
    }
  }

  //This method checks the state of the hostpot for devices>=Oreo
  @RequiresApi(api = Build.VERSION_CODES.O)
  public boolean isHotspotStarted() {
    return hotspotReservation != null;
  }

  void printCurrentConfig(WifiConfiguration wifiConfiguration) {
    Log.v(TAG, "THE PASSWORD IS: "
        + wifiConfiguration.preSharedKey
        + " \n SSID is : "
        + wifiConfiguration.SSID);
  }

  public void registerListener(@NonNull HotspotStateListener hotspotStateListener) {
    this.hotspotStateListener = hotspotStateListener;
  }
}
