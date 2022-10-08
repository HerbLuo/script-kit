package cn.cloudself.script;

import cn.cloudself.query.QueryProSql;
import cn.cloudself.script.util.LogFactory;
import cn.cloudself.script.util.SpringUtil;
import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.Map;

public class CommonFunctionsForScript {
    public static CommonFunctionsForScript INSTANCE = new CommonFunctionsForScript();

    private static final LogFactory.Log log = LogFactory.getLog(CommonFunctionsForScript.class);

    protected CommonFunctionsForScript() { }

    public void log(Object str) {
        log.info(str);
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
