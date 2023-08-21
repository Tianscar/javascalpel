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

public class ScalpelTest {

    @BeforeAll
    public static void setupNativeLibraries() {
        //System.setProperty("javascalpel.libjvm.pathname", /* <LIBRARY PATHNAME> */);
        //System.setProperty("javascalpel.libjavascalpel.pathname", /* <LIBRARY PATHNAME> */);
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
            Assertions.fail(e);
        }
    }

}
