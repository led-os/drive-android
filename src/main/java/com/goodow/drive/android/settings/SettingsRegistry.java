package com.goodow.drive.android.settings;

import com.goodow.drive.android.BusProvider;
import com.goodow.drive.android.toolutils.DeviceInformationTools;
import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.MessageHandler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;

import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class SettingsRegistry {
  private final static String TAG = SettingsRegistry.class.getSimpleName();
  public static final String PREFIX = BusProvider.SID + "settings.";
  private final Bus bus = BusProvider.get();
  private final Context mContext;

  public SettingsRegistry(Context mContext) {
    this.mContext = mContext;
  }

  public void subscribe() {
    bus.registerHandler(PREFIX + "audio", new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject msg = message.body();
        AudioManager mAudioManager =
            (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (msg.has("mute")) {
          mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
        } else if (msg.has("volume")) {
          double volume = msg.getNumber("volume");
          mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (mAudioManager
              .getStreamMaxVolume(AudioManager.STREAM_MUSIC) * volume), AudioManager.FLAG_SHOW_UI);
        } else if (msg.has("range")) {
          double range = msg.getNumber("range");
          mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (mAudioManager
              .getStreamVolume(AudioManager.STREAM_MUSIC) + mAudioManager
              .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
              * range), AudioManager.FLAG_SHOW_UI);
          Log.i(TAG, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + "");
        }
      }
    });
    bus.registerHandler(PREFIX + "location", new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject msg = Json.createObject();
        ConnectivityManager mConnectivityManager =
            (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
          msg.set("NetWorkType", networkInfo.getTypeName());
        } else if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
          msg.set("NetWorkType", networkInfo.getTypeName());
          TelephonyManager mTelephonyManager =
              (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
          // 返回值MCC + MNC
          String operator = mTelephonyManager.getNetworkOperator();
          msg.set("MCC+MNC", operator);
          int MCC = Integer.parseInt(operator.substring(0, 3));
          int MNC = Integer.parseInt(operator.substring(3));
          if (MNC == 0 || MCC == 1) {
            // 中国移动和中国联通获取LAC、CID的方式
            GsmCellLocation gsmLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();
            msg.set("CID", gsmLocation.getCid());
            msg.set("LAC", gsmLocation.getLac());
            msg.set("PSC", gsmLocation.getPsc());
          } else if (MNC == 3) {
            CdmaCellLocation cdmaLocation = (CdmaCellLocation) mTelephonyManager.getCellLocation();
            msg.set("BID", cdmaLocation.getBaseStationId());
            msg.set("NID", cdmaLocation.getNetworkId());
            msg.set("SID", cdmaLocation.getSystemId());
            msg.set("Latitude", cdmaLocation.getBaseStationLatitude());
            msg.set("Longitude", cdmaLocation.getBaseStationLongitude());
          }
        }
        message.reply(msg);
      }
    });
    bus.registerHandler(PREFIX + "information", new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject msg = Json.createObject();
        JsonObject hardwareMsg = Json.createObject();
        JsonObject softwareMsg = Json.createObject();
        // Hardware
        msg.set("hardware", hardwareMsg);
        hardwareMsg.set("MAC", DeviceInformationTools.getLocalMacAddressFromWifiInfo(mContext));
        hardwareMsg.set("IMEI", DeviceInformationTools.getIMEI(mContext));
        hardwareMsg.set("SCREENHEIGH", DeviceInformationTools.getScreenHeight(mContext));
        hardwareMsg.set("SCREENWIDTH", DeviceInformationTools.getScreenWidth(mContext));
        // Software
        msg.set("software", softwareMsg);
        softwareMsg.set("AndroidId", DeviceInformationTools.getAndroidId(mContext));
        softwareMsg.set("IP", DeviceInformationTools.getIp(mContext));
        softwareMsg.set("Model", DeviceInformationTools.getOsModel());
        softwareMsg.set("Version", DeviceInformationTools.getOsVersion());
        softwareMsg.set("SDK", DeviceInformationTools.getSDK());
        message.reply(msg);
      }
    });
  }
}
