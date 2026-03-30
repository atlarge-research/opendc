import PropTypes from 'prop-types'
import React, { useEffect, useState } from 'react'
import { Image } from 'react-konva'

const imageCaches = {}

function ImageComponent({ src, x, y, width, height, opacity }) {
    const [image, setImage] = useState(null)

    useEffect(() => {
        if (imageCaches[src]) {
            setImage(imageCaches[src])
            return
        }

        const image = new window.Image()
        image.src = src
        image.onload = () => {
            setImage(image)
            imageCaches[src] = image
        }
    }, [src])

    // eslint-disable-next-line jsx-a11y/alt-text
    return <Image image={image} x={x} y={y} width={width} height={height} opacity={opacity} />
}

ImageComponent.propTypes = {
    src: PropTypes.string.isRequired,
    x: PropTypes.number.isRequired,
    y: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    opacity: PropTypes.number.isRequired,
}

export default ImageComponent
