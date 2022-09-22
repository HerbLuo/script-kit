package cn.cloudself.script;

public class JavaScriptUtil {
    private static final JavaScript instance = new JavaScript(CommonFunctionsForScript.INSTANCE);

    public static JavaScript.Prepared of(String script) {
        return instance.of(script);
    }

    public static JavaScript.Prepared compile(String script) {
        return instance.compile(script);
    }
}
