#pragma once

// Define a DefaultDC type, capable of holding server rooms.
using DefaultDatacenter = Modeling::Datacenter<Modeling::ServerRoom, Modeling::Hallway, Modeling::PowerRoom>;

// Define a type of simulation, capable of simulating the DefaultDC.
using DefaultSection = Simulation::Section<DefaultDatacenter>;