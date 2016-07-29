package com.mobile.android.scrollshot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ScrollView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class ScreenShotReceiver extends BroadcastReceiver {

  private final String SCREENSHOT_RECEIVER_ACTION = "com.mobile.android.scrollshot";
  private final String SCREENSHOT_PATH = Environment.getExternalStorageDirectory().toString()
      + "/screenshots/";
  private final String SCENE_NAME_BUNDLE_KEY = "scene_name";

  private static WeakReference<Activity> currentActivityReference;
  private String sceneName = "_screenshot";

  public ScreenShotReceiver(){
  }

  @SuppressWarnings("unused")
  public synchronized static void setCurrentActivityReference(WeakReference<Activity> activity) {
    currentActivityReference = activity;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (!TextUtils.isEmpty(intent.getStringExtra("scene_name"))) {
      sceneName = intent.getStringExtra(SCENE_NAME_BUNDLE_KEY);
    }
    Activity activity = currentActivityReference.get();
    if (intent.getAction().equals(SCREENSHOT_RECEIVER_ACTION) && activity != null) {
      ViewGroup rootGroup = (ViewGroup) activity.findViewById(android.R.id.content);

      try {
        WebView webView;
        ScrollView scrollView;

        if ((scrollView = findViewByType(rootGroup, ScrollView.class)) != null) {
          takeScrollShot(scrollView);
        } else if ((webView = findViewByType(rootGroup, WebView.class)) != null) {
          takeScrollShot(webView);
        } else {
          takeScrollShot(rootGroup.getChildAt(0));
        }
      } catch (Exception e) {
        fallBackToNormalScreenShot(rootGroup.getChildAt(0));
      }

    }
  }

  @Nullable
  private <T> T findViewByType(ViewGroup content, Class<T> viewType) {
    if (viewType.isAssignableFrom(content.getClass())) {
      return (T) content;
    } else {
      for (int i = 0; i < content.getChildCount(); i++) {
        if (content.getChildAt(i) != null &&
            viewType.isAssignableFrom(content.getChildAt(i).getClass())) {
          return (T) content.getChildAt(i);
        } else if (content.getChildAt(i) instanceof ViewGroup) {
          return findViewByType((ViewGroup) content.getChildAt(i), viewType);
        }
      }
    }
    return null;
  }

  private void fallBackToNormalScreenShot(View rootView) {
    takeScreenShot(rootView, false);
  }

  private void takeScrollShot(View view) {
    takeScreenShot(view, true);
  }

  private void takeScreenShot(final View view, boolean isScrollShot) {
    if (isScrollShot) {
      view.measure(View.MeasureSpec.makeMeasureSpec(view.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
          View.MeasureSpec.UNSPECIFIED);
    }
    int height = view.getMeasuredHeight();
    int width = view.getMeasuredWidth();

    Bitmap viewScene = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas sceneCanvas = new Canvas(viewScene);

    Drawable sceneBackground = view.getBackground();
    if (sceneBackground != null) {
      sceneBackground.setBounds(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
      sceneBackground.draw(sceneCanvas);
    }

    view.draw(sceneCanvas);
    writeSceneDataToFile(viewScene);
  }

  private void writeSceneDataToFile(Bitmap viewScene) {
    File imageFile = new File(SCREENSHOT_PATH);
    imageFile.mkdirs();
    imageFile = new File(imageFile + "/" + sceneName + ".png");

    try {
      ByteArrayOutputStream screenShotOutputStream = new ByteArrayOutputStream();
      viewScene.compress(Bitmap.CompressFormat.PNG, 90, screenShotOutputStream);
      byte[] sceneData = screenShotOutputStream.toByteArray();

      FileOutputStream fos = new FileOutputStream(imageFile);
      fos.write(sceneData);
      fos.flush();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
