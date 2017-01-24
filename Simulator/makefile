CC=g++
CFLAGS=-std=c++14 -Iinclude -ldl -lpthread
SOURCE=\
external/sqlite3.o\
src/Simulator.cpp\
src/database/Database.cpp \
src/simulation/workloads/Workload.cpp\
src/simulation/workloads/WorkloadPool.cpp\
src/modeling/machine/CPU.cpp\
src/modeling/machine/GPU.cpp\
src/modeling/machine/Machine.cpp\
src/modeling/Entity.cpp\
src/modeling/Rack.cpp


simulatormake:
	$(CC) -o simulator $(SOURCE) $(CFLAGS)
