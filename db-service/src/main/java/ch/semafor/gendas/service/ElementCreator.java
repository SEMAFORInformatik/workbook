/*
 * Copyright 2010 Semafor Informatik & Energie AG, Basel, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package ch.semafor.gendas.service;

import ch.semafor.gendas.dao.jpa.*;
import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.ElementCreationException;
import ch.semafor.gendas.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * create element tree and load bean from id
 *
 * @author tar
 */
public class ElementCreator extends Creator {
    private static final Logger logger = LoggerFactory.getLogger(ElementCreator.class);
    final ElementTypeRepositoryJpa elementTypeDao;
    final ElementRepositoryJpa elementRepository;
    final GroupRepositoryJpa groupRepository;
    final OwnerRepositoryJpa ownerRepository;
    final PropertyTypeRepositoryJpa propertyTypeDao;
    private Map<Object, Element> beanElementMap;

    private Map<Long, Object> loadedBean;

    /**
     * @param elementTypeDao
     * @param propertyTypeDao
     */
    public ElementCreator(final ElementTypeRepositoryJpa elementTypeDao,
                          final ElementRepositoryJpa elementRepository, final OwnerRepositoryJpa ownerRepository,
                          final GroupRepositoryJpa groupRepository, final PropertyTypeRepositoryJpa propertyTypeDao) {
        super();
        this.elementTypeDao = elementTypeDao;
        this.elementRepository = elementRepository;
        this.ownerRepository = ownerRepository;
        this.groupRepository = groupRepository;
        this.propertyTypeDao = propertyTypeDao;
        resetMaps();
    }

    public void resetMaps() {
        beanElementMap = new HashMap<>();
        loadedBean = new HashMap<>();
    }

    /**
     * @throws CoreException
     */
    private ElementType getElementType(final String typeName) throws CoreException {
        final ElementType elType = elementTypeDao.findByName(typeName);
        if (elType == null) {
            throw new CoreException("ElementType " + typeName + " not found");
        }
        return elType;
    }

    /**
     * get method for setting property
     *
     * @param propName of property
     * @return set method
     */
    private Method getGetProperty(final Class<?> beanClass, final String propName) {
        if (propName == null || propName.length() == 0) {
            return null;
        }
        // ToDo, append, PMD: InefficientStringBuffering
        final StringBuffer name = new StringBuffer("get" + propName);
        name.setCharAt(3, Character.toUpperCase(propName.charAt(0)));
        try {
            return beanClass.getMethod(name.toString());
            // 9/20/10 2:54 PM
        } catch (NoSuchMethodException ex) {
            logger.warn("No such Method '{}' in {}", name, beanClass.getCanonicalName());
            return null;
        }
    }

    private boolean isProperty(Element e, String propName) {
        return propertyTypeDao.findByElementTypeAndName(e.getElementType(), propName) != null;
    }

    private boolean isReference(Element e, String refName) {
        return e.getElementType().getReferences().containsKey(refName);
    }

    /**
     * set property value in element
     *
     * @param propName
     * @param value
     * @param element
     */
    private void setProperty(final String propName, final Object value, final Element element) {
        // ownername is not saved, it is added by the toMap method after loading
        final var ignoredProps = List.of("modcomment", "changed", "changer", "changername", "type", "ownername");
        if (ignoredProps.contains(propName))
            return;

        try {
            final PropertyType propType =
                    propertyTypeDao.findByElementTypeAndName(element.getElementType(), propName);
            if (propType == null) {
                logger.error("PropertyType {} of element type {} not found.", propName,
                        element.getElementType().getName());
                throw new CoreException("Unkown property type '" + propName + "'");
            }
            Property prop = element.getProperty(propType);
            if (prop == null) {
                prop = new Property(element, propType);
            }
            if (value instanceof List l) {
                logger.debug("setting {} properties of \"{}\"", l.size(), prop.getType().getName());
                for (int i = 0; i < l.size(); i++) {
                    prop.setValue(i, l.get(i));
                }
            } else {
                logger.debug("setting property \"{}\"", prop.getName());
                prop.setValue(0, value);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("adding property \"{}\"", prop.getType().getName());
            }
        } catch (CoreException ex) {
            logger.warn(ex.getMessage());
        }
    }

