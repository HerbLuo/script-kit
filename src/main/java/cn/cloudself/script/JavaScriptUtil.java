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

    /**
     * @param resultExpression 返回结果表达式，必须为一个表达式
     */
    public static JavaScript.PreparedBatch ofBatch(@NotNull String resultExpression) {
        return instance.ofBatch(resultExpression);
    }

    /**
     * @param sharedScript 共用代码
     * @param resultExpression 返回结果表达式，必须为一个表达式
     */
    public static JavaScript.PreparedBatch ofBatch(@Nullable String sharedScript, @NotNull String resultExpression) {
        return instance.ofBatch(sharedScript, resultExpression);
    }

    /**
     * @param sharedScript 共用代码
     * @param calcStatement 计算语句
     * @param resultExpression 返回结果表达式，必须为一个表达式
     */
    public static JavaScript.PreparedBatch ofBatch(@Nullable String sharedScript, @NotNull String calcStatement, @NotNull String resultExpression) {
        return instance.ofBatch(sharedScript, calcStatement, resultExpression);
    }
}
