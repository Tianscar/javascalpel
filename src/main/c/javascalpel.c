#include "com_tianscar_util_Scalpel.h"

#if defined(_MSC_VER) || defined(__MINGW32__)
#include <libloaderapi.h>
#else
#include <dlfcn.h>
#endif

#include <malloc.h>

static jclass IllegalArgumentException_class;
static jclass OutOfMemoryError_class;

static jmethodID Class_getComponentType_methodID;

static jclass void_class;

static jclass Boolean_class;
static jclass boolean_class;
static jmethodID Boolean_booleanValue_methodID;

static jclass Byte_class;
static jclass byte_class;
static jmethodID Byte_byteValue_methodID;

static jclass Character_class;
static jclass char_class;
static jmethodID Character_charValue_methodID;

static jclass Short_class;
static jclass short_class;
static jmethodID Short_shortValue_methodID;

static jclass Integer_class;
static jclass int_class;
static jmethodID Integer_intValue_methodID;

static jclass Long_class;
static jclass long_class;
static jmethodID Long_longValue_methodID;

static jclass Float_class;
static jclass float_class;
static jmethodID Float_floatValue_methodID;

static jclass Double_class;
static jclass double_class;
static jmethodID Double_doubleValue_methodID;

static jmethodID Executable_getParameterTypes_methodID;
static jmethodID Executable_isVarArgs_methodID;

typedef jclass (*JVM_DefineClass_function)(JNIEnv *env, const char *name, jobject loader, const jbyte *buf, jsize len, jobject pd);

static JVM_DefineClass_function JVM_DefineClass;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_1) != JNI_OK) return -1;

#if defined(_MSC_VER) || defined(__MINGW32__)
    HMODULE handle = GetModuleHandle("jvm.dll");
    JVM_DefineClass = (JVM_DefineClass_function) GetProcAddressA(handle, "JVM_DefineClass");
    // see https://github.com/dorkbox/JNA/blob/Version_1.2/src/dorkbox/jna/ClassUtils.java#L88
    if (!JVM_DefineClass) JVM_DefineClass = (JVM_DefineClass_function) GetProcAddressA(handle, "_JVM_DefineClass@24");
#else
    JVM_DefineClass = (JVM_DefineClass_function) dlsym(NULL, "JVM_DefineClass");
