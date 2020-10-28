import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { setAuthVisibilityFilter } from '../../actions/projects'
import FilterButton from '../../components/projects/FilterButton'

const FilterLink = (props) => {
    const active = useSelector((state) => state.projectList.authVisibilityFilter === props.filter)
    const dispatch = useDispatch()

    return <FilterButton {...props} onClick={() => dispatch(setAuthVisibilityFilter(props.filter))} active={active} />
}

export default FilterLink
