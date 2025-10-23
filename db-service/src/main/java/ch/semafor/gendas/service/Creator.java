/*
 * Copyright 2010 Semafor Informatik & Energie AG, Basel, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 */
package ch.semafor.gendas.service;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.GregorianCalendar;

//ToDO, check, PMD: AbstractClassWithoutAbstractMethod
public abstract class Creator {
    /**
     * checks if clazz is primitive, i.e. does not include other types
     *
     * @param clazz
     * @return true if clazz has no other types false otherwise
     */
    protected boolean isPrimitiveType(final Class clazz) {
        return clazz.equals(String.class)
                || clazz.equals(Integer.class)
                || clazz.equals(Double.class)
                || clazz.equals(java.util.Date.class)
                || clazz.equals(Long.class)
                || clazz.equals(Boolean.class)
                || clazz.equals(java.math.BigDecimal.class)
                || clazz.isEnum()
                || clazz.isAssignableFrom(javax.xml.datatype.XMLGregorianCalendar.class)
                || clazz.isAssignableFrom(GregorianCalendar.class)
                || clazz.isPrimitive();
    }

    protected boolean isSimpleList(Class retType) {
        if (!retType.equals(java.util.List.class)) {
            return false;
        }
        Class genArgType = getGenericArgType(retType);
        if (genArgType != null) {
            return isPrimitiveType(genArgType);
        }
        return true;
    }

    /**
     * creates the property name from method name by
     * eliminating the first 3 characters and setting the 4th character to
     * lower case. Example "getMyProperty" gives "myProperty"
     *
     * @param method
     * @return property name or null if name cannot be extracted
     */
    protected String getPropertyName(final Method method) {
        int startsAt = 3;
        if (method.getName().startsWith("is")) {
            startsAt = 2;
        }
        final StringBuffer name =
                new StringBuffer(method.getName().substring(startsAt));
        if (name.length() < 1) {
            return null; //throw new CoreException("illegal method: " + method.toString());
        }
        name.setCharAt(0, Character
                .toLowerCase(name.charAt(0)));
        return name.toString();
    }

    /**
     * creates the method name from property name by setting first the prefix
     * and appending the property name with the first letter in capital.
     *
     * @param prefix
     * @param prop
     * @return method name (null if not found)
     */
    protected String getMethodName(final String prefix, final String prop) {
        if (prefix == null || prefix.length() < 1 || prop == null || prop.length() < 1)
            return null;
        final StringBuffer buf = new StringBuffer(prefix);
        buf.append(prop);
        buf.setCharAt(prefix.length(), Character.toUpperCase(prop.charAt(0)));
        return buf.toString();
    }

    /**
     * creates the generic arg type from parameterized type name
     * Example: type=List&lt;String&gt; gives String.class
     *
     * @param type to get generic type from
     * @return generic arg type or null if not exactly 1
     */
    protected Class<?> getGenericArgType(final Type type) {
        if (!(type instanceof ParameterizedType pType)) {
            return null;
        }
        final Type[] typeArguments = pType.getActualTypeArguments();
        if (typeArguments.length == 1) {
            return (Class<?>) typeArguments[0];
        }
        return null;
    }
}
