package com.wix.pagedcontacts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.BaseReactPackage;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.uimanager.ViewManager;
import com.wix.pagedcontacts.contacts.permission.RequestPermissionsResultCallback;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PagedContactsPackage extends BaseReactPackage {

    private final RequestPermissionsResultCallback permissionsResultCallback;

    public PagedContactsPackage() {
        permissionsResultCallback = new RequestPermissionsResultCallback();
    }

    public PagedContactsPackage(RequestPermissionsResultCallback permissionsResultCallback) {
        this.permissionsResultCallback = permissionsResultCallback;
    }

    @Nullable
    @Override
    public NativeModule getModule(@NonNull String name, @NonNull ReactApplicationContext reactContext) {
        if (name.equals(PagedContactsModule.NAME)) {
            PagedContactsModule pagedContactsModule = new PagedContactsModule(reactContext);
            permissionsResultCallback.setModule(pagedContactsModule);
            return pagedContactsModule;
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
            boolean isTurboModule = true;
            moduleInfos.put(
                    PagedContactsModule.NAME,
                    new ReactModuleInfo(
                            PagedContactsModule.NAME,
                            PagedContactsModule.NAME,
                            false, // canOverrideExistingModule
                            false, // needsEagerInit
                            true, // hasConstants
                            false, // isCxxModule
                            isTurboModule // isTurboModule
            ));
            return moduleInfos;
        };
    }
}