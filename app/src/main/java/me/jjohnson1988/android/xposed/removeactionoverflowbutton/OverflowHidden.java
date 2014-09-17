package me.jjohnson1988.android.xposed.removeactionoverflowbutton;

import android.os.Build;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageButton;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

//TODO: Pre-4.0 compatibility (2.3 Gingerbread at the very least)
//TODO: Remove overflow buttons from AOSP Clock and Phone apps and re-bind associated software menus to hardware menu key
//TODO: Possible GUI allowing the user to choose which apps the overflow button is whitelisted from

public class OverflowHidden implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private final int devicePlatformVersion = Build.VERSION.SDK_INT;

    @Override
    public void initZygote(final StartupParam sp) throws Throwable {
        if (devicePlatformVersion >= 4.0) {
            try {
                XposedHelpers.findAndHookMethod(ViewConfiguration.class, "hasPermanentMenuKey", XC_MethodReplacement.returnConstant(true));
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            try {
                final Class clsLayoutParams = WindowManager.LayoutParams.class;

                XposedHelpers.setStaticIntField(clsLayoutParams, "FLAG_NEEDS_MENU_KEY", 0);
                XposedHelpers.setStaticIntField(clsLayoutParams, "PRIVATE_FLAG_SET_NEEDS_MENU_KEY", 0);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        if (devicePlatformVersion >= 4.1) {
            try {
                final Class<?> clsActionBarPolicy = XposedHelpers.findClass("com.android.internal.view.ActionBarPolicy", null);

                XposedHelpers.findAndHookMethod(clsActionBarPolicy, "showsOverflowMenuButton", XC_MethodReplacement.returnConstant(false));
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpp) throws Throwable {
        final String pkgBrowserAOSP = "com.android.browser";
        final String pkgCalculatorAOSP = "com.android.calculator2";

        switch (lpp.packageName) {
            case pkgBrowserAOSP:
                if (devicePlatformVersion >= 4.0) {
                    try {
                        XposedHelpers.findAndHookMethod(pkgBrowserAOSP + ".NavScreen", lpp.classLoader, "init", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(final MethodHookParam mhp) {
                                ImageButton ibOverflowButton = (ImageButton) XposedHelpers.getObjectField(mhp.thisObject, "mMore");
                                ibOverflowButton.setVisibility(View.GONE);
                            }
                        });
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            break;

            case pkgCalculatorAOSP:
                if (devicePlatformVersion >= 4.0) {
                    try {
                        XposedHelpers.findAndHookMethod(pkgCalculatorAOSP + ".Calculator", lpp.classLoader, "createFakeMenu", XC_MethodReplacement.DO_NOTHING);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            break;
        }

        //TODO: Check for FC-prone packages (e.g. Clock app on TouchWiz) and implement workarounds inside appropriate methods
        //TODO: Remove overflow button in Wakelock Detector (and /possibly/ Nova Launcher) proprietary app
    }
}