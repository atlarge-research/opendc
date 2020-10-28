import React from 'react'
import { useSelector } from 'react-redux'
import ProjectList from '../../components/projects/ProjectAuthList'

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

const VisibleProjectAuthList = (props) => {
    const authorizations = useSelector((state) => {
        const denormalizedAuthorizations = state.projectList.authorizationsOfCurrentUser.map((authorizationIds) => {
            const authorization = state.objects.authorization[authorizationIds]
            authorization.user = state.objects.user[authorization.userId]
            authorization.project = state.objects.project[authorization.projectId]
            return authorization
        })

        return getVisibleProjectAuths(denormalizedAuthorizations, state.projectList.authVisibilityFilter)
    })
    return <ProjectList {...props} authorizations={authorizations} />
}

export default VisibleProjectAuthList
