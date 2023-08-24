package com.tianscar.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

import static java.lang.reflect.Modifier.isStatic;

/**
 * <p>A simple tool which designed to help deal with some problems (e.g. unresolved internal bugs; strong encapsulation in Java 16+) in the use of JRE without any extra configurations (e.g. javaagent; <code>--add-opens</code>) or modify and/or recompile the JRE files.
 *
 * <p>This library currently contains the following features:
 * <ol>
 * <li>Bypass the strong encapsulation in Java 16+.</li>
 * <li>Define Java Class with any ClassLoader (including the bootstrap ClassLoader) at runtime.</li>
 * </ol>
 */
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

    
    // ---------------- Lookup ----------------

    private static final MethodHandles.Lookup LOOKUP;
    static {
        MethodHandles.Lookup lookup;
        try {
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            if (trySetAccessible(field)) lookup = (MethodHandles.Lookup) field.get(null);
            else lookup = null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            lookup = null;
        }
        LOOKUP = lookup;
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
     * @throws NullPointerException if the specified accessible object is null
     *
     */
    public static boolean trySetAccessible(AccessibleObject accessible) throws SecurityException, NullPointerException {
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
     * @throws NullPointerException    if the specified class is null
     */
    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<T> clazz) throws InstantiationException, NullPointerException {
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
     * to be the enclosing instance; see section 15.9.3 of
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
     * @throws    NullPointerException if the specified constructor is null
     * @throws    InstantiationException    if the class that declares the
     *              underlying constructor represents an abstract class.
     * @throws    InvocationTargetException if the underlying constructor
     *              throws an exception.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    public static <T> T newInstance(Constructor<T> constructor, Object... args) throws InstantiationException, InvocationTargetException,
            NullPointerException, IllegalArgumentException, ExceptionInInitializerError {
        try {
            if (trySetAccessible(constructor)) return constructor.newInstance(args);
        } catch (IllegalAccessException ignored) {
        }
        return NewObject(constructor.getDeclaringClass(), constructor, args);
    }

    /**
     * Gets the value of a static or instance non-primitive field.
     *
     * @param object the object to extract the non-primitive value
     * from
     * @return the value of the non-primitive field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value is primitive.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static Object getObjectField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType().isPrimitive()) throw new IllegalArgumentException("Illegal field type; expected non-primitive");
        try {
            if (trySetAccessible(field)) return field.get(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticObjectField(field.getDeclaringClass(), field) :
                GetObjectField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code boolean} field.
     *
     * @param object the object to extract the {@code boolean} value
     * from
     * @return the value of the {@code boolean} field
     * 
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code boolean} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static boolean getBooleanField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != boolean.class) throw new IllegalArgumentException("Illegal field type; expected boolean");
        try {
            if (trySetAccessible(field)) return field.getBoolean(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticBooleanField(field.getDeclaringClass(), field) :
                GetBooleanField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code byte} field.
     *
     * @param object the object to extract the {@code byte} value
     * from
     * @return the value of the {@code byte} field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code byte} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static byte getByteField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != byte.class) throw new IllegalArgumentException("Illegal field type; expected byte");
        try {
            if (trySetAccessible(field)) return field.getByte(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticByteField(field.getDeclaringClass(), field) :
                GetByteField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code char} field.
     *
     * @param object the object to extract the {@code char} value
     * from
     * @return the value of the {@code char} field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code char} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static char getCharField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != char.class) throw new IllegalArgumentException("Illegal field type; expected char");
        try {
            if (trySetAccessible(field)) return field.getChar(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticCharField(field.getDeclaringClass(), field) :
                GetCharField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code short} field.
     *
     * @param object the object to extract the {@code short} value
     * from
     * @return the value of the {@code short} field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code short} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static short getShortField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != short.class) throw new IllegalArgumentException("Illegal field type; expected short");
        try {
            if (trySetAccessible(field)) return field.getShort(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticShortField(field.getDeclaringClass(), field) :
                GetShortField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code int} field.
     *
     * @param object the object to extract the {@code int} value
     * from
     * @return the value of the {@code int} field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code int} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static int getIntField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != int.class) throw new IllegalArgumentException("Illegal field type; expected int");
        try {
            if (trySetAccessible(field)) return field.getInt(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticIntField(field.getDeclaringClass(), field) :
                GetIntField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code long} field.
     *
     * @param object the object to extract the {@code long} value
     * from
     * @return the value of the {@code long} field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code long} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static long getLongField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != long.class) throw new IllegalArgumentException("Illegal field type; expected long");
        try {
            if (trySetAccessible(field)) return field.getLong(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticLongField(field.getDeclaringClass(), field) :
                GetLongField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code float} field.
     *
     * @param object the object to extract the {@code float} value
     * from
     * @return the value of the {@code float} field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code float} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static float getFloatField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != float.class) throw new IllegalArgumentException("Illegal field type; expected float");
        try {
            if (trySetAccessible(field)) return field.getFloat(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticFloatField(field.getDeclaringClass(), field) :
                GetFloatField(object, field);
    }

    /**
     * Gets the value of a static or instance {@code double} field.
     *
     * @param object the object to extract the {@code double} value
     * from
     * @return the value of the {@code double} field
     *
     * @throws    IllegalArgumentException  if the specified object is not
     *              an instance of the class or interface declaring the
     *              underlying field (or a subclass or implementor
     *              thereof), or if the field value cannot be
     *              converted to the type {@code double} by a
     *              widening conversion.
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #getField(Object, Field)
     */
    public static double getDoubleField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != double.class) throw new IllegalArgumentException("Illegal field type; expected double");
        try {
            if (trySetAccessible(field)) return field.getDouble(object);
        } catch (IllegalAccessException ignored) {
        }
        return isStatic(field.getModifiers()) ?
                GetStaticDoubleField(field.getDeclaringClass(), field) :
                GetDoubleField(object, field);
    }

    /**
     * Returns the value of the field represented by this {@code Field}, on
     * the specified object. The value is automatically wrapped in an
     * object if it has a primitive type.
     *
     * <p>The underlying field's value is obtained as follows:
     *
     * <p>If the underlying field is a static field, the {@code object} argument
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
     * @throws    NullPointerException      if the specified field is null, or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    public static Object getField(Object object, Field field) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        try {
            if (trySetAccessible(field)) return field.get(object);
        } catch (IllegalAccessException ignored) {
        }
        Class<?> fieldType = field.getType();
        if (isStatic(field.getModifiers())) {
            if (fieldType == boolean.class) return GetStaticBooleanField(field.getDeclaringClass(), field);
            else if (fieldType == byte.class) return GetStaticByteField(field.getDeclaringClass(), field);
            else if (fieldType == char.class) return GetStaticCharField(field.getDeclaringClass(), field);
            else if (fieldType == short.class) return GetStaticShortField(field.getDeclaringClass(), field);
            else if (fieldType == int.class) return GetStaticIntField(field.getDeclaringClass(), field);
            else if (fieldType == long.class) return GetStaticLongField(field.getDeclaringClass(), field);
            else if (fieldType == float.class) return GetStaticFloatField(field.getDeclaringClass(), field);
            else if (fieldType == double.class) return GetStaticDoubleField(field.getDeclaringClass(), field);
            else return GetStaticObjectField(field.getDeclaringClass(), field);
        }
        else {
            if (fieldType == boolean.class) return GetBooleanField(object, field);
            else if (fieldType == byte.class) return GetByteField(object, field);
            else if (fieldType == char.class) return GetCharField(object, field);
            else if (fieldType == short.class) return GetShortField(object, field);
            else if (fieldType == int.class) return GetIntField(object, field);
            else if (fieldType == long.class) return GetLongField(object, field);
            else if (fieldType == float.class) return GetFloatField(object, field);
            else if (fieldType == double.class) return GetDoubleField(object, field);
            else return GetObjectField(object, field);
        }
    }

    /**
     * Sets the value of a field as an {@code Object} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setObjectField(Object object, Field field, Object value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (!field.getType().isAssignableFrom(value.getClass()))
            throw new IllegalArgumentException("Illegal field type; expected " + field.getType().getTypeName());
        try {
            if (trySetAccessible(field)) {
                field.set(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticObjectField(field.getDeclaringClass(), field, value);
        else SetObjectField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code boolean} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setBooleanField(Object object, Field field, boolean value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != boolean.class) throw new IllegalArgumentException("Illegal field type; expected boolean");
        try {
            if (trySetAccessible(field)) {
                field.setBoolean(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticBooleanField(field.getDeclaringClass(), field, value);
        else SetBooleanField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code byte} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setByteField(Object object, Field field, byte value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != byte.class) throw new IllegalArgumentException("Illegal field type; expected byte");
        try {
            if (trySetAccessible(field)) {
                field.setByte(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticByteField(field.getDeclaringClass(), field, value);
        else SetByteField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code char} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setCharField(Object object, Field field, char value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != char.class) throw new IllegalArgumentException("Illegal field type; expected char");
        try {
            if (trySetAccessible(field)) {
                field.setChar(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticCharField(field.getDeclaringClass(), field, value);
        else SetCharField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code short} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setShortField(Object object, Field field, short value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != short.class) throw new IllegalArgumentException("Illegal field type; expected short");
        try {
            if (trySetAccessible(field)) {
                field.setShort(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticShortField(field.getDeclaringClass(), field, value);
        else SetShortField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code int} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setIntField(Object object, Field field, int value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != int.class) throw new IllegalArgumentException("Illegal field type; expected int");
        try {
            if (trySetAccessible(field)) {
                field.setInt(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticIntField(field.getDeclaringClass(), field, value);
        else SetIntField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code long} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setLongField(Object object, Field field, long value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != long.class) throw new IllegalArgumentException("Illegal field type; expected long");
        try {
            if (trySetAccessible(field)) {
                field.setLong(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticLongField(field.getDeclaringClass(), field, value);
        else SetLongField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code float} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setFloatField(Object object, Field field, float value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != float.class) throw new IllegalArgumentException("Illegal field type; expected float");
        try {
            if (trySetAccessible(field)) {
                field.setFloat(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticFloatField(field.getDeclaringClass(), field, value);
        else SetFloatField(object, field, value);
    }

    /**
     * Sets the value of a field as a {@code double} on the specified object.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     *
     * @see #setField(Object, Field, Object)
     */
    public static void setDoubleField(Object object, Field field, double value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (field.getType() != double.class) throw new IllegalArgumentException("Illegal field type; expected double");
        try {
            if (trySetAccessible(field)) {
                field.setDouble(object, value);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(field.getModifiers())) SetStaticDoubleField(field.getDeclaringClass(), field, value);
        else SetDoubleField(object, field, value);
    }

    /**
     * Sets the field represented by this {@code Field} object on the
     * specified object argument to the specified new value. The new
     * value is automatically unwrapped if the underlying field has a
     * primitive type.
     *
     * <p>The operation proceeds as follows:
     *
     * <p>If the underlying field is static, the {@code obj} argument is
     * ignored; it may be null.
     *
     * <p>Otherwise the underlying field is an instance field.  If the
     * specified object argument is null, the method throws a
     * {@code NullPointerException}.  If the specified object argument is not
     * an instance of the class or interface declaring the underlying
     * field, the method throws an {@code IllegalArgumentException}.
     *
     * <p> Setting a final field in this way
     * is meaningful only during deserialization or reconstruction of
     * instances of classes with blank final fields, before they are
     * made available for access by other parts of a program. Use in
     * any other context may have unpredictable effects, including cases
     * in which other parts of a program continue to use the original
     * value of this field.
     *
     * <p>If the underlying field is of a primitive type, an unwrapping
     * conversion is attempted to convert the new value to a value of
     * a primitive type.  If this attempt fails, the method throws an
     * {@code IllegalArgumentException}.
     *
     * <p>If, after possible unwrapping, the new value cannot be
     * converted to the type of the underlying field by an identity or
     * widening conversion, the method throws an
     * {@code IllegalArgumentException}.
     *
     * <p>If the underlying field is static, the class that declared the
     * field is initialized if it has not already been initialized.
     *
     * <p>The field is set to the possibly unwrapped and widened new value.
     *
     * <p>If the field is hidden in the type of {@code object},
     * the field's value is set according to the preceding rules.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code object}
     * being modified
     *
     * @throws    IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @throws    NullPointerException      if the specified field is null or the specified object is null
     *              and the field is an instance field.
     * @throws    ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    public static void setField(Object object, Field field, Object value) throws IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        Class<?> fieldType = field.getType();
        try {
            if (fieldType == boolean.class) setBooleanField(object, field, (Boolean) value);
            else if (fieldType == byte.class) setByteField(object, field, (Byte) value);
            else if (fieldType == char.class) setCharField(object, field, (Character) value);
            else if (fieldType == short.class) setShortField(object, field, (Short) value);
            else if (fieldType == int.class) setIntField(object, field, (Integer) value);
            else if (fieldType == long.class) setLongField(object, field, (Long) value);
            else if (fieldType == float.class) setFloatField(object, field, (Float) value);
            else if (fieldType == double.class) setDoubleField(object, field, (Double) value);
            else setObjectField(object, field, value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the method had a return value.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static void invokeVoidMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != void.class && method.getReturnType() != Void.class) throw new IllegalArgumentException("Illegal return type; expected void");
        try {
            if (trySetAccessible(method)) {
                method.invoke(object, args);
                return;
            }
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) CallStaticVoidMethod(method.getDeclaringClass(), method, args);
        else CallVoidMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value is primitive.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static Object invokeObjectMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType().isPrimitive()) throw new IllegalArgumentException("Illegal return type; expected non-primitive");
        try {
            if (trySetAccessible(method)) return method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticObjectMethod(method.getDeclaringClass(), method, args);
        else return CallObjectMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code boolean} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static boolean invokeBooleanMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != boolean.class) throw new IllegalArgumentException("Illegal return type; expected boolean");
        try {
            if (trySetAccessible(method)) return (boolean) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticBooleanMethod(method.getDeclaringClass(), method, args);
        else return CallBooleanMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code byte} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static byte invokeByteMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != byte.class) throw new IllegalArgumentException("Illegal return type; expected byte");
        try {
            if (trySetAccessible(method)) return (byte) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticByteMethod(method.getDeclaringClass(), method, args);
        else return CallByteMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code char} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static char invokeCharMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != char.class) throw new IllegalArgumentException("Illegal return type; expected char");
        try {
            if (trySetAccessible(method)) return (char) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticCharMethod(method.getDeclaringClass(), method, args);
        else return CallCharMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code short} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static short invokeShortMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != short.class) throw new IllegalArgumentException("Illegal return type; expected short");
        try {
            if (trySetAccessible(method)) return (short) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticShortMethod(method.getDeclaringClass(), method, args);
        else return CallShortMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code int} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static int invokeIntMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != int.class) throw new IllegalArgumentException("Illegal return type; expected int");
        try {
            if (trySetAccessible(method)) return (int) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticIntMethod(method.getDeclaringClass(), method, args);
        else return CallIntMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code long} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static long invokeLongMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != long.class) throw new IllegalArgumentException("Illegal return type; expected long");
        try {
            if (trySetAccessible(method)) return (long) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticLongMethod(method.getDeclaringClass(), method, args);
        else return CallLongMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code float} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static float invokeFloatMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != float.class) throw new IllegalArgumentException("Illegal return type; expected float");
        try {
            if (trySetAccessible(method)) return (float) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticFloatMethod(method.getDeclaringClass(), method, args);
        else return CallFloatMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code double} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static double invokeDoubleMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != double.class) throw new IllegalArgumentException("Illegal return type; expected double");
        try {
            if (trySetAccessible(method)) return (double) method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        if (isStatic(method.getModifiers())) return CallStaticDoubleMethod(method.getDeclaringClass(), method, args);
        else return CallDoubleMethod(object, method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as
     * necessary.
     *
     * <p>If the underlying method is static, then the specified {@code object}
     * argument is ignored. It may be null.
     *
     * <p>If the number of formal parameters required by the underlying method is
     * 0, the supplied {@code args} array may be of length 0 or null.
     *
     * <p>If the underlying method is an instance method, it is invoked
     * using dynamic method lookup as documented in The Java Language
     * Specification, section 15.12.4.4; in particular,
     * overriding based on the runtime type of the target object may occur.
     *
     * <p>If the underlying method is static, the class that declared
     * the method is initialized if it has not already been initialized.
     *
     * <p>If the method completes normally, the value it returns is
     * returned to the caller of invoke; if the value has a primitive
     * type, it is first appropriately wrapped in an object. However,
     * if the value has the type of array of a primitive type, the
     * elements of the array are <i>not</i> wrapped in objects; in
     * other words, an array of primitive type is returned.  If the
     * underlying method return type is void, the invocation returns
     * null.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException  if the method is an
     *              instance method and the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method is null,
     *              or specified object is null and the method is an instance method.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static Object invokeMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        try {
            if (trySetAccessible(method)) return method.invoke(object, args);
        } catch (IllegalAccessException ignored) {
        }
        Class<?> returnType = method.getReturnType();
        if (isStatic(method.getModifiers())) {
            if (returnType == void.class) {
                CallStaticVoidMethod(method.getDeclaringClass(), method, args);
                return null;
            }
            else if (returnType == boolean.class) return CallStaticBooleanMethod(method.getDeclaringClass(), method, args);
            else if (returnType == byte.class) return CallStaticByteMethod(method.getDeclaringClass(), method, args);
            else if (returnType == char.class) return CallStaticCharMethod(method.getDeclaringClass(), method, args);
            else if (returnType == short.class) return CallStaticShortMethod(method.getDeclaringClass(), method, args);
            else if (returnType == int.class) return CallStaticIntMethod(method.getDeclaringClass(), method, args);
            else if (returnType == long.class) return CallStaticLongMethod(method.getDeclaringClass(), method, args);
            else if (returnType == float.class) return CallStaticFloatMethod(method.getDeclaringClass(), method, args);
            else if (returnType == double.class) return CallStaticDoubleMethod(method.getDeclaringClass(), method, args);
            else return CallStaticObjectMethod(method.getDeclaringClass(), method, args);
        }
        else {
            if (returnType == void.class) {
                CallVoidMethod(object, method, args);
                return null;
            }
            else if (returnType == boolean.class) return CallBooleanMethod(object, method, args);
            else if (returnType == byte.class) return CallByteMethod(object, method, args);
            else if (returnType == char.class) return CallCharMethod(object, method, args);
            else if (returnType == short.class) return CallShortMethod(object, method, args);
            else if (returnType == int.class) return CallIntMethod(object, method, args);
            else if (returnType == long.class) return CallLongMethod(object, method, args);
            else if (returnType == float.class) return CallFloatMethod(object, method, args);
            else if (returnType == double.class) return CallDoubleMethod(object, method, args);
            else return CallObjectMethod(object, method, args);
        }
    }


    // ---------------- Invoke ----------------

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the method had a return value.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static void invokeNonVirtualVoidMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != void.class && method.getReturnType() != Void.class) throw new IllegalArgumentException("Illegal return type; expected void");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
                return;
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        CallNonvirtualVoidMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value is primitive.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static Object invokeNonVirtualObjectMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType().isPrimitive()) throw new IllegalArgumentException("Illegal return type; expected non-primitive");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualObjectMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code boolean} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static boolean invokeNonVirtualBooleanMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != boolean.class) throw new IllegalArgumentException("Illegal return type; expected boolean");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (boolean) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualBooleanMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code byte} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static byte invokeNonVirtualByteMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != byte.class) throw new IllegalArgumentException("Illegal return type; expected byte");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (byte) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualByteMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code char} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static char invokeNonVirtualCharMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != char.class) throw new IllegalArgumentException("Illegal return type; expected char");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (char) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualCharMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code short} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static short invokeNonVirtualShortMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != short.class) throw new IllegalArgumentException("Illegal return type; expected short");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (short) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualShortMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code int} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static int invokeNonVirtualIntMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != int.class) throw new IllegalArgumentException("Illegal return type; expected int");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (int) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualIntMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code long} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static long invokeNonVirtualLongMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != long.class) throw new IllegalArgumentException("Illegal return type; expected long");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (long) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualLongMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code float} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static float invokeNonVirtualFloatMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != float.class) throw new IllegalArgumentException("Illegal return type; expected float");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (float) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualFloatMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters,
     * bypassing all overriding methods.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion;
     *              or if the returned value cannot be converted to
     *              the type {@code double} by a widening conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     *
     * @see #invokeNonVirtualMethod(Object, Method, Object...)
     */
    public static double invokeNonVirtualDoubleMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (method.getReturnType() != double.class) throw new IllegalArgumentException("Illegal return type; expected double");
        else if (isStatic(method.getModifiers())) throw new IllegalArgumentException("Illegal method modifier; expected non-static");
        if (LOOKUP != null) {
            try {
                return (double) LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        return CallNonvirtualDoubleMethod(object, method.getDeclaringClass(), method, args);
    }

    /**
     * Invokes the underlying method represented by this {@code Method}
     * object, on the specified object with the specified parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as
     * necessary.
     *
     * <p>If the number of formal parameters required by the underlying method is
     * 0, the supplied {@code args} array may be of length 0 or null.
     *
     * <p>If the underlying method is an instance method, it is invoked
     * using special method lookup bypassing all overriding methods which
     * documented in The Java Language Specification, section 15.12.4.4;
     * in particular, overriding based on the runtime type of the target
     * object never occur.
     *
     * <p>If the method completes normally, the value it returns is
     * returned to the caller of invoke; if the value has a primitive
     * type, it is first appropriately wrapped in an object. However,
     * if the value has the type of array of a primitive type, the
     * elements of the array are <i>not</i> wrapped in objects; in
     * other words, an array of primitive type is returned.  If the
     * underlying method return type is void, the invocation returns
     * null.
     *
     * @param object  the object the underlying method is invoked from
     * @param args the arguments used for the method call
     *
     * @throws    IllegalArgumentException if the underlying method is
     *              a static method or the specified object argument
     *              is not an instance of the class or interface
     *              declaring the underlying method (or of a subclass
     *              or implementor thereof); if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion.
     * @throws    InvocationTargetException if the underlying method
     *              throws an exception.
     * @throws    NullPointerException      if the specified method or specified object is null.
     * @throws    ExceptionInInitializerError if the initialization
     * provoked by this method fails.
     */
    public static Object invokeNonVirtualMethod(Object object, Method method, Object... args)
            throws InvocationTargetException, IllegalArgumentException, NullPointerException, ExceptionInInitializerError {
        if (LOOKUP != null) {
            try {
                return LOOKUP.unreflectSpecial(method, method.getDeclaringClass()).bindTo(object).invokeWithArguments(args);
            } catch (WrongMethodTypeException e) {
                throw new IllegalArgumentException(e);
            } catch (RuntimeException | Error | InvocationTargetException e) {
                throw e;
            } catch (Throwable ignored) {
            }
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            invokeNonVirtualVoidMethod(object, method, args);
            return null;
        }
        else if (returnType == int.class) return invokeNonVirtualIntMethod(object, method, args);
        else if (returnType == long.class) return invokeNonVirtualLongMethod(object, method, args);
        else if (returnType == short.class) return invokeNonVirtualShortMethod(object, method, args);
        else if (returnType == char.class) return invokeNonVirtualCharMethod(object, method, args);
        else if (returnType == boolean.class) return invokeNonVirtualBooleanMethod(object, method, args);
        else if (returnType == byte.class) return invokeNonVirtualByteMethod(object, method, args);
        else if (returnType == float.class) return invokeNonVirtualFloatMethod(object, method, args);
        else if (returnType == double.class) return invokeNonVirtualDoubleMethod(object, method, args);
        else return invokeNonVirtualObjectMethod(object, method, args);
    }


    // ---------------- Class ----------------

    /**
     * Converts an array of bytes into an instance of class {@code Class},
     * with a given {@code ProtectionDomain}.
     *
     * <p> If the given {@code ProtectionDomain} is {@code null},
     * then a default protection domain will be assigned to the class.
     * Before the class can be used it must be resolved.
     *
     * <p> You should always pass in the <a href="#binary-name">binary name</a> of the
     * class you are defining as well as the bytes.  This ensures that the
     * class you are defining is indeed the class you think it is.
     *
     * <p> This method defines a package in this class loader corresponding to the
     * package of the {@code Class} (if such a package has not already been defined
     * in this class loader). The name of the defined package is derived from
     * the <a href="#binary-name">binary name</a> of the class specified by
     * the byte array {@code bytecode}.
     * Other properties of the defined package are as specified by {@link Package}.
     *
     * @param  name
     *         The expected <a href="#binary-name">binary name</a> of the class, or
     *         {@code null} if not known
     *
     * @param  bytecode
     *         The bytes that make up the class data. The bytes in positions
     *         {@code offset} through {@code offset+length-1} should have the format
     *         of a valid class file as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *
     * @param  offset
     *         The start offset in {@code bytecode} of the class data
     *
     * @param  length
     *         The length of the class data
     *
     * @param  protectionDomain
     *         The {@code ProtectionDomain} of the class
     *
     * @return  The {@code Class} object created from the data,
     *          and {@code ProtectionDomain}.
     *
     * @throws  ClassFormatError
     *          If the data did not contain a valid class
     *
     * @throws  NoClassDefFoundError
     *          If {@code name} is not {@code null} and not equal to the
     *          <a href="#binary-name">binary name</a> of the class specified by {@code bytecode}
     *
     * @throws  IndexOutOfBoundsException
     *          If either {@code offset} or {@code length} is negative, or if
     *          {@code offset+length} is greater than {@code bytecode.length}.
     *
     * @throws  NullPointerException
     *          If {@code bytecode} is {@code null}.
     */
    public static Class<?> defineClass(String name, ClassLoader classLoader, byte[] bytecode, int offset, int length, ProtectionDomain protectionDomain)
            throws ClassFormatError, NoClassDefFoundError, IndexOutOfBoundsException, NullPointerException {
        if (classLoader != null) {
            try {
                Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
                return (Class<?>) invokeObjectMethod(classLoader, method, name, bytecode, offset, length, protectionDomain);
            } catch (InvocationTargetException | NoSuchMethodException ignored) {
            }
        }
        return JVM_DefineClass(name == null ? null : name.replace('.', '/'), classLoader, bytecode, offset, length, protectionDomain);
    }

    /**
     * Converts a {@link java.nio.ByteBuffer ByteBuffer} into an instance
     * of class {@code Class}, with the given {@code ProtectionDomain}.
     * If the given {@code ProtectionDomain} is {@code null}, then a default
     * protection domain will be assigned to the class.
     * Before the class can be used it must be resolved.
     *
     * <p> An invocation of this method of the form
     * {@code defineClass(}<i>name</i>{@code ,}<i>cl</i>{@code ,}
     * <i>bytecode</i>{@code ,} <i>pd</i>{@code )} yields exactly the same
     * result as the statements
     *
     *<p> <code>
     * ...<br>
     * byte[] temp = new byte[bytecode.{@link
     * java.nio.ByteBuffer#remaining remaining}()];<br>
     *     bytecode.{@link java.nio.ByteBuffer#get(byte[])
     * get}(temp);<br>
     *     return {@link #defineClass(String, ClassLoader, byte[], int, int, ProtectionDomain)
     * defineClass}(name, cl, temp, 0,
     * temp.length, pd);<br>
     * </code></p>
     *
     * @param  name
     *         The expected <a href="#binary-name">binary name</a>. of the class, or
     *         {@code null} if not known
     *
     * @param  bytecode
     *         The bytes that make up the class data. The bytes from positions
     *         {@code bytecode.position()} through {@code bytecode.position() + bytecode.limit() -1
     *         } should have the format of a valid class file as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *
     * @param  protectionDomain
     *         The {@code ProtectionDomain} of the class, or {@code null}.
     *
     * @return  The {@code Class} object created from the data,
     *          and {@code ProtectionDomain}.
     *
     * @throws  ClassFormatError
     *          If the data did not contain a valid class.
     *
     * @throws  NoClassDefFoundError
     *          If {@code name} is not {@code null} and not equal to the
     *          <a href="#binary-name">binary name</a> of the class specified by {@code bytecode}
     *
     * @throws  NullPointerException
     *          If {@code bytecode} is {@code null}.
     *
     * @see      #defineClass(String, ClassLoader, byte[], int, int, ProtectionDomain)
     */
    public static Class<?> defineClass(String name, ClassLoader classLoader, ByteBuffer bytecode, ProtectionDomain protectionDomain)
            throws ClassFormatError, NoClassDefFoundError, NullPointerException {
        if (classLoader != null) {
            try {
                Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, ByteBuffer.class, ProtectionDomain.class);
                return (Class<?>) invokeObjectMethod(classLoader, method, name, bytecode, protectionDomain);
            } catch (InvocationTargetException | NoSuchMethodException ignored) {
            }
        }
        int length = bytecode.remaining();
        if (bytecode.isDirect()) return JVM_DefineClass(name.replace('.', '/'), classLoader, bytecode, length, protectionDomain);
        else if (bytecode.hasArray()) {
            return JVM_DefineClass(name.replace('.', '/'), classLoader, bytecode.array(),
                    bytecode.position() + bytecode.arrayOffset(), length, protectionDomain);
        }
        else {
            byte[] array = new byte[length];
            bytecode.get(array);
            return JVM_DefineClass(name.replace('.', '/'), classLoader, array, 0, length, protectionDomain);
        }
    }


    // ---------------- JNI ----------------

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

    private static native Class<?> JVM_DefineClass(String name, ClassLoader loader, byte[] buf, int off, int len, ProtectionDomain pd) throws ClassFormatError, NoClassDefFoundError, IndexOutOfBoundsException;
    private static native Class<?> JVM_DefineClass(String name, ClassLoader loader, ByteBuffer buf, int len, ProtectionDomain pd) throws ClassFormatError, NoClassDefFoundError;

}
