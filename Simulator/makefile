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
	rm -rf simulator external/

simulator:
	$(CXX) -o simulator -Iexternal $(SOURCE) $(CFLAGS)

sqlite:
	cd ../external/sqlite ; \
	git rev-parse --git-dir >/dev/null || exit 1 ; \
	git log -1 --format=format:%ci%n | sed -e 's/ [-+].*$//;s/ /T/;s/^/D /' > manifest ; \
	echo $(git log -1 --format=format:%H) > manifest.uuid
	mkdir external
	cd external ; \
	./../../external/sqlite/configure ; \
	make sqlite3.o
	mv external/sqlite3.o sqlite3.o
	rm -rf external/

.PHONY: all, clean