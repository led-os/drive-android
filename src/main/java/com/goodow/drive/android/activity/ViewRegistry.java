package com.goodow.drive.android.activity;

import com.goodow.drive.android.BusProvider;
import com.goodow.drive.android.Constant;
import com.goodow.drive.android.toolutils.DeviceInformationTools;
import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.MessageHandler;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

public class ViewRegistry {
  private final Context ctx;
  private final Bus bus;

  public ViewRegistry(Bus bus, Context ctx) {
    this.bus = bus;
    this.ctx = ctx;
  }

  public void subscribe() {
    /**
     * 打开VIEW[主页，收藏，资源库，设置等]
     */
    bus.registerHandler(Constant.ADDR_VIEW, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String redirectto = body.getString(Constant.KEY_REDIRECTTO);
        if ("home".equals(redirectto)) {
          // 去home页面
          Intent intent = new Intent(ctx, HomeActivity.class);
          intent.putExtra("msg", message.body());
          ctx.startActivity(intent);
        }
        if ("favorite".equals(redirectto)) {
          // 去收藏
          Intent intent = new Intent(ctx, FavouriteActivity.class);
          intent.putExtra("msg", body);
          ctx.startActivity(intent);
        }
        if ("repository".equals(redirectto)) {
          // 去资源库
          Intent intent = new Intent(ctx, SourceActivity.class);
          intent.putExtra("msg", body);
          ctx.startActivity(intent);
        }
        if ("settings".equals(redirectto)) {
          Intent intent = new Intent(ctx, SettingActivity.class);
          ctx.startActivity(intent);
        }
        if ("settings.wifi".equals(redirectto)) {
          ctx.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        }
        if ("settings.screenOffset".equals(redirectto)) {
          Toast.makeText(ctx, "设置屏幕偏移", Toast.LENGTH_LONG).show();
        }
        if ("aboutUs".equals(redirectto)) {
          Toast.makeText(ctx, "sid=" + BusProvider.SID, Toast.LENGTH_LONG).show();
        }
      }
    });

    /**
     * 打开活动详情
     */
    bus.registerHandler(Constant.ADDR_ACTIVITY, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject msg = message.body();
        if (!"post".equals(msg.getString("action"))) {
          return;
        }
        Intent intent = new Intent(ctx, BehaveActivity.class);
        intent.putExtra("msg", msg);
        ctx.startActivity(intent);
      }
    });

    /**
     * 打开主题[和谐，托班，示范课，入学准备，智能开发，图画书]
     */
    bus.registerHandler(Constant.ADDR_TOPIC, new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString(Constant.KEY_ACTION);
        if (!"post".equalsIgnoreCase(action)) {
          return;
        }
        JsonArray tags = body.getArray(Constant.KEY_TAGS);
        if (tags == null) {
          Toast.makeText(ctx, "数据不完整，请检查确认后重试", Toast.LENGTH_SHORT).show();
          return;
        }
        int len_tags = tags.length();
        String type = null;
        for (int i = 0; i < len_tags; i++) {
          if (Constant.LABEL_THEMES.contains(tags.getString(i))) {
            type = tags.getString(i);
            break;
          }
        }
        Intent intent = null;
        if (Constant.DATAREGISTRY_TYPE_HARMONY.equals(type)) {
          // 和谐
          intent = new Intent(ctx, HarmonyActivity.class);
        } else if (Constant.DATAREGISTRY_TYPE_SHIP.equals(type)) {
          // 托班
          intent = new Intent(ctx, CareClassesActivity.class);
        } else if (Constant.DATAREGISTRY_TYPE_CASE.equals(type)) {
          // 示范课
          intent = new Intent(ctx, CaseActivity.class);
        } else if (Constant.DATAREGISTRY_TYPE_PREPARE.equals(type)) {
          // 入学准备
          intent = new Intent(ctx, PrepareActivity.class);
        } else if (Constant.DATAREGISTRY_TYPE_EDUCATION.equals(type)) {
          // 智能开发
          intent = new Intent(ctx, SmartActivity.class);
        } else if (Constant.DATAREGISTRY_TYPE_READ.equals(type)) {
          // 图画书
          intent = new Intent(ctx, EbookActivity.class);
        } else {
          return;
        }
        intent.putExtra("msg", body);
        ctx.startActivity(intent);
      }
    });
    bus.registerHandler(Constant.ADDR_PREFIX_VIEW + "status", new MessageHandler<JsonObject>() {
      @Override
      public void handle(Message<JsonObject> message) {
        Intent intent = new Intent(ctx, StatusBarActivity.class);
        ctx.startActivity(intent);
      }
    });
    // 标注
    bus.registerHandler(Constant.ADDR_PREFIX_VIEW + "scrawl", new MessageHandler<JsonObject>() {
      private final WindowManager mWindowManager = (WindowManager) ctx.getApplicationContext()
          .getSystemService(Context.WINDOW_SERVICE);
      private LayoutParams mLayoutParams;
      private DrawView mDrawView;
      private boolean mSwitch = true;

      @Override
      public void handle(Message<JsonObject> message) {
        if (mDrawView == null) {
          mDrawView =
              new DrawView(ctx, DeviceInformationTools.getScreenWidth(ctx), DeviceInformationTools
                  .getScreenHeight(ctx));
          mLayoutParams = new WindowManager.LayoutParams();
          mLayoutParams.gravity = Gravity.LEFT;
          mLayoutParams.format = PixelFormat.RGBA_8888;
          mLayoutParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL;
          // mLayoutParams.width = WindowManager.LayoutParams.FILL_PARENT;
          mLayoutParams.x = 0;
          mLayoutParams.y = 0;
          mLayoutParams.width = DeviceInformationTools.getScreenWidth(ctx) - 80;
          mLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
          mLayoutParams.type = LayoutParams.TYPE_PHONE;
        }
        JsonObject draw = message.body();
        if (draw.has("annotation")) {
          if (draw.getBoolean("annotation")) {
            if (mSwitch) {
              mWindowManager.addView(mDrawView, mLayoutParams);
              mSwitch = false;
            }
          } else {
            if (!mSwitch) {
              mWindowManager.removeView(mDrawView);
              mDrawView = null;
              mSwitch = true;
            }
          }
        } else if (draw.has("clear")) {
          if (!mSwitch) {
            mDrawView.clear();
          }
        }
      }
    });
  }
}
