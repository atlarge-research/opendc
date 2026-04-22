-- Seed workload traces available for simulation.
-- Add new traces here as they become available.
INSERT INTO trace (id, name, type) VALUES ('bitbrains-small', 'Bitbrains Small', 'small') ON CONFLICT (id) DO NOTHING;
INSERT INTO trace (id, name, type) VALUES ('bitbrains-full', 'Bitbrains Full', 'bitbrains') ON CONFLICT (id) DO NOTHING;
INSERT INTO trace (id, name, type) VALUES ('bitbrains-full-delayed', 'Bitbrains Full (Delayed)', 'bitbrains') ON CONFLICT (id) DO NOTHING;
INSERT INTO trace (id, name, type) VALUES ('solvinity-2025', 'Solvinity 2025', 'solvinity') ON CONFLICT (id) DO NOTHING;
INSERT INTO trace (id, name, type) VALUES ('solvinity-2025-extended', 'Solvinity 2025 Extended', 'solvinity') ON CONFLICT (id) DO NOTHING;
INSERT INTO trace (id, name, type) VALUES ('solvinity-2025-extended-reduced-topology', 'Solvinity 2025 Extended (Reduced Topology)', 'solvinity') ON CONFLICT (id) DO NOTHING;
