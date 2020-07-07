import { connect } from 'react-redux'
import AppNavbarComponent from '../../components/navigation/AppNavbarComponent'

const mapStateToProps = state => {
    return {
        project: state.currentProjectId !== '-1' ? state.objects.project[state.currentProjectId] : undefined,
    }
}

const AppNavbarContainer = connect(mapStateToProps)(AppNavbarComponent)

export default AppNavbarContainer
