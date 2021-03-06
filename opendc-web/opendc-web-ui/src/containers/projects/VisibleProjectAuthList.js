import { connect } from 'react-redux'
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

const mapStateToProps = (state) => {
    const denormalizedAuthorizations = state.projectList.authorizationsOfCurrentUser.map((authorizationIds) => {
        const authorization = state.objects.authorization[authorizationIds]
        authorization.user = state.objects.user[authorization.userId]
        authorization.project = state.objects.project[authorization.projectId]
        return authorization
    })

    return {
        authorizations: getVisibleProjectAuths(denormalizedAuthorizations, state.projectList.authVisibilityFilter),
    }
}

const VisibleProjectAuthList = connect(mapStateToProps)(ProjectList)

export default VisibleProjectAuthList
