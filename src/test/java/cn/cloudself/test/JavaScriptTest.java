package cn.cloudself.test;

import cn.cloudself.script.JavaScript;
import cn.cloudself.script.JavaScriptUtil;

import java.util.Date;
import java.util.HashMap;

public class JavaScriptTest {
    public static void main(String[] args) throws InterruptedException {
        final JavaScript.Prepared compiled = JavaScriptUtil.compile("const year = date.getFullYear(); print(year);");
        final HashMap<String, Object> vars = new HashMap<>();
        vars.put("date", new Date());
        System.out.println(compiled.eval(vars));
    }
}
