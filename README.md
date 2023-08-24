# Javascalpel
A simple tool which designed to help deal with some problems (e.g. unresolved internal bugs; strong encapsulation in Java 16+) in the use of JRE without any extra configurations (e.g. javaagent; `--add-opens`) or modify and/or recompile the JRE files.

This library currently contains the following features: 
1. Bypass the strong encapsulation in Java 16+.
2. Define Java Class with any ClassLoader (including the bootstrap ClassLoader) at runtime.

## Compatibility
### Runtime
Java SE 8+  
C99
### System
Windows XP+  
POSIX

## Building
This project using Gradle 8.0+ and CMake 3.25+ as its build system, but without them, it's also easy to compile because of there are only 3 source files: [Scalpel.java](/src/main/java/com/tianscar/util/Scalpel.java),
[javascalpel.c](/src/main/c/javascalpel.c) and [the auto-generated JNI header file](/src/main/c/com_tianscar_util_Scalpel.h).

The native part of this library only using a few Win32/POSIX/JNI API, so it is fully portable.

## Usage
### Preparation
You need to do the following steps before using any API of this library to make it working properly:
1. Make sure you have the native shared library binary (i.e. `libjavascalpel`) of this library.
2. Sets the Java property `javascalpel.libjvm.pathname` to the absolute path of `libjvm` (on Windows `jvm.dll`, on macOS `libjvm.dylib`, on most *nix platforms `libjvm.so`).
3. Sets the Java property `javascalpel.libjavascalpel.pathname` to the absolute path of `libjavascalpel`.

### API
To make the API available, the simplest way is copy the `com.tianscar.util.Scalpel.java` (keep the class hierarchy if you don't want to see `UnsatisfiedLinkError`) to your project.

[JavaDoc](https://docs.tianscar.com/javascalpel)  
[Examples](/src/test/java/com/tianscar/util/test/ScalpelTest.java)

## License
[MIT](/LICENSE)

### Windows dependencies
| Library  | License                 | Comptime | Runtime |
|----------|-------------------------|----------|---------|
| Kernel32 | implementation-specific | Yes      | Yes     |

### POSIX dependencies
| Library | License                 | Comptime | Runtime |
|---------|-------------------------|----------|---------|
| libdl   | implementation-specific | Yes      | Yes     |

### Code usage
| Library                                                  | License    |
|----------------------------------------------------------|------------|
| [Narcissus](https://github.com/toolfactory/narcissus)    | MIT        |
