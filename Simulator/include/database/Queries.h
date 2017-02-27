#pragma once
#include "Query.h"

namespace Database
{
	namespace Queries
	{
		Query<int> GET_QUEUED_EXPERIMENTS(std::string(R"query(
			SELECT id FROM experiments WHERE state LIKE 'QUEUED';
		)query"));

		Query<> SET_EXPERIMENT_STATE_SIMULATING(std::string(R"query(
			UPDATE experiments SET state='SIMULATING' WHERE id=$id;		
		)query"));

		Query<> SET_EXPERIMENT_STATE_FINISHED(std::string(R"query(
			UPDATE experiments SET state='FINISHED' WHERE id=$id;		
		)query"));

		Query<int, int, int, int, std::string, std::string> GET_EXPERIMENT_BY_ID(std::string(R"query(
			SELECT id, simulation_id, path_id, trace_id, scheduler_name, name FROM experiments WHERE id = $id;
		)query"));

		Query<int, int, std::string, std::string> GET_PATH_BY_ID(std::string(R"query(
			SELECT id, simulation_id, name, datetime_created FROM paths WHERE id = $id;
		)query"));

		Query<int, int, int, int> GET_SECTION_BY_PATH_ID(std::string(R"query(
			SELECT id, path_id, datacenter_id, start_tick FROM sections WHERE path_id = $id;
		)query"));

		Query<> WRITE_EXPERIMENT_LAST_SIMULATED_TICK(std::string(R"query(
			UPDATE experiments SET last_simulated_tick = $val WHERE id = $id;
		)query"));
		
		/*
			Returns the type of the scheduler of the given simulation section.
			Returns: <std::string : scheduler_name>
			Binds: <int : id>
		*/
		Query<std::string> GET_SCHEDULER_TYPE_OF_EXPERIMENT(std::string(R"query(
			SELECT scheduler_name FROM experiments WHERE id = $id;
		)query"));

		/*
			Returns the id of the trace of the given simulation section.
			Returns: <int : trace_id>
			Binds: <int : id>
		*/
		Query<int> GET_TRACE_OF_EXPERIMENT(std::string(R"query(
			SELECT trace_id FROM experiments WHERE id = $id;
		)query"));

		/*
			Returns all columns of each room belonging to the given datacenter.
			Returns: <int : id, std::string : name, int : datacenter_id, std::string : type>
			Binds: <int : datacenter_id>
		*/
		Query<int, std::string, int, std::string> GET_ROOMS_OF_DATACENTER(std::string(R"query(
			SELECT * FROM rooms WHERE datacenter_id = $id;
		)query"));

		/*
			Returns all columns of each rack belonging to the given room.
			Returns: <int : id, std::string : name, int : capacity>
			Binds: <int : tile.room_id>
		*/
		Query<int, std::string, int> GET_RACKS_OF_ROOM(std::string(R"query(
			SELECT racks.* FROM tiles, objects, racks
			WHERE objects.id = tiles.object_id
			AND objects.id = racks.id
			AND tiles.room_id = $id;
		)query"));

		/*
			Returns the machine in a given rack.
			Returns: <int : id, int : position>
			Binds: <int : rack_id>
		*/
		Query<int, int> GET_MACHINES_OF_RACK(std::string(R"query(
			SELECT id, position FROM machines
			WHERE rack_id = $rid;
		)query"));

		/*
			Returns all columns of each task belonging to the given trace.
			Returns: <int : id, int : start_tick, inn : total_flop_count, int : trace_id, int : task_dependency_id>
			Binds: <int : trace_id>
		*/
		Query<int, int, int, int, int> GET_TASKS_OF_TRACE(std::string(R"query(
			SELECT * FROM tasks WHERE trace_id = $id;
		)query"));

		/*
			Returns the information of each cpu in the given rack, and their corresponding machine.
			Returns: <int : slot, int : machine_speed, int : cores, int : energy_consumption, int : failure_model_id>
			Binds: <int : machine.rack_id>
		*/
		Query<int, int, int, int, int> GET_CPUS_IN_RACK(std::string(R"query(
			SELECT machines.position AS slot, cpus.clock_rate_mhz AS machine_speed, cpus.number_of_cores AS cores, cpus.energy_consumption_w AS energy_consumption, cpus.failure_model_id AS failure_model_id FROM cpus, machine_cpus, machines
			WHERE machine_cpus.cpu_id = cpus.id
			AND machine_cpus.machine_id = machines.id
			AND machines.rack_id = $id;	
		)query"));

		/*
			Returns the information of each gpu in the given rack, and their corresponding machine.
			Returns: <int : slot, int : machine_speed, int : cores, int : energy_consumption, int : failure_model_id>
			Binds: <int : machine.rack_id>
		*/
		Query<int, int, int, int, int> GET_GPUS_IN_RACK(std::string(R"query(
			SELECT machines.position AS slot, gpus.clock_rate_mhz AS speed, gpus.number_of_cores AS cores,	gpus.energy_consumption_w AS energy_consumption, gpus.failure_model_id AS failure_model_id FROM gpus, machine_gpus, machines
			WHERE machine_gpus.gpu_id = gpus.id
			AND machine_gpus.machine_id = machines.id
			AND machines.rack_id = $id;	
		)query"));

		/*
			Inserts the state of a workload into the task_state table.
			Returns: <>
			Binds: <int : task_id, int : experiment_id, int : tick, int : flops_left, int : cores_used>
		*/
		Query<> WRITE_WORKLOAD_STATE(std::string(R"query(
			INSERT INTO task_states (task_id, experiment_id, tick, flops_left, cores_used)
			VALUES ($tid, $ssid, $tick, $flops, $cores_used);
		)query"));
	
		/*
			Inserts the state of a machine into the machine_state table.
			Returns: <>
			Binds: <int : task_id, int : machine_id, int : experiment_id, int : tick, float : temperature_c, int : in_use_memory_mb, float : load_fraction>
		*/
		Query<> WRITE_MACHINE_STATE(std::string(R"query(
			INSERT INTO machine_states (task_id, machine_id, experiment_id, tick, temperature_c, in_use_memory_mb, load_fraction)
			VALUES ($tid, $mid, $ssid, $tick, $temp, $mem, $load);
		)query"));
	}
}
