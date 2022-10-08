package cn.cloudself.script;

import cn.cloudself.script.util.LogFactory;
import cn.cloudself.script.util.Ref;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JavaScript {

    public static String JavaFunctionName = "Jv_FN";
    public static boolean doesRuntimeCompilationSupported = false;

    private static final ThreadLocal<Boolean> disableLog = ThreadLocal.withInitial(() -> false);
    public static void disableLogThreadLocal(Boolean disable) {
        disableLog.set(disable);
    }

    private static final LogFactory.Log log = LogFactory.getLog(CommonFunctionsForScript.class);

    private final Context.Builder builder = Context
            .newBuilder()
            .allowAllAccess(true)
            .option("engine.WarnInterpreterOnly", doesRuntimeCompilationSupported + "");
    private final Object functions;
    private final String functionsHelper;

    /**
     * @param functions 公共方法 参考CommonFunctionsForScript，也可以继承或扩展该类。
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
         * @param vars 变量 map
         *             会自动将long范围内的BigDecimal转成double
         *             会自动将long范围内的BigInteger转成long
         *             如希望阻止该行为，使用<code>Ref.of(BigDecimal)</code>，js端直接使用<code>BigDecimal</code>的 <code>.add()</code><code>.subtract()</code>访问该对象
         *
         * @return Java对象，如下表
         * <ul>
         *     <li><code><pre>undefined    -> null  </pre></code></li>
         *     <li><code><pre>string       -> String</pre></code></li>
         *     <li><code><pre>number       -> Number</pre></code></li>
         *     <li><code><pre>[]           -> Map   </pre></code></li>
         *     <li><code><pre>{}           -> Map   </pre></code></li>
         *     <li><code><pre>new Date()   -> LocalDateTime </pre></code></li>
         *     <li><code><pre>function(){} -> Function      </pre></code></li>
         *     <li><code><pre>[{}]         -> ArrayList.get(0) instanceof HashMap</code></li>
         * </ul>
         */
        public Object eval(Map<String, ?> vars) {
            return eval(vars.entrySet().iterator(), this::toJavaObject);
        }

        public Object eval() {
            return eval(Collections.emptyIterator(), this::toJavaObject);
        }

        public <T> T eval(Map<String, ?> vars, Class<T> resultType) {
            return eval(vars.entrySet().iterator(), v -> v.as(resultType));
        }

        /**
         * 支持 new TypeLiteral<Map<String, Object>>() { }
         * 但不支持 new TypeLiteral<HashMap<String, Object>>() { }
         *
         */
        public <T> T eval(Map<String, ?> vars, TypeLiteral<T> resultType) {
            return eval(vars.entrySet().iterator(), v ->  v.as(resultType));
        }

        public <T> T eval(Iterator<? extends Map.Entry<String, ?>> vars, TypeLiteral<T> resultType) {
            return eval(vars, v ->  v.as(resultType));
        }

        private <T> T eval(Iterator<? extends Map.Entry<String, ?>> vars, Function<Value, T> resultHandler) {
            try (final Context context = builder.build()) {
                final Value bindings = context.getBindings("js");
                bindings.putMember(JavaFunctionName, functions);
                if (vars != null) {
                    toJsObject(vars, bindings);
                }
                final Boolean d = disableLog.get();
                if (!d) {
                    log.info(source.getCharacters());
                }
                T result = resultHandler.apply(context.eval(source));
                if (result instanceof List) {
                    //noinspection unchecked
                    result = (T) new ArrayList<>((List<?>) result);
                }
                if (result instanceof Map) {
                    //noinspection unchecked
                    result = (T) new HashMap<>((Map<?, ?>) result);
                }

                if (!d) {
                    log.info("eval result: " + result);
                }
                return result;
            }
        }

        private void toJsObject(@NotNull Iterator<? extends Map.Entry<String, ?>> vars, Value bindings) {
            while (vars.hasNext()) {
                final Map.Entry<String, ?> entry = vars.next();
                final String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Ref<?>) {
                    value = ((Ref<?>) value).getValue();
                } else if (value instanceof BigDecimal) {
                    final BigDecimal bigDecimal = (BigDecimal) value;
                    if (bigDecimal.compareTo(maxLongValueBigDecimalView) < 0) {
                        value = bigDecimal.doubleValue();
                    }
                } else if (value instanceof BigInteger) {
                    final BigInteger bigInteger = (BigInteger) value;
                    if (bigInteger.compareTo(maxLongValueBigIntegerView) < 0) {
                        value = bigInteger.longValue();
                    }
                }
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
        final String finalScript = finalScriptBuilder.toString();
        final Source source = Source
                .newBuilder("js", finalScript, null)
                .cached(cache)
                .buildLiteral();
        return new Prepared(source);
    }

    private final static BigInteger maxLongValueBigIntegerView = new BigInteger(String.valueOf(Long.MAX_VALUE));
    private final static BigDecimal maxLongValueBigDecimalView = new BigDecimal(Long.MAX_VALUE);
}
