package com.marz.snapprefs;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getParameterTypes;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

public class HookSendList{
	static void initSelectAll (final LoadPackageParam lpparam) {
		HookMethods.refreshPreferences();
	        findAndHookMethod(Common.Class_SendToFragment, lpparam.classLoader, Common.Method_CreateView, new XC_MethodHook() {
	            @Override
	            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    CheckBox selectAll;
	                try {
	                    View title = (View) getObjectField(param.thisObject, Common.titleSendTo);
	                    Context c = (Context) callMethod(param.thisObject, "getActivity");
	                    selectAll = getCheckbox(c);
	                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
	                    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
	                    ((RelativeLayout) title.getParent().getParent()).addView(selectAll, params);
	                    setAdditionalInstanceField(param.thisObject, Common.select_name, selectAll);
	                } catch (Throwable t) {
	                    HookMethods.logging("SnapPrefs: Checkbox init. failed");
	                    HookMethods.logging(t.toString());
	                    return;
	                }


	                selectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	                    @Override
	                    public void onCheckedChanged(CompoundButton compoundButton, boolean set) {
	                        //SendToAdapter
	                        Object hopefullyArrayAdapter =  getObjectField(param.thisObject, Common.sendToList);

	                        if (hopefullyArrayAdapter != null && hopefullyArrayAdapter instanceof ArrayAdapter) {
	                            ArrayAdapter aa = (ArrayAdapter) hopefullyArrayAdapter;
	                            ArrayList friendList;
	                            Set FriendSet;
	                            List StoryList;

	                            try {
	                                friendList = (ArrayList) getObjectField(aa, "c");
	                                FriendSet = (Set) getObjectField(param.thisObject, Common.sendToFriendSet);
	                                StoryList = (List) getObjectField(param.thisObject, Common.sendToStoryList);
	                                Class<?>[] types = getParameterTypes(friendList.toArray());
	                                for (int i = 0; i < types.length; i++) {
	                                    Object thingToAdd = friendList.get(i);
	                                    if (types[i].getCanonicalName().equals(Common.Class_Friend)) {
	                                        if (set)
	                                            FriendSet.add(thingToAdd);
	                                        else
	                                            FriendSet.remove(thingToAdd);
	                                    } else if (types[i].getCanonicalName().equals(Common.Class_PostToStory) && HookMethods.selectStory == true) {
	                                        if (set)
	                                            StoryList.add(thingToAdd);
	                                        else
	                                            StoryList.remove(thingToAdd);
	                                    } else {
	                                        HookMethods.logging("SnapPrefs: Unknown type value at: " + types[i].toString());
	                                    }
	                                }
	                                callMethod(param.thisObject, Common.Method_AddToList);
	                            } catch (Throwable t) {
	                            	HookMethods.logging("SnapPrefs: Your Snapchat is outdated, update it.");
	                            	HookMethods.logging(t.toString());
	                            }

	                        }
	                    }
	                });
	            }
	        });

	        findAndHookMethod(Common.Class_SendToFragment, lpparam.classLoader, Common.Method_Visible, new XC_MethodHook() {
	            @Override
	            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
	                View v = (View) getAdditionalInstanceField(param.thisObject, Common.select_name);
	                v.setVisibility(View.VISIBLE);
	            }
	        });

	        findAndHookMethod(Common.Class_SendToFragment, lpparam.classLoader, Common.Method_Invisible, new XC_MethodHook() {
	            @Override
	            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
	                View v = (View) getAdditionalInstanceField(param.thisObject, Common.select_name);
	                v.setVisibility(View.INVISIBLE);
	            }
	        });
	    }
     /**
     * Opens SnapChat's Resources and gets the pretty checkbox, for reuse & consistent appearance
     * @param c SNAPCHAT's context
     * @return A pretty checkbox (hopefully)
     */
    public static CheckBox getCheckbox(Context c) {
        CheckBox cb = new CheckBox(c);
        try {
            //Setting properties from snapchat's res/layout/send_to_item.xml checkbox
            cb.setButtonDrawable(c.getResources().getIdentifier("send_to_button_selector", "drawable", "com.snapchat.android"));
            //May need to scale drawable bitmap...
            cb.setScaleX(0.7F);
            cb.setScaleY(0.7F);
        } catch (Exception e) {
            HookMethods.logging("Snapprefs: Error getting Checkbox");
        }
        return cb;
    }
}
