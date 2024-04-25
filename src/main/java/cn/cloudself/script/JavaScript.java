package cn.cloudself.script;

import cn.cloudself.script.util.LogFactory;
import cn.cloudself.script.util.Ref;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JavaScript {

    public static String JavaFunctionName = "Jv_FN";
    public static boolean doesRuntimeCompilationSupported = false;

    private static final ThreadLocal<Boolean> disableLog = ThreadLocal.withInitial(() -> false);
    public static boolean logDisabled() {
        return disableLog.get();
    }
    public static void disableLogThreadLocal(Boolean disable) {
        disableLog.set(disable);
    }

    private static final LogFactory.Log log = LogFactory.getLog(CommonFunctionsForScript.class);

    private final Context.Builder builder = Context
            .newBuilder()
            .allowAllAccess(true)
            .option("js.foreign-object-prototype", "true")
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

    public class PreparedBatch {
        private final String sharedScript;
        private final String calcStatement;
        private final String resultExpression;

        private PreparedBatch(String sharedScript, String calcStatement, String resultExpression) {
            this.sharedScript = sharedScript;
            this.calcStatement = calcStatement;
            this.resultExpression = resultExpression;
        }

        public <T> List<T> evalBatch(Iterable<String> varNames, Iterable<? extends Iterator<? extends Map.Entry<String, ?>>> varsBatch) {
            return evalBatch(varNames, varsBatch, null);
        }

        /**
         * 例如 ofBatch("const a_number = 10;", "a + b * c - a_number")
         *
         * @param varNames a b c
         * @param varsBatch JSON.parse([{a:1,b:2,c:3},{a:10,b:20,c:30}])  .map(vars -> vars.entrySet().iterator())
         * @param resultType <pre><code>new TypeLiteral&lt;Map&lt;String, Object>>() { }</code></pre>
         * @return <pre><code>List&lt;TypeLiteral&lt;Map&lt;String, Object>>() { }></code></pre>
         */
        public <T> List<T> evalBatch(
                Iterable<String> varNames,
                Iterable<? extends Iterator<? extends Map.Entry<String, ?>>> varsBatch,
                TypeLiteral<T> resultType
        ) {
            final StringBuilder finalScriptBuilder = new StringBuilder();
            finalScriptBuilder.append(functionsHelper);
            finalScriptBuilder.append("\nconst _var_names = [...varNames];\n");
            if (sharedScript != null) {
                final String trimmedSharedScript = sharedScript.trim();
                finalScriptBuilder.append(trimmedSharedScript);
                if (!trimmedSharedScript.endsWith(";")) {
                    finalScriptBuilder.append(';');
                }
                finalScriptBuilder.append('\n');
            }
            finalScriptBuilder.append("function _calc(");
            for (String varName : varNames) {
                finalScriptBuilder.append(varName);
                finalScriptBuilder.append(", ");
            }
            finalScriptBuilder.append(") { ");
            if (calcStatement != null) {
                final String trimmedCalcStatement = calcStatement.trim();
                finalScriptBuilder.append(trimmedCalcStatement);
                if (!trimmedCalcStatement.endsWith(";")) {
                    finalScriptBuilder.append(';');
                }
                finalScriptBuilder.append('\n');
            }
            finalScriptBuilder.append("\nreturn ");
            finalScriptBuilder.append(resultExpression.trim());
            finalScriptBuilder.append(";\n}\n");
            finalScriptBuilder.append("const _results = [];\n");
            finalScriptBuilder.append("for (const vars of varsBatch) { _results.push(_calc.apply(null, _var_names.map(n => vars[n]))) }\n");
            finalScriptBuilder.append("_results;");
            final String finalScript = finalScriptBuilder.toString();

            final Map<String, Object> vars = new HashMap<>();
            vars.put("varNames", varNames);
            final List<Map<String, Object>> varsBatchConverted = StreamSupport.stream(varsBatch.spliterator(), false).map(entries -> {
                final Map<String, Object> var = new HashMap<>();
                while (entries.hasNext()) {
                    final Map.Entry<String, ?> entry = entries.next();
                    var.put(entry.getKey(), JavaScript.toJsObject(entry.getValue()));
                }
                return var;
            }).collect(Collectors.toList());
            vars.put("varsBatch",  varsBatchConverted);

            final Source source = Source.create("js", finalScript);
            final List<Object> results = new Prepared(source)
                    .addValueTranslator(value -> value.as(new TypeLiteral<List<Value>>() { }))
                    .evalAsList(vars.entrySet().iterator());
            //noinspection unchecked
            return (List<T>) results;
        }
    }

    private static Object toJsObject(Object value) {
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
        return value;
    }

    /**
     * <li>If the value has {@link #hasArrayElements() array elements} and it has an
     * {@link Value#getArraySize() array size} that is smaller or equal than
     * {@link Integer#MAX_VALUE} then the result value will implement {@link List}. Every array
     * element of the value maps to one list element. The size of the returned list maps to the
     * array size of the value.
     *
     * <li>If the value has {@link #hasHashEntries() hash entries} then the result value will
     * implement {@link Map}. The {@link Map#size() size} of the returned {@link Map} is equal to
     * the {@link #getHashSize() hash entries count}. The returned value may also implement
     * {@link Function} if the value can be {@link #canExecute() executed} or
     * {@link #canInstantiate() instantiated}.
     *
     *
     * <li>If the value {@link #hasMembers() has members} then the result value will implement
     * {@link Map}. If this value {@link #hasMembers() has members} then all members are accessible
     * using {@link String} keys. The {@link Map#size() size} of the returned {@link Map} is equal
     * to the count of all members. The returned value may also implement {@link Function} if the
     * value can be {@link #canExecute() executed} or {@link #canInstantiate() instantiated}.
     *
     *
     * <li>If the value has an {@link #hasIterator()} iterator} then the result value will implement
     * {@link Iterable}. The returned value may also implement {@link Function} if the value can be
     * {@link #canExecute() executed} or {@link #canInstantiate() instantiated}.
     *
     * <li>If the value is an {@link #isIterator()} iterator} then the result value will implement
     * {@link Iterator}. The returned value may also implement {@link Function} if the value can be
     * {@link #canExecute() executed} or {@link #canInstantiate() instantiated}.
     */
    private static <T> T transResult(T result, @NotNull Function<Value, Object> itemTranslator, @NotNull Function<Value, Object> finalTranslator) {
        if (result instanceof List) {
            //noinspection unchecked
            return (T) ((List<?>) result).stream().map(it -> transResult(it, itemTranslator, finalTranslator)).collect(Collectors.toList());
        }
        if (result instanceof Map) {
            @SuppressWarnings("rawtypes") final HashMap hashMap = new HashMap<>((Map<?, ?>) result);
            //noinspection unchecked
            hashMap.replaceAll((k, v) -> transResult(v, itemTranslator, finalTranslator));
            //noinspection unchecked
            return (T) hashMap;
        }
        if (result instanceof Value) {
            final Value value = (Value) result;
            Object tempResult = itemTranslator.apply(value);
            if ((tempResult instanceof Value)) {
                final Value step1value = (Value) tempResult;
                if (step1value.hasArrayElements()) {
                    tempResult = step1value.as(new TypeLiteral<List<Value>>() { });
                } else if (step1value.hasHashEntries() || step1value.hasMembers()) {
                    tempResult = step1value.as(new TypeLiteral<Map<String, Value>>() { });
                } else {
                    tempResult = finalTranslator.apply(step1value);
                }
            }
            //noinspection unchecked
            return (T) transResult(tempResult, itemTranslator, finalTranslator);
        }
        return result;
    }


    private static Object finalTranslator(Value value) {
        return value.as(Object.class);
    }

    private static Object baseTranslator(Value value) {
        if (value.isNull()) {
            return null;
        }
        if (value.isHostObject()) {
            return value.asHostObject();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.as(Object.class);
        }
        return value;
    }

    private static Object dateTimeTranslator(Value value) {
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
        return value;
    }

    public class Prepared {
        private final Source source;
        private final Stack<Function<Value, Object>> translators = new Stack<>();

        private Prepared(Source source) {
            this.source = source;
            translators.add(JavaScript::baseTranslator);
            translators.add(JavaScript::dateTimeTranslator);
        }

        /**
         * 添加值转换器，value不会是List或者Map类型，针对List或者Map，会根据值的数量，调用多次转换器
         */
        public Prepared addValueTranslator(Function<Value, Object> itemTranslator) {
            translators.add(itemTranslator);
            return this;
        }

        /**
         * 执行JS
         * @return Java对象，如下表
         */
        public Map<String, Object> evalAsMap() {
            return evalAsMap(Collections.emptyIterator());
        }

        /**
         * 执行JS
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
        public Map<String, Object> evalAsMap(Map<String, ?> vars) {
            return evalAsMap(vars.entrySet().iterator());
        }

        /**
         * 执行JS
         * @param vars 变量 可通过map.entrySet().iterator() 获得
         *             会自动将long范围内的BigDecimal转成double
         *             会自动将long范围内的BigInteger转成long
         *             如希望阻止该行为，使用<code>Ref.of(BigDecimal)</code>，js端直接使用<code>BigDecimal</code>的 <code>.add()</code><code>.subtract()</code>访问该对象
         */
        public Map<String, Object> evalAsMap(Iterator<? extends Map.Entry<String, ?>> vars) {
            //noinspection unchecked,rawtypes
            return (Map) eval(vars, v -> v.as(new TypeLiteral<Map<String, Value>>() { }), true);
        }

        public void exec() {
            exec(Collections.emptyIterator());
        }

        public void exec(Iterator<? extends Map.Entry<String, ?>> vars) {
            eval(vars, v -> null, false);
        }

        /**
         * 执行JS
         * @return Java对象，如下表
         */
        public List<Object> evalAsList() {
            return evalAsList(Collections.emptyIterator());
        }

        /**
         * 执行JS
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
        public List<Object> evalAsList(Map<String, ?> vars) {
            return evalAsList(vars.entrySet().iterator());
        }

        /**
         * 执行JS
         * @param vars 变量 可通过map.entrySet().iterator() 获得
         *             会自动将long范围内的BigDecimal转成double
         *             会自动将long范围内的BigInteger转成long
         *             如希望阻止该行为，使用<code>Ref.of(BigDecimal)</code>，js端直接使用<code>BigDecimal</code>的 <code>.add()</code><code>.subtract()</code>访问该对象
         */
        public List<Object> evalAsList(Iterator<? extends Map.Entry<String, ?>> vars) {
            //noinspection unchecked,rawtypes
            return (List) eval(vars, v -> v.as(new TypeLiteral<List<Value>>() { }), true);
        }

        /**
         * 执行JS
         * @param vars 变量 map
         *             会自动将long范围内的BigDecimal转成double
         *             会自动将long范围内的BigInteger转成long
         *             如希望阻止该行为，使用<code>Ref.of(BigDecimal)</code>，js端直接使用<code>BigDecimal</code>的 <code>.add()</code><code>.subtract()</code>访问该对象
         */
        public <T> T eval(Map<String, ?> vars, Class<T> resultType) {
            return eval(vars.entrySet().iterator(), resultType);
        }

        /**
         * 执行JS
         * @param vars 变量 可通过map.entrySet().iterator() 获得
         *             会自动将long范围内的BigDecimal转成double
         *             会自动将long范围内的BigInteger转成long
         *             如希望阻止该行为，使用<code>Ref.of(BigDecimal)</code>，js端直接使用<code>BigDecimal</code>的 <code>.add()</code><code>.subtract()</code>访问该对象
         */
        public <T> T eval(Iterator<? extends Map.Entry<String, ?>> vars, Class<T> resultType) {
            return eval(vars, v -> v.as(resultType), false);
        }

        /**
         * 执行JS
         * @param vars 变量 map
         *             会自动将long范围内的BigDecimal转成double
         *             会自动将long范围内的BigInteger转成long
         *             如希望阻止该行为，使用<code>Ref.of(BigDecimal)</code>，js端直接使用<code>BigDecimal</code>的 <code>.add()</code><code>.subtract()</code>访问该对象
         * @param resultType 1. 针对Map或者List，建议使用evalAsMap或evalAsList。
         *                   2. 支持 new TypeLiteral&lt;Map&lt;?, ?>>() { }
         *                   但不支持 new TypeLiteral&lt;HashMap&lt;?, ?>>() { }
         */
        public <T> T eval(Map<String, ?> vars, TypeLiteral<T> resultType) {
            return eval(vars.entrySet().iterator(), resultType);
        }

        /**
         * 执行JS
         * @param vars 变量 可通过map.entrySet().iterator() 获得
         *             会自动将long范围内的BigDecimal转成double
         *             会自动将long范围内的BigInteger转成long
         *             如希望阻止该行为，使用<code>Ref.of(BigDecimal)</code>，js端直接使用<code>BigDecimal</code>的 <code>.add()</code><code>.subtract()</code>访问该对象
         * @param resultType 1. 针对Map或者List，建议使用evalAsMap或evalAsList。
         *                   2. 支持new TypeLiteral&lt;Map&lt;?, ?>>() { }
         *                   但不支持 new TypeLiteral&lt;HashMap&lt;?, ?>>() { }
         */
        public <T> T eval(Iterator<? extends Map.Entry<String, ?>> vars, TypeLiteral<T> resultType) {
            return eval(vars, v -> v.as(resultType), false);
        }

        <T> T eval(Iterator<? extends Map.Entry<String, ?>> vars, Function<Value, T> resultHandler, boolean translateValueToObject) {
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

                final Value step0result = context.eval(source);
                final T step1result = resultHandler.apply(step0result);
                final T result = transResult(step1result, v -> {
                    if (!translateValueToObject) {
                        return v;
                    }
                    for (Function<Value, Object> translator : translators) {
                        final Object r = translator.apply(v);
                        if (!(r instanceof Value)) {
                            return r;
                        }
                        v = (Value) r;
                    }
                    return v;
                }, JavaScript::finalTranslator);
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
                value = JavaScript.toJsObject(value);
                bindings.putMember(key, value);
            }
        }
    }

    public Prepared of(String script) {
        return prepare(script, false);
    }

    public Prepared compile(String script) {
        return prepare(script, true);
    }

    /**
     * @param resultExpression 返回结果表达式，必须为一个表达式
     */
    public PreparedBatch ofBatch(@NotNull String resultExpression) {
        return new PreparedBatch(null, null, resultExpression);
    }

    /**
     * @param sharedScript 共用代码
     * @param resultExpression 返回结果表达式，必须为一个表达式
     */
    public PreparedBatch ofBatch(@Nullable String sharedScript, @NotNull String resultExpression) {
        return new PreparedBatch(sharedScript, null, resultExpression);
    }

    /**
     * @param sharedScript 共用代码
     * @param calcStatement 计算语句
     * @param resultExpression 返回结果表达式，必须为一个表达式
     */
    public PreparedBatch ofBatch(@Nullable String sharedScript, @NotNull String calcStatement, @NotNull String resultExpression) {
        return new PreparedBatch(sharedScript, calcStatement, resultExpression);
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
