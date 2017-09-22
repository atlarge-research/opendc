import {connect} from "react-redux";
import TaskComponent from "../../../../components/app/sidebars/simulation/TaskComponent";

const mapStateToProps = (state, ownProps) => {
    let flopsLeft = state.objects.task[ownProps.taskId].totalFlopCount;

    if (state.states.task[state.currentTick] && state.states.task[state.currentTick][ownProps.taskId]) {
        flopsLeft = state.states.task[state.currentTick][ownProps.taskId].flopsLeft;
    } else if (state.objects.task[ownProps.taskId].startTick < state.currentTick) {
        flopsLeft = 0;
    }

    return {
        task: state.objects.task[ownProps.taskId],
        flopsLeft,
    };
};

const TaskContainer = connect(
    mapStateToProps
)(TaskComponent);

export default TaskContainer;