#endif
    if (!JVM_DefineClass) return -1;

    IllegalArgumentException_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"));
    OutOfMemoryError_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"));

    jclass Class_class = (*env)->FindClass(env, "java/lang/Class");
    if ((*env)->ExceptionOccurred(env)) return -1;
    Class_getComponentType_methodID = (*env)->GetMethodID(env, Class_class, "getComponentType", "()Ljava/lang/Class;");
    if ((*env)->ExceptionOccurred(env)) return -1;

    jclass Void_class = (*env)->FindClass(env, "java/lang/Void");
    if ((*env)->ExceptionOccurred(env)) return -1;
    void_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Void_class, (*env)->GetStaticFieldID(env, Void_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;

    Boolean_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Boolean"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    boolean_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Boolean_class, (*env)->GetStaticFieldID(env, Boolean_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Boolean_booleanValue_methodID = (*env)->GetMethodID(env, Boolean_class, "booleanValue", "()Z");
    if ((*env)->ExceptionOccurred(env)) return -1;

    Byte_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Byte"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    byte_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Byte_class, (*env)->GetStaticFieldID(env, Byte_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Byte_byteValue_methodID = (*env)->GetMethodID(env, Byte_class, "byteValue", "()B");
    if ((*env)->ExceptionOccurred(env)) return -1;

    Character_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Character"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    char_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Character_class, (*env)->GetStaticFieldID(env, Character_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Character_charValue_methodID = (*env)->GetMethodID(env, Character_class, "charValue", "()C");
    if ((*env)->ExceptionOccurred(env)) return -1;

    Short_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Short"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    short_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Short_class, (*env)->GetStaticFieldID(env, Short_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Short_shortValue_methodID = (*env)->GetMethodID(env, Short_class, "shortValue", "()S");
    if ((*env)->ExceptionOccurred(env)) return -1;

    Integer_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Integer"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    int_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Integer_class, (*env)->GetStaticFieldID(env, Integer_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Integer_intValue_methodID = (*env)->GetMethodID(env, Integer_class, "intValue", "()I");
    if ((*env)->ExceptionOccurred(env)) return -1;

    Long_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Long"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    long_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Long_class, (*env)->GetStaticFieldID(env, Long_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Long_longValue_methodID = (*env)->GetMethodID(env, Long_class, "longValue", "()J");
    if ((*env)->ExceptionOccurred(env)) return -1;

    Float_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Float"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    float_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Float_class, (*env)->GetStaticFieldID(env, Float_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Float_floatValue_methodID = (*env)->GetMethodID(env, Float_class, "floatValue", "()F");
    if ((*env)->ExceptionOccurred(env)) return -1;

    Double_class = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Double"));
    if ((*env)->ExceptionOccurred(env)) return -1;
    double_class = (*env)->NewGlobalRef(env, (*env)->GetStaticObjectField(env, Double_class, (*env)->GetStaticFieldID(env, Double_class, "TYPE", "Ljava/lang/Class;")));
    if ((*env)->ExceptionOccurred(env)) return -1;
    Double_doubleValue_methodID = (*env)->GetMethodID(env, Double_class, "doubleValue", "()D");
    if ((*env)->ExceptionOccurred(env)) return -1;

    jclass Executable_class = (*env)->FindClass(env, "java/lang/reflect/Executable");
    if ((*env)->ExceptionOccurred(env)) return -1;
    Executable_getParameterTypes_methodID = (*env)->GetMethodID(env, Executable_class, "getParameterTypes", "()[Ljava/lang/Class;");
    if ((*env)->ExceptionOccurred(env)) return -1;
    Executable_isVarArgs_methodID = (*env)->GetMethodID(env, Executable_class, "isVarArgs", "()Z");
    if ((*env)->ExceptionOccurred(env)) return -1;

    return JNI_VERSION_1_1;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void) reserved;
    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_1) != JNI_OK) return;

    (*env)->DeleteGlobalRef(env, IllegalArgumentException_class);
    (*env)->DeleteGlobalRef(env, OutOfMemoryError_class);

    (*env)->DeleteGlobalRef(env, void_class);

    (*env)->DeleteGlobalRef(env, Boolean_class);
    (*env)->DeleteGlobalRef(env, boolean_class);

    (*env)->DeleteGlobalRef(env, Byte_class);
    (*env)->DeleteGlobalRef(env, byte_class);

    (*env)->DeleteGlobalRef(env, Character_class);
    (*env)->DeleteGlobalRef(env, char_class);

    (*env)->DeleteGlobalRef(env, Short_class);
    (*env)->DeleteGlobalRef(env, short_class);

    (*env)->DeleteGlobalRef(env, Integer_class);
    (*env)->DeleteGlobalRef(env, int_class);

    (*env)->DeleteGlobalRef(env, Long_class);
    (*env)->DeleteGlobalRef(env, long_class);

    (*env)->DeleteGlobalRef(env, Float_class);
    (*env)->DeleteGlobalRef(env, float_class);

    (*env)->DeleteGlobalRef(env, Double_class);
    (*env)->DeleteGlobalRef(env, double_class);
}

