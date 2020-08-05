#!/bin/bash

echo "Adding domain-specific prefabs"

MONGO_CMD="mongo $OPENDC_DB -u $OPENDC_DB_USERNAME -p $OPENDC_DB_PASSWORD --authenticationDatabase $OPENDC_DB"

echo "Adding HPC prefabs"

# Dell R440
$MONGO_CMD --eval 'db.prefabs.insertOne(
    {
    "_id" : 440,
    "name" : "Dell R440",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "Dell R440",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Gold 6252",
                        "clockRateMhz": 2100,
                        "numberOfCores": 24,
                        "energyConsumptionW": 150
                    },
                    {
                        "name": "Intel Xeon Gold 6252",
                        "clockRateMhz": 2100,
                        "numberOfCores": 24,
                        "energyConsumptionW": 150
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "SK Hynix RDIMM HMA84GR7MFR4N-VK",
                        "speedMbPerS": 42656,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# Dell R640
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 640,
    "name" : "Dell R640",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "Dell R640",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# Dell R740xd
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 740,
    "name" : "Dell R740xd",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "Dell R740xd",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# Dell R940
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 940,
    "name" : "Dell R940",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "Dell R940",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Dell 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# Inspur NF5280M5
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 5280,
    "name" : "Inspur NF5280M5",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "Inspur NF5280M5",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386ABG40M51-CAE",
                        "speedMbPerS": 51200,
                        "sizeMb": 262144,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },{
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },{
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },{
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Intel 7.6TB D3-S4610 SSD SATA",
                        "speedMbPerS": 550,
                        "sizeMb": 7600000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# Inspur power systems FP5180G2
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 5180,
    "name" : "Inspur Power Systems FP5180G2",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "Inspur Power Systems FP5180G2",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "IBM POWER9 CP9M08",
                        "clockRateMhz": 2750,
                        "numberOfCores": 22,
                        "energyConsumptionW": 190
                    },
                    {
                        "name": "IBM POWER9 CP9M08",
                        "clockRateMhz": 2750,
                        "numberOfCores": 22,
                        "energyConsumptionW": 190
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Micron LRDIMM MTA144ASQ16G72LSZ-2S6",
                        "speedMbPerS": 42656,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    },
                    {
                        "name": "Intel 6.4TB D7-P5600 SSD SATA",
                        "speedMbPerS": 7000,
                        "sizeMb": 6400000,
                        "energyConsumptionW": 20
                    }
                ],
            }
        ]
    }
});'

# HPE Superdome Flex node
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 7873736633539,
    "name" : "HPE Superdome Flex Node",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "HPE Superdome Flex Node",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    }
                ],
                "gpus": [
                    {
                        "name": "NVIDIA TESLA V100 32GB",
                        "clockRateMhz": 1230,
                        "numberOfCores": 5120,
                        "energyConsumptionW": 250
                    },
                    {
                        "name": "NVIDIA TESLA V100 32GB",
                        "clockRateMhz": 1230,
                        "numberOfCores": 5120,
                        "energyConsumptionW": 250
                    },
                    {
                        "name": "NVIDIA TESLA V100 32GB",
                        "clockRateMhz": 1230,
                        "numberOfCores": 5120,
                        "energyConsumptionW": 250
                    },
                    {
                        "name": "NVIDIA TESLA V100 32GB",
                        "clockRateMhz": 1230,
                        "numberOfCores": 5120,
                        "energyConsumptionW": 250
                    }
                ],
                "memories": [
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# HPE DL360 G10
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 36010,
    "name" : "HPE DL360 Gen10",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "HPE DL360 Gen10",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Gold 6248",
                        "clockRateMhz": 2500,
                        "numberOfCores": 20,
                        "energyConsumptionW": 150
                    },
                    {
                        "name": "Intel Xeon Gold 6248",
                        "clockRateMhz": 2500,
                        "numberOfCores": 20,
                        "energyConsumptionW": 150
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung RDIMM M393A4K40CB2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 32768,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# HPE DL380 G10
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 38010,
    "name" : "HPE DL380 Gen10",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "HPE DL380 Gen10",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Platinum 8280M",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280M",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 1.92TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 1920000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'

# HPE DL580 G10
$MONGO_CMD --eval 'db.prefabs.insertOne({
    "_id" : 58010,
    "name" : "HPE DL580 Gen10",
    "tags" : ["hpc"],
    "visibility" : "public",
    "rack" : {
        "name": "HPE DL580 Gen10",
        "capacity": "42",
        "powerCapacityW": "25000",
        "machines" : [
            {
                "position": 1,
                "cpus": [
                    {
                        "name": "Intel Xeon Platinum 8280M",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280M",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280M",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    },
                    {
                        "name": "Intel Xeon Platinum 8280M",
                        "clockRateMhz": 2700,
                        "numberOfCores": 28,
                        "energyConsumptionW": 205
                    }
                ],
                "gpus": [],
                "memories": [
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "Samsung LRDIMM M386AAG40MM2-CVF",
                        "speedMbPerS": 46928,
                        "sizeMb": 131072,
                        "energyConsumptionW": 10
                    }
                ],
                "storages": [
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    },
                    {
                        "name": "HPE 3.84TB SSD SATA",
                        "speedMbPerS": 600,
                        "sizeMb": 3840000,
                        "energyConsumptionW": 10
                    }
                ],
            }
        ]
    }
});'
