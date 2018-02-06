import os
import sys

import mysql.connector as mariadb


class Job:
    def __init__(self, gwf_id):
        self.gwf_id = gwf_id
        self.db_id = -1
        self.tasks = []


class Task:
    def __init__(self, gwf_id, job, submit_time, run_time, num_processors, dependency_gwf_ids):
        self.gwf_id = gwf_id
        self.job = job
        self.submit_time = submit_time
        self.run_time = run_time
        self.flops = 10 ** 9 * run_time * num_processors
        self.dependency_gwf_ids = dependency_gwf_ids
        self.db_id = -1
        self.dependencies = []


def get_jobs_from_gwf_file(file_name):
    jobs = {}
    tasks = {}

    with open(file_name, "r") as f:
        # Skip first CSV header line
        f.readline()

        for line in f:
            if line.startswith("#") or len(line.strip()) == 0:
                continue

            values = [col.strip() for col in line.split(",")]
            cast_values = [int(values[i]) for i in range(len(values) - 1)]
            job_id, task_id, submit_time, run_time, num_processors, req_num_processors = cast_values
            dependency_gwf_ids = [int(val) for val in values[-1].split(" ") if val != ""]

            if job_id not in jobs:
                jobs[job_id] = Job(job_id)

            new_task = Task(task_id, jobs[job_id], submit_time, run_time, num_processors, dependency_gwf_ids)
            tasks[task_id] = new_task
            jobs[job_id].tasks.append(new_task)

    for task in tasks.values():
        for dependency_gwf_id in task.dependency_gwf_ids:
            if dependency_gwf_id in tasks:
                task.dependencies.append(tasks[dependency_gwf_id])

    return jobs.values()


def write_to_db(trace_name, jobs):
    conn = mariadb.connect(user='opendc', password='opendcpassword', database='opendc')
    cursor = conn.cursor()

    trace_id = execute_insert_query(conn, cursor, "INSERT INTO traces (name) VALUES ('%s')" % trace_name)

    for job in jobs:
        job.db_id = execute_insert_query(conn, cursor, "INSERT INTO jobs (name, trace_id) VALUES ('%s',%d)"
                                         % ("Job %d" % job.gwf_id, trace_id))

        for task in job.tasks:
            task.db_id = execute_insert_query(conn, cursor, "INSERT INTO tasks (start_tick, total_flop_count, job_id, "
                                                            "parallelizability) VALUES (%d,%d,%d,'SEQUENTIAL')"
                                              % (task.submit_time, task.flops, job.db_id))

    for job in jobs:
        for task in job.tasks:
            for dependency in task.dependencies:
                execute_insert_query(conn, cursor, "INSERT INTO task_dependencies (first_task_id, second_task_id) "
                                                   "VALUES (%d,%d)"
                                     % (dependency.db_id, task.db_id))

    conn.close()


def execute_insert_query(conn, cursor, sql):
    try:
        cursor.execute(sql)
    except mariadb.Error as error:
        print("SQL Error: {}".format(error))

    conn.commit()
    return cursor.lastrowid


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("Usage: %s trace-name" % sys.argv[0])

    gwf_trace_name = sys.argv[1]
    gwf_jobs = get_jobs_from_gwf_file(os.path.join("traces", gwf_trace_name + ".gwf"))
    write_to_db(gwf_trace_name, gwf_jobs)