#define UNBOX_PRIMITIVE(primitive_keyword, primitive_keyword_upper, boxed_class_name, jvalue_member) \
    ((*env)->IsSameObject(env, param_class, primitive_keyword ## _class)) { \
        if (jarg == NULL) (*env)->ThrowNew(env, IllegalArgumentException_class, "Cannot unbox a null argument; expected " #boxed_class_name); \
        else if (!(*env)->IsSameObject(env, jarg_class, boxed_class_name ## _class)) \
            (*env)->ThrowNew(env, IllegalArgumentException_class, "Cannot unbox an argument with wrong type; expected " #boxed_class_name); \
        else \
            args[i].jvalue_member = (*env)->Call ## primitive_keyword_upper ## Method(env, jarg, boxed_class_name ## _ ## primitive_keyword ## Value_methodID); \
    }
#define UNBOX_VARARGS(primitive_keyword, primitive_keyword_upper, boxed_class_name) \
        ((*env)->IsSameObject(env, varargs_elements_class, primitive_keyword ## _class)) { \
            j ## primitive_keyword ## Array jarray = (*env)->New ## primitive_keyword_upper ## Array(env, num_varargs_params); \
            if ((*env)->ExceptionOccurred(env)) return JNI_FALSE; \
            if (!jarray) { \
                (*env)->ThrowNew(env, OutOfMemoryError_class, NULL); \
                return JNI_FALSE; \
            } \
            args[num_non_varargs_params].l = jarray; \
            j ## primitive_keyword *varargs = (*env)->Get ## primitive_keyword_upper ## ArrayElements(env, jarray, NULL); \
            if ((*env)->ExceptionOccurred(env)) return JNI_FALSE; \
            if (!varargs) { \
                (*env)->ThrowNew(env, OutOfMemoryError_class, NULL); \
                return JNI_FALSE; \
            } \
            for (jsize i = 0; i < num_varargs_params; i ++) { \
                jobject jarg = (*env)->GetObjectArrayElement(env, jargs, i + num_non_varargs_params); \
                if ((*env)->ExceptionOccurred(env)) return JNI_FALSE; \
                if (jarg == NULL) { \
                    (*env)->ThrowNew(env, IllegalArgumentException_class, "Cannot unbox a null argument; expected " #boxed_class_name); \
                    return JNI_FALSE; \
                } \
                jclass arg_class = (*env)->GetObjectClass(env, jarg); \
                if ((*env)->ExceptionOccurred(env)) return JNI_FALSE; \
                if (!(*env)->IsSameObject(env, arg_class, boxed_class_name ## _class)) { \
                    (*env)->ThrowNew(env, IllegalArgumentException_class, "Cannot unbox an argument with wrong type; expected " #boxed_class_name); \
                    return JNI_FALSE; \
                } else \
                    varargs[i] = (*env)->Call ## primitive_keyword_upper ## Method(env, jarg, boxed_class_name ## _ ## primitive_keyword ## Value_methodID); \
            } \
            (*env)->Release ## primitive_keyword_upper ## ArrayElements(env, jarray, varargs, 0); \
        }
jboolean unbox(JNIEnv *env, jobject executable, jobjectArray jargs, jvalue *args, jsize num_args) {
    jobject parameterTypes = (*env)->CallObjectMethod(env, executable, Executable_getParameterTypes_methodID);
    if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
    jsize num_params = (*env)->GetArrayLength(env, parameterTypes);
    if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
    jboolean has_varargs = (*env)->CallBooleanMethod(env, executable, Executable_isVarArgs_methodID);
    if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
    jclass varargs_array_class;
    jclass varargs_elements_class;
    if (has_varargs) {
        varargs_array_class = (*env)->GetObjectArrayElement(env, parameterTypes, num_params - 1);
        if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
        varargs_elements_class = (*env)->CallObjectMethod(env, varargs_array_class, Class_getComponentType_methodID);
        if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
    }
    jsize num_non_varargs_params = num_params - (has_varargs ? 1 : 0);
    if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
    if ((!has_varargs && num_args != num_params) || (has_varargs && num_args < num_non_varargs_params)) {
        (*env)->ThrowNew(env, IllegalArgumentException_class, "Wrong number of arguments");
        return JNI_FALSE;
    }
    jsize num_varargs_params = num_args - num_non_varargs_params;
    // Unbox non-varargs
    for (jsize i = 0; i < num_non_varargs_params; i ++) {
        jclass param_class = (*env)->GetObjectArrayElement(env, parameterTypes, i);
        if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
        jobject jarg = (*env)->GetObjectArrayElement(env, jargs, i);
        if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
        jclass jarg_class = jarg == NULL ? NULL : (*env)->GetObjectClass(env, jarg);
        if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
        if UNBOX_PRIMITIVE(int, Int, Integer, i)
        else if UNBOX_PRIMITIVE(long, Long, Long, j)
        else if UNBOX_PRIMITIVE(short, Short, Short, s)
        else if UNBOX_PRIMITIVE(char, Char, Character, c)
        else if UNBOX_PRIMITIVE(boolean, Boolean, Boolean, z)
        else if UNBOX_PRIMITIVE(byte, Byte, Byte, b)
        else if UNBOX_PRIMITIVE(float, Float, Float, f)
        else if UNBOX_PRIMITIVE(double, Double, Double, d)
        else {
            if (jarg != NULL && !(*env)->IsAssignableFrom(env, jarg_class, param_class))
                (*env)->ThrowNew(env, IllegalArgumentException_class, "Incompatible argument type");
            else args[i].l = jarg;
        }
        if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
    }
    // Unbox varargs
    if (has_varargs) {
        if UNBOX_VARARGS(int, Int, Integer)
        else if UNBOX_VARARGS(long, Long, Long)
        else if UNBOX_VARARGS(short, Short, Short)
        else if UNBOX_VARARGS(char, Char, Character)
        else if UNBOX_VARARGS(boolean, Boolean, Boolean)
        else if UNBOX_VARARGS(byte, Byte, Byte)
        else if UNBOX_VARARGS(float, Float, Float)
        else if UNBOX_VARARGS(double, Double, Double)
        else {
            jobjectArray jarray = args[num_non_varargs_params].l = (*env)->NewObjectArray(env, num_varargs_params, varargs_elements_class, NULL);
            if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
            if (!jarray) {
                (*env)->ThrowNew(env, OutOfMemoryError_class, NULL);
                return JNI_FALSE;
            }
            for (jsize i = 0; i < num_varargs_params; i ++) {
                jobject jarg = (*env)->GetObjectArrayElement(env, jargs, i + num_non_varargs_params);
                if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
                jclass arg_class = jarg == NULL ? NULL : (*env)->GetObjectClass(env, jarg);
                if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
                if (jarg != NULL && !(*env)->IsAssignableFrom(env, arg_class, varargs_elements_class))
                    (*env)->ThrowNew(env, IllegalArgumentException_class, "Incompatible argument type");
                else (*env)->SetObjectArrayElement(env, jarray, i, jarg);
                if ((*env)->ExceptionOccurred(env)) return JNI_FALSE;
            }
        }
    }
    return JNI_TRUE;
}

JNIEXPORT jobject JNICALL Java_com_tianscar_util_Scalpel_AllocObject
        (JNIEnv *env, jclass unused, jclass clazz) {
    (void) unused;
    return (*env)->AllocObject(env, clazz);
}

JNIEXPORT jobject JNICALL Java_com_tianscar_util_Scalpel_NewObject
        (JNIEnv *env, jclass unused, jclass clazz, jobject constructor, jobjectArray jargs) {
    (void) unused;
    jmethodID constructor_methodID = (*env)->FromReflectedMethod(env, constructor);
    if ((*env)->ExceptionOccurred(env)) return NULL;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return NULL;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, constructor, jargs, args, num_args)) return (*env)->NewObjectA(env, clazz, constructor_methodID, args);
    else return NULL;
}

#define GET_FIELD_FUNCTION(keyword, keyword_upper) \
JNIEXPORT j ## keyword JNICALL Java_com_tianscar_util_Scalpel_Get ## keyword_upper ## Field \
        (JNIEnv *env, jclass unused, jobject object, jobject field) { \
    (void) unused; \
    jfieldID field_fieldID = (*env)->FromReflectedField(env, field); \
    if ((*env)->ExceptionOccurred(env)) return (j ## keyword) 0; \
    return (*env)->Get ## keyword_upper ## Field(env, object, field_fieldID); \
}

GET_FIELD_FUNCTION(object, Object)
GET_FIELD_FUNCTION(boolean, Boolean)
GET_FIELD_FUNCTION(byte, Byte)
GET_FIELD_FUNCTION(char, Char)
GET_FIELD_FUNCTION(short, Short)
GET_FIELD_FUNCTION(int, Int)
GET_FIELD_FUNCTION(long, Long)
GET_FIELD_FUNCTION(float, Float)
GET_FIELD_FUNCTION(double, Double)

#define SET_FIELD_FUNCTION(keyword, keyword_upper) \
JNIEXPORT void JNICALL Java_com_tianscar_util_Scalpel_Set ## keyword_upper ## Field \
        (JNIEnv *env, jclass unused, jobject object, jobject field, j ## keyword value) { \
    (void) unused; \
    jfieldID field_fieldID = (*env)->FromReflectedField(env, field); \
    if ((*env)->ExceptionOccurred(env)) return; \
    return (*env)->Set ## keyword_upper ## Field(env, object, field_fieldID, value); \
}

SET_FIELD_FUNCTION(object, Object)
SET_FIELD_FUNCTION(boolean, Boolean)
SET_FIELD_FUNCTION(byte, Byte)
SET_FIELD_FUNCTION(char, Char)
SET_FIELD_FUNCTION(short, Short)
SET_FIELD_FUNCTION(int, Int)
SET_FIELD_FUNCTION(long, Long)
SET_FIELD_FUNCTION(float, Float)
SET_FIELD_FUNCTION(double, Double)

JNIEXPORT void JNICALL Java_com_tianscar_util_Scalpel_CallVoidMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) (*env)->CallVoidMethodA(env, object, method_methodID, args);
}

JNIEXPORT jobject JNICALL Java_com_tianscar_util_Scalpel_CallObjectMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jobject) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jobject) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallObjectMethodA(env, object, method_methodID, args);
    else return (jobject) 0;
}

JNIEXPORT jboolean JNICALL Java_com_tianscar_util_Scalpel_CallBooleanMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallBooleanMethodA(env, object, method_methodID, args);
    else return (jboolean) 0;
}

JNIEXPORT jbyte JNICALL Java_com_tianscar_util_Scalpel_CallByteMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jbyte) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jbyte) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallByteMethodA(env, object, method_methodID, args);
    else return (jbyte) 0;
}

