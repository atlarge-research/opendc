import PropTypes from "prop-types";
import React from "react";
import {Image} from "react-konva";

class ImageComponent extends React.Component {
    static propTypes = {
        src: PropTypes.string.isRequired,
        x: PropTypes.number.isRequired,
        y: PropTypes.number.isRequired,
        width: PropTypes.number.isRequired,
        height: PropTypes.number.isRequired,
        opacity: PropTypes.number.isRequired,
    };

    state = {
        image: null
    };

    componentDidMount() {
        const image = new window.Image();
        image.src = this.props.src;
        image.onload = () => this.setState({image});
    }

    render() {
        return (
            <Image
                image={this.state.image}
                x={this.props.x}
                y={this.props.y}
                width={this.props.width}
                height={this.props.height}
                opacity={this.props.opacity}
            />
        )
    }
}

export default ImageComponent;
