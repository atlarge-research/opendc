import {connect} from "react-redux";
import {setAuthVisibilityFilter} from "../../actions/projects";
import FilterButton from "../../components/projects/FilterButton";

const mapStateToProps = (state, ownProps) => {
    return {
        active: state.authVisibilityFilter === ownProps.filter
    };
};

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onClick: () => dispatch(setAuthVisibilityFilter(ownProps.filter))
    };
};

const FilterLink = connect(
    mapStateToProps,
    mapDispatchToProps
)(FilterButton);

export default FilterLink;
