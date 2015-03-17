package com.marz.snapprefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.marz.snapprefs.Util.CommonUtils;
import com.marz.snapprefs.Util.ImageUtils;
import com.marz.snapprefs.Util.VideoUtils;
import com.marz.snapprefs.Util.XposedUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static de.robv.android.xposed.XposedHelpers.removeAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;


public class HookMethods implements IXposedHookInitPackageResources, IXposedHookLoadPackage, IXposedHookZygoteInit  {
	
	
    public static final String SNAPCHAT_PACKAGE_NAME = "com.snapchat.android";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS", Locale.getDefault());
    private static XModuleResources mResources;
    
    //SNAPSHARE
    private Uri initializedUri;
    private static int snapchatVersion;
    
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
    public static int mModeSnapImage = SAVE_AUTO;
    public static int mModeSnapVideo = SAVE_AUTO;
    public static int mModeStoryImage = SAVE_AUTO;
    public static int mModeStoryVideo = SAVE_AUTO;
    public static int mTimerMinimum = TIMER_MINIMUM_DISABLED;
    public static boolean mToastEnabled = true;
    public static int mToastLength = TOAST_LENGTH_LONG;
    public static String mSavePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Snapprefs";
    public static boolean mSaveSentSnaps = false;
    public static boolean mSortByCategory = true;
    public static boolean mSortByUsername = true;
    public static boolean mDebugging = true;
	
    private static String MODULE_PATH = null;
    
    private static final String PACKAGE_NAME = HookMethods.class.getPackage().getName();
    
    static XSharedPreferences prefs;

	private static boolean fullCaption;
	private static boolean selectAll;
	private static boolean hideBf;
	private static boolean hideRecent;
	private static boolean colours;
	static boolean selectStory;
	static boolean txtcolours;
	static boolean bgcolours;
	static boolean size;
	static boolean transparency;
	static boolean rainbow;
	static boolean bg_transparency;
	static boolean txtstyle;
	static boolean txtgravity;
	static boolean debug;
	static EditText editText;
	static XModuleResources modRes;
	
	private static Context SnapContext;
	Class CaptionEditText;
	