JNIEXPORT jchar JNICALL Java_com_tianscar_util_Scalpel_CallCharMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jchar) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jchar) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallCharMethodA(env, object, method_methodID, args);
    else return (jchar) 0;
}

JNIEXPORT jshort JNICALL Java_com_tianscar_util_Scalpel_CallShortMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jshort) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jshort) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallShortMethodA(env, object, method_methodID, args);
    else return (jshort) 0;
}

JNIEXPORT jint JNICALL Java_com_tianscar_util_Scalpel_CallIntMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jint) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jint) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallIntMethodA(env, object, method_methodID, args);
    else return (jint) 0;
}

JNIEXPORT jlong JNICALL Java_com_tianscar_util_Scalpel_CallLongMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jlong) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jlong) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallLongMethodA(env, object, method_methodID, args);
    else return (jlong) 0;
}

JNIEXPORT jfloat JNICALL Java_com_tianscar_util_Scalpel_CallFloatMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jfloat) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jfloat) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallFloatMethodA(env, object, method_methodID, args);
    else return (jfloat) 0;
}

JNIEXPORT jdouble JNICALL Java_com_tianscar_util_Scalpel_CallDoubleMethod
        (JNIEnv *env, jclass unused, jobject object, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallBooleanMethodA(env, object, method_methodID, args);
    else return (jboolean) 0;
}

