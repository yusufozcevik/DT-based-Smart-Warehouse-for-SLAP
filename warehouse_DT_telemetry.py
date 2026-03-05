import time
import json
import math
import threading

# Thread safety for console output
print_lock = threading.Lock()

class FleetUnit:
    def __init__(self, unit_id, v_type, battery_limit, vmax):
        self.unit_id = unit_id
        self.v_type = v_type
        self.position = {"x": 0.0, "y": 0.0, "z": 0.0}
        self.battery_limit = battery_limit 
        self.energy_consumed = 0.0
        self.vmax = vmax
        self.status = "IDLE"
        self.current_job = None
        self.total_distance = 0.0
        self.recharge_count = 0
        
        # Power Consumption (Watts)
        self.avg_power = 250.0 if v_type == "DRONE" else 3500.0

    def emit_telemetry(self):
        """Generates real-time telemetry including battery status and recharge events."""
        remaining_energy = self.battery_limit - self.energy_consumed
        battery_pct = max(0, (remaining_energy / self.battery_limit) * 100)
        
        telemetry = {
            "ts": time.strftime("%H:%M:%S"),
            "unit": self.unit_id,
            "type": self.v_type,
            "status": self.status,
            "job": self.current_job,
            "battery_pct": f"{battery_pct:.4f}%",
            "recharges": self.recharge_count,
            "pos": {
                "x": round(self.position['x'], 2),
                "y": round(self.position['y'], 2),
                "z": round(self.position['z'], 2)
            }
        }
        
        with print_lock:
            print(json.dumps(telemetry))

    def handle_recharge(self):
        """Triggers the recharge sequence if energy is depleted."""
        if self.energy_consumed >= self.battery_limit:
            self.status = "RECHARGING"
            self.recharge_count += 1
            # Emit telemetry to show the start of recharging
            self.emit_telemetry()
            
            # Simulated recharge time (3 seconds for the simulation)
            time.sleep(3) 
            
            self.energy_consumed = 0.0
            self.status = "ACTIVE"
            return True
        return False

    def move_to(self, target):
        # Check for recharge before moving
        if self.handle_recharge():
            return False

        dx = target['x'] - self.position['x']
        dy = target['y'] - self.position['y']
        dz = target['z'] - self.position['z']
        distance = math.sqrt(dx**2 + dy**2 + dz**2)
        
        if distance < 0.5:
            self.position = target.copy()
            return True
            
        dt = 0.15 
        step_size = min(distance, self.vmax * dt)
        ratio = step_size / distance
        
        self.position['x'] += dx * ratio
        self.position['y'] += dy * ratio
        self.position['z'] += dz * ratio
        self.total_distance += step_size
        
        # Energy consumption: J = W * s
        self.energy_consumed += (self.avg_power * dt)
        return False

