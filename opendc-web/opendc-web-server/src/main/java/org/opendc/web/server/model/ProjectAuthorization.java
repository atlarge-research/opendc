/*
 * Copyright (c) 2022 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.web.server.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import org.opendc.web.proto.user.ProjectRole;

/**
 * An authorization for some user to participate in a project.
 */
@Entity
@Table(name = "project_authorizations")
@NamedQueries({
    @NamedQuery(
            name = "ProjectAuthorization.findByUser",
            query =
                    """
            SELECT a
            FROM ProjectAuthorization a
            WHERE a.key.userId = :userId
        """),
})
public class ProjectAuthorization extends PanacheEntityBase {
    /**
     * The user identifier of the authorization.
     */
    @EmbeddedId
    public ProjectAuthorization.Key key;

    /**
     * The project that the user is authorized to participate in.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(
            name = "project_id",
            updatable = false,
            insertable = false,
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_authorizations"))
    public Project project;

    /**
     * The role of the user in the project.
     */
    @Type(type = "io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType")
    @Column(nullable = false, columnDefinition = "enum")
    @Enumerated(EnumType.STRING)
    public ProjectRole role;

    /**
     * Construct a {@link ProjectAuthorization} object.
     */
    public ProjectAuthorization(Project project, String userId, ProjectRole role) {
        this.key = new ProjectAuthorization.Key(project.id, userId);
        this.project = project;
        this.role = role;
    }

    /**
     * JPA constructor
     */
    protected ProjectAuthorization() {}

    /**
     * List all projects for the user with the specified <code>userId</code>.
     *
     * @param userId The identifier of the user that is requesting the list of projects.
     * @return A query returning projects that the user has received authorization for.
     */
    public static PanacheQuery<ProjectAuthorization> findByUser(String userId) {
        return find("#ProjectAuthorization.findByUser", Parameters.with("userId", userId));
    }

    /**
     * Find the project with <code>id</code> for the user with the specified <code>userId</code>.
     *
     * @param userId The identifier of the user that is requesting the list of projects.
     * @param id The unique identifier of the project.
     * @return The project with the specified identifier or <code>null</code> if it does not exist or is not accessible
     *         to the user with the specified identifier.
     */
    public static ProjectAuthorization findByUser(String userId, long id) {
        return findById(new ProjectAuthorization.Key(id, userId));
    }

    /**
     * Determine whether the authorization allows the user to edit the project.
     */
    public boolean canEdit() {
        return switch (role) {
            case OWNER, EDITOR -> true;
            case VIEWER -> false;
        };
    }

    /**
     * Determine whether the authorization allows the user to delete the project.
     */
    public boolean canDelete() {
        return role == ProjectRole.OWNER;
    }

    /**
     * Key for representing a {@link ProjectAuthorization} object.
     */
    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "project_id", nullable = false)
        public long projectId;

        @Column(name = "user_id", nullable = false)
        public String userId;

        public Key(long projectId, String userId) {
            this.projectId = projectId;
            this.userId = userId;
        }

        protected Key() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return projectId == key.projectId && userId.equals(key.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectId, userId);
        }
    }
}