JNIEXPORT void JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualVoidMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) (*env)->CallNonvirtualVoidMethodA(env, object, clazz, method_methodID, args);
}

JNIEXPORT jobject JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualObjectMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jobject) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jobject) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualObjectMethodA(env, object, clazz, method_methodID, args);
    else return (jobject) 0;
}

JNIEXPORT jboolean JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualBooleanMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualBooleanMethodA(env, object, clazz, method_methodID, args);
    else return (jboolean) 0;
}

JNIEXPORT jbyte JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualByteMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jbyte) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jbyte) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualByteMethodA(env, object, clazz, method_methodID, args);
    else return (jbyte) 0;
}

JNIEXPORT jchar JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualCharMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jchar) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jchar) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualCharMethodA(env, object, clazz, method_methodID, args);
    else return (jchar) 0;
}

JNIEXPORT jshort JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualShortMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jshort) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jshort) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualShortMethodA(env, object, clazz, method_methodID, args);
    else return (jshort) 0;
}

JNIEXPORT jint JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualIntMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jint) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jint) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualIntMethodA(env, object, clazz, method_methodID, args);
    else return (jint) 0;
}

JNIEXPORT jlong JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualLongMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jlong) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jlong) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualLongMethodA(env, object, clazz, method_methodID, args);
    else return (jlong) 0;
}

