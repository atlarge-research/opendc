import React from 'react'
import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import ProjectList from '../../components/projects/ProjectList'

const getVisibleProjectAuths = (projectAuths, filter) => {
    switch (filter) {
        case 'SHOW_ALL':
            return projectAuths
        case 'SHOW_OWN':
            return projectAuths.filter((projectAuth) => projectAuth.authorizationLevel === 'OWN')
        case 'SHOW_SHARED':
            return projectAuths.filter((projectAuth) => projectAuth.authorizationLevel !== 'OWN')
        default:
            return projectAuths
    }
}

const VisibleProjectAuthList = ({ filter }) => {
    const authorizations = useSelector((state) => {
        const denormalizedAuthorizations = state.projectList.authorizationsOfCurrentUser.map((authorizationIds) => {
            const authorization = state.objects.authorization[authorizationIds]
            authorization.user = state.objects.user[authorization.userId]
            authorization.project = state.objects.project[authorization.projectId]
            return authorization
        })

        return getVisibleProjectAuths(denormalizedAuthorizations, filter)
    })
    return <ProjectList authorizations={authorizations} />
}

VisibleProjectAuthList.propTypes = {
    filter: PropTypes.string.isRequired,
}

export default VisibleProjectAuthList
