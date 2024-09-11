/*
 * Copyright (C) 2015-2024 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Lookup {

    public void setObjectProperty(Object o, String propertyName, Object value) {
        Class<?> c = o.getClass();
        String methodSuffix = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        String methodName = "set" + methodSuffix;
        if (value == null) {
            return;
        }
        Class<? extends Object> type = value.getClass();
        if (List.class.isAssignableFrom(type)) {
            type = List.class;
        } else if (Map.class.isAssignableFrom(type)) {
            type = Map.class;
        } else if (Boolean.class.isAssignableFrom(type)) {
            type = boolean.class;
        }

        Method m = getMethod(c, methodName, new Class[] {type});
        if (m != null) {
            try {
                invokeMethod(m, o, value);
            } catch (Exception e) {
            }
        }
    }

    protected Method getMethod(Class c, String methodName, Class[] args) {
        try {
            return c.getMethod(methodName, args);
        } catch (NoSuchMethodException nsme) {
            return null;
        }
    }

    protected Object invokeMethod(Method m, Object o, Object value)
            throws IllegalAccessException, InvocationTargetException {
        Object[] args = null;
        if (value != null) {
            args = new Object[] {value};
        }
        return m.invoke(o, args);
    }
}
