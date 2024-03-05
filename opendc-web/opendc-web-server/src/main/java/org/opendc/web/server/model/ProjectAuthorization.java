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
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.opendc.web.proto.user.ProjectRole;

/**
 * An authorization for some user to participate in a project.
 */
@Entity
@Table
@NamedQueries({
    @NamedQuery(
            name = "ProjectAuthorization.findByUser",
            query =
                    """
            SELECT a
            FROM ProjectAuthorization a
            WHERE a.key.userName = :userName
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
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public ProjectRole role;

    /**
     * Construct a {@link ProjectAuthorization} object.
     */
    public ProjectAuthorization(Project project, String userName, ProjectRole role) {
        this.key = new ProjectAuthorization.Key(project.id, userName);
        this.project = project;
        this.role = role;
    }

    /**
     * JPA constructor
     */
    protected ProjectAuthorization() {}

    /**
     * List all projects for the user with the specified <code>userName</code>.
     *
     * @param userName The identifier of the user that is requesting the list of projects.
     * @return A query returning projects that the user has received authorization for.
     */
    public static PanacheQuery<ProjectAuthorization> findByUser(String userName) {
        return find("#ProjectAuthorization.findByUser", Parameters.with("userName", userName));
    }

    /**
     * Find the project with <code>id</code> for the user with the specified <code>userName</code>.
     *
     * @param userName The identifier of the user that is requesting the list of projects.
     * @param project_id The unique identifier of the project.
     * @return The project with the specified identifier or <code>null</code> if it does not exist or is not accessible
     *         to the user with the specified identifier.
     */
    public static ProjectAuthorization findByUser(String userName, long project_id) {
        return findById(new ProjectAuthorization.Key(project_id, userName));
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

        @Column(name = "user_name", nullable = false)
        public String userName;

        public Key(long projectId, String userName) {
            this.projectId = projectId;
            this.userName = userName;
        }

        protected Key() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return projectId == key.projectId && userName.equals(key.userName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectId, userName);
        }
    }
}
