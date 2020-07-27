import { connect } from 'react-redux'
import {addPrefab} from "../../../../../actions/prefabs";
import AddPrefabComponent from "../../../../../components/app/sidebars/topology/rack/AddPrefabComponent";

const mapDispatchToProps = (dispatch) => {
    return {
        onClick: () => dispatch(addPrefab('name')),
    }
}

const AddPrefabContainer = connect(undefined, mapDispatchToProps)(AddPrefabComponent)

export default AddPrefabContainer