	public static int px(float f){
		return Math.round((f * SnapContext.getResources().getDisplayMetrics().density));
	}
	
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        mResources = XModuleResources.createInstance(startupParam.modulePath, null);
        refreshPreferences();  
    }
	
    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(Common.PACKAGE_SNAP))
            return;
        //modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        refreshPreferences();
		printSettings();	
		
		if (colours == true){
			addGhost(resparam);
		}
    }
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable{
		if (!lpparam.packageName.equals(Common.PACKAGE_SNAP))
            return;
		logging("Snap Prefs: Opened package com.snapchat.android!");
		
        refreshPreferences();   
		printSettings();
		getEditText(lpparam);
		
		//SNAPSHARE
		
        // Thanks to KeepChat for the following snippet
        try {
            XposedUtils.log("----------------- SNAPSHARE HOOKED -----------------", false);
            Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
            Context context = (Context) callMethod(activityThread, "getSystemContext");

            PackageInfo piSnapChat = context.getPackageManager().getPackageInfo(lpparam.packageName, 0);
            XposedUtils.log("SnapChat Version: " + piSnapChat.versionName + " (" + piSnapChat.versionCode + ")", false);
            XposedUtils.log("SnapPrefs Version: " + Common.VERSION_NAME + " (" + Common.VERSION_CODE + ")", false);

            snapchatVersion = Obfuscator_share.getVersion(piSnapChat.versionCode);
        } catch (Exception e) {
            XposedUtils.log("Exception while trying to get version info", e);
            return;
        }

        final Class snapCapturedEventClass = findClass("com.snapchat.android.util.eventbus.SnapCapturedEvent", lpparam.classLoader);
        final Media media = new Media(); // a place to store the media

        // This is where the media is loaded and transformed. Hooks after the onCreate() call of the main Activity.
        findAndHookMethod("com.snapchat.android.LandingPageActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                refreshPreferences();
                XposedUtils.log("----------------- SNAPSHARE STARTED -----------------", false);
                final Activity activity = (Activity) param.thisObject;
                // Get intent, action and MIME type
                Intent intent = activity.getIntent();
                String type = intent.getType();
                String action = intent.getAction();
                XposedUtils.log("Intent type: " + type + ", intent action:" + action);

                // Check if this is a normal launch of Snapchat or actually called by Snapshare and if loaded from recents
                if (type != null && Intent.ACTION_SEND.equals(action) && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                    Uri mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    // Check for bogus call
                    if (mediaUri == null) {
                        return;
                    }
                    /* We check if the current media got already initialized and should exit instead
                     * of doing the media initialization again. This check is necessary
                     * because onCreate() is also called if the phone is just rotated. */
                    if (initializedUri == mediaUri) {
                        XposedUtils.log("Media already initialized, exit onCreate() hook");
                        return;
                    }

                    ContentResolver contentResolver = activity.getContentResolver();

                    if (type.startsWith("image/")) {
                        XposedUtils.log("Image URI: " + mediaUri.toString());
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(contentResolver, mediaUri);
                            XposedUtils.log("Image shared, size: " + bitmap.getWidth() + " x " + bitmap.getHeight() + " (w x h)");

                            String filePath = mediaUri.getPath();
                            if (mediaUri.getScheme().equals("content")) {
                                filePath = CommonUtils.getPathFromContentUri(contentResolver, mediaUri);
                                XposedUtils.log("Converted content URI to file path " + filePath);
                            }
                            // Rotate image using EXIF-data
                            bitmap = ImageUtils.rotateUsingExif(bitmap, filePath);
                            // Landscape images have to be rotated 90 degrees clockwise for Snapchat to be displayed correctly
                            if (Common.ROTATION_MODE != Common.ROTATION_NONE) {
                                if (bitmap.getWidth() > bitmap.getHeight()) {
                                    XposedUtils.log("Landscape image detected, rotating image " + Common.ROTATION_MODE + " degrees");
                                    bitmap = ImageUtils.rotateBitmap(bitmap, Common.ROTATION_MODE);
                                } else {
                                    XposedUtils.log("Image is in portrait, rotation not needed");
                                }
                            }

                            // Snapchat will break if the image is too large and it will scale the image up if the Display rectangle is larger than the image.
                            ImageUtils imageUtils = new ImageUtils(activity);
                            switch (Common.ADJUST_METHOD) {
                                case Common.ADJUST_CROP:
                                    XposedUtils.log("Adjustment Method: Crop");
                                    bitmap = imageUtils.adjustmentMethodCrop(bitmap);
                                    break;
                                case Common.ADJUST_SCALE:
                                    XposedUtils.log("Adjustment Method: Scale");
                                    bitmap = imageUtils.adjustmentMethodScale(bitmap);
                                    break;
                                case Common.ADJUST_NONE:
                                    XposedUtils.log("Adjustment Method: None");
                                    bitmap = imageUtils.adjustmentMethodNone(bitmap);
                                    break;
                            }

                            // Make Snapchat show the image
                            media.setContent(bitmap);
                        } catch (Exception e) {
                            XposedUtils.log(e);
                            return;
                        }
                    }
                    else if (type.startsWith("video/")) {
                        Uri videoUri;
                        // Snapchat expects the video URI to be in the file:// scheme, not content:// scheme
                        if (URLUtil.isFileUrl(mediaUri.toString())) {
                            videoUri = mediaUri;
                            XposedUtils.log("Already had File URI: " + mediaUri.toString());
                        } else { // No file URI, so we have to convert it
                            videoUri = CommonUtils.getFileUriFromContentUri(contentResolver, mediaUri);
                            if (videoUri != null) {
                                XposedUtils.log("Converted content URI to file URI " + videoUri.toString());
                            } else {
                                XposedUtils.log("Couldn't resolve URI to file:// scheme: " + mediaUri.toString());
                                return;
                            }
                        }

                        File videoFile = new File(videoUri.getPath());
                        File tempFile = File.createTempFile("snapshare_video", null);

                        try {
                            if (Common.ROTATION_MODE == Common.ROTATION_NONE) {
                                XposedUtils.log("Rotation disabled, creating a temporary copy");
                                CommonUtils.copyFile(videoFile, tempFile);
                            } else {
                                VideoUtils.rotateVideo(videoFile, tempFile);
                            }

                            videoUri = Uri.fromFile(tempFile);
                            XposedUtils.log("Temporary file path: " + videoUri);
                        } catch (Exception e) {
                            XposedUtils.log(e);
                            return;
                        }

                        long fileSize = tempFile.length();
                        // Get size of video and compare to the maximum size
                        if (Common.CHECK_SIZE && fileSize > Common.MAX_VIDEO_SIZE) {
                            String readableFileSize = CommonUtils.formatBytes(fileSize);
                            String readableMaxSize = CommonUtils.formatBytes(Common.MAX_VIDEO_SIZE);
                            XposedUtils.log("Video too big (" + readableFileSize + ")");
                            // Inform the user with a dialog
                            createSizeDialog(activity, readableFileSize, readableMaxSize).show();
                        }
                        media.setContent(videoUri);
                    }

                    /**
                     * Mark image as initialized
                     * @see initializedUri
                     */
                    initializedUri = mediaUri;
                } else {
                    XposedUtils.log("Regular call of Snapchat.");
                    initializedUri = null;
                }
            }

        });

        /**
         * We want to send our media once the camera is ready, that's why we hook the refreshFlashButton/onCameraStateEvent method.
         * The media is injected by calling the eventbus to send a snapcapture event with our own media.
         */
        XC_MethodHook cameraLoadedHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                refreshPreferences(); // Refresh preferences for captions
                if (initializedUri == null) {
                    return; // We don't have an image to send, so don't try to send one
                }

                XposedUtils.log("Doing it's magic!");
                Object snapCaptureEvent;

                // Since 4.1.10 a new Class called Snapbryo stores all the data for snaps
                // SnapCapturedEvent(Snapbryo(Builder(Media)))
                if (snapchatVersion >= Obfuscator_share.FOUR_ONE_TEN) {
                    Object builder = newInstance(findClass("com.snapchat.android.model.Snapbryo.Builder", lpparam.classLoader));
                    builder = callMethod(builder, Obfuscator_share.BUILDER_CONSTRUCTOR.getValue(snapchatVersion), media.getContent());
                    Object snapbryo = callMethod(builder, Obfuscator_share.CREATE_SNAPBRYO.getValue(snapchatVersion));
                    snapCaptureEvent = newInstance(snapCapturedEventClass, snapbryo);
                } else {
                    snapCaptureEvent = newInstance(snapCapturedEventClass, media.getContent());
                }

                // Call the eventbus to post our SnapCapturedEvent, this will take us to the SnapPreviewFragment
                Object busProvider = callStaticMethod(findClass("com.snapchat.android.util.eventbus.BusProvider", lpparam.classLoader), Obfuscator_share.GET_BUS.getValue(snapchatVersion));
                callMethod(busProvider, Obfuscator_share.BUS_POST.getValue(snapchatVersion), snapCaptureEvent);
                // Clean up after ourselves, otherwise snapchat will crash
                initializedUri = null;
            }
        };

        // In 5.0.2 CameraPreviewFragment was renamed to CameraFragment
        String cameraFragment = "com.snapchat.android.camera." + (snapchatVersion < Obfuscator_share.FIVE_ZERO_TWO ? "CameraPreviewFragment" : "CameraFragment");
        // In 5.0.36.0 (beta) refreshFlashButton was removed, we use onCameraStateEvent instead
        if (snapchatVersion >= Obfuscator_share.FIVE_ZERO_THIRTYSIX) {
            Class<?> cameraStateEventClass = findClass("com.snapchat.android.util.eventbus.CameraStateEvent", lpparam.classLoader);
            findAndHookMethod(cameraFragment, lpparam.classLoader, Obfuscator_share.CAMERA_STATE_EVENT, cameraStateEventClass, cameraLoadedHook);
            XposedUtils.log("Hooked onCameraStateEvent");
        } else {
            findAndHookMethod(cameraFragment, lpparam.classLoader, Obfuscator_share.CAMERA_LOAD.getValue(snapchatVersion), cameraLoadedHook);
            XposedUtils.log("Hooked refreshFlashButton");
        }

        // VanillaCaptionEditText was moved from an inner-class to a separate class in 8.1.0
        String vanillaCaptionEditTextClassName = "com.snapchat.android.ui." + (snapchatVersion < Obfuscator_share.EIGHT_ONE_ZERO ? "VanillaCaptionView$VanillaCaptionEditText" : "caption.VanillaCaptionEditText");
        hookAllConstructors(findClass(vanillaCaptionEditTextClassName, lpparam.classLoader), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (Common.CAPTION_UNLIMITED_VANILLA) {
                    XposedUtils.log("Unlimited vanilla captions");
                    EditText vanillaCaptionEditText = (EditText) param.thisObject;
                    // Set single lines mode to false
                    vanillaCaptionEditText.setSingleLine(false);

                    // Remove actionDone IME option, by only setting flagNoExtractUi
                    vanillaCaptionEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    // Remove listener hiding keyboard when enter is pressed by setting the listener to null
                    vanillaCaptionEditText.setOnEditorActionListener(null);
                    // Remove listener for cutting of text when the first line is full by setting the text change listeners list to null
                    setObjectField(vanillaCaptionEditText, "mListeners", null);
                }
            }
        });

        // FatCaptionEditText was moved from an inner-class to a separate class in 8.1.0
        String fatCaptionEditTextClassName = "com.snapchat.android.ui." + (snapchatVersion < Obfuscator_share.EIGHT_ONE_ZERO ? "FatCaptionView$FatCaptionEditText" : "caption.FatCaptionEditText");
        hookAllConstructors(findClass(fatCaptionEditTextClassName, lpparam.classLoader), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (Common.CAPTION_UNLIMITED_FAT) {
                    XposedUtils.log("Unlimited fat captions");
                    EditText fatCaptionEditText = (EditText) param.thisObject;
                    // Remove InputFilter with character limit
                    fatCaptionEditText.setFilters(new InputFilter[0]);

                    // Remove actionDone IME option, by only setting flagNoExtractUi
                    fatCaptionEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                    // Remove listener hiding keyboard when enter is pressed by setting the listener to null
                    fatCaptionEditText.setOnEditorActionListener(null);
                    // Remove listener for removing new lines by setting the text change listeners list to null
                    setObjectField(fatCaptionEditText, "mListeners", null);
                }
            }
        });

        // Enable Snapchat's internal debugging class
        if (Common.TIMBER) {
            XposedUtils.log("Timber enabled");
            Class<?> timberClass = findClass("com.snapchat.android.Timber", lpparam.classLoader);
            Class<?> releaseManagerClass = findClass("com.snapchat.android.util.debug.ReleaseManager", lpparam.classLoader);
            Class<?> logTypeClass = findClass("com.snapchat.android.Timber.LogType", lpparam.classLoader);
            // Return true when checking whether it should debug
            findAndHookMethod(releaseManagerClass, "b", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(releaseManagerClass, "c", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(releaseManagerClass, "d", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(releaseManagerClass, "e", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(releaseManagerClass, "f", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(releaseManagerClass, "g", XC_MethodReplacement.returnConstant(true));
            findAndHookMethod(releaseManagerClass, "h", XC_MethodReplacement.returnConstant(true));
            // Usually returns the class name to use as the Log Tag, however we want a custom one
            findAndHookMethod(timberClass, "a", logTypeClass, String.class, boolean.class, Throwable.class, String.class, Object[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String logTag = (String) param.args[1];
                    param.args[1] = "Timber_" + logTag;
                }
            });
        }
		
		//KEEPCHAT
		
		try {
            Logger.log("----------------------- SAVING IS LOADING ------------------------", false);
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

                    Logger.log("----------------------- SNAPPREFS ------------------------", false);
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

                    Logger.log("----------------------- SNAPPREFS ------------------------", false);
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
                    Logger.log("----------------------- SNAPPREFS ------------------------", false);

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
                            Logger.log("----------------------- SNAPPREFS ------------------------", false);
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
            Logger.log("Error occured: Snapprefs doesn't currently support this version, wait for an update", e);

            findAndHookMethod("com.snapchat.android.LandingPageActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Toast.makeText((Context) param.thisObject, "This version of snapchat is currently not supported by Snapprefs.", Toast.LENGTH_LONG).show();
                }
            });
        }
		//SNAPPREFS
		
		findAndHookMethod(Common.Class_Landing, lpparam.classLoader, Common.Method_onCreate, Bundle.class, new XC_MethodHook(){
			protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
				prefs.reload();
				refreshPreferences(); 
				SnapContext = (Activity)methodHookParam.thisObject;
			}
		});

			
		findAndHookMethod(Common.Class_StateBuilder, lpparam.classLoader, Common.Method_ScreenshotCount, long.class, new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.args[0] = 0L;
				logging("Snap Prefs: Not reporting screenshots - ScreenshotCount");
			}
		});
		findAndHookMethod(Common.Class_ScreenshotDetector, lpparam.classLoader, Common.Method_DetectionSession, List.class, long.class, XC_MethodReplacement.returnConstant(null));	
		logging("Snap Prefs: Not reporting screenshots. DetectionSession");
		findAndHookMethod(Common.Class_Screenshot, lpparam.classLoader, Common.Method_Screenshot, new XC_MethodReplacement(){
			@Override
			protected Object replaceHookedMethod(MethodHookParam param)
					throws Throwable {
		logging("Snap Prefs: Not reporting screenshots. Last Method");
		return false;         }
			});

		if (hideBf == true){
		findAndHookMethod(Common.Class_Friend, lpparam.classLoader, Common.Method_BestFriend, new XC_MethodReplacement(){
		@Override
		protected Object replaceHookedMethod(MethodHookParam param)
				throws Throwable {
			logging("Snap Prefs: Removing Best-friends");
			return false;
        }
		});
		}
		
		if (hideRecent == true){
		findAndHookMethod(Common.Class_Friend, lpparam.classLoader, Common.Method_Recent, new XC_MethodReplacement(){
		@Override
		protected Object replaceHookedMethod(MethodHookParam param)
				throws Throwable {
			logging("Snap Prefs: Removing Recents");
			return false;
        }
		});
		}
		if (selectAll == true){
			HookSendList.initSelectAll(lpparam);
		}
	}

	//SNAPSHARE
	
    /**
     * Creates a dialog saying the image is too large. Two options are given: continue or quit.
     * @param activity The activity to be used to create the dialog
     * @param readableFileSize The human-readable current file size
     * @param readableMaxSize The human-readable maximum file size
     * @return The dialog to show
     */
    private static AlertDialog createSizeDialog(final Activity activity, String readableFileSize, String readableMaxSize) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(mResources.getString(R.string.app_name));
        dialogBuilder.setMessage(mResources.getString(R.string.size_error, readableFileSize, readableMaxSize));
        dialogBuilder.setPositiveButton(mResources.getString(R.string.continue_anyway), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialogBuilder.setNegativeButton(mResources.getString(R.string.go_back), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                activity.finish();
            }
        });
        return dialogBuilder.create();
    }

