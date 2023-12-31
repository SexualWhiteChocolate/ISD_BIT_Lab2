package ru.sber.karimullin.hw_3.Factory;

import ru.sber.karimullin.hw_3.Generator.*;
import ru.sber.karimullin.hw_3.RuntimeCompiler.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@SuppressWarnings({"unchecked", "StringConcatenationInsideStringBufferAppend"})
public class Factory {
    public static <T> Generator<T> factoryToJSON(T obj)
            throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException,
                   IllegalAccessException, ClassNotFoundException {
        Class<?> clazz = obj.getClass();

        Counter.clear();

        if (clazz.isArray()) {
            throw new IllegalArgumentException();
        }

        StringBuilder toCompile = new StringBuilder();
        toCompile.append("import ru.sber.karimullin.hw_3.Generator.*;\n");

        if (clazzIsPrintable(clazz)) {
            toCompile.append("import " + clazz.toString().split(" ")[1] + ";\n");
        } else if (!clazzIsPrintable(clazz) && !clazz.getPackageName().isEmpty()) {
            toCompile.append("import " + clazz.getPackageName() + "." + clazz.toString().split(" ")[1] + ";\n");
        }
        toCompile.append("import java.util.Collection;\n");
        toCompile.append("import java.util.Iterator;\n");
        toCompile.append("import java.util.Map;\n");
        toCompile.append("\n");
        toCompile.append("public class GeneratorToJSONFrom" + clazz.getSimpleName() + "<T extends " +
                clazz.toString().split(" ")[1] + "> implements Generator<T> {\n");
        toCompile.append("@Override\n");
        toCompile.append("public String generate(T elem0_0) {\n");
        toCompile.append("StringBuilder output = new StringBuilder();\n");
        toCompile.append("output.append(\"{\\n\");\n");

        if (clazzIsPrintable(clazz)) {
            toCompile.append("output.append(\"    \\\"\" + \"" + clazz.getSimpleName() + "\" + \"\\\": \");\n");
            recursiveJSON(clazz, null, obj, toCompile, 8 + clazz.getSimpleName().length(), 0,
                    true, false, false, false);
            toCompile.append("output.append(\"\\n\");\n");
        } else {
            recursiveJSON(clazz, null, obj, toCompile, 4, 0, true,
                    false, false, true);
        }

        toCompile.append("output.append(\"}\\n\");\n");
        toCompile.append("return output.toString();\n");
        toCompile.append("}\n");
        toCompile.append("}\n");

        RuntimeCompiler.compile("GeneratorToJSONFrom" + clazz.getSimpleName(), toCompile.toString());

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{RuntimeCompiler.getFile().toURI().toURL()});
        Class<?> compiledClass = Class.forName("GeneratorToJSONFrom" + clazz.getSimpleName(),
                true, classLoader);