JNIEXPORT jfloat JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualFloatMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jfloat) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jfloat) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualFloatMethodA(env, object, clazz, method_methodID, args);
    else return (jfloat) 0;
}

JNIEXPORT jdouble JNICALL Java_com_tianscar_util_Scalpel_CallNonvirtualDoubleMethod
        (JNIEnv *env, jclass unused, jobject object, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jdouble) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jdouble) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallNonvirtualDoubleMethodA(env, object, clazz, method_methodID, args);
    else return (jdouble) 0;
}

#define GET_STATIC_FIELD_FUNCTION(keyword, keyword_upper) \
JNIEXPORT j ## keyword JNICALL Java_com_tianscar_util_Scalpel_GetStatic ## keyword_upper ## Field \
        (JNIEnv *env, jclass unused, jclass clazz, jobject field) { \
    (void) unused; \
    jfieldID field_fieldID = (*env)->FromReflectedField(env, field); \
    if ((*env)->ExceptionOccurred(env)) return (j ## keyword) 0; \
    return (*env)->GetStatic ## keyword_upper ## Field(env, clazz, field_fieldID); \
}

GET_STATIC_FIELD_FUNCTION(object, Object)
GET_STATIC_FIELD_FUNCTION(boolean, Boolean)
GET_STATIC_FIELD_FUNCTION(byte, Byte)
GET_STATIC_FIELD_FUNCTION(char, Char)
GET_STATIC_FIELD_FUNCTION(short, Short)
GET_STATIC_FIELD_FUNCTION(int, Int)
GET_STATIC_FIELD_FUNCTION(long, Long)
GET_STATIC_FIELD_FUNCTION(float, Float)
GET_STATIC_FIELD_FUNCTION(double, Double)

#define SET_STATIC_FIELD_FUNCTION(keyword, keyword_upper) \
JNIEXPORT void JNICALL Java_com_tianscar_util_Scalpel_SetStatic ## keyword_upper ## Field \
        (JNIEnv *env, jclass unused, jclass clazz, jobject field, j ## keyword value) { \
    (void) unused; \
    jfieldID field_fieldID = (*env)->FromReflectedField(env, field); \
    if ((*env)->ExceptionOccurred(env)) return; \
    return (*env)->SetStatic ## keyword_upper ## Field(env, clazz, field_fieldID, value); \
}

SET_STATIC_FIELD_FUNCTION(object, Object)
SET_STATIC_FIELD_FUNCTION(boolean, Boolean)
SET_STATIC_FIELD_FUNCTION(byte, Byte)
SET_STATIC_FIELD_FUNCTION(char, Char)
SET_STATIC_FIELD_FUNCTION(short, Short)
SET_STATIC_FIELD_FUNCTION(int, Int)
SET_STATIC_FIELD_FUNCTION(long, Long)
SET_STATIC_FIELD_FUNCTION(float, Float)
SET_STATIC_FIELD_FUNCTION(double, Double)

JNIEXPORT void JNICALL Java_com_tianscar_util_Scalpel_CallStaticVoidMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) (*env)->CallStaticVoidMethodA(env, clazz, method_methodID, args);
}

JNIEXPORT jobject JNICALL Java_com_tianscar_util_Scalpel_CallStaticObjectMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jobject) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jobject) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticObjectMethodA(env, clazz, method_methodID, args);
    else return (jobject) 0;
}

JNIEXPORT jboolean JNICALL Java_com_tianscar_util_Scalpel_CallStaticBooleanMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jboolean) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticBooleanMethodA(env, clazz, method_methodID, args);
    else return (jboolean) 0;
}

