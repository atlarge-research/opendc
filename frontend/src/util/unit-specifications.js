export const CPU_UNITS = {
    'cpu-1': {
        _id: 'cpu-1',
        name: 'Intel i7 v6 6700k',
        clockRateMhz: 4100,
        numberOfCores: 4,
        energyConsumptionW: 70,
    },
    'cpu-2': {
        _id: 'cpu-2',
        name: 'Intel i5 v6 6700k',
        clockRateMhz: 3500,
        numberOfCores: 2,
        energyConsumptionW: 50,
    },
}

export const GPU_UNITS = {
    'gpu-1': {
        _id: 'gpu-1',
        name: 'NVIDIA GTX 4 1080',
        clockRateMhz: 1200,
        numberOfCores: 200,
        energyConsumptionW: 250,
    },
}

export const MEMORY_UNITS = {
    'memory-1': {
        _id: 'memory-1',
        name: 'Samsung PC DRAM K4A4G045WD',
        speedMbPerS: 16000,
        sizeMb: 4000,
        energyConsumptionW: 10,
    },
}

export const STORAGE_UNITS = {
    'storage-1': {
        _id: 'storage-1',
        name: 'Samsung EVO 2016 SATA III',
        speedMbPerS: 6000,
        sizeMb: 250000,
        energyConsumptionW: 10,
    },
}
