package cn.cloudself.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JavaScript {
    public static String JavaFunctionName = "Jv_FN";
    public static boolean doesRuntimeCompilationSupported = false;

    private final Context.Builder builder = Context
            .newBuilder()
            .allowAllAccess(true)
            .option("engine.WarnInterpreterOnly", doesRuntimeCompilationSupported + "");
    private final Object functions;
    private final String functionsHelper;

    /**
     * @param functions 公共方法
     */
    public JavaScript(Object functions) {
        this.functions = functions;
        this.functionsHelper = Arrays.stream(functions.getClass().getDeclaredMethods())
            .map(jf -> "const " + jf.getName() + "=s=>" + JavaFunctionName + "." + jf.getName() + "(s);")
            .collect(Collectors.joining());
    }

    public class Prepared {
        private final Source source;
        private Prepared(Source source) {
            this.source = source;
        }

        /**
         * @param vars 变量
         * @return Java对象，如下表
         * <ul>
         *     <li><code><pre>undefined    -> null  </pre></code></li>
         *     <li><code><pre>string       -> String</pre></code></li>
         *     <li><code><pre>number       -> Number</pre></code></li>
         *     <li><code><pre>[]           -> Map   </pre></code></li>
         *     <li><code><pre>{}           -> Map   </pre></code></li>
         *     <li><code><pre>new Date()   -> LocalDateTime </pre></code></li>
         *     <li><code><pre>function(){} -> Function      </pre></code></li>
         *     <li><code><pre>[{}]         -> Map.get(0) instanceof Map</code></li>
         * </ul>
         */
        public Object eval(Map<String, ?> vars) {
            return eval(vars, this::toJavaObject);
        }

        public <T> T eval(Map<String, ?> vars, Class<T> resultType) {
            return eval(vars, v -> v.as(resultType));
        }

        public <T> T eval(Map<String, ?> vars, TypeLiteral<T> resultType) {
            return eval(vars, v -> v.as(resultType));
        }

        private <T> T eval(Map<String, ?> vars, Function<Value, T> resultHandler) {
            try (final Context context = builder.build()) {
                final Value bindings = context.getBindings("js");
                bindings.putMember(JavaFunctionName, functions);
                if (vars != null) {
                    toJsObject(vars, bindings);
                }
                return resultHandler.apply(context.eval(source));
            }
        }

        private void toJsObject(Map<String, ?> vars, Value bindings) {
            for (Map.Entry<String, ?> entry : vars.entrySet()) {
                final String key = entry.getKey();
                Object value = entry.getValue();
                bindings.putMember(key, value);
            }
        }

        private Object toJavaObject(Value value) {
            LocalDate date = null;
            LocalTime time = null;
            if (value.isDate()) {
                date = value.asDate();
            }
            if (value.isTime()) {
                time = value.asTime();
            }
            if (date != null) {
                if (time != null) {
                    return LocalDateTime.of(date, time);
                }
                return date;
            }
            if (time != null) {
                return time;
            }

            return value.as(Object.class);
        }
    }

    public Prepared of(String script) {
        return prepare(script, false);
    }

    public Prepared compile(String script) {
        return prepare(script, true);
    }

    private Prepared prepare(String script, Boolean cache) {
        final StringBuilder finalScriptBuilder = new StringBuilder();
        finalScriptBuilder.append(functionsHelper);
        if (script.startsWith("{")) {
            finalScriptBuilder.append('(');
            finalScriptBuilder.append(script);
            finalScriptBuilder.append(')');
        } else {
            finalScriptBuilder.append(script);
        }
        final Source source = Source
                .newBuilder("js", finalScriptBuilder.toString(), null)
                .cached(cache)
                .buildLiteral();
        return new Prepared(source);
    }
}
