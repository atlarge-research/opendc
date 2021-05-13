import PropTypes from 'prop-types'
import React from 'react'
import { Authorization } from '../../shapes'
import ProjectAuthRow from './ProjectAuthRow'

const ProjectAuthList = ({ authorizations }) => {
    return (
        <div className="vertically-expanding-container">
            {authorizations.length === 0 ? (
                <div className="alert alert-info">
                    <span className="info-icon fa fa-question-circle mr-2" />
                    <strong>No projects here yet...</strong> Add some with the 'New Project' button!
                </div>
            ) : (
                <table className="table table-striped">
                    <thead>
                        <tr>
                            <th>Project name</th>
                            <th>Last edited</th>
                            <th>Access rights</th>
                            <th />
                        </tr>
                    </thead>
                    <tbody>
                        {authorizations.map((authorization) => (
                            <ProjectAuthRow projectAuth={authorization} key={authorization.project._id} />
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    )
}

ProjectAuthList.propTypes = {
    authorizations: PropTypes.arrayOf(Authorization).isRequired,
}

export default ProjectAuthList
