-- Seed nodes
INSERT INTO nodes (id, name, ip_address, mac_address, status, temperature, humidity, pressure, light, firmware_version, location)
VALUES
  (1, 'Node 1', '192.168.1.10', 'AA:BB:CC:01', 'ONLINE', 25.4, 60, 1012, 80, '1.0.0', 'Lab - East'),
  (2, 'Node 2', '192.168.1.11', 'AA:BB:CC:02', 'ONLINE', 27.1, 55, 1010, 70, '1.0.1', 'Lab - West'),
  (3, 'Node 3', '192.168.1.12', 'AA:BB:CC:03', 'OFFLINE', 0, 0, 0, 0, '0.9.8', 'Storage');

-- Seed a small rolling history for nodes 1 & 2 (last 15 minutes, 1/min)
-- H2 accepts ISO strings; we’ll generate a simple ladder of timestamps
-- Most recent first is handled in repository via ORDER BY
INSERT INTO readings (node_id, timestamp, temperature, humidity) VALUES
  (1, TIMESTAMP '2025-08-19 08:00:00', 25.4, 60),
  (1, TIMESTAMP '2025-08-19 08:01:00', 25.5, 60.5),
  (1, TIMESTAMP '2025-08-19 08:02:00', 25.6, 61),
  (1, TIMESTAMP '2025-08-19 08:03:00', 25.7, 61.2),
  (1, TIMESTAMP '2025-08-19 08:04:00', 25.8, 61.5),
  (1, TIMESTAMP '2025-08-19 08:05:00', 25.9, 61.7),
  (1, TIMESTAMP '2025-08-19 08:06:00', 26.0, 62),
  (1, TIMESTAMP '2025-08-19 08:07:00', 26.0, 62.1),
  (1, TIMESTAMP '2025-08-19 08:08:00', 26.1, 62.3),
  (1, TIMESTAMP '2025-08-19 08:09:00', 26.2, 62.5),

  (2, TIMESTAMP '2025-08-19 08:00:00', 27.1, 55),
  (2, TIMESTAMP '2025-08-19 08:01:00', 27.2, 55),
  (2, TIMESTAMP '2025-08-19 08:02:00', 27.2, 55.2),
  (2, TIMESTAMP '2025-08-19 08:03:00', 27.3, 55.3),
  (2, TIMESTAMP '2025-08-19 08:04:00', 27.4, 55.4),
  (2, TIMESTAMP '2025-08-19 08:05:00', 27.5, 55.5),
  (2, TIMESTAMP '2025-08-19 08:06:00', 27.6, 55.6),
  (2, TIMESTAMP '2025-08-19 08:07:00', 27.6, 55.6),
  (2, TIMESTAMP '2025-08-19 08:08:00', 27.7, 55.7),
  (2, TIMESTAMP '2025-08-19 08:09:00', 27.8, 55.8);
