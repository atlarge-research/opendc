const EXCLUDED_IDENTIFIERS = [
    "objectId",
    "googleId",
];

export function denormalize(state, objectType, id) {
    return denormalizeWithRecursionCheck(state, objectType, id, undefined);
}

function denormalizeWithRecursionCheck(state, objectType, id, previousType) {
    const object = Object.assign({}, state.objects[objectType][id]);

    for (let prop in object) {
        if (prop.indexOf(previousType) !== -1) {
            continue;
        }

        if (prop.endsWith("Id") && EXCLUDED_IDENTIFIERS.indexOf(prop) === -1) {
            const propType = prop.replace("Id", "");
            object[propType] = denormalizeWithRecursionCheck(state, propType, object[prop], objectType);
        }

        if (prop.endsWith("Ids")) {
            const propType = prop.replace("Ids", "");
            object[propType + "s"] = object[prop].map(id => denormalizeWithRecursionCheck(state, propType, id, objectType));
        }
    }

    return object;
}
