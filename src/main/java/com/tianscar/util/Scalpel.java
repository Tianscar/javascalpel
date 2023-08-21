package com.tianscar.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;

public final class Scalpel {


    // ---------------- Loader ----------------

    private static final String KEY_JAVASCALPEL_LIBJVM_PATHNAME = "javascalpel.libjvm.pathname";
    private static final String KEY_JAVASCALPEL_LIBJAVASCALPEL_PATHNAME = "javascalpel.libjavascalpel.pathname";

    static {
        System.load(System.getProperty(KEY_JAVASCALPEL_LIBJVM_PATHNAME));
        System.load(System.getProperty(KEY_JAVASCALPEL_LIBJAVASCALPEL_PATHNAME));
    }

    private Scalpel() {
        throw new AssertionError("No " + Scalpel.class.getName() + " instances for you!");
    }


    // ---------------- Unsafe ----------------

    private static final Unsafe UNSAFE;
    static {
        Unsafe unsafe;
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            unsafe = null;
        }
        UNSAFE = unsafe;
    }


    // ---------------- Reflect ----------------

    /**
     * Set the {@code accessible} flag for this reflected object to {@code true}
     * if possible. This method sets the {@code accessible} flag, as if by
     * invoking {@link AccessibleObject#setAccessible(boolean) setAccessible(true)}, and returns
     * the possibly-updated value for the {@code accessible} flag. If access
     * cannot be enabled, i.e. the checks or Java language access control cannot
     * be suppressed, this method returns {@code false} (as opposed to {@code
     * setAccessible(true)} throwing {@code InaccessibleObjectException} when
     * it fails).
     *
     * <p> This method is a no-op if the {@code accessible} flag for
     * this reflected object is {@code true}.
     *
     * <p> For example, a caller can invoke {@code trySetAccessible}
     * on a {@code Method} object for a private instance method
     * {@code p.T::privateMethod} to suppress the checks for Java language access
     * control when the {@code Method} is invoked.
     * If {@code p.T} class is in a different module to the caller and
     * package {@code p} is open to at least the caller's module,
     * the code below successfully sets the {@code accessible} flag
     * to {@code true}.
     *
     * <pre>
     * {@code
     *     p.T obj = ....;  // instance of p.T
     *     :
     *     Method m = p.T.class.getDeclaredMethod("privateMethod");
     *     if (m.trySetAccessible()) {
     *         m.invoke(obj);
     *     } else {
     *         // package p is not opened to the caller to access private member of T
     *         ...
     *     }
     * }</pre>
     *
     * <p> If there is a security manager, its {@code checkPermission} method
     * is first called with a {@code ReflectPermission("suppressAccessChecks")}
     * permission. </p>
     *
     * @return {@code true} if the {@code accessible} flag is set to {@code true};
     *         {@code false} if access cannot be enabled.
     * @throws SecurityException if the request is denied by the security manager
     *
     */
    public static boolean trySetAccessible(AccessibleObject accessible) {
        try {
            // Java 9
            return (boolean) AccessibleObject.class.getDeclaredMethod("trySetAccessible").invoke(accessible);
        } catch (NoSuchMethodException e) {
            accessible.setAccessible(true);
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    /**
     * Allocates an instance but does not run any constructor.
     * Initializes the class if it has not yet been.
     *
     * @throws InstantiationException    if the class that declares the
     *           underlying constructor represents an abstract class.
     */
    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<T> clazz) throws InstantiationException {
        if (UNSAFE == null) return AllocObject(clazz);
        else return (T) UNSAFE.allocateInstance(clazz);
    }

    /**
     * Uses the constructor represented by this {@code Constructor} object to
     * create and initialize a new instance of the constructor's
     * declaring class, with the specified initialization parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as necessary.
     *
     * <p>If the number of formal parameters required by the underlying constructor
     * is 0, the supplied {@code initargs} array may be of length 0 or null.
     *
     * <p>If the constructor's declaring class is an inner class in a
     * non-static context, the first argument to the constructor needs
     * to be the enclosing instance; see section {&#064;jls 15.9.3} of
     * <cite>The Java Language Specification</cite>.
     *
     * <p>If the required access and argument checks succeed and the
     * instantiation will proceed, the constructor's declaring class
     * is initialized if it has not already been initialized.
     *
     * <p>If the constructor completes normally, returns the newly
     * created and initialized instance.
     *
     * @param args array of objects to be passed as arguments to
     * the constructor call; values of primitive types are wrapped in
     * a wrapper object of the appropriate type (e.g. a {@code float}
     * in a {@link java.lang.Float Float})
     *
     * @return a new object created by calling the constructor
     * this object represents
     *
     * @throws    IllegalArgumentException  if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion; if
     *              this constructor pertains to an enum class.
     * @throws    InstantiationException    if the class that declares the
     *              underlying constructor represents an abstract class.
     * @throws    InvocationTargetException if the underlying constructor
     *              throws an exception.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    public static <T> T newInstance(Constructor<T> constructor, Object... args) throws InstantiationException, InvocationTargetException {
        try {
            if (trySetAccessible(constructor)) return constructor.newInstance(args);
        } catch (IllegalAccessException ignored) {
        }
        return NewObject(constructor.getDeclaringClass(), constructor, args);
    }

    /**
     * Returns the value of the field represented by this {@code Field}, on
     * the specified object. The value is automatically wrapped in an
     * object if it has a primitive type.
     *
     * <p>The underlying field's value is obtained as follows:
     *
     * <p>If the underlying field is a static field, the {@code obj} argument
     * is ignored; it may be null.
     *
     * <p>Otherwise, the underlying field is an instance field.  If the
     * specified {@code object} argument is null, the method throws a
     * {@code NullPointerException}. If the specified object is not an
     * instance of the class or interface declaring the underlying
     * field, the method throws an {@code IllegalArgumentException}.
     *
     * <p>If this {@code Field} object is enforcing Java language access control, and
     * the underlying field is inaccessible, the method throws an
     * {@code IllegalAccessException}.
     * If the underlying field is static, the class that declared the
     * field is initialized if it has not already been initialized.
     *
     * <p>Otherwise, the value is retrieved from the underlying instance
     * or static field.  If the field has a primitive type, the value
     * is wrapped in an object before being returned, otherwise it is
     * returned as is.
     *
     * <p>If the field is hidden in the type of {@code object},
     * the field's value is obtained according to the preceding rules.
     *
     * @param object object from which the represented field's value is
     * to be extracted
     * @return the value of the represented field in object
     * {@code object}; primitive values are wrapped in an appropriate
     * object before being returned
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof).
     * @throws    NullPointerException      if the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    public static Object getObjectField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.get(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticObjectField(field.getDeclaringClass(), field) : GetObjectField(object, field);
    }

    public static boolean getBooleanField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getBoolean(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticBooleanField(field.getDeclaringClass(), field) : GetBooleanField(object, field);
    }

    public static byte getByteField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getByte(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticByteField(field.getDeclaringClass(), field) : GetByteField(object, field);
    }

    public static char getCharField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getChar(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticCharField(field.getDeclaringClass(), field) : GetCharField(object, field);
    }

    public static short getShortField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getShort(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticShortField(field.getDeclaringClass(), field) : GetShortField(object, field);
    }

    public static int getIntField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getInt(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticIntField(field.getDeclaringClass(), field) : GetIntField(object, field);
    }

    public static long getLongField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getLong(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticLongField(field.getDeclaringClass(), field) : GetLongField(object, field);
    }

    public static float getFloatField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getFloat(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticFloatField(field.getDeclaringClass(), field) : GetFloatField(object, field);
    }

    public static double getDoubleField(Object object, Field field) {
        try {
            if (trySetAccessible(field)) return field.getDouble(object);
        } catch (IllegalAccessException ignored) {
        }
        return object == null ? GetStaticDoubleField(field.getDeclaringClass(), field) : GetDoubleField(object, field);
    }

    public static Object getField(Object object, Field field) {
        Class<?> clazz = field.getType();
        if (clazz == boolean.class) return getBooleanField(object, field);
        else if (clazz == byte.class) return getByteField(object, field);
        else if (clazz == char.class) return getCharField(object, field);
        else if (clazz == short.class) return getShortField(object, field);
        else if (clazz == int.class) return getIntField(object, field);
        else if (clazz == long.class) return getLongField(object, field);
        else if (clazz == float.class) return getFloatField(object, field);
        else if (clazz == double.class) return getDoubleField(object, field);
        else return getObjectField(object, field);
    }

    public static void setObjectField(Object object, Field field, Object value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticObjectField(field.getDeclaringClass(), field, value);
        else SetObjectField(object, field, value);
    }

    public static void setBooleanField(Object object, Field field, boolean value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticBooleanField(field.getDeclaringClass(), field, value);
        else SetBooleanField(object, field, value);
    }

    public static void setByteField(Object object, Field field, byte value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticByteField(field.getDeclaringClass(), field, value);
        else SetByteField(object, field, value);
    }

    public static void setCharField(Object object, Field field, char value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticCharField(field.getDeclaringClass(), field, value);
        else SetCharField(object, field, value);
    }

    public static void setShortField(Object object, Field field, short value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticShortField(field.getDeclaringClass(), field, value);
        else SetShortField(object, field, value);
    }

    public static void setIntField(Object object, Field field, int value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticIntField(field.getDeclaringClass(), field, value);
        else SetIntField(object, field, value);
    }

    public static void setLongField(Object object, Field field, long value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticLongField(field.getDeclaringClass(), field, value);
        else SetLongField(object, field, value);
    }

    public static void setFloatField(Object object, Field field, float value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticFloatField(field.getDeclaringClass(), field, value);
        else SetFloatField(object, field, value);
    }

    public static void setDoubleField(Object object, Field field, double value) {
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        }
        catch (IllegalAccessException ignored) {
        }
        if (object == null) SetStaticDoubleField(field.getDeclaringClass(), field, value);
        else SetDoubleField(object, field, value);
    }

    public static void setField(Object object, Field field, Object value) {
        Class<?> fieldType = field.getType();
        if (fieldType == boolean.class) setBooleanField(object, field, (Boolean) value);
        else if (fieldType == byte.class) setByteField(object, field, (Byte) value);
        else if (fieldType == char.class) setCharField(object, field, (Character) value);
        else if (fieldType == short.class) setShortField(object, field, (Short) value);
        else if (fieldType == int.class) setIntField(object, field, (Integer) value);
        else if (fieldType == long.class) setLongField(object, field, (Long) value);
        else if (fieldType == float.class) setFloatField(object, field, (Float) value);
        else if (fieldType == double.class) setDoubleField(object, field, (Double) value);
        else setObjectField(object, field, value);
    }
    
    public static void invokeVoidMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) {
                method.invoke(object, args);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) CallStaticVoidMethod(method.getDeclaringClass(), method, args);
        else CallVoidMethod(object, method, args);
    }

    public static Object invokeObjectMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticObjectMethod(method.getDeclaringClass(), method, args);
        else return CallObjectMethod(object, method, args);
    }

    public static boolean invokeBooleanMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (boolean) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticBooleanMethod(method.getDeclaringClass(), method, args);
        else return CallBooleanMethod(object, method, args);
    }

    public static byte invokeByteMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (byte) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticByteMethod(method.getDeclaringClass(), method, args);
        else return CallByteMethod(object, method, args);
    }

    public static char invokeCharMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (char) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticCharMethod(method.getDeclaringClass(), method, args);
        else return CallCharMethod(object, method, args);
    }

    public static short invokeShortMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (short) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticShortMethod(method.getDeclaringClass(), method, args);
        else return CallShortMethod(object, method, args);
    }

    public static int invokeIntMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (int) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticIntMethod(method.getDeclaringClass(), method, args);
        else return CallIntMethod(object, method, args);
    }

    public static long invokeLongMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (long) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticLongMethod(method.getDeclaringClass(), method, args);
        else return CallLongMethod(object, method, args);
    }

    public static float invokeFloatMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (float) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticFloatMethod(method.getDeclaringClass(), method, args);
        else return CallFloatMethod(object, method, args);
    }

    public static double invokeDoubleMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        try {
            if (trySetAccessible(method)) return (double) method.invoke(object, args);
        } catch (ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (IllegalAccessException ignored) {
        }
        if (object == null) return CallStaticDoubleMethod(method.getDeclaringClass(), method, args);
        else return CallDoubleMethod(object, method, args);
    }

    public static Object invokeMethod(Object object, Method method, Object... args) throws InvocationTargetException {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            invokeVoidMethod(object, method, args);
            return null;
        }
        else if (returnType == int.class) return invokeIntMethod(object, method, args);
        else if (returnType == long.class) return invokeLongMethod(object, method, args);
        else if (returnType == short.class) return invokeShortMethod(object, method, args);
        else if (returnType == char.class) return invokeCharMethod(object, method, args);
        else if (returnType == boolean.class) return invokeBooleanMethod(object, method, args);
        else if (returnType == byte.class) return invokeByteMethod(object, method, args);
        else if (returnType == float.class) return invokeFloatMethod(object, method, args);
        else if (returnType == double.class) return invokeDoubleMethod(object, method, args);
        else return invokeObjectMethod(object, method, args);
    }

    public static void invokeNonVirtualVoidMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
            return;
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        CallNonvirtualVoidMethod(object, clazz, method, args);
    }

    public static Object invokeNonVirtualObjectMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualObjectMethod(object, clazz, method, args);
    }

    public static boolean invokeNonVirtualBooleanMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (boolean) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualBooleanMethod(object, clazz, method, args);
    }

    public static byte invokeNonVirtualByteMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (byte) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualByteMethod(object, clazz, method, args);
    }

    public static char invokeNonVirtualCharMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (char) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualCharMethod(object, clazz, method, args);
    }

    public static short invokeNonVirtualShortMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (short) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualShortMethod(object, clazz, method, args);
    }

    public static int invokeNonVirtualIntMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (int) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualIntMethod(object, clazz, method, args);
    }

    public static long invokeNonVirtualLongMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (long) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualLongMethod(object, clazz, method, args);
    }

    public static float invokeNonVirtualFloatMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (float) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualFloatMethod(object, clazz, method, args);
    }

    public static double invokeNonVirtualDoubleMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        try {
            return (double) MethodHandles.lookup().unreflectSpecial(method, clazz).invoke(args);
        } catch (WrongMethodTypeException | ClassCastException e) {
            throw new InvocationTargetException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
        }
        return CallNonvirtualDoubleMethod(object, clazz, method, args);
    }

    public static Object invokeNonVirtualMethod(Object object, Class<?> clazz, Method method, Object... args) throws InvocationTargetException {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            invokeNonVirtualVoidMethod(object, clazz, method, args);
            return null;
        }
        else if (returnType == int.class) return invokeNonVirtualIntMethod(object, clazz, method, args);
        else if (returnType == long.class) return invokeNonVirtualLongMethod(object, clazz, method, args);
        else if (returnType == short.class) return invokeNonVirtualShortMethod(object, clazz, method, args);
        else if (returnType == char.class) return invokeNonVirtualCharMethod(object, clazz, method, args);
        else if (returnType == boolean.class) return invokeNonVirtualBooleanMethod(object, clazz, method, args);
        else if (returnType == byte.class) return invokeNonVirtualByteMethod(object, clazz, method, args);
        else if (returnType == float.class) return invokeNonVirtualFloatMethod(object, clazz, method, args);
        else if (returnType == double.class) return invokeNonVirtualDoubleMethod(object, clazz, method, args);
        else return invokeNonVirtualObjectMethod(object, clazz, method, args);
    }

    private static native<T> T AllocObject(Class<T> clazz) throws InstantiationException;
    private static native<T> T NewObject(Class<T> clazz, Constructor<T> constructor, Object... args)
            throws InstantiationException, InvocationTargetException;

    private static native Object GetObjectField(Object obj, Field field);
    private static native boolean GetBooleanField(Object obj, Field field);
    private static native byte GetByteField(Object obj, Field field);
    private static native char GetCharField(Object obj, Field field);
    private static native short GetShortField(Object obj, Field field);
    private static native int GetIntField(Object obj, Field field);
    private static native long GetLongField(Object obj, Field field);
    private static native float GetFloatField(Object obj, Field field);
    private static native double GetDoubleField(Object obj, Field field);

    private static native void SetObjectField(Object obj, Field field, Object value);
    private static native void SetBooleanField(Object obj, Field field, boolean value);
    private static native void SetByteField(Object obj, Field field, byte value);
    private static native void SetCharField(Object obj, Field field, char value);
    private static native void SetShortField(Object obj, Field field, short value);
    private static native void SetIntField(Object obj, Field field, int value);
    private static native void SetLongField(Object obj, Field field, long value);
    private static native void SetFloatField(Object obj, Field field, float value);
    private static native void SetDoubleField(Object obj, Field field, double value);

    private static native void CallVoidMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native Object CallObjectMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native boolean CallBooleanMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native byte CallByteMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native char CallCharMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native short CallShortMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native int CallIntMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native long CallLongMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native float CallFloatMethod(Object obj, Method method, Object... args) throws InvocationTargetException;
    private static native double CallDoubleMethod(Object obj, Method method, Object... args) throws InvocationTargetException;

    private static native void CallNonvirtualVoidMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native Object CallNonvirtualObjectMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native boolean CallNonvirtualBooleanMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native byte CallNonvirtualByteMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native char CallNonvirtualCharMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native short CallNonvirtualShortMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native int CallNonvirtualIntMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native long CallNonvirtualLongMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native float CallNonvirtualFloatMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native double CallNonvirtualDoubleMethod(Object obj, Class<?> clazz, Method method, Object... args) throws InvocationTargetException;

    private static native Object GetStaticObjectField(Class<?> clazz, Field field);
    private static native boolean GetStaticBooleanField(Class<?> clazz, Field field);
    private static native byte GetStaticByteField(Class<?> clazz, Field field);
    private static native char GetStaticCharField(Class<?> clazz, Field field);
    private static native short GetStaticShortField(Class<?> clazz, Field field);
    private static native int GetStaticIntField(Class<?> clazz, Field field);
    private static native long GetStaticLongField(Class<?> clazz, Field field);
    private static native float GetStaticFloatField(Class<?> clazz, Field field);
    private static native double GetStaticDoubleField(Class<?> clazz, Field field);

    private static native void SetStaticObjectField(Class<?> clazz, Field field, Object value);
    private static native void SetStaticBooleanField(Class<?> clazz, Field field, boolean value);
    private static native void SetStaticByteField(Class<?> clazz, Field field, byte value);
    private static native void SetStaticCharField(Class<?> clazz, Field field, char value);
    private static native void SetStaticShortField(Class<?> clazz, Field field, short value);
    private static native void SetStaticIntField(Class<?> clazz, Field field, int value);
    private static native void SetStaticLongField(Class<?> clazz, Field field, long value);
    private static native void SetStaticFloatField(Class<?> clazz, Field field, float value);
    private static native void SetStaticDoubleField(Class<?> clazz, Field field, double value);

    private static native void CallStaticVoidMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native Object CallStaticObjectMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native boolean CallStaticBooleanMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native byte CallStaticByteMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native char CallStaticCharMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native short CallStaticShortMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native int CallStaticIntMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native long CallStaticLongMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native float CallStaticFloatMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;
    private static native double CallStaticDoubleMethod(Class<?> clazz, Method method, Object... args) throws InvocationTargetException;


    // ---------------- Class ----------------

    public static Class<?> defineClass(String name, ClassLoader classLoader, byte[] bytes, int offset, int length, ProtectionDomain protectionDomain) {
        if (classLoader != null) {
            try {
                Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
                return (Class<?>) invokeObjectMethod(classLoader, method, name, bytes, offset, length, protectionDomain);
            } catch (InvocationTargetException | NoSuchMethodException ignored) {
            }
        }
        return JVM_DefineClass(name.replace('.', '/'), classLoader, bytes, offset, length, protectionDomain);
    }

    private static native Class<?> JVM_DefineClass(String name, ClassLoader loader, byte[] buf, int off, int len, ProtectionDomain pd);

}
