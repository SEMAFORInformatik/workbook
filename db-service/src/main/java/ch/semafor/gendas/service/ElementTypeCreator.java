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

import ch.semafor.gendas.dao.jpa.ElementTypeRepositoryJpa;
import ch.semafor.gendas.dao.jpa.PropertyTypeRepositoryJpa;
import ch.semafor.gendas.exceptions.ElementTypeCreationException;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementTypeCreator extends Creator {
    private static final Logger logger = LoggerFactory.getLogger(ElementTypeCreator.class);

    private final ElementTypeRepositoryJpa elementTypeRepositoryJpa;
    final private PropertyTypeRepositoryJpa propertyTypeRepositoryJpa;
    final private Map<String, ElementType> elementTypes;

    public ElementTypeCreator(final ElementTypeRepositoryJpa elementTypeDao,
                              final PropertyTypeRepositoryJpa propertyTypeDao) {
        super();
        this.elementTypeRepositoryJpa = elementTypeDao;
        this.propertyTypeRepositoryJpa = propertyTypeDao;
        this.elementTypes = new HashMap<String, ElementType>();
    }

    public void reset() {
        logger.debug("reset map {}", elementTypes.size());
        this.elementTypes.clear();
        logger.debug("resetted map {}", elementTypes.size());
    }

    private ElementType getElementType(
            final String name,
            final String idName,
            final String idVersion) {
        logger.debug("Begin getElementType({})", name);
        logger.debug(" -- {} types already available", elementTypes.size());
        ElementType et = elementTypes.get(name);
        if (et == null) { // not yet found
            logger.debug("Element Type {} not yet found", name);
            et = elementTypeRepositoryJpa.findByName(name);
            if (et == null) { // not yet created
                et = new ElementType(name);
                logger.debug("Prepare new Element Type {}", name);
                if (idName != null && idName.length() > 0) {
                    et.setBeanId(idName);
                }
                if (idVersion != null && idVersion.length() > 0) {
                    et.setBeanVersionId(idVersion);
                }
            }
            elementTypes.put(name, et);
        } else {
            et.setCreated();
        }
        return et;
    }

    /**
     * create property type of method
     *
     * @param method
     * @return property type
     */
    protected PropertyType createPropertyType(final Method method) {
        String name = getPropertyName(method);
        if (name != null && !name.equals("class")) {
            return createPropertyType(name, method.getGenericReturnType().toString(), null);
        }
        logger.warn("cannot determine property of method {}", method.getName());
        return null;
    }

    /**
     * create property type
     *
     * @param name of property type
     * @param type
     * @param unit
     * @return created property type
     */
    protected PropertyType createPropertyType(
            final String name,
            String type,
            String unit) {
        PropertyType p;
        logger.debug("check name '{}' type '{}' unit '{}'",
                name, type, unit);
        p = propertyTypeRepositoryJpa.findByName(name);
        if (p != null) {
            if ((unit == null && (p.getUnit()==null || p.getUnit().isEmpty())
            || p.getUnit().equals(unit))){ // note: liquibase will not create null units
                return p;
            }
            throw new ElementTypeCreationException("Conflicting units for " + name +": "
            + unit + " <--> " + p.getUnit());
        }
        if (unit == null) {
            p = new PropertyType(name, PropertyType.Type.get(type), "");
        } else {
            p = new PropertyType(name, PropertyType.Type.get(type), unit);
        }
        logger.debug("saving property type name: {} type: {}", p.getName(), type);
        return propertyTypeRepositoryJpa.save(p);
    }

    /**
     * create Element type
     *
     * @param typename of element type
     * @param typedef list of property and elementref types
     * @param idName
     * @param versionName
     * @return created element type
     */    public ElementType create(
            final String typename,
            final List<Map<String, Object>> typedef,
            final String idName,
            final String versionName) {
        //logger.debug("Begin create({})", typename);
        final ElementType elementType = getElementType(typename, idName, versionName);

        if (elementType.isCreated()) { // prevent infinite recursion
            logger.debug("ElementType {} is already created!", typename);
        } else {
            logger.debug("create new ElementType {}", typename);
            for (Map<String, Object> propdef : typedef) {
                if (!propdef.containsKey("props")) {
                    String propname = (String) propdef.get("name");
                    String unit = (String) propdef.get("unit");
                    String type = (String) propdef.get("type");
                    logger.debug("Type {} add property {}", elementType.getName(), propname);
                    PropertyType propType = this.createPropertyType(propname, type, unit);
                    elementType.add(propType);
                } else {
                    logger.debug("Type {} add Reference {}", elementType.getName(), propdef.get("name"));
                    final ElementType eType = this.create(
                            (String) propdef.get("type"),
                            (List<Map<String, Object>>) propdef.get("props"),
                            idName,
                            versionName);
                    elementTypeRepositoryJpa.save(eType);
                    elementType.addReference((String) propdef.get("name"), eType);
                }
            }
        }
        logger.debug("End create({})", typename);
        return elementType;
    }

    public ElementType create(
            final Class beanClass,
            final String idName,
            final String versionName) {
        final String name = beanClass.getCanonicalName();
        final ElementType elementType = getElementType(name, idName, versionName);
        logger.debug("{} created? {}", name, elementType.isCreated());
        if (!elementType.isCreated()) {
            // ToDo, unnest, PMD:AvoidDeeplyNestedIfStmts
            ReflectionUtils.doWithMethods(beanClass,
                    method -> {
                        if (method.getName().startsWith("get")
                                || method.getName().startsWith("is")
                                && !method.getName().equals("getClass")) {
                            Class retType = method.getReturnType();
                            PropertyType propType = createPropertyType(method);
                            if (isPrimitiveType(retType)) {
                                if (!(idName != null && idName.equals(propType.getName())) &&
                                        !(versionName != null && versionName.equals(propType.getName()))) {
                                    elementType.add(propType);//-------------
                                }
                            } else if (!retType.equals(ClassLoader.class)
                                    && !retType.equals(java.lang.annotation.Annotation.class)
                                    && !retType.equals(Class.class)) {
                                Class argType = retType;
                                if (retType.equals(List.class)) { // see above
                                    final Class genArgType = getGenericArgType(method
                                            .getGenericReturnType());
                                    if (genArgType != null) {
                                        argType = genArgType;
                                    }
                                }
                                if (!elementType.hasReference(name)
                                        && !elementType.getName().equals(
                                        argType.getCanonicalName())) {
                                    if (isPrimitiveType(argType)) {
                                        logger.debug("adding list property {}", name);
                                        elementType.add(propType);
                                    } else {
                                        logger.debug("adding reference {} for {}",
                                                argType.getCanonicalName(), propType.getName());
                                        elementType.addReference(propType.getName(),
                                                create(argType, idName, versionName));
                                    }
                                }
                            }
                        }
                    }); // ToDo, unnest, PMD:AvoidDeeplyNestedIfStmts
        }
        return elementType;
    }
}
