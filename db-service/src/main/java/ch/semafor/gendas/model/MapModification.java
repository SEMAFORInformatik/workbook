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
package ch.semafor.gendas.model;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "modifications")
public class MapModification extends Modification {
    public static final long MaxRevision = 9999999L;
    @Transient
    public static final String SEQUENCE_NAME = "modifications";
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(MapModification.class);
    Map<String, Object> diff;
    private Long elementRef;

    // Constructor
    public MapModification() {
    }

    public MapModification(Long elementRef, Owner user, String changeComment,
                           Map<String, Object> diff) {
        super(user, changeComment);
        this.elementRef = elementRef;
        this.diff = diff;
    }

    public String toString() {
        final ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", getId());
        sb.append("timestamp", getTimestamp());
        return sb.toString();
    }

    public Long getElementRef() {
        return this.elementRef;
    }

    public void setElementRef(Long refid) {
        this.elementRef = refid;
    }

    public Map<String, Object> getDiff() {
        return diff;
    }

    public Map<String, Object> getOld() {
        return getOld(diff);
    }

    public Map<String, Object> getOld(Map<String, Object> diff) {
        Map<String, Object> old = new HashMap<String, Object>();
        if (diff == null)
            return old;
        for (String key : diff.keySet()) {
            if (diff.get(key) instanceof Map) { // subelement
                Map<String, Object> m = (Map<String, Object>) diff.get(key);
                old.put(key, getOld(m));
                continue;
            }
            try {
                List l = (List) diff.get(key);
                if (l.get(0) instanceof Map && // first element is a Map
                        !(l.size() == 2 && l.get(1) == null) // [Map, null] means entire Map was deleted, so Map is old
                ) { // modified list of subelements
                    List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
                    for (int i = 0; i < l.size(); i++) {
                        children.add(getOld((Map<String, Object>) l.get(i)));
                    }
                    old.put(key, children);
                } else { // diff list [old, new] -> l.get(0) is old
                    old.put(key, l.get(0));
                }
            } catch (ClassCastException ex) {
                logger.error("Illegal diff {}: {}", key, diff.get(key));
                throw ex;
            }
        }
        return old;
    }
}
