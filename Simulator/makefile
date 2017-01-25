CXX=g++-6
CC=gcc-6

CFLAGS = -std=c++14 -Iinclude -lpthread -ldl

SOURCE=\
sqlite3.o\
src/Simulator.cpp\
src/database/Database.cpp \
src/simulation/workloads/Workload.cpp\
src/simulation/workloads/WorkloadPool.cpp\
src/modeling/machine/CPU.cpp\
src/modeling/machine/GPU.cpp\
src/modeling/machine/Machine.cpp\
src/modeling/Entity.cpp\
src/modeling/Rack.cpp

all: sqlite simulator

clean:
	rm -f simulator sqlite3.o

simulator:
	$(CXX) -o simulator $(SOURCE) $(CFLAGS)

sqlite: 
	$(CC) -o sqlite3.o -c ../opendc-simulator-dependencies/sqlite/sqlite3.c

.PHONY: all, clean