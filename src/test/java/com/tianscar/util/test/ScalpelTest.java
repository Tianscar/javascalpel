package com.tianscar.util.test;

import com.tianscar.util.Scalpel;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.awt.AWTEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ScalpelTest {

    public static class TestClassA {
        public int returnInt() {
            return 0;
        }
    }

    public static class TestClassB extends TestClassA {
        @Override
        public int returnInt() {
            return 1;
        }
    }

    public static class TestClassC extends TestClassB {
        @Override
        public int returnInt() {
            return 2;
        }
    }

    private static final int TEST_FIELD = 0;

    @BeforeAll
    public static void setupNativeLibraries() {
        //System.setProperty("javascalpel.libjvm.pathname", /* <LIBRARY PATHNAME> */);
        //System.setProperty("javascalpel.libjavascalpel.pathname", /* <LIBRARY PATHNAME> */);
    }

    @Test
    public void testInvokeMethod() {
        try {
            Method method = TestClassA.class.getDeclaredMethod("returnInt");
            TestClassB object = new TestClassB();
            Assertions.assertEquals(1, Scalpel.invokeIntMethod(object, method));
            Assertions.assertEquals(0, Scalpel.invokeNonVirtualIntMethod(object, method));
        }
        catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail(e);
        }
    }

    @Test
    public void testAccessFieldWithIncompatibleType() {
        try {
            Field field = String.class.getDeclaredField("hash");
            Assertions.assertThrows(IllegalArgumentException.class, () -> Scalpel.getObjectField("STRING", field));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Scalpel.getBooleanField("STRING", field));
        }
        catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail(e);
        }
    }

    @Test
    public void testSetFinalField() {
        try {
            Field field = ScalpelTest.class.getDeclaredField("TEST_FIELD");
            Assertions.assertEquals(field.getInt(null), 0);
            Scalpel.setIntField(null, field, 1);
            Assertions.assertEquals(field.getInt(null), 1);
        }
        catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail(e);
        }
    }

    private volatile boolean failed;

    @Test
    public void testModifyJREInternalClass() {
        try {
            failed = false;
            ClassPool pool = ClassPool.getDefault();
            CtClass eventQueueCtClass = pool.get("java.awt.EventQueue");
            eventQueueCtClass.defrost();
            CtMethod invokeAndWaitCtMethod = eventQueueCtClass.getDeclaredMethod("invokeAndWait");
            invokeAndWaitCtMethod.insertBefore("return;");
            byte[] bytecode = eventQueueCtClass.toBytecode();
            Assertions.assertNull(AWTEvent.class.getClassLoader()); // null is the bootstrap ClassLoader
            Scalpel.defineClass("java.awt.EventQueue",
                    java.awt.AWTEvent.class.getClassLoader(),
                    bytecode, 0, bytecode.length,
                    java.awt.AWTEvent.class.getProtectionDomain());
            eventQueueCtClass.detach();
            EventQueue.invokeAndWait(() -> failed = true);
            if (failed) Assertions.fail("Method insertion failed");
        }
        catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail(e);
        }
    }

}
