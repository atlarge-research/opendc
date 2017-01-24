///<reference path="../util.ts" />
///<reference path="../../../typings/globals/jasmine/index.d.ts" />
import {Util} from "../util";


class TestUtils {
    /**
     * Checks whether the two (three-dimensional) wall lists are equivalent in content.
     *
     * This is a set-compare method, meaning that the order of the elements does not matter, but that they are present
     * in both arrays.
     *
     * Example of such a list: [[[1, 1], [2, 1]], [[3, 1], [0, 0]]]
     *
     * @param list1 The first list
     * @param list2 The second list
     */
    public static wallListEquals(list1: IRoomWall[], list2: IRoomWall[]): void {
        let current, found, counter;

        counter = 0;
        while (list1.length > 0) {
            current = list1.pop();
            found = false;
            list2.forEach((e: IRoomWall) => {
                if (current.startPos[0] === e.startPos[0] && current.startPos[1] === e.startPos[1] &&
                    current.horizontal === e.horizontal && current.length === e.length) {
                    counter++;
                    found = true;
                }
            });
            if (!found) {
                fail();
            }
        }
        expect(list2.length).toEqual(counter);
    }

    /**
     * Does the same as wallList3DEquals, only for two lists of tiles.
     *
     * @param expected
     * @param actual
     */
    public static positionListEquals(expected: IGridPosition[], actual: IGridPosition[]): void {
        let current, found;
        let counter = 0;

        while (expected.length > 0) {
            current = expected.pop();
            found = false;
            actual.forEach((e) => {
                if (current.x === e.x && current.y === e.y) {
                    counter++;
                    found = true;
                }
            });
            if (!found) {
                fail();
            }
        }

        expect(actual.length).toEqual(counter);
    }

    public static boundsEquals(actual: IBounds, expected: IBounds): void {
        expect(actual.min[0]).toBe(expected.min[0]);
        expect(actual.min[1]).toBe(expected.min[1]);
        expect(actual.center[0]).toBe(expected.center[0]);
        expect(actual.center[1]).toBe(expected.center[1]);
        expect(actual.max[0]).toBe(expected.max[0]);
        expect(actual.max[1]).toBe(expected.max[1]);
    }
}

describe("Deriving wall locations", () => {
    it("should generate walls around a single tile", () => {
            let room = {
                id: -1,
                datacenterId: -1,
                name: "testroom",
                roomType: "none",
                tiles: [{
                    id: -1,
                    roomId: -1,
                    position: {x: 1, y: 1}
                }]
            };

        let result = Util.deriveWallLocations([
                room
            ]);
        let expected: IRoomWall[] = [
            {
                startPos: [1, 1],
                horizontal: false,
                length: 1
            },
            {
                startPos: [2, 1],
                horizontal: false,
                length: 1
            },
            {
                startPos: [1, 1],
                horizontal: true,
                length: 1
            },
            {
                startPos: [1, 2],
                horizontal: true,
                length: 1
            }
            ];

        TestUtils.wallListEquals(expected, result);
        }
    );

    it("should generate walls around two tiles connected by an edge", () => {
            let room = {
                id: -1,
                datacenterId: -1,
                name: "testroom",
                roomType: "none",
                tiles: [
                    {
                        id: -1,
                        roomId: -1,
                        position: {x: 1, y: 1}
                    }, {
                        id: -1,
                        roomId: -1,
                        position: {x: 1, y: 2}
                    }
                ]
            };

        let result = Util.deriveWallLocations([
                room
            ]);
            let expected: IRoomWall[] = [
                {
                    startPos: [1, 1],
                    horizontal: false,
                    length: 2
                },
                {
                    startPos: [2, 1],
                    horizontal: false,
                    length: 2
                },
                {
                    startPos: [1, 1],
                    horizontal: true,
                    length: 1
                },
                {
                    startPos: [1, 3],
                    horizontal: true,
                    length: 1
                }
            ];

            TestUtils.wallListEquals(expected, result);
        }
    );

    it("should generate walls around two independent rooms with one tile each", () => {
            let room1 = {
                id: -1,
                datacenterId: -1,
                name: "testroom",
                roomType: "none",
                tiles: [
                    {
                        id: -1,
                        roomId: -1,
                        position: {x: 1, y: 1}
                    }
                ]
            };

            let room2 = {
                id: -1,
                datacenterId: -1,
                name: "testroom",
                roomType: "none",
                tiles: [{
                    id: -1,
                    roomId: -1,
                    position: {x: 1, y: 3}
                }
                ]
            };

        let result = Util.deriveWallLocations([
                room1, room2
            ]);
            let expected: IRoomWall[] = [
                {
                    startPos: [1, 1],
                    horizontal: false,
                    length: 1
                },
                {
                    startPos: [1, 3],
                    horizontal: false,
                    length: 1
                },
                {
                    startPos: [2, 1],
                    horizontal: false,
                    length: 1
                },
                {
                    startPos: [2, 3],
                    horizontal: false,
                    length: 1
                },
                {
                    startPos: [1, 1],
                    horizontal: true,
                    length: 1
                },
                {
                    startPos: [1, 2],
                    horizontal: true,
                    length: 1
                },
                {
                    startPos: [1, 3],
                    horizontal: true,
                    length: 1
                },
                {
                    startPos: [1, 4],
                    horizontal: true,
                    length: 1
                }
            ];

            TestUtils.wallListEquals(expected, result);
        }
    );
});

