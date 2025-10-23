package ch.semafor.gendas.repldbfunc;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * The MIT License
 * <p>
 * Copyright (c) 2010-2012 www.myjeeva.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * http://myjeeva.com/generic-comparator-in-java.html
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Date;

/**
 * Sorting - Generic Comparator
 *
 * GenericComparator.java <a href="mailto:jeeva@myjeeva.com">Jeevanandam
 * Madanagopal</a> 2010-2012 www.myjeeva.com
 */

public final class GenericComparator implements Comparator, Serializable {
    private static final long serialVersionUID = -2293914106471884607L;
    // Logger
    private static final Log LOG = LogFactory.getLog(GenericComparator.class);
    private static final int LESSER = -1;
    private static final int EQUAL = 0;
    private static final int GREATER = 1;
    private static final String METHOD_GET_PREFIX = "get";
    private static final String DATATYPE_STRING = "java.lang.String";
    private static final String DATATYPE_DATE = "java.util.Date";
    private static final String DATATYPE_SQL_TIMESTAMP = "java.sql.Timestamp";
    private static final String DATATYPE_INTEGER = "java.lang.Integer";
    private static final String DATATYPE_LONG = "java.lang.Long";
    private static final String DATATYPE_FLOAT = "java.lang.Float";
    private static final String DATATYPE_DOUBLE = "java.lang.Double";
    // generic comparator attributes
    private final String targetMethod;
    private final boolean sortAscending;
    private boolean ignorecase;
    /**
     * <p>
     * default constructor - assumes comparator for Type List
     * </p>
     *
     * <p>
     * For Example-
     * </p>
     * <code>List&lt;Integer&gt; aa = new ArrayList&lt;Integer&gt;();</code>
     * <code>List&lt;String&gt; bb = new ArrayList&lt;String&gt;();</code>
     * <code>List&lt;Date&gt; cc = new ArrayList&lt;Date&gt;();</code>
     * <p>
     * and so on..
     * </p>
     * <p>
     * Invoking sort method with passing
     * <code>{com.myjeeva.comparator.GenericComparator}</code> for
     * <code>Collections.sort(aa, new GenericComparator(false));</code>
     * </p>
     * @param ignorecase
     * @param sortAscending
     *            - a {boolean} - <code>true</code> ascending order or
     *            <code>false</code> descending order
     */
    public GenericComparator(boolean sortAscending, boolean ignorecase) {
        super();
        this.targetMethod = null;
        this.sortAscending = sortAscending;
        this.ignorecase = ignorecase;
    }

    /**
     * <p>
     * constructor with <code>sortField</code> parameter for Derived type of
     * <code>Class</code> default sorting is ascending order
     * </p>
     *
     * <p>
     * For Example-
     * </p>
     * <p>
     * <code>PersonVO person = new PersonVO();
     * person.setId(10001);
     * person.setName("Jacob");
     * person.setHeight(5.2F);
     * person.setEmailId("jacob@example.example");
     * person.setSalary(10500L);
     * person.setDob(new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).parse("Jan 1, 1970"));</code>
     * <p>
     * and person2, person3, so on.. And Defining &amp; adding all the created
     * objects in to below list
     * </p>
     * <p>
     * <code>List&lt;PersonVO&gt; persons = new ArrayList&lt;PersonVO&gt;();
     * persons.add(person1);
     * persons.add(person2);
     * persons.add(person3); </code>and so on
     * <p>
     * Invoking sort method with passing
     * <code>{com.myjeeva.comparator.GenericComparator}</code> for
     * <code>Collections.sort(persons, new GenericComparator("name"));</code>
     * </p>
     *
     * @param sortField
     *            - a {@link java.lang.String} - which field requires sorting;
     *            as per above example "sorting required for <code>name</code>
     *            field"
     */
    public GenericComparator(String sortField) {
        super();
        this.targetMethod = prepareTargetMethod(sortField);
        this.sortAscending = true;
    }

    /**
     * <p>
     * constructor with <code>sortField, sortAscending</code> parameter for
     * Derived type of <code>Class</code>
     * </p>
     *
     * <p>
     * For Example-
     * </p>
     * <p>
     * <code>PersonVO person = new PersonVO();
     * person.setId(10001);
     * person.setName("Jacob");
     * person.setHeight(5.2F);
     * person.setEmailId("jacob@example.example");
     * person.setSalary(10500L);
     * person.setDob(new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).parse("Jan 1, 1970"));</code>
     * <p>
     * and person2, person3, so on.. And Defining &amp; adding all the created
     * objects in to below list
     * </p>
     * <p>
     * <code>List&lt;PersonVO&gt; persons = new ArrayList&lt;PersonVO&gt;();
     * persons.add(person1);
     * persons.add(person2);
     * persons.add(person3); </code>and so on
     * <p>
     * Invoking sort method with passing
     * <code>{com.myjeeva.comparator.GenericComparator}</code> for
     * <code>Collections.sort(persons, new GenericComparator("name"), false);</code>
     *
     * </p>
     *
     * @param sortField
     *            - a {java.lang.String} - which field requires sorting; as per
     *            above example "sorting required for <code>name</code> field"
     * @param sortAscending
     *            - a {boolean} - <code>true</code> ascending order or
     *            <code>false</code> descending order
     */
    public GenericComparator(String sortField, boolean sortAscending) {
        super();
        this.targetMethod = prepareTargetMethod(sortField);
        this.sortAscending = sortAscending;
    }

    /**
     * preparing target name of getter method for given sort field
     *
     * @param name
     *            a {@link java.lang.String}
     * @return methodName a {@link java.lang.String}
     */
    private static String prepareTargetMethod(String name) {
        String fieldName = METHOD_GET_PREFIX + name.substring(0, 1).toUpperCase() +
                name.substring(1);
        return fieldName;
    }

