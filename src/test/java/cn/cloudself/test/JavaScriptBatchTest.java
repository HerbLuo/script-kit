package cn.cloudself.test;

import cn.cloudself.script.JavaScript;
import cn.cloudself.script.JavaScriptUtil;
import cn.cloudself.test.helper.Helper;
import org.graalvm.polyglot.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

public class JavaScriptBatchTest {
    @Test
    public void testBatch() {
        Helper.initLogger();

        final List<Map<String, Object>> varsList = new ArrayList<Map<String, Object>>() {{
            add(MapBuilder.createAndPut("a", (Object) 88).put("b", 888).build());
            add(MapBuilder.createAndPut("a", (Object) 880).put("b", 8880).build());
        }};
        final List<String> varNames = ListBuilder.create("a", "b");
        final JavaScript.PreparedBatch preparedBatch = JavaScriptUtil.ofBatch("var s = 100;", "s + a");
        final List<Long> ts = preparedBatch.evalBatch(varNames, varsList.stream().map(v -> v.entrySet().iterator()).collect(Collectors.toList()), new TypeLiteral<Long>() {
        });
        System.out.println(ts);
    }

    private static class ListBuilder {
        public static <V> List<V> create(V ...value) {
            return Arrays.stream(value).collect(Collectors.toList());
        }
    }
    private static class MapBuilder<K, V> {
        private final Map<K, V> map;
        private MapBuilder(Map<K, V> map) {
            this.map = map;
        }

        public static <K, V> MapBuilder<K, V> createAndPut(K key, V value) {
            final Map<K, V> map = new HashMap<>();
            map.put(key, value);
            return new MapBuilder<>(map);
        }
        public MapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }
        public Map<K, V> build() {
            return map;
        }

    }
}
