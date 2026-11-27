package com.merchantassistant.store;

public final class StoreContext {
    public static final long DEFAULT_STORE_ID = 1L;
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private StoreContext() { }

    public static long get() { return CURRENT.get() == null ? DEFAULT_STORE_ID : CURRENT.get(); }
    public static void set(long storeId) { CURRENT.set(storeId); }
    public static void clear() { CURRENT.remove(); }
}
