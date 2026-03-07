# DT-based-Smart-Warehouse-for-SLAP

A **Digital Twin (DT)-based Smart Warehouse simulation and optimization framework** designed to address the **Storage Location Assignment Problem (SLAP)** in modern logistics systems.

This project explores how **Digital Twin technology** can be used to simulate warehouse operations and evaluate different storage allocation strategies to improve operational efficiency, reduce warehouse block usage and power consumption of warehouse equipment.

---

## Features

-  **Digital Twin Concept**  
  Simulates telemtry communication between physical warehouse and SLAP optimization module.

-  **SLAP Optimization**  
  Implements different strategies (conventional, Particle Swarm Optimization, Genetic Algorithm) to assign optimal storage locations for order of customer.

-  **Performance Evaluation**  
  Enables comparison of different storage allocation strategies based on metrics like:
    - travel distance between order of block and (0,0,0) location in a warehouse 
    - picking efficiency
    - block location management for efficient storage utilization

-  **Research-Oriented Framework**  
  Designed for experimentation with warehouse optimization algorithms.

---

##  Concept

Modern logistics systems require intelligent decision-making supported by real-time data.  

In this project:

The main objective is to improve warehouse efficiency by solving the **Storage Location Assignment Problem (SLAP)**.

---

##  Technologies

The project is built using technologies commonly used in simulation and optimization environments, including:

- Python  
- Data processing libraries  
- Particle Swarm Optimization, Genetic Algorithm
- Telemetry communication based simulation techniques  

---

## Project Structure
The project has three main sections. In DATA, there are two different input data for Case A and Case B which include the demands of customer orders. In SLAP, the Particle Swarm Optimization and Genetic Algorithm solutions are implemented in Java environment. In warehouse_DT_telemtry.py, the telemetry communication between physical warehouse and SLAP module in Digital Twin is simulated. 
```
DT-based-Smart-Warehouse-for-SLAP
│
├── DATA/
├── SLAP/
└── warehouse_DT_telemetry.py
```

---

## Installation

Clone the repository:

```bash
git clone https://github.com/yusufozcevik/DT-based-Smart-Warehouse-for-SLAP.git
cd DT-based-Smart-Warehouse-for-SLAP
```

## Usage

To run SLAP optimization module in Digital Twin, IntelliJ IDEA is preferred.

To run the telemtry based communication of Digital Twin simulation:
```bash
python3 warehouse_DT_telemtry.py
```

## License

This project is licensed under the MIT License.
