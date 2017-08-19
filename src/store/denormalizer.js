export function denormalize(state, objectType, id) {
    const object = Object.assign({}, state.objects[objectType][id]);

    for (let prop in object) {
        if (!object.hasOwnProperty(prop)) {
            continue;
        }

        if (prop.endsWith("Id")) {
            const propType = prop.replace("Id", "");
            object[propType] = state.objects[propType][object[prop]];
        }
    }

    return object;
}