describe("Deriving valid next tile positions", () => {
    it("should derive correctly 4 valid tile positions around 1 selected tile with no other rooms", () => {
        let result = Util.deriveValidNextTilePositions([], [{
            id: -1,
            roomId: -1,
            position: {x: 1, y: 1}
        }]);
        let expected = [
            {x: 1, y: 0}, {x: 2, y: 1}, {x: 1, y: 2}, {x: 0, y: 1}
        ];

        TestUtils.positionListEquals(expected, result);
    });

    it("should derive correctly 6 valid tile positions around 2 selected tiles with no other rooms", () => {
        let result = Util.deriveValidNextTilePositions([], [{
            id: -1,
            roomId: -1,
            position: {x: 1, y: 1}
        }, {
            id: -1,
            roomId: -1,
            position: {x: 2, y: 1}
        }]);
        let expected = [
            {x: 1, y: 0}, {x: 2, y: 0}, {x: 3, y: 1}, {x: 1, y: 2}, {x: 2, y: 2}, {x: 0, y: 1}
        ];

        TestUtils.positionListEquals(expected, result);
    });

    it("should derive correctly 3 valid tile positions around 1 selected tiles with 1 adjacent room", () => {
        let room = {
            id: -1,
            datacenterId: -1,
            name: "testroom",
            roomType: "none",
            tiles: [{
                id: -1,
                roomId: -1,
                position: {x: 0, y: 1}
            }]
        };
        let result = Util.deriveValidNextTilePositions([room], [{
            id: -1,
            roomId: -1,
            position: {x: 1, y: 1}
        }]);
        let expected = [
            {x: 1, y: 0}, {x: 2, y: 1}, {x: 1, y: 2}
        ];

        TestUtils.positionListEquals(expected, result);
    });
});

describe("Calculating the bounds and average point of a list of rooms", () => {
    it("should calculate correctly the bounds of a 1-tile room", () => {
        let room = {
            id: -1,
            datacenterId: -1,
            name: "testroom",
            roomType: "none",
            tiles: [{
                id: -1,
                roomId: -1,
                position: {x: 1, y: 1}
            }]
        };
        let result = Util.calculateRoomListBounds([room]);
        let expected = {
            min: [1, 1],
            center: [1.5, 1.5],
            max: [2, 2]
        };

        TestUtils.boundsEquals(result, expected);
    });
});
