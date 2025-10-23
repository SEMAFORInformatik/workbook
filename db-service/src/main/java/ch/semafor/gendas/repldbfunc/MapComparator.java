package ch.semafor.gendas.repldbfunc;

import java.util.Comparator;
import java.util.Map;

/**
 * Comparator for sorting lists of map
 */

public class MapComparator implements Comparator<Object> {
    public Integer order;  // order 1 = ASC, 0 = DESC
    public String key; // to be sorted
    public boolean ignorecase; // true if case to be ignored

    public MapComparator() {
        this.order = null;
        this.key = null;
        this.ignorecase = false;
    }

    public int compare(Object obj1, Object obj2) {
        try {
            if (this.order == 1) {     // ascending order
                return new GenericComparator(true, this.ignorecase).compare(
                        ((Map<String, Object>) obj1).get(this.key),
                        ((Map<String, Object>) obj2).get(this.key));
            } else {  // descending order
                return new GenericComparator(false, this.ignorecase).compare(
                        ((Map<String, Object>) obj1).get(this.key),
                        ((Map<String, Object>) obj2).get(this.key));
            }
        } catch (Exception exp) {
            return 0;
        }
    }

}
