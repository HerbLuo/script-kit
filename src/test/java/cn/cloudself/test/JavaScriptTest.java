package cn.cloudself.test;

import cn.cloudself.script.JavaScript;
import cn.cloudself.script.JavaScriptUtil;
import cn.cloudself.test.helper.Helper;
import org.graalvm.polyglot.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JavaScriptTest {
    @Test
    public void javaToJs() {
        Helper.initLogger();

        final Object res1 = JavaScriptUtil.of("v + 8;").eval(Collections.singletonMap("v", new BigDecimal(100L)));
        assertInstanceOf(Number.class, res1);
        assertEquals(Double.valueOf("108"), Double.valueOf(res1 + ""));

        final Object res2 = JavaScriptUtil.of("NaN + 8000;").eval(new HashMap<>());
        System.out.println(res2.getClass());
        final Object res3 = JavaScriptUtil.of("null + NaN + v;").eval(Collections.singletonMap("v", new BigDecimal(100L)));
        System.out.println(res3);
    }

    @Test
    public void jsToJava() {
        Helper.initLogger();

        final Map<String, Object> res1 = JavaScriptUtil.of("{a: 1, 1: 2}").eval(new HashMap<>(), new TypeLiteral<Map<String, Object>>() { });
        assertEquals("1", res1.get("a").toString());
        final List<Object> res2 = JavaScriptUtil.of("[2, 2]").eval(new HashMap<>(), new TypeLiteral<List<Object>>() { });
        assertEquals("2", res2.get(0).toString());

        final Object[] res3 = JavaScriptUtil.of("[3, 2]").eval(new HashMap<>(), new TypeLiteral<Object[]>() { });
        assertEquals("3", res3[0].toString());
    }

    @Test
    public void test() {
        final JavaScript.Prepared compiled = JavaScriptUtil.compile("const year = date.getFullYear(); print(year);");
        final HashMap<String, Object> vars = new HashMap<>();
        vars.put("date", new Date());
        System.out.println(compiled.eval(vars));
    }
}
