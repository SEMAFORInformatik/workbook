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

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
public abstract class Modification implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mod_seq_gen")
    @SequenceGenerator(name = "mod_seq_gen", sequenceName = "MOD_SEQ", allocationSize = 50)
    private Long id;

    @Column(name = "modcomment") //( nullable=false, length=1024 )
    private String comment;

    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @ManyToOne
    @JoinColumn(name = "username")
    private Owner user;

    // Constructor
    public Modification() {
    }

    public Modification(Owner user, String changeComment) {
        this.user = user;
        this.comment = changeComment;
        this.timestamp = new Date();
    }

    // getter and setter of Id
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    // Timestamp
    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date created) {
        this.timestamp = created;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String toString() {
        final ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("timestamp", this.timestamp);
        return sb.toString();
    }

    public Owner getUser() {
        return this.user;
    }

    public void setUser(Owner owner) {
        this.user = owner;
    }

    public Long getRevision() {
        return getId().longValue();
    }
}
