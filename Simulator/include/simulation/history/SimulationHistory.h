#pragma once
#include "History.h"
#include "WorkloadSnapshot.h"
#include "MachineSnapshot.h"

#include <unordered_map>

namespace Simulation
{
	using WorkloadHistory = History<WorkloadSnapshot>;
	using MachineHistory = History<MachineSnapshot>;
	using HistoryRef = std::tuple<std::reference_wrapper<WorkloadHistory>, std::reference_wrapper<MachineHistory>>;

	class SimulationHistory
	{
	public:
		/*
			Adds the workload snapshot at the given tick.
		*/
		void addSnapshot(uint32_t tick, WorkloadSnapshot snapshots)
		{
			workloadHistory.addSnapshotAtTick(tick, snapshots);
		}
		
		/*
			Adds the machine snapshot at the given tick.
		*/
		void addSnapshot(uint32_t tick, MachineSnapshot snapshots)
		{
			machineHistory.addSnapshotAtTick(tick, snapshots);
		}

		/*
			Returns the equal_range of the workload snapshots at the given tick.
		*/
		auto getWorkloadSnapshot(uint32_t tick)
		{
			return workloadHistory.snapshotsAtTick(tick);
		}

		/*
			Returns the equal_range of the machine snapshots at the given tick.
		*/
		auto getMachineSnapshot(uint32_t tick)
		{
			return machineHistory.snapshotsAtTick(tick);
		}

		/*
			Returns a const tuple ref of the entire cached history of machines and workloads.
		*/
		const HistoryRef getHistory()
		{
			return std::make_tuple(
				std::ref(workloadHistory),
				std::ref(machineHistory)
			);
		}

		/*
			Clears the cache of history.
		*/
		void clearHistory()
		{
			workloadHistory.clear();
			machineHistory.clear();
		}

		/*
			Returns the number of snapshots that are in the history cache.
		*/
		size_t historySize()
		{
			return workloadHistory.size();
		}

	private:
		WorkloadHistory workloadHistory;
		MachineHistory machineHistory;
	};
}