package ch.semafor.gendas.service;

import ch.semafor.gendas.exceptions.CoreException;
import com.google.common.collect.MapDifference.ValueDifference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Maps {
    private static final Logger logger = LoggerFactory.getLogger(Maps.class);

    static public Map<String, Object> diff(final Map<String, Object> left,
                                           final Map<String, Object> right) {
        // null -> empty Map
        // Left is entity from database
        // right comes from user
        Map<String, Object> myleft = left == null ? new HashMap<String, Object>() : left;
        Map<String, Object> myright = right == null ? new HashMap<String, Object>() : right;

        Map<String, ValueDifference<Object>> diff =
                com.google.common.collect.Maps.difference(myleft, myright).entriesDiffering();
        Map<String, Object> d = new HashMap<String, Object>();
        logger.debug("diff parameters {}", diff.keySet().size());

        // This map shows only the difference when both key are found in left and right.
        for (String k : diff.keySet()) {
            // _id is only in mongo database -> it is not possible to have _id in both, cause the user
            // always send id
            if (k.equals("version") || k.equals("_id") || k.equals("id")) {
                continue;
            }
            logger.debug("check {}", k);
            if (diff.get(k).leftValue() instanceof Map && diff.get(k).rightValue() instanceof Map) {
                Map dm = diff((Map<String, Object>) diff.get(k).leftValue(),
                        (Map<String, Object>) diff.get(k).rightValue());
                if (!dm.isEmpty()) {
                    d.put(k, dm);
                }
                continue;
            }
            if (diff.get(k).leftValue() instanceof List leftList) {
                if (diff.get(k).rightValue() instanceof List rightList) {
                    List dl = new ArrayList();
                    for (int i = 0; i < leftList.size() && i < rightList.size(); i++) {
                        // both null or identical
                        if (leftList.get(i) == rightList.get(i)) {
                            dl.add(new HashMap<String, Object>());
                            continue;
                        }

                        // Maps
                        if (leftList.get(i) instanceof Map && rightList.get(i) instanceof Map) {
                            Map<String, Object> l = (Map<String, Object>) leftList.get(i);
                            Map<String, Object> r = (Map<String, Object>) rightList.get(i);
                            dl.add(diff(l, r));
                            continue;
                        }

                        // not Maps
                        if (!sameValue(leftList.get(i), rightList.get(i))) {
                            dl = new ArrayList(Arrays.asList(leftList, rightList));
                            break;
                        }
                    }
                    // left larger than right
                    for (int i = rightList.size(); i < leftList.size(); i++) {
                        if (leftList.get(i) instanceof Map) {
                            Map<String, Object> l = (Map<String, Object>) leftList.get(i);
                            dl.add(diff(l, null));
                            continue;
                        }
                        dl = new ArrayList(Arrays.asList(leftList, rightList));
                        break;
                    }
                    // right larger than left
                    for (int i = leftList.size(); i < rightList.size(); i++) {
                        if (rightList.get(i) instanceof Map) {
                            Map<String, Object> r = (Map<String, Object>) rightList.get(i);
                            dl.add(diff(null, r));
                            continue;
                        }
                        dl = new ArrayList(Arrays.asList(leftList, rightList));
                        break;
                    }
                    removeTrailingEmptyMaps(dl);
                    if (!dl.isEmpty()) {
                        d.put(k, dl);
                    }
                    continue;
                }

                // rightValue is not list
                if (leftList.size() == 1) {
                    if (leftList.get(0) instanceof Map || diff.get(k).rightValue() instanceof Map) {
                        Map dm = diff((Map<String, Object>) leftList.get(0),
                                (Map<String, Object>) diff.get(k).rightValue());
                        if (!dm.isEmpty()) {
                            d.put(k, dm);
                        }
                        continue;
                    }
                    if (!sameValue(leftList.get(0), diff.get(k).rightValue())) {
                        d.put(k, new ArrayList(
                                Arrays.asList(leftList.get(0), diff.get(k).rightValue())));
                    }
                    continue;
                }
            }

            // leftValue is not list
            if (diff.get(k).rightValue() instanceof List) {
                List rightList = (List) diff.get(k).rightValue();
                if (rightList.size() == 1) {
                    if (rightList.get(0) instanceof Map || diff.get(k).leftValue() instanceof Map) {
                        Map dm = diff((Map<String, Object>) diff.get(k).leftValue(),
                                (Map<String, Object>) rightList.get(0));
                        if (!dm.isEmpty()) {
                            d.put(k, dm);
                        }
                        continue;
                    }
                    if (!sameValue(rightList.get(0), diff.get(k).leftValue())) {
                        d.put(k, new ArrayList(
                                Arrays.asList(diff.get(k).leftValue(), rightList.get(0))));
                    }
                    continue;
                }

                // rightValue is list with size > 1
                d.put(k, new ArrayList(Arrays.asList(diff.get(k).leftValue(), rightList)));
                continue;
            }

            // neither leftValue nor rightValue are lists
            if (!sameValue(diff.get(k).leftValue(), diff.get(k).rightValue())) {
                d.put(k, new Object[]{diff.get(k).leftValue(), diff.get(k).rightValue()});
            }
        }

        // include left or right only values
        Map<String, Object> diffonly =
                com.google.common.collect.Maps.difference(myleft, myright).entriesOnlyOnLeft();
        for (String k : diffonly.keySet()) {
            if (k.equals("version") || k.equals("id") || k.equals("_id") || k.equals("ownername")
                    || Maps.isEmpty(diffonly.get(k))) {
                continue;
            }
            d.put(k, new Object[]{diffonly.get(k), null});
        }

        diffonly = com.google.common.collect.Maps.difference(myleft, myright).entriesOnlyOnRight();
        for (String k : diffonly.keySet()) {
            if (k.equals("version") || k.equals("id") || Maps.isEmpty(diffonly.get(k))) {
                continue;
            }
            d.put(k, new Object[]{null, diffonly.get(k)});
        }
        return d;
    }

    private static boolean isEmpty(Object value) {
        // null
        if (value == null) {
            return true;
        }

        // Map
        if (value instanceof Map) {
            Map<String, Object> mapValue = ((Map<String, Object>) value);
            for (String key : mapValue.keySet()) {
                if (!Maps.isEmpty(mapValue.get(key))) {
                    return false;
                }
            }
            return true;
        }

        // List
        if (value instanceof List) {
            for (Object o : (List) value) {
                if (!Maps.isEmpty(o)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static void removeTrailingEmptyMaps(List dl) {
        ListIterator li = dl.listIterator(dl.size());
        while (li.hasPrevious()) {
            Object obj = li.previous();
            if (obj instanceof Map) {
                Map<String, Object> el_list = ((Map<String, Object>) obj);
                if (el_list.size() > 0) {
                    return;
                }
                li.remove();
            } else {
                // its not a map
                if (!Maps.isEmpty(obj)) {
                    return;
                }
                li.remove();
            }
        }
    }

    private static boolean sameValue(Object leftValue, Object rightValue) {
        if (leftValue == rightValue) { // identic objects (i.E. both null)
            return true;
        }
        if (leftValue == null || rightValue == null) { // exactly one is null
            return false;
        }
        // long and int
        if (leftValue instanceof Long && rightValue instanceof Integer) {
            return leftValue.equals(Long.valueOf((Integer) rightValue));
        }
        if (leftValue instanceof Integer && rightValue instanceof Long) {
            return rightValue.equals(Long.valueOf((Integer) leftValue));
        }
        // double
        if (leftValue instanceof Double && rightValue instanceof Double) {
            return ((Double) (leftValue)).floatValue() == ((Double) (rightValue)).floatValue();
        }
        // Date and GregorianCalendar
        if (leftValue instanceof java.util.GregorianCalendar && rightValue instanceof java.util.Date) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime((Date) rightValue);
            return leftValue.equals(c);
        }
        if (rightValue instanceof java.util.GregorianCalendar && leftValue instanceof java.util.Date) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime((Date) leftValue);
            return rightValue.equals(c);
        }

        return leftValue.equals(rightValue);
    }

    /**
     * merge diff into dest
     *
     * @param dest destination map
     * @param diff difference map
     * @return map
     * @throws CoreException
     */
    static public Map<String, Object> merge(Map<String, Object> dest, Map<String, Object> diff)
            throws CoreException {
        logger.debug("merge: dest {} diff {}", dest, diff);
        if (diff == null) {
            logger.debug("return null");
            return diff;
        }
        if (dest == null) {
            logger.debug("return {}", diff);
            return diff;
        }
        for (String key : diff.keySet()) {
            logger.debug("Key {}", key);
            if (diff.get(key) == null) {
                dest.remove(key);
                logger.debug("Key {} removed", key);
                continue;
            }

            try {
                // Map
                if (diff.get(key) instanceof Map) {
                    logger.debug("Key {} is a Map", key);
                    dest.put(key,
                            merge((Map<String, Object>) dest.get(key), (Map<String, Object>) diff.get(key)));
                    continue;
                }

                // List
                if (diff.get(key) instanceof List oldlist) {
                    logger.debug("Key {} is a List", key);
                    List elementlist = (List) dest.get(key);
                    removeTrailingEmptyMaps(oldlist);
                    // list was removed
                    if (elementlist == null) {
                        dest.put(key, oldlist);
                        continue;
                    }
                    if (oldlist.size() == 0) {
                        continue;
                    }
                    if (oldlist.get(0) instanceof Map) {
                        for (int i = 0; i < oldlist.size(); i++) {
                            int el_sz = elementlist.size();
                            if (i < el_sz) {
                                elementlist.set(i, merge((Map<String, Object>) elementlist.get(i),
                                        (Map<String, Object>) oldlist.get(i)));
                            } else {
                                elementlist.add(oldlist.get(i));
                            }
                        }
                        removeTrailingEmptyMaps(elementlist);
                        // remove empty list
                        if (elementlist.isEmpty()) {
                            dest.remove(key);
                        }
                        continue;
                    }
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new CoreException(ex.getMessage());
            }
            dest.put(key, diff.get(key));
        }
        logger.debug("merged last {}", dest);
        return dest;
    }
}
