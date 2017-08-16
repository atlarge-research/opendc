import {connect} from "react-redux";
import {setAuthVisibilityFilter} from "../../actions/simulations";
import FilterButton from "../../components/simulations/FilterButton";

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
