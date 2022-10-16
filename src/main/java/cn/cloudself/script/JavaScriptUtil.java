package cn.cloudself.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaScriptUtil {
    private static final JavaScript instance = new JavaScript(CommonFunctionsForScript.INSTANCE);

    public static JavaScript.Prepared of(String script) {
        return instance.of(script);
    }

    public static JavaScript.Prepared compile(String script) {
        return instance.compile(script);
    }

    public static JavaScript.PreparedBatch ofBatch(@Nullable String script, @NotNull String result) {
        return instance.ofBatch(script, result);
    }
}
