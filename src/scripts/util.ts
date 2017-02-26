///<reference path="definitions.ts" />
import {Colors} from "./colors";


export enum IntensityLevel {
    LOW,
    MID_LOW,
    MID_HIGH,
    HIGH
}


export class Util {
    private static authorizationLevels = [
        "OWN", "EDIT", "VIEW"
    ];


    /**
     * Derives the wall locations given a list of rooms.
     *
     * Does so by computing an outline around all tiles in the rooms.
     */
    public static deriveWallLocations(rooms: IRoom[]): IRoomWall[] {
        const verticalWalls = {};
        const horizontalWalls = {};
        let doInsert;
        rooms.forEach((room: IRoom) => {
            room.tiles.forEach((tile: ITile) => {
                const x = tile.position.x, y = tile.position.y;
                for (let dX = -1; dX <= 1; dX++) {
                    for (let dY = -1; dY <= 1; dY++) {
                        if (Math.abs(dX) === Math.abs(dY)) {
                            continue;
                        }

                        doInsert = true;
                        room.tiles.forEach((otherTile: ITile) => {
                            if (otherTile.position.x === x + dX && otherTile.position.y === y + dY) {
                                doInsert = false;
                            }
                        });

                        if (doInsert) {
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
                }
            });
        });

        const result: IRoomWall[] = [];
        const walls = [verticalWalls, horizontalWalls];
        for (let i = 0; i < 2; i++) {
            const wallList = walls[i];
            for (let a in wallList) {
                if (!wallList.hasOwnProperty(a)) {
                    return;
                }

                wallList[a].sort((a: number, b: number) => {
                    return a - b;
                });

                let startPos = wallList[a][0];
                const positionArray = (i === 1 ? <number[]>[startPos, parseInt(a)] : <number[]>[parseInt(a), startPos]);

                if (wallList[a].length === 1) {
                    result.push({
                        startPos: positionArray,
                        horizontal: i === 1,
                        length: 1
                    });
                } else {
                    let consecutiveCount = 1;
                    for (let b = 0; b < wallList[a].length - 1; b++) {
                        if (b + 1 === wallList[a].length - 1) {
                            if (wallList[a][b + 1] - wallList[a][b] > 1) {
                                result.push({
                                    startPos: (i === 1 ? <number[]>[startPos, parseInt(a)] : <number[]>[parseInt(a), startPos]),
                                    horizontal: i === 1,
                                    length: consecutiveCount
                                });
                                consecutiveCount = 0;
                                startPos = wallList[a][b + 1];
                            }
                            result.push({
                                startPos: (i === 1 ? <number[]>[startPos, parseInt(a)] : <number[]>[parseInt(a), startPos]),
                                horizontal: i === 1,
                                length: consecutiveCount + 1
                            });
                            break;
                        } else if (wallList[a][b + 1] - wallList[a][b] > 1) {
                            result.push({
                                startPos: (i === 1 ? <number[]>[startPos, parseInt(a)] : <number[]>[parseInt(a), startPos]),
                                horizontal: i === 1,
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

    /**
     * Generates a list of all valid tile positions around the currently selected room under construction.
     *
     * @param rooms The rooms that already exist in the model
     * @param selectedTiles The tiles that the user has already selected to form a new room
     * @returns {Array} A 2D list of tile positions that are valid next tile choices.
     */
    public static deriveValidNextTilePositions(rooms: IRoom[], selectedTiles: ITile[]): IGridPosition[] {
        const result = [], newPosition = {x: 0, y: 0};
        let isSurroundingTile;

        selectedTiles.forEach((tile: ITile) => {
            const x = tile.position.x, y = tile.position.y;
            for (let dX = -1; dX <= 1; dX++) {
                for (let dY = -1; dY <= 1; dY++) {
                    if (Math.abs(dX) === Math.abs(dY)) {
                        continue;
                    }

                    newPosition.x = x + dX;
                    newPosition.y = y + dY;

                    isSurroundingTile = true;
                    selectedTiles.forEach((otherTile: ITile) => {
                        if (otherTile.position.x === newPosition.x && otherTile.position.y === newPosition.y) {
                            isSurroundingTile = false;
                        }
                    });

                    if (isSurroundingTile && !Util.checkRoomCollision(rooms, newPosition)) {
                        result.push({x: newPosition.x, y: newPosition.y});
                    }
                }
            }
        });

        return result;
    }

    /**
     * Determines whether a position is contained in a list of tiles.
     *
     * @param list A list of tiles
     * @param position A position
     * @returns {boolean} Whether the list contains the position
     */
    public static tileListContainsPosition(list: ITile[], position: IGridPosition): boolean {
        return Util.tileListPositionIndexOf(list, position) !== -1;
    }

    /**
     * Determines the index of a position in a list of tiles.
     *
     * @param list A list of tiles
     * @param position A position
     * @returns {number} Index of the position in the list of tiles, -1 if not found
     */
    public static tileListPositionIndexOf(list: ITile[], position: IGridPosition): number {
        let index = -1;

        for (let i = 0; i < list.length; i++) {
            const element = list[i];
            if (position.x === element.position.x && position.y === element.position.y) {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * Determines whether a position is contained in a list of positions.
     *
     * @param list A list of positions
     * @param position A position
     * @returns {boolean} Whether the list contains the position
     */
    public static positionListContainsPosition(list: IGridPosition[], position: IGridPosition): boolean {
        return Util.positionListPositionIndexOf(list, position) !== -1;
    }

    /**
     * Determines the index of a position in a list of positions.
     *
     * @param list A list of positions
     * @param position A position
     * @returns {number} Index of the position in the list of tiles, -1 if not found
     */
    public static positionListPositionIndexOf(list: IGridPosition[], position: IGridPosition): number {
        let index = -1;

        for (let i = 0; i < list.length; i++) {
            const element = list[i];
            if (position.x === element.x && position.y === element.y) {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * Determines the index of a room that is colliding with a given grid tile.
     *
     * Returns -1 if no collision is found.
     *
     * @param rooms An array of Room objects that should be checked for collisions
     * @param position A position
     * @returns {number} The index of the room in the rooms list if found, else -1
     */
    public static roomCollisionIndexOf(rooms: IRoom[], position: IGridPosition): number {
        let index = -1;

        for (let i = 0; i < rooms.length; i++) {
            const room = rooms[i];
            if (Util.tileListContainsPosition(room.tiles, position)) {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * Checks whether a tile location collides with an existing room.
     *
     * @param rooms A list of rooms to be analyzed
     * @param position A position
     * @returns {boolean} Whether the tile lies in an existing room
     */
    public static checkRoomCollision(rooms: IRoom[], position: IGridPosition): boolean {
        return Util.roomCollisionIndexOf(rooms, position) !== -1;
    }

    /**
     * Calculates the minimum, center, and maximum of a list of rooms in stage coordinates.
     *
     * This center is calculated by averaging the most outlying tiles of all rooms.
     *
     * @param rooms The rooms to be analyzed
     * @returns {IBounds} The coordinates of the minimum, center, and maximum
     */
    public static calculateRoomListBounds(rooms: IRoom[]): IBounds {
        const min = [Number.MAX_VALUE, Number.MAX_VALUE];
        const max = [-1, -1];

        rooms.forEach((room: IRoom) => {
            room.tiles.forEach((tile: ITile) => {
                if (tile.position.x < min[0]) {
                    min[0] = tile.position.x;
                }
                if (tile.position.y < min[1]) {
                    min[1] = tile.position.y;
                }

                if (tile.position.x > max[0]) {
                    max[0] = tile.position.x;
                }
                if (tile.position.y > max[1]) {
                    max[1] = tile.position.y;
                }
            });
        });

        max[0]++;
        max[1]++;

        const gridCenter = [min[0] + (max[0] - min[0]) / 2.0, min[1] + (max[1] - min[1]) / 2.0];

        return {
            min: min,
            center: gridCenter,
            max: max
        };
    }

    /**
     * Does the same as 'calculateRoomListBounds', only for one room.
     *
     * @param room The room to be analyzed
     * @returns {IBounds} The coordinates of the minimum, center, and maximum
     */
    public static calculateRoomBounds(room: IRoom): IBounds {
        return Util.calculateRoomListBounds([room]);
    }

    public static calculateRoomNamePosition(room: IRoom): IRoomNamePos {
        const result: IRoomNamePos = {
            topLeft: {x: 0, y: 0},
            length: 0
        };

        // Look for the top-most tile y-coordinate
        let topMin = Number.MAX_VALUE;
        room.tiles.forEach((tile: ITile) => {
            if (tile.position.y < topMin) {
                topMin = tile.position.y;
            }
        });

        // If there is no tile at the top, meaning that the room has no tiles, exit
        if (topMin === Number.MAX_VALUE) {
            return null;
        }

        // Find the left-most tile at the top and the length of its adjacent tiles to the right
        const topTilePositions: number[] = [];
        room.tiles.forEach((tile: ITile) => {
            if (tile.position.y === topMin) {
                topTilePositions.push(tile.position.x);
            }
        });
        topTilePositions.sort();
        const leftMin = topTilePositions[0];
        let length = 0;

        while (length < topTilePositions.length && topTilePositions[length] - leftMin === length) {
            length++;
        }

        result.topLeft.x = leftMin;
        result.topLeft.y = topMin;
        result.length = length;

        return result;
    }

    /**
     * Analyzes an array of objects and calculates its fill ratio, by looking at the number of elements != null and
     * comparing that number to the array length.
     *
     * @param inputList The list to be analyzed
     * @returns {number} A fill ratio (between 0 and 1), representing the relative amount of objects != null in the list
     */
    public static getFillRatio(inputList: any[]): number {
        let numNulls = 0;

        if (inputList.length === 0) {
            return 0;
        }

        inputList.forEach((element: any) => {
            if (element == null) {
                numNulls++;
            }
        });

        return (inputList.length - numNulls) / inputList.length;
    }

    /**
     * Calculates the energy consumption ration of the given rack.
     *
     * @param rack The rack of which the power consumption should be analyzed
     * @returns {number} The energy consumption ratio
     */
    public static getEnergyRatio(rack: IRack): number {
        let energySum = 0;

        rack.machines.forEach((machine: IMachine) => {
            if (machine === null) {
                return;
            }

            let machineConsumption = 0;

            let nodeUnitList: INodeUnit[] = <INodeUnit[]>machine.cpus.concat(machine.gpus);
            nodeUnitList = nodeUnitList.concat(<INodeUnit[]>machine.memories);
            nodeUnitList = nodeUnitList.concat(<INodeUnit[]>machine.storages);
            nodeUnitList.forEach((unit: INodeUnit) => {
                machineConsumption += unit.energyConsumptionW;
            });

            energySum += machineConsumption;
        });

        return energySum / rack.powerCapacityW;
    }

    /**
     * Parses date-time expresses of the form YYYY-MM-DDTHH:MM:SS and returns a parsed object.
     *
     * @param input A string expressing a date and a time, in the above mentioned format
     * @returns {IDateTime} A DateTime object with the parsed date and time information as content
     */
    public static parseDateTime(input: string): IDateTime {
        const output: IDateTime = {
            year: 0,
            month: 0,
            day: 0,
            hour: 0,
            minute: 0,
            second: 0
        };

        const dateAndTime = input.split("T");
        const dateComponents = dateAndTime[0].split("-");
        output.year = parseInt(dateComponents[0], 10);
        output.month = parseInt(dateComponents[1], 10);
        output.day = parseInt(dateComponents[2], 10);

        const timeComponents = dateAndTime[1].split(":");
        output.hour = parseInt(timeComponents[0], 10);
        output.minute = parseInt(timeComponents[1], 10);
        output.second = parseInt(timeComponents[2], 10);

        return output;
    }

    public static formatDateTime(input: IDateTime) {
        let date;
        const currentDate = new Date();

        date = Util.addPaddingToTwo(input.day) + "/" +
            Util.addPaddingToTwo(input.month) + "/" +
            Util.addPaddingToTwo(input.year);

        if (input.year === currentDate.getFullYear() &&
            input.month === currentDate.getMonth() + 1) {
            if (input.day === currentDate.getDate()) {
                date = "Today";
            } else if (input.day === currentDate.getDate() - 1) {
                date = "Yesterday";
            }
        }

        return date + ", " +
            Util.addPaddingToTwo(input.hour) + ":" +
            Util.addPaddingToTwo(input.minute);
    }

    public static getCurrentDateTime(): string {
        const currentDate = new Date();
        return currentDate.getFullYear() + "-" + Util.addPaddingToTwo(currentDate.getMonth() + 1) + "-" +
            Util.addPaddingToTwo(currentDate.getDate()) + "T" + Util.addPaddingToTwo(currentDate.getHours()) + ":" +
            Util.addPaddingToTwo(currentDate.getMinutes()) + ":" + Util.addPaddingToTwo(currentDate.getSeconds());
    }

    /**
     * Removes all populated object properties from a given object, and returns a copy without them.
     *
     * An exception of such an object property is made in the case of a position object (of type GridPosition), which
     * is copied over as well.
     *
     * Does not manipulate the original object in any way, except if your object has quantum-like properties, which
     * change upon inspection. In such a case, I'm afraid that this method can do little for you.
     *
     * @param object The input object
     * @returns {any} A copy of the object without any populated properties (of type object).
     */
    public static packageForSending(object: any) {
        const result: any = {};
        for (let prop in object) {
            if (object.hasOwnProperty(prop)) {
                if (typeof object[prop] !== "object") {
                    result[prop] = object[prop];
                } else {
                    if (object[prop] instanceof Array) {
                        if (object[prop].length === 0 || !(object[prop][0] instanceof Object)) {
                            result[prop] = [];
                            for (let i = 0; i < object[prop].length; i++) {
                                result[prop][i] = object[prop][i];
                            }
                        }
                    }
                    if (object[prop] != null && object[prop].hasOwnProperty("x") && object[prop].hasOwnProperty("y")) {
                        result["positionX"] = object[prop].x;
                        result["positionY"] = object[prop].y;
                    }
                }
            }
        }
        return result;
    }

    public static addPaddingToTwo(integer: number): string {
        if (integer < 10) {
            return "0" + integer.toString();
        } else {
            return integer.toString();
        }
    }

    public static convertSecondsToFormattedTime(seconds: number): string {
        let hour = Math.floor(seconds / 3600);
        let minute = Math.floor(seconds / 60) % 60;
        let second = seconds % 60;

        hour = isNaN(hour) ? 0 : hour;
        minute = isNaN(minute) ? 0 : minute;
        second = isNaN(second) ? 0 : second;

        return this.addPaddingToTwo(hour) + ":" +
            this.addPaddingToTwo(minute) + ":" +
            this.addPaddingToTwo(second);
    }

    public static determineLoadIntensityLevel(loadFraction: number): IntensityLevel {
        if (loadFraction < 0.25) {
            return IntensityLevel.LOW;
        } else if (loadFraction < 0.5) {
            return IntensityLevel.MID_LOW;
        } else if (loadFraction < 0.75) {
            return IntensityLevel.MID_HIGH;
        } else {
            return IntensityLevel.HIGH;
        }
    }

    public static convertIntensityToColor(intensityLevel: IntensityLevel): string {
        if (intensityLevel === IntensityLevel.LOW) {
            return Colors.SIM_LOW;
        } else if (intensityLevel === IntensityLevel.MID_LOW) {
            return Colors.SIM_MID_LOW;
        } else if (intensityLevel === IntensityLevel.MID_HIGH) {
            return Colors.SIM_MID_HIGH;
        } else if (intensityLevel === IntensityLevel.HIGH) {
            return Colors.SIM_HIGH;
        }
    }

    /**
     * Gives the sentence-cased alternative for a given string.
     *
     * @example Input: TEST, Output: Test
     *
     * @param input The input string
     * @returns {any} The sentence-cased string
     */
    public static toSentenceCase(input: string): string {
        if (input === undefined || input === null) {
            return undefined;
        }
        if (input.length === 0) {
            return "";
        }

        return input[0].toUpperCase() + input.substr(1).toLowerCase();
    }

    /**
     * Sort a list of authorizations based on the levels of authorizations.
     *
     * @param list The list to be sorted (in-place)
     */
    public static sortAuthorizations(list: IAuthorization[]): void {
        list.sort((a: IAuthorization, b: IAuthorization): number => {
            return this.authorizationLevels.indexOf(a.authorizationLevel) -
                this.authorizationLevels.indexOf(b.authorizationLevel);
        });
    }

    /**
     * Returns an array containing all numbers of a range from 0 to x (including x).
     */
    public static range(x: number): number[] {
        return Array.apply(null, Array(x + 1)).map((_, i) => {
            return i.toString();
        })
    }

    /**
     * Returns an array containing all numbers of a range from 0 to x (including x).
     */
    public static timeRange(x: number): Date[] {
        return Util.range(x).map((tick: number) => {
            const t = new Date(1970, 0, 1); // Epoch
            t.setSeconds(tick);
            return t;
        });
    }
}
