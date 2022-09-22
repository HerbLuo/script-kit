package cn.cloudself.script;

import cn.cloudself.query.QueryProSql;
import cn.cloudself.script.util.SpringUtil;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CommonFunctionsForScript {
    public static CommonFunctionsForScript INSTANCE = new CommonFunctionsForScript();
    private static final boolean isSlf4jPresent = isPresent("org.slf4j.Logger");
    private static final Logger log = isSlf4jPresent ? LoggerFactory.getLogger(CommonFunctionsForScript.class) : null;

    protected CommonFunctionsForScript() { }

    public void log(String str) {
        if (isSlf4jPresent) {
            log.info(str);
        } else {
            print(str);
        }
    }

    public void print(Object obj) {
        System.out.println(obj);
    }

    public Object springBean(String interfaceName) throws ClassNotFoundException {
        final Class<?> clazz = Class.forName(interfaceName);
        return SpringUtil.getBean(clazz);
    }

    public Map<String, Object> queryOne(@Language("SQL") String sql) {
        return QueryProSql.create(sql).queryOne();
    }

    public List<Map<String, Object>> queryAll(@Language("SQL") String sql) {
        return QueryProSql.create(sql).query();
    }

    private static boolean isPresent(@SuppressWarnings("SameParameterValue") String className) {
        try {
            Class.forName(className, false, CommonFunctionsForScript.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
