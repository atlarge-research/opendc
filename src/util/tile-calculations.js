export function deriveWallLocations(tiles) {
    const verticalWalls = {};
    const horizontalWalls = {};

    // Determine wall segments

    tiles.forEach(tile => {
        const x = tile.positionX, y = tile.positionY;
        for (let dX = -1; dX <= 1; dX++) {
            for (let dY = -1; dY <= 1; dY++) {
                if (Math.abs(dX) === Math.abs(dY)) {
                    continue;
                }

                let doInsert = true;
                tiles.forEach(otherTile => {
                    if (otherTile.positionX === x + dX && otherTile.positionY === y + dY) {
                        doInsert = false;
                    }
                });
                if (!doInsert) {
                    continue;
                }

                if (dX === -1) {
                    if (verticalWalls[x] === undefined) {
                        verticalWalls[x] = [];
                    }
                    if (verticalWalls[x].indexOf(y) === -1) {
                        verticalWalls[x].push(y);
                    }
                } else if (dX === 1) {
                    if (verticalWalls[x + 1] === undefined) {
                        verticalWalls[x + 1] = [];
                    }
                    if (verticalWalls[x + 1].indexOf(y) === -1) {
                        verticalWalls[x + 1].push(y);
                    }
                } else if (dY === -1) {
                    if (horizontalWalls[y] === undefined) {
                        horizontalWalls[y] = [];
                    }
                    if (horizontalWalls[y].indexOf(x) === -1) {
                        horizontalWalls[y].push(x);
                    }
                } else if (dY === 1) {
                    if (horizontalWalls[y + 1] === undefined) {
                        horizontalWalls[y + 1] = [];
                    }
                    if (horizontalWalls[y + 1].indexOf(x) === -1) {
                        horizontalWalls[y + 1].push(x);
                    }
                }
            }
        }
    });

    // Merge walls into longer segments

    const result = [];
    const walls = [verticalWalls, horizontalWalls];
    for (let i = 0; i < 2; i++) {
        const wallList = walls[i];
        for (let a in wallList) {
            a = parseInt(a, 10);

            wallList[a].sort((a, b) => {
                return a - b;
            });

            let startPos = wallList[a][0];
            const isHorizontal = i === 1;

            if (wallList[a].length === 1) {
                const startPosX = isHorizontal ? startPos : a;
                const startPosY = isHorizontal ? a : startPos;
                result.push({
                    startPosX,
                    startPosY,
                    isHorizontal,
                    length: 1
                });
            } else {
                let consecutiveCount = 1;
                for (let b = 0; b < wallList[a].length - 1; b++) {
                    if (b + 1 === wallList[a].length - 1) {
                        if (wallList[a][b + 1] - wallList[a][b] > 1) {
                            const startPosX = isHorizontal ? startPos : a;
                            const startPosY = isHorizontal ? a : startPos;
                            result.push({
                                startPosX,
                                startPosY,
                                isHorizontal,
                                length: consecutiveCount
                            });
                            consecutiveCount = 0;
                            startPos = wallList[a][b + 1];
                        }
                        const startPosX = isHorizontal ? startPos : a;
                        const startPosY = isHorizontal ? a : startPos;
                        result.push({
                            startPosX,
                            startPosY,
                            isHorizontal,
                            length: consecutiveCount + 1
                        });
                        break;
                    } else if (wallList[a][b + 1] - wallList[a][b] > 1) {
                        const startPosX = isHorizontal ? startPos : a;
                        const startPosY = isHorizontal ? a : startPos;
                        result.push({
                            startPosX,
                            startPosY,
                            isHorizontal,
                            length: consecutiveCount
                        });
                        startPos = wallList[a][b + 1];
                        consecutiveCount = 0;
                    }
                    consecutiveCount++;
                }
            }
        }
    }

    return result;
}