static void refreshPreferences() {

	prefs = new XSharedPreferences(new File(
			Environment.getDataDirectory(), "data/"
					+ PACKAGE_NAME + "/shared_prefs/" + PACKAGE_NAME
					+ "_preferences" + ".xml"));
	prefs.reload();
	fullCaption = prefs.getBoolean("pref_key_fulltext", false);
	selectAll = prefs.getBoolean("pref_key_selectall", false);
	selectStory = prefs.getBoolean("pref_key_selectstory", false);
	hideBf = prefs.getBoolean("pref_key_hidebf", false);
	hideRecent = prefs.getBoolean("pref_key_hiderecent", false);
	txtcolours = prefs.getBoolean("pref_key_txtcolour", false);
	bgcolours = prefs.getBoolean("pref_key_bgcolour", false);
	size = prefs.getBoolean("pref_key_size", false);
	transparency = prefs.getBoolean("pref_key_size", false);
	rainbow = prefs.getBoolean("pref_key_rainbow", false);
	bg_transparency = prefs.getBoolean("pref_key_bg_transparency", false);
	txtstyle = prefs.getBoolean("pref_key_txtstyle", false);
	txtgravity = prefs.getBoolean("pref_key_txtgravity", false);
	debug = prefs.getBoolean("pref_key_debug", false);
	
	//KEEPCHAT
	
    mModeSnapImage = prefs.getInt("pref_key_snaps_images", mModeSnapImage);
    mModeSnapVideo = prefs.getInt("pref_key_snaps_videos", mModeSnapVideo);
    mModeStoryImage = prefs.getInt("pref_key_stories_images", mModeStoryImage);
    mModeStoryVideo = prefs.getInt("pref_key_stories_videos", mModeStoryVideo);
    mTimerMinimum = prefs.getInt("pref_key_timer_minimum", mTimerMinimum);
    mToastEnabled = prefs.getBoolean("pref_key_toasts_checkbox", mToastEnabled);
    mToastLength = prefs.getInt("pref_key_toasts_duration", mToastLength);
    mSavePath = prefs.getString("pref_key_save_location", mSavePath);
    mSaveSentSnaps = prefs.getBoolean("pref_key_save_sent_snaps", mSaveSentSnaps);
    mSortByCategory = prefs.getBoolean("pref_key_sort_files_mode", mSortByCategory);
    mSortByUsername = prefs.getBoolean("pref_key_sort_files_username", mSortByUsername);
    mDebugging = prefs.getBoolean("pref_key_debug_mode", mDebugging);
    
    //SNAPSHARE
    
    Common.ROTATION_MODE = Integer.parseInt(prefs.getString("pref_rotation", Integer.toString(Common.ROTATION_MODE)));
    Common.ADJUST_METHOD = Integer.parseInt(prefs.getString("pref_adjustment", Integer.toString(Common.ADJUST_METHOD)));
    Common.CAPTION_UNLIMITED_VANILLA = prefs.getBoolean("pref_caption_unlimited_vanilla", Common.CAPTION_UNLIMITED_VANILLA);
    Common.CAPTION_UNLIMITED_FAT = prefs.getBoolean("pref_caption_unlimited_fat", Common.CAPTION_UNLIMITED_FAT);
    Common.DEBUGGING = prefs.getBoolean("pref_debug", Common.DEBUGGING);
    Common.CHECK_SIZE = !prefs.getBoolean("pref_size_disabled", !Common.CHECK_SIZE);
    Common.TIMBER = prefs.getBoolean("pref_timber", Common.TIMBER);
	
	if (txtcolours == true || bgcolours == true || size == true || rainbow == true || bg_transparency == true || txtstyle == true || txtgravity == true){
		colours = true;	
	}
	else {
		colours = false;
	}	
}