def execute_mission(unit, orders):
    unit.status = "ACTIVE"
    for order_id in orders:
        unit.current_job = f"BLK_{order_id:03d}"
        
        target_coords = {
            "x": (order_id % 10) * 10.0,
            "y": (order_id // 10) * 10.0,
            "z": 8.0 if unit.v_type == "DRONE" else 0.0
        }
        
        # Task Cycle: Pickup -> Return
        for destination in [target_coords, {"x": 0.0, "y": 0.0, "z": 0.0}]:
            while not unit.move_to(destination):
                unit.emit_telemetry()
                time.sleep(0.005)
                
    unit.status = "COMPLETED"
    unit.current_job = "FINISH"
    unit.emit_telemetry()

# --- DATASET ---
full_route = [96, 59, 17, 25, 32, 65, 60, 10, 37, 77, 67, 99, 69, 66, 79, 89, 82, 40, 94, 34, 63, 97, 88, 18, 42, 46, 69, 78, 32, 66, 80, 9, 13, 78, 97, 97, 31, 85, 15, 6, 26, 77, 66, 93, 66, 73, 69, 74, 4, 49, 28, 61, 38, 30, 0, 83, 48, 93, 89, 81, 49, 10, 51, 30, 10, 31, 57, 98, 89, 29, 39, 32, 3, 16, 13, 11, 30, 6, 82, 80, 6, 11, 50, 40, 15, 75, 97, 74, 13, 76, 12, 45, 61, 73, 23, 54, 64, 7, 33, 28, 10, 22, 32, 54, 28, 51, 99, 37, 83, 89, 47, 78, 3, 80, 6, 4, 68, 20, 65, 58, 91, 36, 90, 25, 31, 77, 17, 52, 0, 50, 33, 92, 76, 61, 55, 88, 56, 73, 80, 44, 13, 53, 23, 14, 87, 60, 47, 68, 91, 0, 71, 17, 74, 93, 46, 53, 35, 67, 87, 15, 2, 32, 87, 51, 22, 97, 42, 84, 10, 69, 75, 72, 21, 79, 13, 6, 49, 35, 14, 79, 71, 3, 72, 76, 1, 0, 63, 8, 1, 8, 74, 78, 68, 0, 71, 73, 73, 49, 9, 44, 0, 70, 78, 89, 65, 12, 95, 74, 77, 61, 49, 59, 92, 62, 81, 18, 72, 95, 47, 17, 17, 84, 24, 0, 10, 31, 10, 99, 82, 29, 79, 2, 27, 76, 61, 11, 0, 83, 51, 40, 88, 62, 86, 88, 6, 47, 72, 81, 60, 85, 39, 16, 21, 46, 27, 62, 73, 0, 95, 7, 96, 89, 27, 83, 63, 84, 53, 65, 59, 23, 24, 13, 35, 77, 43, 33, 42, 2, 19, 59, 98, 5, 92, 29, 81, 0, 84, 84, 62, 57, 60, 3, 50, 79, 28, 81, 45, 85, 76, 66, 89, 37, 2, 15, 50, 62, 36, 85, 89, 57, 66, 12, 49, 84, 93, 50, 55, 21, 68, 97, 88, 7, 13, 40, 39, 89, 81, 17, 37, 46, 32, 18, 56, 17, 86, 95, 60, 13, 32, 17, 22, 23, 83, 55, 11, 91, 44, 93, 56, 30, 35, 49, 87, 83, 31, 69, 12, 28, 80, 83, 80, 40, 30, 54, 33, 6, 12, 35, 55, 30, 43, 4, 8, 8, 64, 27, 71, 91, 6, 58, 89, 67, 40, 0, 74, 29, 51, 50, 57, 71, 88, 78, 0, 27, 74, 30, 71, 85, 20, 77, 58, 33, 2, 15, 77, 95, 90, 63, 25, 94, 51, 85, 81, 48, 53, 7, 15, 2, 35, 93, 90, 93, 70, 4, 20, 63, 11, 29, 70, 30, 20, 22, 33, 32, 58, 2, 24, 24, 48, 15, 1, 5, 17, 78, 41, 47, 36, 92, 34, 24, 28, 92, 14, 8, 90, 5, 28, 95, 35, 72, 9, 4, 32, 24, 71, 77, 11, 59, 39, 21, 79, 72, 43, 37, 86, 21, 12, 85, 50, 64, 86, 69, 23, 27, 55, 72, 59, 65, 12, 48, 45, 64, 38, 61, 93, 46, 89, 51, 52, 79, 30, 45, 42, 47, 70, 16, 40, 78, 6, 81, 42, 32, 23, 80, 14, 7, 4, 70, 8, 69, 91, 10, 95, 48, 10, 87, 45, 65, 48, 10, 91, 83, 7, 54, 74, 41, 98, 52, 44, 35, 98, 10, 45, 97, 4, 89, 7, 81, 84, 95, 91, 18, 79, 92, 7, 81, 94, 96, 18, 97, 77, 55, 36, 60, 54, 72, 89, 68, 22, 58, 48, 62, 15, 4, 83, 15, 69, 84, 39, 94, 70, 59, 62, 56, 73, 8, 56, 72, 0, 43, 8, 88, 15, 6, 7, 19, 64, 23, 75, 1, 99, 81, 14, 62, 1, 49, 71, 62, 25, 94, 30, 12, 16, 1, 96, 68, 35, 14, 12, 34, 23, 96, 35, 88, 73, 78, 91, 54, 7, 21, 65, 10, 86, 3, 28, 68, 34, 71, 61, 1, 20, 83, 92, 37, 1, 75, 47, 17, 99, 71, 85, 35, 66, 43, 94, 47, 72, 51, 4, 19, 81, 18, 76, 42, 61, 90, 96, 84, 53, 0, 1, 2, 12, 84, 54, 91, 59, 60, 53, 95, 93, 65, 55, 44, 23, 94, 79, 4, 92, 97, 16, 73, 4, 16, 20, 60, 92, 36, 7, 81, 86, 50, 10, 80, 21, 17, 23, 85, 60, 62, 86, 44, 83, 2, 17, 17, 81, 68, 15, 22, 51, 11, 43, 1, 1, 29, 96, 78, 14, 16, 53, 65, 71, 78, 22, 16, 90, 90, 30, 20, 1, 51, 38, 80, 44, 7, 43, 75, 65, 61, 81, 80, 64, 94, 67, 37, 65, 13, 88, 30, 55, 11, 37, 0, 25, 90, 28, 79, 89, 50, 62, 8, 45, 49, 3, 85, 49, 27, 94, 3, 30, 17, 39, 55, 52, 92, 38, 27, 56, 45, 27, 57, 34, 36, 35, 2, 8, 70, 63, 79, 74, 99, 52, 45, 68, 37, 35, 17, 77, 58, 5, 61, 41, 14, 69, 23, 37, 58, 35, 97, 6, 60, 22, 50, 27, 12, 66, 79, 1, 75, 23, 28, 79, 95, 88, 71, 54, 74, 44, 19, 5, 31, 50, 59, 58, 86, 97, 50, 35, 15, 36, 48, 71, 0, 93, 65, 71, 19, 92, 6, 86, 14, 6, 23, 29, 32, 35, 35, 87, 64, 56, 17, 50, 44, 73, 50, 93, 49, 34, 12, 10, 10, 15, 17, 85, 18, 92, 23, 45, 7, 72, 13, 92, 18, 9, 41, 37, 84, 11, 88, 83, 90, 87, 5, 6, 59, 92, 38, 56, 14, 91, 94, 28, 56, 73, 23, 36, 15, 96, 89, 49, 12, 27, 38, 53, 95, 52, 15, 28, 20, 67, 37, 11, 12, 21, 87, 81, 37, 77, 95, 62, 31, 12, 21, 69, 37, 80, 57, 10, 63, 10, 17, 4, 93, 14, 79, 69, 95, 30, 13, 89, 20, 39, 3, 29, 44, 41, 13, 73, 52, 10, 6, 70, 33, 93, 79, 31, 61, 90, 36, 77, 47, 2, 24, 25, 50, 40, 97, 8, 1, 13, 37, 46, 77, 64, 5, 45, 57, 12, 89]

drone_orders = [o for o in full_route if o >= 50]
forklift_orders = [o for o in full_route if o < 50]

# Initializing units
drone_unit = FleetUnit("ACE-DRONE-X", "DRONE", battery_limit=200000, vmax=6.5)
forklift_unit = FleetUnit("TITAN-FORK-Y", "FORKLIFT", battery_limit=70000000, vmax=2.8)

# Start Concurrent Missions
t1 = threading.Thread(target=execute_mission, args=(drone_unit, drone_orders))
t2 = threading.Thread(target=execute_mission, args=(forklift_unit, forklift_orders))

print("--- STARTING INDUSTRIAL MISSION WITH RECHARGE TELEMETRY ---")
t1.start(); t2.start()
t1.join(); t2.join()

print("\nMISSION COMPLETE.")
