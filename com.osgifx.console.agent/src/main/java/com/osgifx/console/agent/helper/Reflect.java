/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.helper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class Reflect {

    // ---------------------------------------------------------------------
    // Static API used as entrance points to the fluent API
    // ---------------------------------------------------------------------

    /**
     * Wrap a class name.
     * <p>
     * This is the same as calling <code>onClass(Class.forName(name))</code>
     *
     * @param name A fully qualified class name
     * @return A wrapped class object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #onClass(Class)
     */
    public static Reflect onClass(final String name) throws ReflectException {
        return onClass(forName(name));
    }

    /**
     * Wrap a class name, loading it via a given class loader.
     * <p>
     * This is the same as calling
     * <code>onClass(Class.forName(name, classLoader))</code>
     *
     * @param name A fully qualified class name.
     * @param classLoader The class loader in whose context the class should be
     *            loaded.
     * @return A wrapped class object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #onClass(Class)
     */
    public static Reflect onClass(final String name, final ClassLoader classLoader) throws ReflectException {
        return onClass(forName(name, classLoader));
    }

    /**
     * Wrap a class.
     * <p>
     * Use this when you want to access static fields and methods on a {@link Class}
     * object, or as a basis for constructing objects of that class using
     * {@link #create(Object...)}
     *
     * @param clazz The class to be wrapped
     * @return A wrapped class object, to be used for further reflection.
     */
    public static Reflect onClass(final Class<?> clazz) {
        return new Reflect(clazz);
    }

    /**
     * Wrap an object.
     * <p>
     * Use this when you want to access instance fields and methods on any
     * {@link Object}
     *
     * @param object The object to be wrapped
     * @return A wrapped object, to be used for further reflection.
     */
    public static Reflect on(final Object object) {
        return new Reflect(object == null ? Object.class : object.getClass(), object);
    }

    private static Reflect on(final Class<?> type, final Object object) {
        return new Reflect(type, object);
    }

    /**
     * Conveniently render an {@link AccessibleObject} accessible.
     * <p>
     * To prevent {@link SecurityException}, this is only done if the argument
     * object and its declaring class are non-public.
     *
     * @param accessible The object to render accessible
     * @return The argument object rendered accessible
     */
    public static <T extends AccessibleObject> T accessible(final T accessible) {
        if (accessible == null) {
            return null;
        }
        if (accessible instanceof Member) {
            final Member member = (Member) accessible;
            if (Modifier.isPublic(member.getModifiers())
                    && Modifier.isPublic(member.getDeclaringClass().getModifiers())) {
                return accessible;
            }
        }
        // The accessible flag is set to false by default, also for public members.
        if (!accessible.isAccessible()) {
            accessible.setAccessible(true);
        }
        return accessible;
    }

    // ---------------------------------------------------------------------
    // Members
    // ---------------------------------------------------------------------

    static final Constructor<MethodHandles.Lookup> CACHED_LOOKUP_CONSTRUCTOR;

    static {
        Constructor<MethodHandles.Lookup> result;
        try {
            try {
                Optional.class.getMethod("stream");
                result = null;
            }
            // A JDK 9 guard that prevents "Illegal reflective access operation"
            // warnings when running the below on JDK 9+
            catch (final NoSuchMethodException e) {
                result = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                if (!result.isAccessible()) {
                    result.setAccessible(true);
                }
            }
        }
        // Can no longer access the above in JDK 9
        catch (final Throwable ignore) {
            result = null;
        }
        CACHED_LOOKUP_CONSTRUCTOR = result;
    }

    /**
     * The type of the wrapped object.
     */
    private final Class<?> type;

    /**
     * The wrapped object.
     */
    private final Object object;

    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------

    private Reflect(final Class<?> type) {
        this(type, type);
    }

    private Reflect(final Class<?> type, final Object object) {
        this.type   = type;
        this.object = object;
    }

    // ---------------------------------------------------------------------
    // Fluent Reflection API
    // ---------------------------------------------------------------------

    /**
     * Get the wrapped object
     *
     * @param <T> A convenience generic parameter for automatic unsafe casting
     */
    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) object;
    }

    /**
     * Set a field value.
     * <p>
     * This is roughly equivalent to {@link Field#set(Object, Object)}. If the
     * wrapped object is a {@link Class}, then this will set a value to a static
     * member field. If the wrapped object is any other {@link Object}, then this
     * will set a value to an instance member field.
     * <p>
     * This method is also capable of setting the value of (static) final fields.
     * This may be convenient in situations where no {@link SecurityManager} is
     * expected to prevent this, but do note that (especially static) final fields
     * may already have been inlined by the javac and/or JIT and relevant code
     * deleted from the runtime verison of your program, so setting these fields
     * might not have any effect on your execution.
     * <p>
     * For restrictions of usage regarding setting values on final fields check:
     * <a href=
     * "http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection">http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection</a>
     * ... and <a href=
     * "http://pveentjer.blogspot.co.at/2017/01/final-static-boolean-jit.html">http://pveentjer.blogspot.co.at/2017/01/final-static-boolean-jit.html</a>
     *
     * @param name The field name
     * @param value The new field value
     * @return The same wrapped object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect set(final String name, final Object value) throws ReflectException {
        try {
            final Field field = field0(name);

            if ((field.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
                try {
                    final Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                }

                // [#48] E.g. Android doesn't have this field
                catch (final NoSuchFieldException ignore) {
                }
            }

            field.set(object, unwrap(value));
            return this;
        } catch (final Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Get a field value.
     * <p>
     * This is roughly equivalent to {@link Field#get(Object)}. If the wrapped
     * object is a {@link Class}, then this will get a value from a static member
     * field. If the wrapped object is any other {@link Object}, then this will get
     * a value from an instance member field.
     * <p>
     * If you want to "navigate" to a wrapped version of the field, use
     * {@link #field(String)} instead.
     *
     * @param name The field name
     * @return The field value
     * @throws ReflectException If any reflection exception occurred.
     * @see #field(String)
     */
    public <T> T get(final String name) throws ReflectException {
        return field(name).<T> get();
    }

    /**
     * Get a wrapped field.
     * <p>
     * This is roughly equivalent to {@link Field#get(Object)}. If the wrapped
     * object is a {@link Class}, then this will wrap a static member field. If the
     * wrapped object is any other {@link Object}, then this wrap an instance member
     * field.
     *
     * @param name The field name
     * @return The wrapped field
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect field(final String name) throws ReflectException {
        try {
            final Field field = field0(name);
            return on(field.getType(), field.get(object));
        } catch (final Exception e) {
            throw new ReflectException(e);
        }
    }

    private Field field0(final String name) throws ReflectException {
        Class<?> t = type();

        // Try getting a public field
        try {
            return accessible(t.getField(name));
        }
        // Try again, getting a non-public field
        catch (final NoSuchFieldException e) {
            do {
                try {
                    return accessible(t.getDeclaredField(name));
                } catch (final NoSuchFieldException ignore) {
                }

                t = t.getSuperclass();
            } while (t != null);
            throw new ReflectException(e);
        }
    }

    /**
     * Get a Map containing field names and wrapped values for the fields' values.
     * <p>
     * If the wrapped object is a {@link Class}, then this will return static
     * fields. If the wrapped object is any other {@link Object}, then this will
     * return instance fields.
     * <p>
     * These two calls are equivalent <code><pre>
     * on(object).field("myField");
     * on(object).fields().get("myField");
     * </pre></code>
     *
     * @return A map containing field names and wrapped values.
     */
    public Map<String, Reflect> fields() {
        final Map<String, Reflect> result = new LinkedHashMap<>();
        Class<?>                   t      = type();
        do {
            for (final Field field : t.getDeclaredFields()) {
                if (type != object ^ Modifier.isStatic(field.getModifiers())) {
                    final String name = field.getName();
                    result.computeIfAbsent(name, key -> field(name));
                }
            }
            t = t.getSuperclass();
        } while (t != null);
        return result;
    }

    /**
     * Call a method by its name.
     * <p>
     * This is a convenience method for calling
     * <code>call(name, new Object[0])</code>
     *
     * @param name The method name
     * @return The wrapped method result or the same wrapped object if the method
     *         returns <code>void</code>, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #call(String, Object...)
     */
    public Reflect call(final String name) throws ReflectException {
        return call(name, new Object[0]);
    }

    /**
     * Call a method by its name.
     * <p>
     * This is roughly equivalent to {@link Method#invoke(Object, Object...)}. If
     * the wrapped object is a {@link Class}, then this will invoke a static method.
     * If the wrapped object is any other {@link Object}, then this will invoke an
     * instance method.
     * <p>
     * Just like {@link Method#invoke(Object, Object...)}, this will try to wrap
     * primitive types or unwrap primitive type wrappers if applicable. If several
     * methods are applicable, by that rule, the first one encountered is called.
     * i.e. when calling <code><pre>
     * on(...).call("method", 1, 1);
     * </pre></code> The first of the following methods will be called: <code><pre>
     * public void method(int param1, Integer param2);
     * public void method(Integer param1, int param2);
     * public void method(Number param1, Number param2);
     * public void method(Number param1, Object param2);
     * public void method(int param1, Object param2);
     * </pre></code>
     * <p>
     * The best matching method is searched for with the following strategy:
     * <ol>
     * <li>public method with exact signature match in class hierarchy</li>
     * <li>non-public method with exact signature match on declaring class</li>
     * <li>public method with similar signature in class hierarchy</li>
     * <li>non-public method with similar signature on declaring class</li>
     * </ol>
     *
     * @param name The method name
     * @param args The method arguments
     * @return The wrapped method result or the same wrapped object if the method
     *         returns <code>void</code>, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect call(final String name, final Object... args) throws ReflectException {
        final Class<?>[] types = types(args);

        // Try invoking the "canonical" method, i.e. the one with exact
        // matching argument types
        try {
            final Method method = exactMethod(name, types);
            return on(method, object, args);
        }
        // If there is no exact match, try to find a method that has a "similar"
        // signature if primitive argument types are converted to their wrappers
        catch (final NoSuchMethodException e) {
            try {
                final Method method = similarMethod(name, types);
                return on(method, object, args);
            } catch (final NoSuchMethodException e1) {
                throw new ReflectException(e1);
            }
        }
    }

    /**
     * Searches a method with the exact same signature as desired.
     * <p>
     * If a public method is found in the class hierarchy, this method is returned.
     * Otherwise a private method with the exact same signature is returned. If no
     * exact match could be found, we let the {@code NoSuchMethodException} pass
     * through.
     */
    private Method exactMethod(final String name, final Class<?>[] types) throws NoSuchMethodException {
        Class<?> t = type();

        // first priority: find a public method with exact signature match in class
        // hierarchy
        try {
            return t.getMethod(name, types);
        }
        // second priority: find a private method with exact signature match on
        // declaring class
        catch (final NoSuchMethodException e) {
            do {
                try {
                    return t.getDeclaredMethod(name, types);
                } catch (final NoSuchMethodException ignore) {
                }

                t = t.getSuperclass();
            } while (t != null);
            throw new NoSuchMethodException();
        }
    }

    /**
     * Searches a method with a similar signature as desired using
     * {@link #isSimilarSignature(java.lang.reflect.Method, String, Class[])}.
     * <p>
     * First public methods are searched in the class hierarchy, then private
     * methods on the declaring class. If a method could be found, it is returned,
     * otherwise a {@code NoSuchMethodException} is thrown.
     */
    private Method similarMethod(final String name, final Class<?>[] types) throws NoSuchMethodException {
        Class<?> t = type();

        // first priority: find a public method with a "similar" signature in class
        // hierarchy
        // similar interpreted in when primitive argument types are converted to their
        // wrappers
        for (final Method method : t.getMethods()) {
            if (isSimilarSignature(method, name, types)) {
                return method;
            }
        }
        // second priority: find a non-public method with a "similar" signature on
        // declaring class
        do {
            for (final Method method : t.getDeclaredMethods()) {
                if (isSimilarSignature(method, name, types)) {
                    return method;
                }
            }
            t = t.getSuperclass();
        } while (t != null);

        throw new NoSuchMethodException("No similar method " + name + " with params " + Arrays.toString(types)
                + " could be found on type " + type() + ".");
    }

    /**
     * Determines if a method has a "similar" signature, especially if wrapping
     * primitive argument types would result in an exactly matching signature.
     */
    private boolean isSimilarSignature(final Method possiblyMatchingMethod,
                                       final String desiredMethodName,
                                       final Class<?>[] desiredParamTypes) {
        return possiblyMatchingMethod.getName().equals(desiredMethodName)
                && match(possiblyMatchingMethod.getParameterTypes(), desiredParamTypes);
    }

    /**
     * Call a constructor.
     * <p>
     * This is a convenience method for calling <code>create(new Object[0])</code>
     *
     * @return The wrapped new object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #create(Object...)
     */
    public Reflect create() throws ReflectException {
        return create(new Object[0]);
    }

    /**
     * Call a constructor.
     * <p>
     * This is roughly equivalent to {@link Constructor#newInstance(Object...)}. If
     * the wrapped object is a {@link Class}, then this will create a new object of
     * that class. If the wrapped object is any other {@link Object}, then this will
     * create a new object of the same type.
     * <p>
     * Just like {@link Constructor#newInstance(Object...)}, this will try to wrap
     * primitive types or unwrap primitive type wrappers if applicable. If several
     * constructors are applicable, by that rule, the first one encountered is
     * called. i.e. when calling <code><pre>
     * on(C.class).create(1, 1);
     * </pre></code> The first of the following constructors will be applied:
     * <code><pre>
     * public C(int param1, Integer param2);
     * public C(Integer param1, int param2);
     * public C(Number param1, Number param2);
     * public C(Number param1, Object param2);
     * public C(int param1, Object param2);
     * </pre></code>
     *
     * @param args The constructor arguments
     * @return The wrapped new object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect create(final Object... args) throws ReflectException {
        final Class<?>[] types = types(args);

        // Try invoking the "canonical" constructor, i.e. the one with exact
        // matching argument types
        try {
            final Constructor<?> constructor = type().getDeclaredConstructor(types);
            return on(constructor, args);
        }
        // If there is no exact match, try to find one that has a "similar"
        // signature if primitive argument types are converted to their wrappers
        catch (final NoSuchMethodException e) {
            for (final Constructor<?> constructor : type().getDeclaredConstructors()) {
                if (match(constructor.getParameterTypes(), types)) {
                    return on(constructor, args);
                }
            }
            throw new ReflectException(e);
        }
    }

    /**
     * Create a proxy for the wrapped object allowing to typesafely invoke methods
     * on it using a custom interface.
     *
     * @param proxyType The interface type that is implemented by the proxy
     * @return A proxy for the wrapped object
     */
    public <P> P as(final Class<P> proxyType) {
        return as(proxyType, new Class[0]);
    }

    /**
     * Create a pro(tom i, , aram) -> rface type that is implemented by the proxy
     *
     * @param additionalInterfaces Additional interfaces that are implemented by the
     *            proxy
     * @return A proxy for the wrapped object
     */
    @SuppressWarnings("unchecked")
    public <P> P as(final Class<P> proxyType, final Class<?>... additionalInterfaces) {
        final boolean           isMap   = object instanceof Map;
        final InvocationHandler handler = (proxy, method, args) -> {
                                            final String name = method.getName();

                                            // Actual method name matches always come first
                                            try {
                                                return on(type, object).call(name, args).get();
                                            } catch (final ReflectException e) {
                                                if (isMap) {
                                                    final Map<String, Object> map    = (Map<String, Object>) object;
                                                    final int                 length = args == null ? 0 : args.length;

                                                    if (length == 0 && name.startsWith("get")) {
                                                        return map.get(property(name.substring(3)));
                                                    }
                                                    if (length == 0 && name.startsWith("is")) {
                                                        return map.get(property(name.substring(2)));
                                                    }
                                                    if (length == 1 && name.startsWith("set")) {
                                                        map.put(property(name.substring(3)), args[0]);
                                                        return null;
                                                    }
                                                }

                                                if (method.isDefault()) {
                                                    Lookup proxyLookup = null;
                                                    // Java 9 version
                                                    if (CACHED_LOOKUP_CONSTRUCTOR == null) {

                                                        // Java 9 version for Java 8 distribution
                                                        if (proxyLookup == null) {
                                                            proxyLookup = onClass(MethodHandles.class)
                                                                    .call("privateLookupIn", proxyType,
                                                                            MethodHandles.lookup())
                                                                    .call("in", proxyType).<Lookup> get();
                                                        }
                                                    } else {
                                                        proxyLookup = CACHED_LOOKUP_CONSTRUCTOR.newInstance(proxyType);
                                                    }
                                                    return proxyLookup.unreflectSpecial(method, proxyType).bindTo(proxy)
                                                            .invokeWithArguments(args);
                                                }
                                                throw e;
                                            }
                                        };

        final Class<?>[] interfaces = new Class[1 + additionalInterfaces.length];
        interfaces[0] = proxyType;
        System.arraycopy(additionalInterfaces, 0, interfaces, 1, additionalInterfaces.length);
        return (P) Proxy.newProxyInstance(proxyType.getClassLoader(), interfaces, handler);
    }

    /**
     * Get the POJO property name of an getter/setter
     */
    private static String property(final String string) {
        final int length = string.length();

        if (length == 0) {
            return "";
        }
        if (length == 1) {
            return string.toLowerCase();
        }
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    // ---------------------------------------------------------------------
    // Object API
    // ---------------------------------------------------------------------

    /**
     * Check whether two arrays of types match, converting primitive types to their
     * corresponding wrappers.
     */
    private boolean match(final Class<?>[] declaredTypes, final Class<?>[] actualTypes) {
        if (declaredTypes.length != actualTypes.length) {
            return false;
        }
        for (int i = 0; i < actualTypes.length; i++) {
            if (actualTypes[i] == NULL.class || wrapper(declaredTypes[i]).isAssignableFrom(wrapper(actualTypes[i]))) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Reflect) {
            return object.equals(((Reflect) obj).get());
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(object);
    }

    // ---------------------------------------------------------------------
    // Utility methods
    // ---------------------------------------------------------------------

    /**
     * Wrap an object created from a constructor
     */
    private static Reflect on(final Constructor<?> constructor, final Object... args) throws ReflectException {
        try {
            return on(constructor.getDeclaringClass(), accessible(constructor).newInstance(args));
        } catch (final Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Wrap an object returned from a method
     */
    private static Reflect on(final Method method, final Object object, final Object... args) throws ReflectException {
        try {
            accessible(method);

            if (method.getReturnType() == void.class) {
                method.invoke(object, args);
                return on(object);
            }
            return on(method.invoke(object, args));
        } catch (final Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Unwrap an object
     */
    private static Object unwrap(final Object object) {
        if (object instanceof Reflect) {
            return ((Reflect) object).get();
        }

        return object;
    }

    /**
     * Get an array of types for an array of objects
     *
     * @see Object#getClass()
     */
    private static Class<?>[] types(final Object... values) {
        if (values == null) {
            return new Class[0];
        }

        final Class<?>[] result = new Class[values.length];

        for (int i = 0; i < values.length; i++) {
            final Object value = values[i];
            result[i] = value == null ? NULL.class : value.getClass();
        }

        return result;
    }

    /**
     * Load a class
     *
     * @see Class#forName(String)
     */
    private static Class<?> forName(final String name) throws ReflectException {
        try {
            return Class.forName(name);
        } catch (final Exception e) {
            throw new ReflectException(e);
        }
    }

    private static Class<?> forName(final String name, final ClassLoader classLoader) throws ReflectException {
        try {
            return Class.forName(name, true, classLoader);
        } catch (final Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Get the type of the wrapped object.
     *
     * @see Object#getClass()
     */
    public Class<?> type() {
        return type;
    }

    /**
     * Get a wrapper type for a primitive type, or the argument type itself, if it
     * is not a primitive type.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> wrapper(final Class<T> type) {
        if (type == null) {
            return null;
        }
        if (type.isPrimitive()) {
            if (boolean.class == type) {
                return (Class<T>) Boolean.class;
            }
            if (int.class == type) {
                return (Class<T>) Integer.class;
            }
            if (long.class == type) {
                return (Class<T>) Long.class;
            }
            if (short.class == type) {
                return (Class<T>) Short.class;
            }
            if (byte.class == type) {
                return (Class<T>) Byte.class;
            }
            if (double.class == type) {
                return (Class<T>) Double.class;
            }
            if (float.class == type) {
                return (Class<T>) Float.class;
            }
            if (char.class == type) {
                return (Class<T>) Character.class;
            }
            if (void.class == type) {
                return (Class<T>) Void.class;
            }
        }

        return type;
    }

    private static class NULL {
    }

    public static class ReflectException extends RuntimeException {

        private static final long serialVersionUID = -6213149635297151442L;

        public ReflectException(final String message) {
            super(message);
        }

        public ReflectException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public ReflectException() {
        }

        public ReflectException(final Throwable cause) {
            super(cause);
        }
    }
}
