package com.fox2code.foxevents.unsafe;

import io.github.karlatemp.unsafeaccessor.Root;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@SuppressWarnings("JavaReflectionInvocation")
final class UnsafeFoxEventsJVMHelper {
    private static final MethodHandles.Lookup TRUSTED_LOOKUP;
    private static final JVMImplementation IMPLEMENTATION;

    static {
        MethodHandles.Lookup lookup = null;
        JVMImplementation implementation = null;
        try {
            lookup = Root.getTrusted(Object.class);
            Method privateGetPublicMethodsMethod = Class.class.getDeclaredMethod("privateGetPublicMethods");
            // OpenJDK implementations work on both OpenJDK, Hotspot, and GraalVM
            try {
                Method makeDirectMethodHandleMethod =
                        Class.forName("java.lang.invoke.DirectMethodHandle")
                                .getDeclaredMethod("make", Method.class);
                Root.setAccessible(makeDirectMethodHandleMethod, true);
                MethodHandle makeDirectMethodHandle = (MethodHandle)
                        makeDirectMethodHandleMethod.invoke(null, makeDirectMethodHandleMethod);
                MethodHandle privateGetPublicMethods = (MethodHandle)
                        makeDirectMethodHandle.invoke(privateGetPublicMethodsMethod);
                implementation = new OpenJDKImplementation1(privateGetPublicMethods, makeDirectMethodHandle);
            } catch (NoSuchMethodException e) {
                Class<?> memberName = Class.forName("java.lang.invoke.MemberName");
                Constructor<?> memberNameConstructor = memberName.getDeclaredConstructor(Method.class);
                Constructor<?> memberNameConstructor2 = memberName.getDeclaredConstructor(Constructor.class);
                Root.setAccessible(memberNameConstructor, true);
                Root.setAccessible(memberNameConstructor2, true);
                Method makeDirectMethodHandleMethod =
                        Class.forName("java.lang.invoke.DirectMethodHandle")
                                .getDeclaredMethod("make", memberName);
                Root.setAccessible(makeDirectMethodHandleMethod, true);
                MethodHandle makeMemberName = (MethodHandle) makeDirectMethodHandleMethod
                        .invoke(null, memberNameConstructor2.newInstance(memberNameConstructor));
                MethodHandle makeDirectMethodHandle = (MethodHandle) makeDirectMethodHandleMethod
                        .invoke(null, makeMemberName.invoke(memberNameConstructor));
                MethodHandle privateGetPublicMethods = (MethodHandle)
                        makeDirectMethodHandle.invoke(makeMemberName.invoke(privateGetPublicMethodsMethod));
                implementation = new OpenJDKImplementation2(
                        privateGetPublicMethods, makeMemberName, makeDirectMethodHandle);
            }
        } catch (Throwable ignored) {}
        TRUSTED_LOOKUP = lookup;
        IMPLEMENTATION = implementation;
    }

    public static MethodHandles.Lookup getTrustedLookup() {
        return TRUSTED_LOOKUP;
    }

    static JVMImplementation getJVMImplementation() {
        return IMPLEMENTATION;
    }

    static abstract class JVMImplementation {
        abstract Method[] getMethods(Class<?> cls);

        abstract MethodHandle unReflectAndBind(Method method, Object instance) throws IllegalAccessException;
    }

    private static final class OpenJDKImplementation1 extends JVMImplementation {
        private final MethodHandle privateGetPublicMethods;
        private final MethodHandle makeDirectMethodHandle;

        private OpenJDKImplementation1(MethodHandle privateGetPublicMethods, MethodHandle makeDirectMethodHandle) {
            this.privateGetPublicMethods = privateGetPublicMethods;
            this.makeDirectMethodHandle = makeDirectMethodHandle;
        }

        @Override
        Method[] getMethods(Class<?> cls) {
            try {
                return (Method[]) this.privateGetPublicMethods.invokeExact(cls);
            } catch (Throwable ignored) {
                return cls.getMethods();
            }
        }

        @Override
        MethodHandle unReflectAndBind(Method method, Object instance) throws IllegalAccessException {
            MethodHandle methodHandle;
            try {
                methodHandle = (MethodHandle) this.makeDirectMethodHandle.invoke(method);
            } catch (Throwable e) {
                methodHandle = TRUSTED_LOOKUP.unreflect(method);
            }
            return instance == null ? methodHandle :
                    methodHandle.bindTo(instance);
        }
    }

    private static final class OpenJDKImplementation2 extends JVMImplementation {
        private final MethodHandle privateGetPublicMethods;
        private final MethodHandle makeMemberName;
        private final MethodHandle makeDirectMethodHandle;

        private OpenJDKImplementation2(
                MethodHandle privateGetPublicMethods,
                MethodHandle makeMemberName, MethodHandle makeDirectMethodHandle) {
            this.privateGetPublicMethods = privateGetPublicMethods;
            this.makeMemberName = makeMemberName;
            this.makeDirectMethodHandle = makeDirectMethodHandle;
        }

        @Override
        Method[] getMethods(Class<?> cls) {
            try {
                return (Method[]) this.privateGetPublicMethods.invokeExact(cls);
            } catch (Throwable ignored) {
                return cls.getMethods();
            }
        }

        @Override
        MethodHandle unReflectAndBind(Method method, Object instance) throws IllegalAccessException {
            MethodHandle methodHandle;
            try {
                methodHandle = (MethodHandle)
                        this.makeDirectMethodHandle.invoke(
                                this.makeMemberName.invoke(method));
            } catch (Throwable e) {
                methodHandle = TRUSTED_LOOKUP.unreflect(method);
            }
            return instance == null ? methodHandle :
                    methodHandle.bindTo(instance);
        }
    }
}
