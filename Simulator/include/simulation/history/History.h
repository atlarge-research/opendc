#pragma once
#include <stdint.h>
#include <map>

namespace Simulation {
	template<typename Type>
	class History {
	public:
		void addSnapshotAtTick(uint32_t tick, Type snapshot)
		{
			history.insert(std::make_pair(tick, snapshot));
		}

		const auto& snapshotsAtTick(uint32_t tick)
		{
			return history.equal_range(tick);
		}

		typename std::unordered_map<uint32_t, Type>::const_iterator begin()
		{
			return history.begin();
		}

		typename std::unordered_map<uint32_t, Type>::const_iterator end()
		{
			return history.end();
		}

		void clear()
		{
			history.clear();
		}

		size_t size()
		{
			return history.size();
		}

	private:
		// Maps ticks to histories of workloads
		std::unordered_multimap<uint32_t, Type> history;
	};
}