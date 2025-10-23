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

import ch.semafor.gendas.exceptions.CoreException;
import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

@Entity
@Table(name = "modifications")
public class TableModification extends Modification {
    public static final long MaxRevision = 9999999L;
    private static final long serialVersionUID = 1L;
    @ManyToOne
    @JoinColumn(name = "element_id")
    private Element element;

    @Column(nullable = false)
    private Long revision;

    @Column(nullable = false)
    private Long nextRevision;

    // Constructor
    public TableModification() {
    }

    // Constructor
    public TableModification(final Element el, final TableModification last_history) throws CoreException {
        this.setTimestamp(new Date());
        this.element = el;
        if (last_history == null) {
            this.revision = 1L;
        } else if (last_history.isTransient()) {
            throw new CoreException("New Revision of History exists already");
        } else {
            this.revision = last_history.getRevision() + 1;
        }
        this.nextRevision = MaxRevision;
    }

    // getter and setter of Element
    public Element getElement() {
        return element;
    }

    public void setElement(final Element el) {
        this.element = el;
    }

    public Long getRevision() {
        return this.revision;
    }

    // setter and getter of Revision
    public void setRevision(final long rev) {
        this.revision = rev;
    }

    public Long getNextRevision() {
        return this.nextRevision;
    }

    // NextRevision
    public void setNextRevision(final long rev) {
        this.nextRevision = rev;
    }

    public String toString() {
        final ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", getId());
        sb.append("revision", revision + " -> " + nextRevision);
        sb.append("timestamp", getTimestamp());
        return sb.toString();
    }

    public void print(final int ind) {
        element.indent(ind);
        System.out.println(this);
    }

    public boolean checkParent(final Element e) {
        return this.element == e;
    }

    public boolean isTransient() {
        return getId() == null;
    }

}
