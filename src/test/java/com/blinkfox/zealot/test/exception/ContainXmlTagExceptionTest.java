package com.blinkfox.zealot.test.exception;

import com.blinkfox.zealot.exception.ContainXmlTagException;

import org.junit.Test;

/**
 * ContainXmlTagException单元测试类.
 * @author blinkfox on 2017/4/29.
 */
public class ContainXmlTagExceptionTest {

    /**
     * 测试构造方法.
     */
    @Test(expected = ContainXmlTagException.class)
    public void testContainXmlTagException() {
        throw new ContainXmlTagException("未包含xml标签异常");
    }

}