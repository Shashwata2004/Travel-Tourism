package com.travel.frontend.controller;

import java.util.UUID;

public final class AdminPackagesState {
    private static String packageId;
    private static String packageName;

    private AdminPackagesState() {}

    public static void set(String id, String name) {
        packageId = id;
        packageName = name;
    }

    public static String getPackageId() { return packageId; }
    public static String getPackageName() { return packageName; }

    public static void clear() {
        packageId = null;
        packageName = null;
    }
}
