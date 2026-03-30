export const CPU_UNITS = {
    'cpu-1': {
        id: 'cpu-1',
        name: 'Intel i7 v6 6700k',
        clockRateMhz: 4100,
        numberOfCores: 4,
        energyConsumptionW: 70,
    },
    'cpu-2': {
        id: 'cpu-2',
        name: 'Intel i5 v6 6700k',
        clockRateMhz: 3500,
        numberOfCores: 2,
        energyConsumptionW: 50,
    },
    'cpu-3': {
        id: 'cpu-3',
        name: 'Intel® Xeon® E-2224G',
        clockRateMhz: 3500,
        numberOfCores: 4,
        energyConsumptionW: 71,
    },
    'cpu-4': {
        id: 'cpu-4',
        name: 'Intel® Xeon® E-2244G',
        clockRateMhz: 3800,
        numberOfCores: 8,
        energyConsumptionW: 71,
    },
    'cpu-5': {
        id: 'cpu-5',
        name: 'Intel® Xeon® E-2246G',
        clockRateMhz: 3600,
        numberOfCores: 12,
        energyConsumptionW: 80,
    },
}

export const GPU_UNITS = {
    'gpu-1': {
        id: 'gpu-1',
        name: 'NVIDIA GTX 4 1080',
        clockRateMhz: 1200,
        numberOfCores: 200,
        energyConsumptionW: 250,
    },
    'gpu-2': {
        id: 'gpu-2',
        name: 'NVIDIA Tesla V100',
        clockRateMhz: 1200,
        numberOfCores: 5120,
        energyConsumptionW: 250,
    },
}

export const MEMORY_UNITS = {
    'memory-1': {
        id: 'memory-1',
        name: 'Samsung PC DRAM K4A4G045WD',
        speedMbPerS: 16000,
        sizeMb: 4000,
        energyConsumptionW: 10,
    },
    'memory-2': {
        id: 'memory-2',
        name: 'Samsung PC DRAM M393A2K43BB1-CRC',
        speedMbPerS: 2400,
        sizeMb: 16000,
        energyConsumptionW: 10,
    },
    'memory-3': {
        id: 'memory-3',
        name: 'Crucial MTA18ASF4G72PDZ-3G2E1',
        speedMbPerS: 3200,
        sizeMb: 32000,
        energyConsumptionW: 10,
    },
    'memory-4': {
        id: 'memory-4',
        name: 'Crucial MTA9ASF2G72PZ-3G2E1',
        speedMbPerS: 3200,
        sizeMb: 16000,
        energyConsumptionW: 10,
    },
}

export const STORAGE_UNITS = {
    'storage-1': {
        id: 'storage-1',
        name: 'Samsung EVO 2016 SATA III',
        speedMbPerS: 6000,
        sizeMb: 250000,
        energyConsumptionW: 10,
    },
    'storage-2': {
        id: 'storage-2',
        name: 'Western Digital MTA9ASF2G72PZ-3G2E1',
        speedMbPerS: 6000,
        sizeMb: 4000000,
        energyConsumptionW: 10,
    },
}
