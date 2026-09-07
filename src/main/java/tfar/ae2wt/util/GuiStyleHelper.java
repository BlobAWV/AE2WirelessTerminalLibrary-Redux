package tfar.ae2wt.util;

import appeng.client.gui.me.common.MEMonitorableScreen;
import appeng.client.gui.style.TerminalStyle;

import java.lang.reflect.Field;

public class GuiStyleHelper {

    private static final Field styleField;

    static {
        try {
            styleField = MEMonitorableScreen.class.getDeclaredField("style");
            styleField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access MEMonitorableScreen.style", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public static TerminalStyle getGuiStyle(MEMonitorableScreen screen) {
        try {
            return (TerminalStyle) styleField.get(screen);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