JNIEXPORT jbyte JNICALL Java_com_tianscar_util_Scalpel_CallStaticByteMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jbyte) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jbyte) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticByteMethodA(env, clazz, method_methodID, args);
    else return (jbyte) 0;
}

JNIEXPORT jchar JNICALL Java_com_tianscar_util_Scalpel_CallStaticCharMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jchar) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jchar) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticCharMethodA(env, clazz, method_methodID, args);
    else return (jchar) 0;
}

JNIEXPORT jshort JNICALL Java_com_tianscar_util_Scalpel_CallStaticShortMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jshort) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jshort) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticShortMethodA(env, clazz, method_methodID, args);
    else return (jshort) 0;
}

JNIEXPORT jint JNICALL Java_com_tianscar_util_Scalpel_CallStaticIntMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jint) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jint) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticIntMethodA(env, clazz, method_methodID, args);
    else return (jint) 0;
}

JNIEXPORT jlong JNICALL Java_com_tianscar_util_Scalpel_CallStaticLongMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jlong) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jlong) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticLongMethodA(env, clazz, method_methodID, args);
    else return (jlong) 0;
}

JNIEXPORT jfloat JNICALL Java_com_tianscar_util_Scalpel_CallStaticFloatMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jfloat) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jfloat) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticFloatMethodA(env, clazz, method_methodID, args);
    else return (jfloat) 0;
}

JNIEXPORT jdouble JNICALL Java_com_tianscar_util_Scalpel_CallStaticDoubleMethod
        (JNIEnv *env, jclass unused, jclass clazz, jobject method, jobjectArray jargs) {
    (void) unused;
    jmethodID method_methodID = (*env)->FromReflectedMethod(env, method);
    if ((*env)->ExceptionOccurred(env)) return (jdouble) 0;
    jsize num_args = (*env)->GetArrayLength(env, jargs);
    if ((*env)->ExceptionOccurred(env)) return (jdouble) 0;
#ifdef _MSC_VER
    jvalue *args = _alloca(num_args);
#else
    jvalue args[num_args];
#endif
    if (unbox(env, method, jargs, args, num_args)) return (*env)->CallStaticDoubleMethodA(env, clazz, method_methodID, args);
    else return (jdouble) 0;
}

JNIEXPORT jclass JNICALL Java_com_tianscar_util_Scalpel_JVM_1DefineClass__Ljava_lang_String_2Ljava_lang_ClassLoader_2_3BIILjava_security_ProtectionDomain_2
        (JNIEnv *env, jclass unused, jstring jname, jobject loader, jbyteArray jbuf, jint off, jint len, jobject pd) {
    (void) unused;
    const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
    if ((*env)->ExceptionOccurred(env)) return NULL;
    jbyte *buf = malloc(len);
    if (!buf) {
        (*env)->ReleaseStringUTFChars(env, jname, name);
        (*env)->ThrowNew(env, OutOfMemoryError_class, NULL);
        return NULL;
    }
    (*env)->GetByteArrayRegion(env, jbuf, off, len, buf);
    jclass clazz = JVM_DefineClass(env, name, loader, buf, len, pd);
    (*env)->ReleaseStringUTFChars(env, jname, name);
    free(buf);
    return clazz;
}

JNIEXPORT jclass JNICALL Java_com_tianscar_util_Scalpel_JVM_1DefineClass__Ljava_lang_String_2Ljava_lang_ClassLoader_2Ljava_nio_ByteBuffer_2ILjava_security_ProtectionDomain_2
        (JNIEnv *env, jclass unused, jstring jname, jobject loader, jobject jbuf, jint len, jobject pd) {
    (void) unused;
    const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
    if ((*env)->ExceptionOccurred(env)) return NULL;
    jbyte *buf = (*env)->GetDirectBufferAddress(env, jbuf);
    if (buf == (jbyte *) -1) buf = NULL;
    jclass clazz = JVM_DefineClass(env, name, loader, buf, len, pd);
    (*env)->ReleaseStringUTFChars(env, jname, name);
    return clazz;
}
