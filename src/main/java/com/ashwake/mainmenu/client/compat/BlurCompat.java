package com.ashwake.mainmenu.client.compat;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

public final class BlurCompat {
    private static final Set<String> KNOWN_PROVIDER_MODIDS = Set.of(
            "blur",
            "blurplus",
            "blurify",
            "fancymenu");

    private static final Set<String> LOGGED_PROVIDER_FAILURES = ConcurrentHashMap.newKeySet();
    private static final Deque<BlurSnapshot> SNAPSHOTS = new ArrayDeque<>();

    private static boolean initialized;
    private static int pushDepth;

    private BlurCompat() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        detectKnownProviders();
        if (AshwakeClientConfig.disableMenuBlurGlobally()) {
            tryDisableBlurEverywhere("init");
        }
    }

    public static void onAshwakeScreenOpen() {
        if (!AshwakeClientConfig.forceSharpBackground() || !AshwakeClientConfig.disableBlurOnAshwakeScreens()) {
            return;
        }

        if (AshwakeClientConfig.disableMenuBlurGlobally()) {
            tryDisableBlurEverywhere("screen-open-global");
            return;
        }

        if (pushDepth == 0) {
            SNAPSHOTS.push(captureSnapshot());
        }
        pushDepth++;
        tryDisableBlurEverywhere("screen-open");
    }

    public static void onAshwakeScreenClose() {
        if (!AshwakeClientConfig.disableBlurOnAshwakeScreens()) {
            return;
        }
        if (AshwakeClientConfig.disableMenuBlurGlobally()) {
            return;
        }
        if (pushDepth <= 0) {
            return;
        }

        pushDepth--;
        if (pushDepth == 0 && !SNAPSHOTS.isEmpty()) {
            restoreSnapshot(SNAPSHOTS.pop());
        }
    }

    private static void detectKnownProviders() {
        Set<String> detected = new HashSet<>();
        for (String modId : KNOWN_PROVIDER_MODIDS) {
            if (ModList.get().isLoaded(modId)) {
                detected.add(modId);
            }
        }
        if (!detected.isEmpty()) {
            AshwakeMainMenuMod.LOGGER.info("Detected blur providers: {}", detected);
        }
    }

    private static void tryDisableBlurEverywhere(String reason) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }

        try {
            disableOptionBasedBlur(minecraft.options);
        } catch (Throwable throwable) {
            logProviderFailure("options-disable", throwable);
        }

        try {
            disablePostEffectChain(minecraft);
        } catch (Throwable throwable) {
            logProviderFailure("post-effect-disable", throwable);
        }
    }

    private static BlurSnapshot captureSnapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return new BlurSnapshot(List.of(), List.of());
        }
        return new BlurSnapshot(
                captureOptionFieldSnapshots(minecraft.options),
                captureOptionObjectSnapshots(minecraft.options));
    }

    private static void restoreSnapshot(BlurSnapshot snapshot) {
        for (OptionFieldSnapshot fieldSnapshot : snapshot.optionFieldSnapshots()) {
            try {
                fieldSnapshot.field().setAccessible(true);
                fieldSnapshot.field().set(fieldSnapshot.owner(), fieldSnapshot.value());
            } catch (Throwable throwable) {
                logProviderFailure("restore-field-" + fieldSnapshot.field().getName(), throwable);
            }
        }

        for (OptionObjectSnapshot optionSnapshot : snapshot.optionObjectSnapshots()) {
            try {
                optionSnapshot.setMethod().invoke(optionSnapshot.optionObject(), optionSnapshot.value());
            } catch (Throwable throwable) {
                logProviderFailure("restore-option-" + optionSnapshot.optionObject().getClass().getName(), throwable);
            }
        }
    }

    private static void disableOptionBasedBlur(Object optionsObject) {
        for (Field field : collectDeclaredFields(optionsObject.getClass())) {
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("blur")) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(optionsObject);
                if (value == null) {
                    continue;
                }

                Class<?> type = field.getType();
                if (type == boolean.class || type == Boolean.class) {
                    field.set(optionsObject, false);
                    continue;
                }
                if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                    field.set(optionsObject, zeroForType(type));
                    continue;
                }

                disableOptionLikeObject(value);
            } catch (Throwable throwable) {
                logProviderFailure("disable-field-" + field.getName(), throwable);
            }
        }
    }

    private static List<OptionFieldSnapshot> captureOptionFieldSnapshots(Object optionsObject) {
        List<OptionFieldSnapshot> snapshots = new ArrayList<>();
        for (Field field : collectDeclaredFields(optionsObject.getClass())) {
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("blur")) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(optionsObject);
                if (value == null) {
                    continue;
                }
                Class<?> type = field.getType();
                if (type == boolean.class || type == Boolean.class || Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                    snapshots.add(new OptionFieldSnapshot(optionsObject, field, value));
                }
            } catch (Throwable throwable) {
                logProviderFailure("snapshot-field-" + field.getName(), throwable);
            }
        }
        return snapshots;
    }

    private static List<OptionObjectSnapshot> captureOptionObjectSnapshots(Object optionsObject) {
        List<OptionObjectSnapshot> snapshots = new ArrayList<>();
        for (Field field : collectDeclaredFields(optionsObject.getClass())) {
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("blur")) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(optionsObject);
                if (value == null) {
                    continue;
                }
                Method getMethod = findNoArgMethod(value.getClass(), "get");
                Method setMethod = findSingleArgMethod(value.getClass(), "set");
                if (getMethod == null || setMethod == null) {
                    continue;
                }
                Object currentValue = getMethod.invoke(value);
                if (currentValue == null) {
                    continue;
                }
                snapshots.add(new OptionObjectSnapshot(value, setMethod, currentValue));
            } catch (Throwable throwable) {
                logProviderFailure("snapshot-option-" + field.getName(), throwable);
            }
        }
        return snapshots;
    }

    private static void disableOptionLikeObject(Object optionObject) {
        try {
            Method getMethod = findNoArgMethod(optionObject.getClass(), "get");
            Method setMethod = findSingleArgMethod(optionObject.getClass(), "set");
            if (getMethod == null || setMethod == null) {
                return;
            }

            Object oldValue = getMethod.invoke(optionObject);
            if (oldValue instanceof Boolean) {
                setMethod.invoke(optionObject, false);
                return;
            }
            if (oldValue instanceof Number number) {
                setMethod.invoke(optionObject, zeroForNumber(number));
            }
        } catch (Throwable throwable) {
            logProviderFailure("disable-option-object-" + optionObject.getClass().getName(), throwable);
        }
    }

    private static void disablePostEffectChain(Minecraft minecraft) {
        Object renderer = getFieldValueByName(minecraft, "gameRenderer");
        if (renderer == null) {
            return;
        }
        Method shutdownEffect = findNoArgMethod(renderer.getClass(), "shutdownEffect");
        if (shutdownEffect != null) {
            try {
                shutdownEffect.invoke(renderer);
            } catch (Throwable throwable) {
                logProviderFailure("shutdownEffect", throwable);
            }
        }
    }

    private static Object zeroForType(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) 0;
        }
        if (type == short.class || type == Short.class) {
            return (short) 0;
        }
        if (type == int.class || type == Integer.class) {
            return 0;
        }
        if (type == long.class || type == Long.class) {
            return 0L;
        }
        if (type == float.class || type == Float.class) {
            return 0F;
        }
        if (type == double.class || type == Double.class) {
            return 0D;
        }
        return 0;
    }

    private static Object zeroForNumber(Number number) {
        if (number instanceof Byte) {
            return (byte) 0;
        }
        if (number instanceof Short) {
            return (short) 0;
        }
        if (number instanceof Integer) {
            return 0;
        }
        if (number instanceof Long) {
            return 0L;
        }
        if (number instanceof Float) {
            return 0F;
        }
        if (number instanceof Double) {
            return 0D;
        }
        return 0;
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Method findSingleArgMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Object getFieldValueByName(Object owner, String fieldName) {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable throwable) {
                logProviderFailure("field-access-" + fieldName, throwable);
                return null;
            }
        }
        return null;
    }

    private static List<Field> collectDeclaredFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> cursor = type;
        while (cursor != null) {
            for (Field field : cursor.getDeclaredFields()) {
                fields.add(field);
            }
            cursor = cursor.getSuperclass();
        }
        return fields;
    }

    private static void logProviderFailure(String key, Throwable throwable) {
        if (LOGGED_PROVIDER_FAILURES.add(key)) {
            AshwakeMainMenuMod.LOGGER.debug("BlurCompat fallback for [{}] failed once; continuing safely.", key, throwable);
        }
    }

    private record BlurSnapshot(
            List<OptionFieldSnapshot> optionFieldSnapshots,
            List<OptionObjectSnapshot> optionObjectSnapshots) {
    }

    private record OptionFieldSnapshot(
            Object owner,
            Field field,
            Object value) {
    }

    private record OptionObjectSnapshot(
            Object optionObject,
            Method setMethod,
            Object value) {
    }
}
