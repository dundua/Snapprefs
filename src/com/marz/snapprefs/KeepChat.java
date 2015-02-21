/*
 * Copyright (C) 2014  Sturmen, stammler, Ramis and P1nGu1n
 *
 * This file is part of Keepchat.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.marz.snapprefs;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.removeAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

public class KeepChat implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static final String SNAPCHAT_PACKAGE_NAME = "com.snapchat.android";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
    private static XModuleResources mResources;
    private XSharedPreferences sharedPreferences;

    private GestureModel gestureModel;
    private int screenHeight;

    // Modes for saving Snapchats
    public static final int SAVE_AUTO = 0;
    public static final int SAVE_S2S = 1;
    public static final int DO_NOT_SAVE = 2;
    // Length of toasts
    public static final int TOAST_LENGTH_SHORT = 0;
    public static final int TOAST_LENGTH_LONG = 1;
    // Minimum timer duration disabled
    public static final int TIMER_MINIMUM_DISABLED = 0;

    // Preferences and their default values
    public int mModeSnapImage = SAVE_AUTO;
    public int mModeSnapVideo = SAVE_AUTO;
    public int mModeStoryImage = SAVE_AUTO;
    public int mModeStoryVideo = SAVE_AUTO;
    public int mTimerMinimum = TIMER_MINIMUM_DISABLED;
    public boolean mToastEnabled = true;
    public int mToastLength = TOAST_LENGTH_LONG;
    public String mSavePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Snapprefs";
    public boolean mSaveSentSnaps = false;
    public boolean mSortByCategory = true;
    public boolean mSortByUsername = true;
    public boolean mDebugging = true;


    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(SNAPCHAT_PACKAGE_NAME))
            return;

        refreshPreferences();

        try {
            Logger.log("----------------------- KEEPCHAT LOADING ------------------------", false);
            Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
            Context context = (Context) callMethod(activityThread, "getSystemContext");

            PackageInfo piSnapChat = context.getPackageManager().getPackageInfo(lpparam.packageName, 0);
            Logger.log("Snapchat Version: " + piSnapChat.versionName + " (" + piSnapChat.versionCode + ")");
            Logger.log("Module Version: " + Common.VERSION_NAME + " (" + Common.VERSION_CODE + ")");

            if (!Obfuscator.isSupported(piSnapChat.versionCode)) {
                Logger.log("This snapchat version is unsupported, now quiting", true, true);
            }

            // Get screen height for S2S
            screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        } catch (Exception e) {
            Logger.log("Exception while trying to get version info", e);
            return;
        }

        try {
            final Class<?> storySnapClass = findClass(Obfuscator.STORYSNAP_CLASS, lpparam.classLoader);

            /**
             * Method used to get the bitmap of a snap. We retrieve this return value and store it in a field for future saving.
             */
            findAndHookMethod(Obfuscator.RECEIVEDSNAP_CLASS, lpparam.classLoader, Obfuscator.RECEIVEDSNAP_GETIMAGEBITMAP, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Return if this snap is a video, we only use this for saving images
                    boolean isVideo = (Boolean) callMethod(param.thisObject, Obfuscator.SNAP_ISVIDEO);
                    if (isVideo) return;

                    setAdditionalInstanceField(param.thisObject, "snap_bitmap", param.getResult());
                    setAdditionalInstanceField(param.thisObject, "snap_media_type", MediaType.IMAGE);
                    setAdditionalInstanceField(param.thisObject, "snap_type", SnapType.SNAP);
                }
            });

            /**
             * Method used to get the bitmap of a story. We retrieve this return value and store it in a field for future saving.
             */
            findAndHookMethod(Obfuscator.STORYSNAP_CLASS, lpparam.classLoader, Obfuscator.STORYSNAP_GETIMAGEBITMAP, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Return if this snap is a video, we only use this for saving images
                    boolean isVideo = (Boolean) callMethod(param.thisObject, Obfuscator.SNAP_ISVIDEO);
                    if (isVideo) return;

                    setAdditionalInstanceField(param.thisObject, "snap_bitmap", param.getResult());
                    setAdditionalInstanceField(param.thisObject, "snap_media_type", MediaType.IMAGE);
                    setAdditionalInstanceField(param.thisObject, "snap_type", SnapType.STORY);
                }
            });

            /**
             * Method used to load resources for a video. We get this object from the parameters, get relevant info and store it in fields for future saving.
             */
            final Class<?> videoSnapResourcesClass = findClass(Obfuscator.VIDEOSNAPRESOURCES_CLASS, lpparam.classLoader);
            findAndHookMethod(Obfuscator.VIDEOSNAPRENDERER_CLASS, lpparam.classLoader, Obfuscator.VIDEOSNAPRENDERER_LOADRES, videoSnapResourcesClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object receivedSnap = getObjectField(param.thisObject, Obfuscator.VIDEOSNAPRENDERER_RECEIVEDSNAP);
                    // Check if the video is a snap or a story
                    SnapType snapType = (storySnapClass.isInstance(receivedSnap) ? SnapType.STORY : SnapType.SNAP);

                    Object videoSnapResources = param.args[0];
                    setAdditionalInstanceField(receivedSnap, "snap_bitmap", callMethod(videoSnapResources, Obfuscator.VIDEOSNAPRESOURCES_GETBITMAP));
                    setAdditionalInstanceField(receivedSnap, "snap_video_uri", callMethod(videoSnapResources, Obfuscator.VIDEOSNAPRESOURCES_GETVIDEOURI));
                    setAdditionalInstanceField(receivedSnap, "snap_media_type", MediaType.VIDEO);
                    setAdditionalInstanceField(receivedSnap, "snap_type", snapType);
                }
            });

            /**
             * The ImageSnapRenderer class renders images, this method is called to start the viewing of a image.
             * We get the Context, ReceivedSnap instance and previously stored Bitmap to save the image.
             */
            findAndHookMethod(Obfuscator.IMAGESNAPRENDERER_CLASS, lpparam.classLoader, Obfuscator.IMAGESNAPRENDERER_START, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    refreshPreferences();
                    Object receivedSnap = getObjectField(param.thisObject, Obfuscator.IMAGESNAPRENDERER_RECEIVEDSNAP);
                    SnapType snapType = (SnapType) getAdditionalInstanceField(receivedSnap, "snap_type");

                    Logger.log("----------------------- KEEPCHAT ------------------------", false);
                    Logger.log("Image " + snapType.name + " opened");

                    int saveMode = (snapType == SnapType.SNAP ? mModeSnapImage : mModeStoryImage);
                    if (saveMode == DO_NOT_SAVE) {
                        Logger.log("Mode: don't save");
                    } else if (saveMode == SAVE_S2S) {
                        Logger.log("Mode: sweep to save");
                        gestureModel = new GestureModel(receivedSnap, screenHeight);
                    } else {
                        Logger.log("Mode: auto save");
                        gestureModel = null;

                        Object imageView = getObjectField(param.thisObject, Obfuscator.IMAGESNAPRENDERER_IMAGEVIEW);
                        Context context = (Context) callMethod(imageView, "getContext");

                        saveReceivedSnap(context, receivedSnap);
                    }
                }
            });

            /**
             * The VideoSnapRenderer class renders videos, this method is called to start the viewing of a video.
             * We get the Context, ReceivedSnap instance and previously stored video Uri and overlay to save the video.
             */
            findAndHookMethod(Obfuscator.VIDEOSNAPRENDERER_CLASS, lpparam.classLoader, Obfuscator.VIDEOSNAPRENDERER_START, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    refreshPreferences();
                    Object receivedSnap = getObjectField(param.thisObject, Obfuscator.VIDEOSNAPRENDERER_RECEIVEDSNAP);
                    SnapType snapType = (SnapType) getAdditionalInstanceField(receivedSnap, "snap_type");

                    Logger.log("----------------------- KEEPCHAT ------------------------", false);
                    Logger.log("Video " + snapType.name + " opened");

                    int saveMode = (snapType == SnapType.SNAP ? mModeSnapVideo : mModeStoryVideo);
                    if (saveMode == DO_NOT_SAVE) {
                        Logger.log("Mode: don't save");
                    } else if (saveMode == SAVE_S2S) {
                        Logger.log("Mode: sweep2save");
                        gestureModel = new GestureModel(receivedSnap, screenHeight);
                    } else {
                        Logger.log("Mode: auto save");
                        gestureModel = null;

                        Object snapVideoView = getObjectField(param.thisObject, Obfuscator.VIDEOSNAPRENDERER_SNAPVIDEOVIEW);
                        Context context = (Context) callMethod(snapVideoView, "getContext");

                        saveReceivedSnap(context, receivedSnap);
                    }
                }
            });

            XC_MethodHook gestureMethodHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // Check if it should be saved or already is saved
                    if (gestureModel == null || gestureModel.isSaved()) return;

                    MotionEvent motionEvent = (MotionEvent) param.args[0];
                    if (motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
                        Object snapView = getObjectField(param.thisObject, Obfuscator.SNAPLISTITEMHANDLER_IMAGEVIEW);
                        boolean viewing = (Boolean) callMethod(snapView, Obfuscator.SNAPVIEW_ISVIEWING);
                        if (!viewing) return;

                        // Result true means the event is handled
                        param.setResult(true);

                        if (!gestureModel.isInitialized()) {
                            gestureModel.initialize(motionEvent.getRawX(), motionEvent.getRawY());
                        } else if (!gestureModel.isSaved()){
                            float deltaX = (motionEvent.getRawX() - gestureModel.getStartX());
                            float deltaY = (motionEvent.getRawY() - gestureModel.getStartY());
                            // Pythagorean theorem to calculate the distance between to points
                            float currentDistance = (float) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

                            // Distance is bigger than previous, re-set reference point
                            if (currentDistance > gestureModel.getDistance()) {
                                gestureModel.setDistance(currentDistance);
                            } else { // On its way back
                                // Meaning it's at least 70% back to the start point and the gesture was longer then 20% of the screen
                                if ((currentDistance < (gestureModel.getDistance() * 0.3)) && (gestureModel.getDistance() > (gestureModel.getDisplayHeight() * 0.2))) {
                                    gestureModel.setSaved();
                                    Context context = (Context) callMethod(snapView, "getContext");
                                    saveReceivedSnap(context, gestureModel.getReceivedSnap());
                                }
                            }
                        }
                    }
                }
            };

            // Hook gesture handling for snaps
            findAndHookMethod(Obfuscator.SNAPLISTITEMHANDLER_CLASS, lpparam.classLoader, Obfuscator.SNAPLISTITEMHANDLER_TOUCHEVENT_SNAP,
                    MotionEvent.class, float.class, float.class, int.class, gestureMethodHook);

            // Hook gesture handling for stories
            findAndHookMethod(Obfuscator.SNAPLISTITEMHANDLER_CLASS, lpparam.classLoader, Obfuscator.SNAPLISTITEMHANDLER_TOUCHEVENT_STORY,
                    MotionEvent.class, float.class, float.class, int.class, gestureMethodHook);

            final Class<?> snapImagebryo = findClass(Obfuscator.SNAPIMAGEBRYO_CLASS, lpparam.classLoader);

            /**
             * Method which gets called to prepare an image for sending (before selecting contacts).
             * We check whether it's an image or a video and save it.
             */
            findAndHookMethod(Obfuscator.SNAPPREVIEWFRAGMENT_CLASS, lpparam.classLoader, Obfuscator.SNAPPREVIEWFRAGMENT_PREPARESNAPFORSENDING, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    refreshPreferences();
                    Logger.log("----------------------- KEEPCHAT ------------------------", false);

                    if (!mSaveSentSnaps) {
                        Logger.log("Not saving sent snap");
                        return;
                    }

                    Context context = (Context) callMethod(param.thisObject, "getActivity");
                    Object mediabryo = getObjectField(param.thisObject, Obfuscator.SNAPPREVIEWFRAGMENT_VAR_MEDIABYRO);
                    Bitmap image = (Bitmap) callMethod(mediabryo, Obfuscator.MEDIABRYO_GETSNAPBITMAP);
                    String fileName = dateFormat.format(new Date());

                    // Check if instance of SnapImageBryo and thus an image or a video
                    if (snapImagebryo.isInstance(mediabryo)) {
                        saveSnap(SnapType.SENT, MediaType.IMAGE, context, image, null, fileName, null);
                    } else {
                        Uri videoUri = (Uri) callMethod(mediabryo, Obfuscator.MEDIABRYO_VIDEOURI);
                        saveSnap(SnapType.SENT, MediaType.VIDEO, context, image, videoUri.getPath(), fileName, null);
                    }
                }
            });

            final Class<?> imageResourceViewClass = findClass(Obfuscator.IMAGERESOURCEVIEW_CLASS, lpparam.classLoader);
            hookAllConstructors(imageResourceViewClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final ImageView imageView = (ImageView) param.thisObject;
                    imageView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Logger.log("----------------------- KEEPCHAT ------------------------", false);
                            Logger.log("Long press on chat image detected");

                            Bitmap chatImage = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                            Object imageResource = getObjectField(param.thisObject, Obfuscator.IMAGERESOURCEVIEW_VAR_IMAGERESOURCE);

                            Object chatMedia = getObjectField(imageResource, Obfuscator.IMAGERESOURCE_VAR_CHATMEDIA);
                            Long timestamp = (Long) callMethod(chatMedia, Obfuscator.CHAT_GETTIMESTAMP);
                            String sender = (String) callMethod(chatMedia, Obfuscator.STATEFULCHATFEEDITEM_GETSENDER);
                            String filename = sender + "_" + dateFormat.format(timestamp);

                            saveSnap(SnapType.CHAT, MediaType.IMAGE, imageView.getContext(), chatImage, null, filename, sender);
                            return true;
                        }
                    });
                }
            });



            /**
             * If the snap's duration is under the limit, set it to the limit.
             */
            findAndHookMethod(Obfuscator.RECEIVEDSNAP_CLASS, lpparam.classLoader, Obfuscator.RECEIVEDSNAP_DISPLAYTIME, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Double currentResult = (Double) param.getResult();
                    if (mTimerMinimum != TIMER_MINIMUM_DISABLED && currentResult < mTimerMinimum) {
                        param.setResult(mTimerMinimum);
                    }
                }
            });


            /**
             * Always return false when asked if an ReceivedSnap was screenshotted.
             */
            findAndHookMethod(Obfuscator.RECEIVEDSNAP_CLASS, lpparam.classLoader, Obfuscator.RECEIVEDSNAP_ISSCREENSHOTTED, XC_MethodReplacement.returnConstant(false));

            /**
             * Prevent creation of the ScreenshotDetector class.
             */
            findAndHookMethod(Obfuscator.SCREENSHOTDETECTOR_CLASS, lpparam.classLoader, Obfuscator.SCREENSHOTDETECTOR_RUNDECTECTIONSESSION, List.class, long.class,
                    XC_MethodReplacement.DO_NOTHING);

        } catch (Exception e) {
            Logger.log("Error occured: Keepchat doesn't currently support this version, wait for an update", e);

            findAndHookMethod("com.snapchat.android.LandingPageActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Toast.makeText((Context) param.thisObject, "This version of snapchat is currently not supported by Keepchat.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }



    public enum SnapType {
        SNAP("snap", "/ReceivedSnaps"),
        STORY("story", "/Stories"),
        SENT("sent", "/SentSnaps"),
        CHAT("chat", "/Chat");

        private final String name;
        private final String subdir;

        SnapType(String name, String subdir) {
            this.name = name;
            this.subdir = subdir;
        }
    }

    public enum MediaType {
        IMAGE(".jpg"),
        VIDEO(".mp4");

        private final String fileExtension;

        MediaType(String fileExtension) {
            this.fileExtension = fileExtension;
        }
    }

    private void saveReceivedSnap(Context context, Object receivedSnap) {
        MediaType mediaType = (MediaType) removeAdditionalInstanceField(receivedSnap, "snap_media_type");
        SnapType snapType = (SnapType) removeAdditionalInstanceField(receivedSnap, "snap_type");

        String sender = null;
        if (snapType == SnapType.SNAP) {
            sender = (String) callMethod(receivedSnap, Obfuscator.RECEIVEDSNAP_GETSENDER);
        } else if (snapType == SnapType.STORY) {
            sender = (String) callMethod(receivedSnap, Obfuscator.STORYSNAP_GETSENDER);
        }

        Date timestamp = new Date((Long) callMethod(receivedSnap, Obfuscator.SNAP_GETTIMESTAMP));
        String filename = sender + "_" + dateFormat.format(timestamp);

        Bitmap image = (Bitmap) removeAdditionalInstanceField(receivedSnap, "snap_bitmap");
        String videoUri = (String) removeAdditionalInstanceField(receivedSnap, "snap_video_uri");

        saveSnap(snapType, mediaType, context, image, videoUri, filename, sender);
    }

    private void saveSnap(SnapType snapType, MediaType mediaType, Context context, Bitmap image, String videoUri, String filename, String sender) {
        File directory;
        try {
            directory = createFileDir(snapType.subdir, sender);
        } catch (IOException e) {
            Logger.log(e);
            return;
        }

        File imageFile = new File(directory, filename + MediaType.IMAGE.fileExtension);
        File videoFile = new File(directory, filename + MediaType.VIDEO.fileExtension);

        if (mediaType == MediaType.IMAGE) {
            if (imageFile.exists()) {
                Logger.log("Image already exists");
                showToast(context, mResources.getString(R.string.image_exists));
                return;
            }

            if (saveImage(image, imageFile)) {
                showToast(context, mResources.getString(R.string.image_saved));
                Logger.log("Image " + snapType.name + " has been saved");
                Logger.log("Path: " + imageFile.toString());

                runMediaScanner(context, imageFile.getAbsolutePath());
            } else {
                showToast(context, mResources.getString(R.string.image_not_saved));
            }
        } else if (mediaType == MediaType.VIDEO) {
            boolean hasOverlay = image != null;
            if (videoFile.exists()) {
                Logger.log("Video already exists");
                showToast(context, mResources.getString(R.string.video_exists));
                return;
            }

            if (saveVideo(videoUri, videoFile) && (!hasOverlay || saveImage(image, imageFile))) {
                showToast(context, mResources.getString(R.string.video_saved));
                Logger.log("Video " + snapType.name + " has been saved (" + (hasOverlay ? "has" : "no") + " overlay)");
                Logger.log("Path: " + videoFile.toString());

                if (hasOverlay) {
                    runMediaScanner(context, videoFile.getAbsolutePath(), imageFile.getAbsolutePath());
                } else {
                    runMediaScanner(context, videoFile.getAbsolutePath());
                }
            } else {
                showToast(context, mResources.getString(R.string.video_not_saved));
            }
        }
    }

    private void refreshPreferences() {
        boolean prefsChanged = false;
        if (sharedPreferences == null) {
            //sharedPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID);
            prefsChanged = true;
        } else if (sharedPreferences.hasFileChanged()) {
            sharedPreferences.reload();
            prefsChanged = true;
        }

        if (prefsChanged) {
            mModeSnapImage = sharedPreferences.getInt("pref_key_snaps_images", mModeSnapImage);
            mModeSnapVideo = sharedPreferences.getInt("pref_key_snaps_videos", mModeSnapVideo);
            mModeStoryImage = sharedPreferences.getInt("pref_key_stories_images", mModeStoryImage);
            mModeStoryVideo = sharedPreferences.getInt("pref_key_stories_videos", mModeStoryVideo);
            mTimerMinimum = sharedPreferences.getInt("pref_key_timer_minimum", mTimerMinimum);
            mToastEnabled = sharedPreferences.getBoolean("pref_key_toasts_checkbox", mToastEnabled);
            mToastLength = sharedPreferences.getInt("pref_key_toasts_duration", mToastLength);
            mSavePath = sharedPreferences.getString("pref_key_save_location", mSavePath);
            mSaveSentSnaps = sharedPreferences.getBoolean("pref_key_save_sent_snaps", mSaveSentSnaps);
            mSortByCategory = sharedPreferences.getBoolean("pref_key_sort_files_mode", mSortByCategory);
            mSortByUsername = sharedPreferences.getBoolean("pref_key_sort_files_username", mSortByUsername);
            mDebugging = sharedPreferences.getBoolean("pref_key_debug_mode", mDebugging);
            Logger.setDebuggingEnabled(mDebugging);

            if (mDebugging) {
                Logger.log("----------------------- KEEPCHAT SETTINGS -----------------------", false);
                Logger.log("Preferences have changed:");
                String[] saveModes = {"SAVE_AUTO", "SAVE_S2S", "DO_NOT_SAVE"};
                Logger.log("~ mModeSnapImage: " + saveModes[mModeSnapImage]);
                Logger.log("~ mModeSnapVideo: " + saveModes[mModeSnapVideo]);
                Logger.log("~ mModeStoryImage: " + saveModes[mModeStoryImage]);
                Logger.log("~ mModeStoryVideo: " + saveModes[mModeStoryVideo]);
                Logger.log("~ mTimerMinimum: " + mTimerMinimum);
                Logger.log("~ mToastEnabled: " + mToastEnabled);
                Logger.log("~ mToastLength: " + mToastLength);
                Logger.log("~ mSavePath: " + mSavePath);
                Logger.log("~ mSaveSentSnaps: " + mSaveSentSnaps);
                Logger.log("~ mSortByCategory: " + mSortByCategory);
                Logger.log("~ mSortByUsername: " + mSortByUsername);
            }
        }
    }

    private File createFileDir(String category, String sender) throws IOException {
        File directory = new File(mSavePath);

        if (mSortByCategory || (mSortByUsername && sender == null)) {
            directory = new File(directory, category);
        }

        if (mSortByUsername && sender != null) {
            directory = new File(directory, sender);
        }

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory " + directory);
        }

        return directory;
    }

    // function to saveimage
    private static boolean saveImage(Bitmap image, File fileToSave) {
        try {
            FileOutputStream out = new FileOutputStream(fileToSave);
            image.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            Logger.log("Exception while saving an image", e);
            return false;
        }
    }

    // function to save video
    private static boolean saveVideo(String videoUri, File fileToSave) {
        try {
            FileInputStream in = new FileInputStream(new File(videoUri));
            FileOutputStream out = new FileOutputStream(fileToSave);

            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            Logger.log("Exception while saving a video", e);
            return false;
        }
    }

    /*
     * Tells the media scanner to scan the newly added image or video so that it
     * shows up in the gallery without a reboot. And shows a Toast message where
     * the media was saved.
     * @param context Current context
     * @param filePath File to be scanned by the media scanner
     */
    private void runMediaScanner(Context context, String... mediaPath) {
        try {
            Logger.log("MediaScanner started");
            MediaScannerConnection.scanFile(context, mediaPath, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Logger.log("MediaScanner scanned file: " + uri.toString());
                        }
                    });
        } catch (Exception e) {
            Logger.log("Error occurred while trying to run MediaScanner", e);
        }
    }

    private void showToast(Context context, String toastMessage) {
        if (mToastEnabled) {
            if (mToastLength == TOAST_LENGTH_SHORT) {
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mResources = XModuleResources.createInstance(startupParam.modulePath, null);
    }
}