    /**
     * dynamically invoking given method with given object through reflect
     *
     * @param method
     *            - a {@link java.lang.reflect.Method}
     * @param obj
     *            - a {@link java.lang.Object}
     * @return object - a {@link java.lang.Object} - return of given method
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private static Object invoke(Method method, Object obj)
            throws InvocationTargetException, IllegalAccessException {
        return method.invoke(obj, null);
    }

    // ---------------------------------------------------------------------------------//
    // Private methods used by {@link com.myjeeva.comparator.GenericComparator}
    // //
    // ---------------------------------------------------------------------------------//

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(Object o1, Object o2) {
        int response = LESSER;
        try {
            Object v1 = (null == this.targetMethod) ? o1 : getValue(o1);
            Object v2 = (null == this.targetMethod) ? o2 : getValue(o2);
            CompareMode cm = findCompareMode(v1, v2);

            if (!cm.equals(CompareMode.DEFAULT)) {
                return compareAlternate(cm);
            }

            final String returnType = (null == this.targetMethod) ? o1
                    .getClass().getName() : getMethod(o1).getReturnType()
                    .getName();
            response = compareActual(v1, v2, returnType);
        } catch (NoSuchMethodException nsme) {
            LOG.error("NoSuchMethodException occurred while comparing", nsme);
        } catch (IllegalAccessException iae) {
            LOG.error("IllegalAccessException occurred while comparing", iae);
        } catch (InvocationTargetException ite) {
            LOG.error("InvocationTargetException occurred while comparing", ite);
        }
        return response;
    }

    /**
     * alternate to actual value comparison i.e., either (lsh &amp; rhs) one the
     * value could be null
     *
     * @param cm
     *            - a enum used to idetify the position for sorting
     */
    private int compareAlternate(CompareMode cm) {
        int compareState = LESSER;
        switch (cm) {
            case LESS_THAN:
                compareState = LESSER * determinePosition();
                break;
            case GREATER_THAN:
                compareState = GREATER * determinePosition();
                break;
            case EQUAL:
                compareState = EQUAL * determinePosition();
                break;
        }
        return compareState;
    }

    /**
     * actual value comparison for sorting; both lsh &amp; rhs value available
     *
     * @param v1
     *            - value of lhs
     * @param v2
     *            - value of rhs
     * @param returnType
     *            - datatype of given values
     * @return int - compare return value
     */
    private int compareActual(Object v1, Object v2, String returnType) {
        int acutal = LESSER;
        if (returnType.equals(DATATYPE_INTEGER)) {
            acutal = (((Integer) v1).compareTo((Integer) v2) * determinePosition());
        } else if (returnType.equals(DATATYPE_LONG)) {
            acutal = (((Long) v1).compareTo((Long) v2) * determinePosition());
        } else if (returnType.equals(DATATYPE_STRING)) {
            if (!ignorecase) {
                acutal = (((String) v1).compareTo((String) v2) * determinePosition());
            } else {
                acutal = (((String) v1).compareToIgnoreCase((String) v2) * determinePosition());
            }
            if (NumberUtils.isParsable((String) v1) && NumberUtils.isParsable((String) v2)) {
                acutal = NumberUtils.createFloat((String) v1).compareTo(NumberUtils.createFloat((String) v2)) * determinePosition();
            }
        } else if (returnType.equals(DATATYPE_DATE) || returnType.equals(DATATYPE_SQL_TIMESTAMP)) {
            acutal = (((Date) v1).compareTo((Date) v2) * determinePosition());
        } else if (returnType.equals(DATATYPE_FLOAT)) {
            acutal = (((Float) v1).compareTo((Float) v2) * determinePosition());
        } else if (returnType.equals(DATATYPE_DOUBLE)) {
            acutal = (((Double) v1).compareTo((Double) v2) * determinePosition());
        }
        return acutal;
    }

    /**
     * fetching method from <code>Class</code> object through reflect
     *
     * @param obj
     *            - a {@link java.lang.Object} - input object
     * @return method - a {@link java.lang.reflect.Method}
     * @throws NoSuchMethodException
     */
    private Method getMethod(Object obj) throws NoSuchMethodException {
        return obj.getClass().getMethod(targetMethod, null);
    }

    /**
     * fetching a value from given object
     *
     * @param obj
     *            - a {@link java.lang.Object}
     * @return object - a {@link java.lang.Object} - return of given method
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     */
    private Object getValue(Object obj) throws InvocationTargetException,
            IllegalAccessException, NoSuchMethodException {
        return invoke(getMethod(obj), obj);
    }

    /**
     * identifying the comparison mode for given value
     *
     * @param o1
     *            - a {@link java.lang.Object}
     * @param o2
     *            - a {@link java.lang.Object}
     * @return compareMode - a
     *
     */
    private CompareMode findCompareMode(Object o1, Object o2) {
        CompareMode cm = CompareMode.EQUAL;

        if (null != o1 && null != o2) {
            cm = CompareMode.DEFAULT;
        } else if (null == o1 && null != o2) {
            cm = CompareMode.LESS_THAN;
        } else if (null != o1) {
            cm = CompareMode.GREATER_THAN;
        }

        return cm;
    }

    /**
     * Determining positing for sorting
     *
     * @return -1 to change the sort order if appropriate.
     */
    private int determinePosition() {
        return sortAscending ? GREATER : LESSER;
    }

    private enum CompareMode {
        EQUAL, LESS_THAN, GREATER_THAN, DEFAULT
    }
}
