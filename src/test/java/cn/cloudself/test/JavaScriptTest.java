package cn.cloudself.test;

import cn.cloudself.script.JavaScript;
import cn.cloudself.script.JavaScriptUtil;
import cn.cloudself.test.helper.Helper;
import org.graalvm.polyglot.TypeLiteral;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class JavaScriptTest {
    @Test
    public void javaToJs() {
        Helper.initLogger();

        final Object res1 = JavaScriptUtil.of("v + 8;").eval(Collections.singletonMap("v", new BigDecimal(100L)), Double.class);
        assertInstanceOf(Number.class, res1);
        assertEquals(Double.valueOf("108"), Double.valueOf(res1 + ""));
    }

    public void date() {


    }

    @Test
    public void jsToJava() {
        Helper.initLogger();

        final Map<String, Object> res1 = JavaScriptUtil.of("{a: 1, 1: 2, b: {c: 3, d: new Date(), e: [1, new Date(), {f: 2, g: true}]}}").evalAsMap(new HashMap<>());
        assertEquals("1", res1.get("a").toString());
        final Object bo = res1.get("b");
        assertInstanceOf(Map.class, bo);
        final Map<?, ?> b = (Map<?, ?>)  bo;
        assertEquals(3, b.get("c"));
        assertInstanceOf(LocalDateTime.class, b.get("d"));
        final Object eo = b.get("e");
        assertInstanceOf(List.class, eo);
        final List<?> e = (List<?>) eo;
        assertEquals(e.get(0), 1);
        assertInstanceOf(LocalDateTime.class, e.get(1));
        final Object e3o = e.get(2);
        assertInstanceOf(Map.class, e3o);
        final Map<?, ?> e3 = (Map<?, ?>) e3o;
        assertEquals(e3.get("f"), 2);
        assertEquals(e3.get("g"), true);

        final List<Object> res2 = JavaScriptUtil.of("[2, 2, new Date(), {a: 1}]").eval(new HashMap<>(), new TypeLiteral<List<Object>>() { });
        assertEquals("2", res2.get(0).toString());

        final Object[] res3 = JavaScriptUtil.of("[3, 2]").eval(new HashMap<>(), new TypeLiteral<Object[]>() { });
        assertEquals("3", res3[0].toString());
    }

    @Test
    public void test() {
        final JavaScript.Prepared compiled = JavaScriptUtil.compile("const year = date.getFullYear(); print(year);");
        final HashMap<String, Object> vars = new HashMap<>();
        vars.put("date", new Date());
        System.out.println(compiled.eval(vars, Object.class));
    }

    @Test
    public void test2() {
        final JavaScript.Prepared compiled = JavaScriptUtil.compile("x");
        final HashMap<String, Object> vars = new HashMap<>();
        vars.put("o", new HashMap<String, Object>(){{put("o", 6);}});
        vars.put("x", 66);
        System.out.println(compiled.eval(vars, Object.class));
    }

    static class Bean {
        private Integer num1 = 1;
        private Long num2 = 2L;
        private Boolean bool1 = true;
        private boolean bool2 = false;
        private Date date = new Date();
        private List<Integer> nums = new ArrayList<>();

        public Long getNum2() {
            return num2;
        }

        public Bean setNum2(Long num2) {
            this.num2 = num2;
            return this;
        }

        public Boolean getBool1() {
            return bool1;
        }

        public Bean setBool1(Boolean bool1) {
            this.bool1 = bool1;
            return this;
        }

        public boolean isBool2() {
            return bool2;
        }

        public Bean setBool2(boolean bool2) {
            this.bool2 = bool2;
            return this;
        }

        public Date getDate() {
            return date;
        }

        public Bean setDate(Date date) {
            this.date = date;
            return this;
        }

        public List<Integer> getNums() {
            return nums;
        }

        public Bean setNums(List<Integer> nums) {
            this.nums = nums;
            return this;
        }

        public Integer getNum1() {
            return num1;
        }

        public Bean setNum1(Integer num1) {
            this.num1 = num1;
            return this;
        }
    }
}