        return (Generator<T>) compiledClass.getConstructor().newInstance();
    }

    private static boolean clazzIsPrintable(Class<?> clazz) {
        return clazz == String.class || clazz == Integer.class || clazz == Double.class || clazz == Float.class ||
                clazz == Long.class || clazz == Short.class || clazz == Byte.class || clazz == Date.class ||
                clazz == File.class || Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    private static boolean clazzIsPrimitive(Class<?> clazz) {
        return clazz == String.class || clazz == Integer.class || clazz == Double.class || clazz == Float.class ||
                clazz == Long.class || clazz == Short.class || clazz == Byte.class || clazz == Date.class ||
                clazz == File.class;
    }

    private static <T> void recursiveJSON(Class<?> clazz, ParameterizedType type, T obj, StringBuilder toCompile,
                                          int recursionLevelWithSigns, int recursionLevel, boolean isOuter,
                                          boolean isFromMap, boolean isKey, boolean isCustom)
            throws InvocationTargetException, IllegalAccessException {
        if (clazzIsPrimitive(clazz)) {
            processPrimitive(clazz, toCompile, recursionLevel, isFromMap, isKey);
            return;
        }

        if ((clazz == null || obj == null) && !isCustom) {
            toCompile.append("output.append(\"null\");\n");
            return;
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            processCollection(type, obj, toCompile, recursionLevelWithSigns, recursionLevel, isFromMap, isKey, isCustom);
            return;
        }

        if (clazz.isArray()) {
            processArray(clazz, obj, toCompile, recursionLevelWithSigns, recursionLevel, isFromMap, isKey, isCustom);
            return;
        }

        if (Map.class.isAssignableFrom(clazz)) {
            processMap(type, obj, toCompile, recursionLevelWithSigns, recursionLevel, isFromMap, isKey, isCustom);
            return;
        }

        processCustom(clazz, obj, toCompile, recursionLevelWithSigns, recursionLevel, isOuter, isFromMap, isKey,
                isCustom);
    }

    private static <T> void processCustom(Class<?> clazz, T obj, StringBuilder toCompile, int recursionLevelWithSigns,
                                          int recursionLevel, boolean isOuter, boolean isFromMap, boolean isKey,
                                          boolean isCustom)
            throws InvocationTargetException, IllegalAccessException {

        Method[] methods = clazz.getMethods();
        String signal = Counter.getCounter(recursionLevel, isFromMap, isKey);

        for (Method method : methods) {

            if (method.getName().startsWith("get") && method.getParameterCount() == 0 && method.getName().length() > 3
                    && !method.getReturnType().equals(void.class) && !method.getName().equals("getClass")) {

                Counter.counterPlus();

                String signal_plus1 = Counter.getCounter(recursionLevel + 1, isFromMap, isKey);
                String fieldName = method.getName().substring(3);
                Class<?> anotherClazz = method.getReturnType();
                String anotherClazzType = anotherClazz.toString().split(" ")[1];

                if (anotherClazz.isArray()) {
                    anotherClazzType = anotherClazzType.substring(2, anotherClazzType.length() - 1) + "[]";
                }

                toCompile.append(anotherClazzType + " elem" + signal_plus1 + " = (" +
                        anotherClazzType + ") elem" + signal + ".get" + fieldName + "();\n");

                toCompile.append("output.append(\"" + " ".repeat(recursionLevelWithSigns) + "\\\"" +
                        fieldName.toLowerCase() + "\\\": \");\n");

                if (isCustom) {
                    if (method.getGenericReturnType() instanceof ParameterizedType) {
                        recursiveJSON(anotherClazz, (ParameterizedType) method.getGenericReturnType(), null,
                                toCompile, recursionLevelWithSigns + 4 + fieldName.length(),
                                recursionLevel + 1, false, isFromMap, isKey, true);
                    } else {
                        recursiveJSON(anotherClazz, null, null,
                                toCompile, recursionLevelWithSigns + 4 + fieldName.length(),
                                recursionLevel + 1, false, isFromMap, isKey, true);
                    }
                } else {
                    recursiveJSON(anotherClazz, null, method.invoke(obj),
                            toCompile, recursionLevelWithSigns + 4 + fieldName.length(),
                            recursionLevel + 1, false, isFromMap, isKey, false);
                }

                toCompile.append("output.append(\"\\n\");\n");
            }
        }

        if (methods.length != 0 && !isOuter) {
            toCompile.append("output.delete(output.length() - 1, output.length());\n");
        }
    }

    private static void processPrimitive(Class<?> clazz, StringBuilder toCompile, int recursionLevel,
                                         boolean isFromMap, boolean isKey) {
        String signal = Counter.getCounter(recursionLevel, isFromMap, isKey);
        if (clazz == String.class) {
            toCompile.append("output.append(\"\\\"\" + elem" + signal + " + \"\\\"\");\n");
            return;
        }
        toCompile.append("output.append(elem" + signal + ");\n");
    }

    private static <T> void processCollection(ParameterizedType type, T obj, StringBuilder toCompile,
                                              int recursionLevelWithSigns, int recursionLevel, boolean isFromMap,
                                              boolean isKey, boolean isCustom)
            throws InvocationTargetException, IllegalAccessException {
        Class<?> genericClazz = null;
        Collection<?> collection = null;

        if (isCustom) {
            if (type.getActualTypeArguments()[0] instanceof ParameterizedType) {
                genericClazz = (Class<?>) ((ParameterizedType) type.getActualTypeArguments()[0]).getRawType();
            } else {
                genericClazz = (Class<?>) type.getActualTypeArguments()[0];
            }
        } else {
            collection = (Collection<?>) obj;
            Iterator<?> iterator = collection.iterator();
            boolean gotIt = false;
            while (iterator.hasNext() && !gotIt) {
                genericClazz = collection.iterator().next().getClass();
                gotIt = true;
            }

            if (genericClazz == null) {
                toCompile.append("output.append(\"null\");\n");
                return;
            }
        }

        String signal = Counter.getCounter(recursionLevel, isFromMap, isKey);
        String signal_plus1 = Counter.getCounter(recursionLevel + 1, isFromMap, isKey);

        toCompile.append("output.append(\"[\");\n");
        toCompile.append("boolean isWhileWorked" + signal + " = false;\n");
        toCompile.append("Collection<?> collection" + signal + " = (Collection<?>) elem" + signal + ";\n");
        toCompile.append("Iterator<?> iterator" + signal + " = collection" + signal + ".iterator();\n");
        toCompile.append("while(iterator" + signal + ".hasNext()) {\n");
        toCompile.append(genericClazz.toString().split(" ")[1] + " elem" + signal_plus1 +
                " = (" + genericClazz.toString().split(" ")[1] + ") iterator" + signal + ".next();\n");

        if (isCustom) {
            if (type.getActualTypeArguments()[0] instanceof ParameterizedType) {
                recursiveJSON(genericClazz, (ParameterizedType) type.getActualTypeArguments()[0], null, toCompile,
                        recursionLevelWithSigns,recursionLevel + 1, false, isFromMap, isKey,
                        true);
            } else {
                recursiveJSON(genericClazz, null, null, toCompile, recursionLevelWithSigns,
                        recursionLevel + 1, false, isFromMap, isKey, true);
            }

        } else {
            recursiveJSON(genericClazz, null, collection.iterator().next(), toCompile, recursionLevelWithSigns,
                    recursionLevel + 1, false, isFromMap, isKey, false);
        }

        toCompile.append("output.append(\",\");\n");
        toCompile.append("isWhileWorked" + signal + " = true;\n");
        toCompile.append("}\n");
        toCompile.append("if (isWhileWorked" + signal + ") {\n");
        toCompile.append("output.deleteCharAt(output.length() - 1);\n");
        toCompile.append("}\n");
        toCompile.append("output.append(\"]\");\n");
    }

    private static <T> void processArray(Class<?> clazz, T obj, StringBuilder toCompile, int recursionLevelWithSigns,
                                         int recursionLevel, boolean isFromMap, boolean isKey, boolean isCustom)
            throws InvocationTargetException, IllegalAccessException {

        Class<?> genericClazz = clazz.getComponentType();
        Object[] tempArray = (Object[]) obj;

        String signal = Counter.getCounter(recursionLevel, isFromMap, isKey);
        String signal_plus1 = Counter.getCounter(recursionLevel + 1, isFromMap, isKey);
        String arrayType = clazz.toString().split(" ")[1];

        arrayType = arrayType.substring(2, arrayType.length() - 1);

        toCompile.append("output.append(\"[\");\n");
        toCompile.append("boolean isWhileWorked" + signal + " = false;\n");
        toCompile.append(arrayType + "[] array" + signal + " = (" + arrayType + "[]) elem" + signal + ";\n");
        toCompile.append("for (int i = 0; i < array" + signal + ".length; ++i) {\n");
        toCompile.append(genericClazz.toString().split(" ")[1] + " elem" + signal_plus1 +
                " = (" + genericClazz.toString().split(" ")[1] + ") array" + signal + "[i];\n");

        if (isCustom) {
            recursiveJSON(genericClazz, null, null, toCompile, recursionLevelWithSigns,
                    recursionLevel + 1, false, isFromMap, isKey, true);
        } else {
            if (tempArray.length != 0) {
                recursiveJSON(genericClazz, null, tempArray[0], toCompile, recursionLevelWithSigns,
                        recursionLevel + 1, false, isFromMap, isKey, false);
            } else {
                recursiveJSON(genericClazz, null, null, toCompile, recursionLevelWithSigns,
                        recursionLevel + 1, false, isFromMap, isKey, false);
            }
        }

        toCompile.append("output.append(\",\");\n");
        toCompile.append("isWhileWorked" + signal + " = true;\n");
        toCompile.append("}\n");
        toCompile.append("if (isWhileWorked" + signal + ") {\n");
        toCompile.append("output.deleteCharAt(output.length() - 1);\n");
        toCompile.append("}\n");
        toCompile.append("output.append(\"]\");\n");
    }

    private static <T> void processMap(ParameterizedType type, T obj, StringBuilder toCompile,
                                       int recursionLevelWithSigns, int recursionLevel, boolean isFromMap,
                                       boolean isKey, boolean isCustom)
            throws InvocationTargetException, IllegalAccessException {
        Class<?> genericClazz1 = null;
        Class<?> genericClazz2 = null;
        Map<?, ?> map = null;

        if (isCustom) {
            if (type.getActualTypeArguments()[0] instanceof ParameterizedType) {
                genericClazz1 = (Class<?>) ((ParameterizedType) type.getActualTypeArguments()[0]).getRawType();
            } else {
                genericClazz1 = (Class<?>) type.getActualTypeArguments()[0];
            }

            if (type.getActualTypeArguments()[1] instanceof ParameterizedType) {
                genericClazz2 = (Class<?>) ((ParameterizedType) type.getActualTypeArguments()[1]).getRawType();
            } else {
                genericClazz2 = (Class<?>) type.getActualTypeArguments()[1];
            }
        } else {
            map = (Map<?, ?>) obj;
            Iterator<?> iterator = map.entrySet().iterator();
            boolean gotIt = false;
            while (iterator.hasNext() && !gotIt) {
                genericClazz1 = map.entrySet().iterator().next().getKey().getClass();
                genericClazz2 = map.entrySet().iterator().next().getValue().getClass();
                gotIt = true;
            }

            if (genericClazz1 == null) {
                toCompile.append("output.append(\"null,null\");\n");
                return;
            }
        }

        String signal = Counter.getCounter(recursionLevel, isFromMap, isKey);
        String signal_plus1 = Counter.getCounter(recursionLevel + 1, isFromMap, isKey);

        toCompile.append("output.append(\"{\");\n");
        toCompile.append("boolean isWhileWorked" + signal + " = false;\n");
        toCompile.append("Map<?, ?> map" + signal + " = (Map<?, ?>) elem" + signal + ";\n");
        toCompile.append("Iterator<?> iterator" + signal + " = map" + signal + ".entrySet().iterator();\n");
        toCompile.append("while(iterator" + signal + ".hasNext()) {\n");
        toCompile.append("Map.Entry<?, ?> entry" + signal + " = (Map.Entry<?, ?>) iterator" + signal + ".next();\n");

        toCompile.append("output.append(\"\\\"\");\n");
        toCompile.append(genericClazz1.toString().split(" ")[1] + " elem" + signal_plus1 + "Key" +
                " = (" + genericClazz1.toString().split(" ")[1] + ") entry" + signal + ".getKey();\n");

        if (isCustom) {
            if (type.getActualTypeArguments()[0] instanceof ParameterizedType) {
                recursiveJSON(genericClazz1, (ParameterizedType) type.getActualTypeArguments()[0], null, toCompile,
                        recursionLevelWithSigns,recursionLevel + 1, false, true,
                        true, true);
            } else {
                recursiveJSON(genericClazz1, null, null, toCompile, recursionLevelWithSigns,
                        recursionLevel + 1, false, true, true, true);
            }
        } else {
            recursiveJSON(genericClazz1, null, map.entrySet().iterator().next().getKey(), toCompile,
                    recursionLevelWithSigns,recursionLevel + 1, false, true, true,
                    false);
        }

        toCompile.append("output.append(\"\\\"\");\n");
        toCompile.append("output.append(\":\");\n");
        toCompile.append(genericClazz2.toString().split(" ")[1] + " elem" + signal_plus1 + "Val" +
                " = (" + genericClazz2.toString().split(" ")[1] + ") entry" + signal + ".getValue();\n");

        if (isCustom) {
            if (type.getActualTypeArguments()[1] instanceof ParameterizedType) {
                recursiveJSON(genericClazz2, (ParameterizedType) type.getActualTypeArguments()[1], null, toCompile,
                        recursionLevelWithSigns,recursionLevel + 1, false, true,
                        false, true);
            } else {
                recursiveJSON(genericClazz2, null, null, toCompile, recursionLevelWithSigns,
                        recursionLevel + 1, false, true, false, true);
            }
        } else {
            recursiveJSON(genericClazz2, null, map.entrySet().iterator().next().getValue(), toCompile,
                    recursionLevelWithSigns,recursionLevel + 1, false, true, false,
                    false);
        }

        toCompile.append("output.append(\",\");\n");
        toCompile.append("isWhileWorked" + signal + " = true;\n");
        toCompile.append("}\n");
        toCompile.append("if (isWhileWorked" + signal + ") {\n");
        toCompile.append("output.deleteCharAt(output.length() - 1);\n");
        toCompile.append("}\n");
        toCompile.append("output.append(\"}\");\n");
    }

    static class Counter {
        private static int counter = 0;
        public static void counterPlus() {
            counter++;
        }
        public static String getCounter(int recursionLevel, boolean isFromMap, boolean isKey) {
            if (isFromMap) {
                if (isKey) {
                    return recursionLevel + "_" + counter + "Key";
                } else {
                    return recursionLevel + "_" + counter + "Val";
                }
            }
            return recursionLevel + "_" + counter;

        }
        public static void clear() {
            counter = 0;
        }
    }
}
