# Java Scalpel
I need a handy tool to deal with some problems in the use of JRE without modify the JRE itself, that's why I created this library.

This library currently contains the following features: 
1. Bypass the strong encapsulation in Java 16+.
2. Define Java Class with any ClassLoader (including the bootstrap ClassLoader) at runtime.

## Compatibility
Java SE 8, C99

## Building
This project using Gradle 8.0+ and CMake 3.25+ as its build system, but without them, it's also easy to compile.

There are only 3 source files: [Scalpel.java](/src/main/java/com/tianscar/util/Scalpel.java), 
[javascalpel.c](/src/main/c/javascalpel.c) and [the auto-generated JNI header file](/src/main/c/com_tianscar_util_Scalpel.h).

## Usage
### Preparation
You need to do the following steps before using any API of this library to make it working properly:
1. Make sure you have the native binary file (libjavascalpel) of this library.
2. Sets the Java property `javascalpel.libjvm.pathname` to the absolute path of `libjvm` (on Windows jvm.dll, on macOS libjvm.dylib, etc.).
3. Sets the Java property `javascalpel.libjavascalpel.pathname` to the absolute path of `libjavascalpel`.

[JavaDoc](/docs)  
[Examples](/src/test/java/com/tianscar/util/test/ScalpelTest.java)

## License
[MIT](/LICENSE)

### Code usage
| Library                                                  | License    |
|----------------------------------------------------------|------------|
| [Narcissus](https://github.com/toolfactory/narcissus)    | MIT        |