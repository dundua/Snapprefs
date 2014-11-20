package com.marz.snapprefs;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XC_MethodHook;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.XC_MethodReplacement;
import android.app.Activity;
import android.content.Context;
import android.content.res.XModuleResources;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.marz.snapprefs.Common;


public class HookMethods implements IXposedHookInitPackageResources, IXposedHookLoadPackage, IXposedHookZygoteInit  {
	
    private static String MODULE_PATH = null;
    
    private static final String PACKAGE_NAME = HookMethods.class.getPackage().getName();
    
    static XSharedPreferences prefs;
    
	private static boolean screenshot;
	private static boolean fullCaption;
	private static boolean hideBf;
	private static boolean hideRecent;
	private static boolean colours;
	static boolean txtcolours;
	static boolean bgcolours;
	static boolean size;
	static boolean transparency;
	static boolean rainbow;
	static boolean bg_transparency;
	static boolean txtstyle;
	static boolean txtgravity;
	static boolean debug;
	
	private static Context SnapContext;
	
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        refreshPreferences();  
    }
	
    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(Common.PACKAGE_SNAP))
            return;
        refreshPreferences();
		printSettings();
		
		if (fullCaption == true){
        logging("SnapPrefs: Hooked Snapchat's resources.");
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        resparam.res.setReplacement(Common.PACKAGE_SNAP, "layout", Common.Res_OFatCaption, modRes.fwd(R.layout.new_caption_fat));
        logging("SnapPrefs: Replaced fat-caption resources.");
        resparam.res.setReplacement(Common.PACKAGE_SNAP, "layout", Common.Res_OVanillaCaption, modRes.fwd(R.layout.new_vanilla_cap));
        logging("SnapPrefs: Replaced vanilla-caption resources.");
		}
		
		if (colours == true){
		resparam.res.hookLayout(Common.PACKAGE_SNAP, "layout", "snap_preview", new XC_LayoutInflated(){
			@Override
			public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
				Button text_button = (Button) liparam.view.findViewById(liparam.res.getIdentifier("toggle_caption_btn", "id", Common.PACKAGE_SNAP));
				final TextView editText = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("picture_caption", "id", Common.PACKAGE_SNAP));
				text_button.setLongClickable(true);
				logging("SnapPrefs: Hooking Layouts");
				text_button.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View view){
					Dialogs.MainDialog(SnapContext, editText);
					logging("SnapPrefs: Displaying MainDialog");
					return true;
					}
				});
			}
		});
		}
    } 
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable{
		if (!lpparam.packageName.equals(Common.PACKAGE_SNAP))
            return;
		logging("Snap Prefs: Opened package com.snapchat.android!");
		
        refreshPreferences();   
		printSettings();
		
		findAndHookMethod(Common.Class_Landing, lpparam.classLoader, Common.Method_onCreate, Bundle.class, new XC_MethodHook(){
			protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
				prefs.reload();
				SnapContext = (Activity)methodHookParam.thisObject;
			}
		});
		
		if (screenshot == true){
			
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
				return false;
            }
			});	
		}
		
		if (fullCaption == true){
		findAndHookMethod(Common.Class_AfterText, lpparam.classLoader, Common.Method_AfterTextChanged, Editable.class, new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable{
					logging("Snap Prefs: hooked afterTextChanged");
					param.setResult(null);
					logging("Snap Prefs: result sent Null");
				}
		});
		
		Class<?> Caption = XposedHelpers.findClass(Common.Class_EditText, lpparam.classLoader);
		Class<?> CaptionView =XposedHelpers.findClass(Common.Class_CaptionView, lpparam.classLoader);
		final Constructor<?> Caption_Constructor = Caption.getConstructor(CaptionView,  Context.class, AttributeSet.class);
		XposedBridge.hookMethod(Caption_Constructor, new XC_MethodHook(){
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				logging("SnapPrefs: hooked CaptionEditText");
				XposedHelpers.callMethod(param.thisObject, "setInputType", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
				logging("SnapPrefs: setInputType");
			}
		});
		}
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
	}


static void refreshPreferences() {

	prefs = new XSharedPreferences(new File(
			Environment.getDataDirectory(), "data/"
					+ PACKAGE_NAME + "/shared_prefs/" + PACKAGE_NAME
					+ "_preferences" + ".xml"));
	prefs.reload();
	screenshot = prefs.getBoolean("pref_key_screenshot", false);
	fullCaption = prefs.getBoolean("pref_key_fulltext", false);
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
	debug = false;
	
	if (txtcolours == true || bgcolours == true || size == true || rainbow == true || bg_transparency == true || txtstyle == true || txtgravity == true){
		colours = true;	
	}
}

private void printSettings() {
	logging("\n~~~~~~~~~~~~ SNAPPREFS SETTINGS");

	logging("ScreenshotProtection: " + screenshot);
	logging("FullCaption: " + fullCaption);
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
}

static void logging(String message) {
	if (debug == false)
		XposedBridge.log(message);
}
}
