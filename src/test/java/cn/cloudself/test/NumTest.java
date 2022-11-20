package cn.cloudself.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static cn.cloudself.test.helper.Helper.eval;

public class NumTest {
    @Test
    public void javaNumToJs() {
        Assertions.assertEquals(eval("1 + a0", 1), 2);
        Assertions.assertEquals(eval("a0.toFixed(2)", 1), "1.00");
        Assertions.assertEquals(eval("a0 + a1", 1.1, 2L), 3.1);
        Assertions.assertEquals(eval("a0 + 50", new BigDecimal("82000")), 82050.0);
        Assertions.assertEquals(eval("a0 + a1 + 50.5", new BigDecimal("82000"), new BigInteger("3020")), 85070.5);
    }
}