private void printSettings() {
	logging("\n~~~~~~~~~~~~ SNAPPREFS SETTINGS");
	logging("FullCaption: " + fullCaption);
	logging("SelectAll: " + selectAll);
	logging("SelectStory: " + selectStory);
	logging("HideBF: " + hideBf);
	logging("HideRecent: " + hideRecent);
	logging("Colours: " + colours);
	logging("TxtColours: " + txtcolours);
	logging("BgColours: " + bgcolours);
	logging("Size: " + size);
	logging("Transparency: " + transparency);
	logging("Rainbow: " + rainbow);
	logging("Background Transparency: " + bg_transparency);
	logging("TextStyle: " + txtstyle);
	logging("TextGravity: " + txtgravity);
	logging("*****Debugging: " + debug + " *****");
	logging("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	Logger.setDebuggingEnabled(mDebugging);
	
    Logger.log("----------------------- SAVING SETTINGS -----------------------", false);
    logging("Preferences have changed:");
    String[] saveModes = {"SAVE_AUTO", "SAVE_S2S", "DO_NOT_SAVE"};
    logging("~ mModeSnapImage: " + saveModes[mModeSnapImage]);
    logging("~ mModeSnapVideo: " + saveModes[mModeSnapVideo]);
    logging("~ mModeStoryImage: " + saveModes[mModeStoryImage]);
    logging("~ mModeStoryVideo: " + saveModes[mModeStoryVideo]);
    logging("~ mTimerMinimum: " + mTimerMinimum);
    logging("~ mToastEnabled: " + mToastEnabled);
    logging("~ mToastLength: " + mToastLength);
    logging("~ mSavePath: " + mSavePath);
    logging("~ mSaveSentSnaps: " + mSaveSentSnaps);
    logging("~ mSortByCategory: " + mSortByCategory);
    logging("~ mSortByUsername: " + mSortByUsername);
}

static void logging(String message) {
	if (debug == true)
		XposedBridge.log(message);
}
public void getEditText(LoadPackageParam lpparam){
	this.CaptionEditText = XposedHelpers.findClass("com.snapchat.android.ui.caption.CaptionEditText", lpparam.classLoader);
	XposedBridge.hookAllConstructors(this.CaptionEditText, new XC_MethodHook(){
		@Override
		protected void afterHookedMethod(MethodHookParam param) throws PackageManager.NameNotFoundException {
			refreshPreferences();
			editText = (EditText) param.thisObject;
		}
	});
}

public void addGhost(InitPackageResourcesParam resparam){
	resparam.res.hookLayout(Common.PACKAGE_SNAP, "layout", "snap_preview", new XC_LayoutInflated(){
		public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
			RelativeLayout relativeLayout = (RelativeLayout) liparam.view.findViewById(liparam.res.getIdentifier("snap_preview_relative_layout", "id", Common.PACKAGE_SNAP));
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(liparam.view.findViewById(liparam.res.getIdentifier("drawing_btn", "id", Common.PACKAGE_SNAP)).getLayoutParams());
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.ALIGN_PARENT_TOP);
			layoutParams.topMargin = px(3.0f);
			layoutParams.leftMargin = px(55.0f);
			ImageButton ghost = new ImageButton(SnapContext);
			ghost.setBackgroundColor(0);
			ghost.setImageDrawable(mResources.getDrawable(R.drawable.triangle));
			ghost.setScaleX((float) 0.75);
			ghost.setScaleY((float) 0.75);
			ghost.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Dialogs.MainDialog(SnapContext, editText);
					logging("SnapPrefs: Displaying MainDialog");			
				}
			});
			relativeLayout.addView(ghost, layoutParams);
		}
	});
}


//KEEPCHAT


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
    IMAGE(".png"),
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
    String filename = sender + "_" + dateFormat.format(timestamp) + "___" + dateFormat.format(new Date());

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
        image.compress(Bitmap.CompressFormat.PNG, 100, out);
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
}
