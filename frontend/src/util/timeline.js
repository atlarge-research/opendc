export function convertTickToPercentage(tick, maxTick) {
    if (maxTick === 0) {
        return '0%'
    } else if (tick > maxTick) {
        return maxTick / (maxTick + 1) * 100 + '%'
    }

    return tick / (maxTick + 1) * 100 + '%'
}

export function getDatacenterIdOfTick(tick, sections) {
    for (let i in sections.reverse()) {
        if (tick >= sections[i].startTick) {
            return sections[i].datacenterId
        }
    }

    return -1
}
