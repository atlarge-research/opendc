export function deriveWallLocations(tiles) {
    const { verticalWalls, horizontalWalls } = getWallSegments(tiles)
    return mergeWallSegments(verticalWalls, horizontalWalls)
}

function getWallSegments(tiles) {
    const verticalWalls = {}
    const horizontalWalls = {}

    tiles.forEach((tile) => {
        const x = tile.positionX,
            y = tile.positionY

        for (let dX = -1; dX <= 1; dX++) {
            for (let dY = -1; dY <= 1; dY++) {
                if (Math.abs(dX) === Math.abs(dY)) {
                    continue
                }

                let doInsert = true
                for (const tile of tiles) {
                    if (tile.positionX === x + dX && tile.positionY === y + dY) {
                        doInsert = false
                        break
                    }
                }
                if (!doInsert) {
                    continue
                }

                if (dX === -1) {
                    if (verticalWalls[x] === undefined) {
                        verticalWalls[x] = []
                    }
                    if (verticalWalls[x].indexOf(y) === -1) {
                        verticalWalls[x].push(y)
                    }
                } else if (dX === 1) {
                    if (verticalWalls[x + 1] === undefined) {
                        verticalWalls[x + 1] = []
                    }
                    if (verticalWalls[x + 1].indexOf(y) === -1) {
                        verticalWalls[x + 1].push(y)
                    }
                } else if (dY === -1) {
                    if (horizontalWalls[y] === undefined) {
                        horizontalWalls[y] = []
                    }
                    if (horizontalWalls[y].indexOf(x) === -1) {
                        horizontalWalls[y].push(x)
                    }
                } else if (dY === 1) {
                    if (horizontalWalls[y + 1] === undefined) {
                        horizontalWalls[y + 1] = []
                    }
                    if (horizontalWalls[y + 1].indexOf(x) === -1) {
                        horizontalWalls[y + 1].push(x)
                    }
                }
            }
        }
    })

    return { verticalWalls, horizontalWalls }
}

function mergeWallSegments(vertical, horizontal) {
    const result = []
    const walls = [vertical, horizontal]

    for (let i = 0; i < 2; i++) {
        const wallList = walls[i]
        for (let a in wallList) {
            a = parseInt(a, 10)

            wallList[a].sort((a, b) => {
                return a - b
            })

            let startPos = wallList[a][0]
            const isHorizontal = i === 1

            if (wallList[a].length === 1) {
                const startPosX = isHorizontal ? startPos : a
                const startPosY = isHorizontal ? a : startPos
                result.push({
                    startPosX,
                    startPosY,
                    isHorizontal,
                    length: 1,
                })
            } else {
                let consecutiveCount = 1
                for (let b = 0; b < wallList[a].length - 1; b++) {
                    if (b + 1 === wallList[a].length - 1) {
                        if (wallList[a][b + 1] - wallList[a][b] > 1) {
                            const startPosX = isHorizontal ? startPos : a
                            const startPosY = isHorizontal ? a : startPos
                            result.push({
                                startPosX,
                                startPosY,
                                isHorizontal,
                                length: consecutiveCount,
                            })
                            consecutiveCount = 0
                            startPos = wallList[a][b + 1]
                        }
                        const startPosX = isHorizontal ? startPos : a
                        const startPosY = isHorizontal ? a : startPos
                        result.push({
                            startPosX,
                            startPosY,
                            isHorizontal,
                            length: consecutiveCount + 1,
                        })
                        break
                    } else if (wallList[a][b + 1] - wallList[a][b] > 1) {
                        const startPosX = isHorizontal ? startPos : a
                        const startPosY = isHorizontal ? a : startPos
                        result.push({
                            startPosX,
                            startPosY,
                            isHorizontal,
                            length: consecutiveCount,
                        })
                        startPos = wallList[a][b + 1]
                        consecutiveCount = 0
                    }
                    consecutiveCount++
                }
            }
        }
    }

    return result
}

export function deriveValidNextTilePositions(rooms, selectedTiles) {
    const result = [],
        newPosition = { x: 0, y: 0 }
    let isSurroundingTile

    selectedTiles.forEach((tile) => {
        const x = tile.positionX
        const y = tile.positionY
        result.push({ x, y })

        for (let dX = -1; dX <= 1; dX++) {
            for (let dY = -1; dY <= 1; dY++) {
                if (Math.abs(dX) === Math.abs(dY)) {
                    continue
                }

                newPosition.x = x + dX
                newPosition.y = y + dY

                isSurroundingTile = true
                for (let index in selectedTiles) {
                    if (
                        selectedTiles[index].positionX === newPosition.x &&
                        selectedTiles[index].positionY === newPosition.y
                    ) {
                        isSurroundingTile = false
                        break
                    }
                }

                if (isSurroundingTile && findPositionInRooms(rooms, newPosition.x, newPosition.y) === -1) {
                    result.push({ x: newPosition.x, y: newPosition.y })
                }
            }
        }
    })

    return result
}

export function findPositionInPositions(positions, positionX, positionY) {
    for (let i = 0; i < positions.length; i++) {
        const position = positions[i]
        if (positionX === position.x && positionY === position.y) {
            return i
        }
    }

    return -1
}

export function findPositionInRooms(rooms, positionX, positionY) {
    for (let i = 0; i < rooms.length; i++) {
        const room = rooms[i]
        if (findPositionInTiles(room.tiles, positionX, positionY) !== -1) {
            return i
        }
    }

    return -1
}

function findPositionInTiles(tiles, positionX, positionY) {
    let index = -1

    for (let i = 0; i < tiles.length; i++) {
        const tile = tiles[i]
        if (positionX === tile.positionX && positionY === tile.positionY) {
            index = i
            break
        }
    }

    return index
}

export function findTileWithPosition(tiles, positionX, positionY) {
    for (let i = 0; i < tiles.length; i++) {
        if (tiles[i].positionX === positionX && tiles[i].positionY === positionY) {
            return tiles[i]
        }
    }

    return null
}

export function calculateRoomListBounds(rooms) {
    const min = { x: Number.MAX_VALUE, y: Number.MAX_VALUE }
    const max = { x: -1, y: -1 }

    rooms.forEach((room) => {
        room.tiles.forEach((tile) => {
            if (tile.positionX < min.x) {
                min.x = tile.positionX
            }
            if (tile.positionY < min.y) {
                min.y = tile.positionY
            }

            if (tile.positionX > max.x) {
                max.x = tile.positionX
            }
            if (tile.positionY > max.y) {
                max.y = tile.positionY
            }
        })
    })

    max.x++
    max.y++

    const center = {
        x: min.x + (max.x - min.x) / 2.0,
        y: min.y + (max.y - min.y) / 2.0,
    }

    return { min, center, max }
}