    private void setIdAndVersion(final Object bean, final Element element, final Class<?> genArgType)
            throws CoreException {
        final ElementType elType;
        // on 9/20/10 2:59 PM
        if (bean instanceof java.util.List) {
            logger.debug("bean {} instance of list<{}>", bean.getClass().getCanonicalName(),
                    genArgType.getCanonicalName() + ">");
            elType = getElementType(genArgType.getCanonicalName());
        } else {
            elType = getElementType(bean.getClass().getCanonicalName());
        }
        logger.info("setting bean property {} for type {}", elType.getBeanId(), elType.getName());
        setId(bean, element.getId(), elType.getBeanId());
        if (element.getVersion() != null) {
            Integer v = element.getVersion().intValue();
            setVersion(bean, v, elType.getBeanVersionId());
        }
    }

    /**
     * set all ids and versions recursively (must be called after save)
     *
     * @param bean       to be set id and version
     * @param element    id and version holder
     * @param genArgType type of contained objects if bean is a list
     * @throws CoreException
     */
    public void setMatchingIdsAndVersions(final Object bean, final Element element,
                                          final Class genArgType) throws CoreException {
        final Class<?> clazz = bean.getClass();
        logger.debug("class {}", clazz.getCanonicalName());
        if (beanElementMap.containsKey(bean)) {
            return;
        }
        setIdAndVersion(bean, element, genArgType);

        logger.debug("checking Element  ({})", element.toString());
        beanElementMap.put(bean, element);

        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            public void doWith(final Method method) {
                try {
                    if (((method.getName().startsWith("get") && method.getName().length() > 3)
                            || (method.getName().startsWith("is") && method.getName().length() > 2))
                            && !method.getName().equals("getClass")) {
                        final Class<?> retType = method.getReturnType();
                        logger.debug("checking method return type {}", retType.getCanonicalName());
                        if (!isPrimitiveType(retType)) {
                            Class<?> genArgType = null;
                            String type = retType.getCanonicalName();
                            if (method.getReturnType().equals(java.util.List.class)) {
                                genArgType = getGenericArgType(method.getGenericReturnType());
                                type = genArgType.getCanonicalName();
                            }
                            logger.debug("composite {}", type);
                            final String propName = getPropertyName(method);
                            if (element.getElementType().hasReference(propName)) {
                                logger.debug("about to get reference {}", propName);
                                final Object ref = method.invoke(bean);
                                if (ref != null && ref instanceof java.util.List) {
                                    logger.debug("LIST SIZE {}", ((java.util.List) ref).size());
                                    if (!isPrimitiveType(genArgType)) {
                                        ElementRefs refs = element.getElementRefs(propName);
                                        if (refs != null) {
                                            logger.debug("setting ids/versions for references {}", propName);
                                            final Iterator<Element> eIter = refs.getLastListOfElements().iterator();
                                            final Iterator<Object> oIter = ((java.util.List) ref).iterator();
                                            while (eIter.hasNext() && oIter.hasNext()) {
                                                Object o = oIter.next();
                                                if (o != null) {
                                                    setMatchingIdsAndVersions(o, eIter.next(), genArgType);
                                                }
                                            }
                                        }
                                        logger.debug("end");
                                    }
                                }
                            }
                        }
                    }
                } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException
                         | CoreException e) {
                    logger.error("cannot set id and version", e);
                }
            }
        });
    }

    /**
     * creates a new element tree from object
     *
     * @param bean       object to create element tree from
     * @param genArgType generic type (must be valid if bean is a list)
     * @return created element
     * @throws ElementCreationException
     */
    public Element create(final Object bean, final Class<?> genArgType)
            throws CoreException, ElementCreationException {

        final Class<?> clazz = bean.getClass();
        logger.debug("class {}", clazz.getCanonicalName());
        final ElementType elType;
        // 9/20/10 2:59 PM
        if (bean instanceof java.util.List) {
            logger.debug("bean {} instance of list<{}>", clazz.getCanonicalName(),
                    genArgType.getCanonicalName() + ">");
            elType = getElementType(genArgType.getCanonicalName());
        } else {
            elType = getElementType(clazz.getCanonicalName());
        }
        logger.debug("creating Element  ({})", elType);
        if (beanElementMap.containsKey(bean)) {
            return beanElementMap.get(bean);
        }
        final Element element = new Element(elType);
        beanElementMap.put(bean, element);

        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            public void doWith(final Method method)
                    throws IllegalArgumentException, IllegalAccessException {
                try {
                    logger.debug("Method is {}", method.getName());
                    if (((method.getName().startsWith("get") && method.getName().length() > 3)
                            || (method.getName().startsWith("is") && method.getName().length() > 2))
                            && !method.getName().equals("getClass")) {
                        final Class retType = method.getReturnType();
                        String propName = getPropertyName(method);
                        logger.debug("checking property type {} of method type {}", propName,
                                retType.getCanonicalName());
                        if (elType.isBeanId(propName)) {
                            if (method.getReturnType().equals(Long.class)) {
                                element.setId((Long) method.invoke(bean));
                            } else if (method.getReturnType().equals(String.class)) {
                                String id = (String) method.invoke(bean);
                                if (id != null) {
                                    element.setId(Long.valueOf(id));
                                } else {
                                    element.setId(null);
                                }
                            }
                        } else if (elType.isBeanVersionId(propName)) {
                            Long v = 0L;
                            if (retType.equals(java.lang.Integer.class)
                                    || retType.getCanonicalName().equals("int")) {
                                v = Long.valueOf((Integer) method.invoke(bean));
                                // above line as suggested by FB:
                                // DM_NUMBER_CTOR v = new
                                // Long((Integer)
                                // (value));
                            } else if (retType.equals(java.lang.Long.class)
                                    || retType.getCanonicalName().equals("long")) {
                                v = (Long) method.invoke(bean);
                            }
                            element.setVersion(v); // property must
                            // be Int or Long!!
                        } else if (isProperty(element, propName)) {
                            logger.debug("property name {} type {}", propName, retType.getCanonicalName());
                            setProperty(propName, method.invoke(bean), element);
                        } else if (isReference(element, propName)) {
                            Class<?> genArgType = null;
                            String type = retType.getCanonicalName();
                            if (method.getReturnType().equals(java.util.List.class)) {
                                genArgType = getGenericArgType(method.getGenericReturnType());
                                type = genArgType.getCanonicalName();
                            }
                            logger.debug("composite {}", type);
                            logger.debug("about to add reference {}", propName);
                            final Object ref = method.invoke(bean);
                            if (ref != null) {
                                try {
                                    if (ref instanceof java.util.List) {
                                        logger.debug("LIST SIZE {}", ((java.util.List) ref).size());
                                        if (isPrimitiveType(genArgType)) {
                                            logger.debug("LIST ELEMENTS TYPE {}", genArgType.getCanonicalName());
                                            setProperty(propName, ref, element);
                                        } else { // a list of composite
                                            // types
                                            logger.debug("adding list reference {}", propName);
                                            final List<Element> elements = new ArrayList<Element>();
                                            for (Object o : (java.util.List) ref) {
                                                if (o != null) {
                                                    elements.add(create(o, genArgType));
                                                }
                                            }
                                            logger.debug("end");
                                            element.setListOfElements(propName, elements);
                                        }
                                    } else { // not a list
                                        logger.debug("adding reference {}", propName);
                                        element.addElement(propName, create(ref, genArgType));
                                    }
                                } catch (CoreException | ElementCreationException e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                } catch (InvocationTargetException e) {
                    logger.error(e.getMessage(), e);
                }
            }

        });
        logger.debug("returning Element {}", element);
        return element;
    }

    /**
     * create a new Element tree from Map
     *
     * @param data
     * @param elType
     * @return element
     * @throws ElementCreationException
     */
    public Element create(Map<String, ?> data, final ElementType elType)
            throws ElementCreationException {
        Element element = null;
        if (data == null || elType == null)
            throw new ElementCreationException("Incomplete ElementType");
        try {
            element = new Element(elType);
            logger.debug("creating element of type {}", elType.getName());
            for (String key : data.keySet()) {
                if (elType.isBeanId(key)) {
                    Number num = (Number) data.get(key);
                    element.setId(num.longValue());
                } else if (elType.isBeanVersionId(key)) {
                    Number num = (Number) data.get(key);
                    element.setVersion(num.longValue()); // property must be Int or Long!!
                } else if (elType.isOwner(key)) {
                    element.setOwner(ownerRepository.findByUsername((String) data.get(key)));
                } else if (elType.isGroup(key)) {
                    element.setGroup(groupRepository.findByName((String) data.get(key)));
                } else {
                    Object value = data.get(key);
                    if ((value != null && isPrimitiveType(value.getClass()))
                            || (value == null && elType.getPropertyType(key) != null)) {
                        setProperty(key, data.get(key), element);
                    } else if (value instanceof java.util.List) {
                        if (((List) value).size() > 0) {
                            if (elType.getPropertyType(key) != null) {
                                setPropertyMatrix(key, value, element);
                            } else {
                                final ElementType subtype = elType.getElementType(key);
                                if (subtype == null) {
                                    // throw new ElementCreationException(elType.getName() +": no such child type
                                    // "+key);
                                    continue;
                                }
                                final List<Element> elements = new ArrayList<Element>();
                                logger.debug("add elements to {}", key);
                                try {
                                    for (Map<String, ?> o : (List<Map<String, ?>>) value) {
                                        if (o != null) {
                                            elements.add(create(o, subtype));
                                        }
                                    }
                                    element.setListOfElements(key, elements);
                                } catch (ClassCastException ex) {
                                    logger.error("cannot cast {} (type {})", key, value.getClass().getCanonicalName(),
                                            ex);
                                    throw new ElementCreationException("Cannot cast element " + key + " to list");
                                }
                            }
                        }
                    } else {
                        final ElementType subtype = elType.getElementType(key);
                        if (subtype == null) {
                            continue;
                            // throw new ElementCreationException(elType.getName() +": no such child type "+key);
                        }
                        if (!((Map<String, ?>) data.get(key)).isEmpty()) {
                            logger.debug("add element to {}", key);
                            element.addElement(key, create((Map<String, ?>) data.get(key), subtype));
                        }
                    }
                }
            }
        } catch (CoreException e) {
            throw new ElementCreationException("Failed to map to element", e);
        }
        return element;
    }

    private int setDims(List<Object> lo, List<Integer> dims) {
        dims.add(lo.size());
        if (lo.size() > 0 && lo.get(0) instanceof java.util.List) {
            if (setDims((List) lo.get(0), dims) < 0) {
                // this is the last dimension: find the largest of all:
                int max = 0;
                for (Object o : lo) {
                    List l = (List) o;
                    if (l.size() > max) {
                        max = l.size();
                    }
                }
                dims.set(dims.size() - 1, max);
                return max;
            }
            return dims.get(dims.size() - 1);
        }
        if (dims.size() > 1)
            return -1; // this is the last dim of a matrix
        return lo.size(); // it is a vector
    }

    private void setVector(int size, List<Object> lo, List<Object> vec) {
        if (lo.size() > 0 && lo.get(0) instanceof java.util.List) {
            for (Object l : lo) {
                setVector(size, (List<Object>) l, vec);
            }
        } else {
            int s = 0;
            for (Object o : lo) {
                vec.add(o);
                s++;
            }
            logger.debug("actual {} size {}", s, size);
            for (int i = s; i < size; i++) {
                // all vecs of last dim must have equal size
                vec.add(null);
            }
        }
    }

    private void setPropertyMatrix(final String propName, final Object object,
                                   final Element element) {
        List<Integer> dims = new ArrayList<Integer>();
        List<Object> vector = new ArrayList<Object>();
        int lastSize = setDims((List) object, dims);
        setVector(lastSize, (List) object, vector);
        try {
            logger.debug("{} VEC {} DIMS {}",
                    propName, vector, dims);
            final PropertyType propType =
                    propertyTypeDao.findByElementTypeAndName(element.getElementType(), propName);
            if (propType == null) {
                logger.error("PropertyType {} of element type {} not found.", propName,
                        element.getElementType().getName());
                throw new CoreException("Unkown property type '" + propName + "'");
                // of element type +'"+element.getElementType()+"'");
            }
            Property prop = element.getProperty(propType);
            if (prop == null) {
                prop = new Property(element, propType);
            }
            for (int i = 0; i < vector.size(); i++) {
                prop.setValue(i, vector.get(i));
            }
            prop.setDims(dims);
        } catch (CoreException ex) {
            logger.warn(ex.getMessage());
        }
    }

    // private void setPropertyValue(final Property prop, int i,
    // final PropertyType retType, final Object value) throws CoreException {
    //
    // }

    /**
     * find method based on name only
     *
     * @param clazz
     * @param name
     * @return method or null if not found
     */
    private Method findMethod(final Class<?> clazz, final String name) {
        for (Method m : ReflectionUtils.getAllDeclaredMethods(clazz)) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private void loadSimpleProperty(Method method, final PropertyValueList vlist,
                                    final Class paramType, final Object bean) throws CoreException {
        try {
            if (paramType.equals(java.util.List.class)) {
                List<Object> l = new ArrayList<Object>();
                for (PropertyValue pv : vlist.getValues()) {
                    l.add(pv.getValue());
                }
                method.invoke(bean, l);
            } else if (paramType.isAssignableFrom(javax.xml.datatype.XMLGregorianCalendar.class)) {
                XMLGregorianCalendar gcal =
                        DatatypeFactory.newInstance().newXMLGregorianCalendar(vlist.getGregorianDate(0));
                method.invoke(bean, gcal);
            } else if (paramType.isEnum()) {
                Object[] e = paramType.getEnumConstants();
                // 9/20/10 3:00 PM
                final String eval = (String) vlist.getValue(0);
                for (int i = 0; i < e.length; i++) {
                    if (eval != null && eval.equals(e[i].toString())) {
                        method.invoke(bean, e[i]);
                    }
                }
            } else {
                logger.debug("value {} type {}", vlist.getValue(0),
                        vlist.getValue(0).getClass().getCanonicalName());
                method.invoke(bean, vlist.getValue(0));
            }
        } catch (IllegalAccessException |
                 InvocationTargetException |
                 DatatypeConfigurationException e) {
            throw new CoreException(e.getMessage());
        }
    }

    private void loadProperties(final Element element, final long revision, final Object bean,
                                final Class<?> clazz) throws CoreException {
        for (Property p : element.getProperties()) {
            logger.debug("load properties for {}", p.getType().getName());
            if (p.isInRevision(revision)) {
                final PropertyValueList vlist = p.getValueList(revision);
                if (vlist != null) {
                    if (vlist.isValid()) {
                        String name = getMethodName("set", p.getType().getName());
                        Method method = findMethod(clazz, name);
                        if (method != null) { // ok this is a normal setter
                            final Class[] paramTypes = method.getParameterTypes();
                            loadSimpleProperty(method, vlist, paramTypes[0], bean);
                        } else { // no setter found, assume we have to get a
                            // list first
                            logger.debug("no setter found assuming list");
                            name = getMethodName("get", p.getType().getName());
                            method = findMethod(clazz, name);
                            if (method != null) {
                                try {
                                    Object ref = method.invoke(bean);
                                    if (ref instanceof List l) {
                                        logger.debug("load {} list properties for {}", l.size(), p.getType().getName());
                                        for (int i = 0; i < vlist.getValues().size(); i++) {
                                            l.add((vlist.getValues().get(i)).getObject());
                                        }
                                    }
                                } catch (IllegalArgumentException |
                                         IllegalAccessException |
                                         InvocationTargetException e) {
                                    throw new CoreException(e.getMessage());
                                }
                            } else {
                                logger.warn("No method found for {}", p.getType().getName());
                            }
                        }
                    }
                } else {
                    logger.debug("  no values");
                }
            } else {
                logger.debug("   not in revision");
            }
        }
    }

    private void loadElementRefs(final Element element, final long revision, final Object bean,
                                 final Class<?> clazz) {
        for (ElementRefs c : element.getListOfElementRefs()) {
            if (c.isInRevision(revision)) {
                try {
                    String name = getMethodName("set", c.getRefName());
                    Method method = findMethod(clazz, name);
                    if (method != null) {
                        final Class<?>[] paramTypes = method.getParameterTypes();
                        String typeName = paramTypes[0].getCanonicalName();
                        List l = new ArrayList();
                        // 3:00 PM
                        boolean isList = false;
                        if ("java.util.List".equals(typeName)) {
                            isList = true;
                            logger.debug("    LIST {}", typeName);
                        }
                        final ElementRefList reflist = c.getElementRefList(revision);
                        if (reflist != null) {
                            for (Element e : reflist.getElementList()) {
                                l.add(loadElement(e, revision));
                            }
                        }
                        if (isList) {
                            method.invoke(bean, l);
                        } else if (!l.isEmpty()) {
                            method.invoke(bean, l.get(0));
                        }
                    } else {
                        name = getMethodName("get", c.getRefName());
                        method = findMethod(clazz, name);
                        if (method == null) {
                            name = getMethodName("is", c.getRefName());
                            method = findMethod(clazz, name);
                        }
                        if (method != null) {
                            final Class<?> retType = method.getReturnType();
                            // String typeName = retType.getCanonicalName();
                            List l = (List) method.invoke(bean);
                            // on
                            // 9/20/10
                            // 3:04 PM
                            final ElementRefList reflist = c.getElementRefList(revision);
                            if (reflist != null) {
                                for (Element e : reflist.getElementList()) {
                                    l.add(loadElement(e, revision));
                                }
                            }
                        }
                    }
                }
                // ToDo, needs investigation, FB: REC_CATCH_EXCEPTION
                catch (Exception ex) {
                    logger.error("cannot load element", ex);
                }
            }
        }
    }

    /**
     * load an object
     *
     * @param element
     * @return loaded object
     * @throws CoreException
     */
    public Object load(final Element element) throws CoreException {
        return load(element, null);
    }

    /**
     * load an map by revision
     *
     * @param element
     * @param revision
     * @return loaded map
     * @throws CoreException
     */

    public Object load(final Element element, final Long revision) throws CoreException {
        if (revision == null) {
            return loadElement(element, TableModification.MaxRevision);
        }
        return loadElement(element, revision);
    }

    /**
     * get revision of element by timestamp
     *
     * @param element
     * @param timestamp
     * @return revision
     * @throws CoreException
     */
    private long getRevision(final Element element, final Date timestamp) throws CoreException {
        if (timestamp == null) {
            return TableModification.MaxRevision;
        }
        final TableModification hist = element.getModification(timestamp);
        if (hist == null) {
            throw new CoreException("No modification with Timestamp " + timestamp + " found");
        }
        return hist.getRevision();
    }

    /**
     * get bean of element by rev. check if already loaded
     *
     * @param element
     * @param rev
     * @return bean
     * @throws CoreException
     */
    private Object loadElement(final Element element, final Long rev) throws CoreException {
        if (logger.isDebugEnabled()) {
            logger.debug("creating Element  (" + element.getElementType().getName() + ")" // + " id " +
                    // getId(bean)
                    + " element id " + element.getId() + " assigned "
                    + loadedBean.containsKey(element.getId()));
        }

        if (loadedBean.containsKey(element.getId())) {
            // we have this bean already loaded
            return loadedBean.get(element.getId());
        }
        Class<?> clazz = null;
        Object bean = null;
        try {
            clazz = Class.forName(element.getElementType().getName());
            Constructor<?> constr = clazz.getConstructor();
            bean = constr.newInstance();
        } catch (Exception e) {
            throw new CoreException(e.getMessage());
        }
        loadedBean.put(element.getId(), bean);

        // load valid Properties
        loadProperties(element, rev, bean, clazz);
        // Load valid Elements of ElementRefs
        loadElementRefs(element, rev, bean, clazz);
        // attempt to set version and id
        setIdAndVersion(bean, element, null);
        return bean;
    }


    /**
     * set id of bean if possible
     *
     * @param bean   to set id
     * @param id     to set
     * @param idName name of id
     */
    void setId(Object bean, final Long id, final String idName) {
        final String name = getMethodName("set", idName);
        final Method setId = findMethod(bean.getClass(), name);
        // final Method setId = getSetProperty(bean.getClass(), idName);
        if (setId != null) {
            if (setId.getParameterTypes().length != 1) {
                logger.warn("set id error for class {}", bean.getClass().getCanonicalName());
                return;
            }
            try {
                if (setId.getParameterTypes()[0].equals(Long.class)) {
                    setId.invoke(bean, id);
                } else if (setId.getParameterTypes()[0].equals(String.class)) {
                    setId.invoke(bean, id.toString());
                } else {
                    logger.warn("unsupported type for setting id {}", setId.getParameterTypes()[0]);
                }
            } catch (IllegalAccessException |
                     IllegalArgumentException |
                     InvocationTargetException ite) {
                logger.warn("set id error {}", ite.getMessage());
            }
        } else {
            logger.warn("property {} for type {} not found", idName, bean.getClass().getCanonicalName());
        }
    }

    private void setVersion(final Object bean, final Integer ver, final String idVersion) {
        final String name = getMethodName("set", idVersion);
        final Method setVersion = findMethod(bean.getClass(), name);
        if (setVersion != null) {
            try {
                setVersion.invoke(bean, ver);
                logger.debug("set version {}", ver);
            } catch (IllegalAccessException |
                     IllegalArgumentException |
                     InvocationTargetException ite) {
                logger.warn("set version error {}", ite.getMessage());
            }
        }
    }

    private Long getId(final Object bean, final String idName) {
        final Method getId = getGetProperty(bean.getClass(), idName);
        if (getId != null) {
            try {
                return (Long) getId.invoke(bean, new Object[]{});
            } catch (Exception e) {
                logger.warn("cannot set id {}", e.getMessage());
            }
        }
        return null;
    }

}
