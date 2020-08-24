import { sendRequest } from "../index";
import { deleteById, getById } from "./util";

export function getRoom(roomId) {
  return getById("/rooms/{roomId}", { roomId });
}

export function updateRoom(room) {
  return sendRequest({
    path: "/rooms/{roomId}",
    method: "PUT",
    parameters: {
      body: {
        room
      },
      path: {
        roomId: room.id
      },
      query: {}
    }
  });
}

export function deleteRoom(roomId) {
  return deleteById("/rooms/{roomId}", { roomId });
}

export function getTilesOfRoom(roomId) {
  return getById("/rooms/{roomId}/tiles", { roomId });
}

export function addTileToRoom(tile) {
  return sendRequest({
    path: "/rooms/{roomId}/tiles",
    method: "POST",
    parameters: {
      body: {
        tile
      },
      path: {
        roomId: tile.roomId
      },
      query: {}
    }
  });
}
