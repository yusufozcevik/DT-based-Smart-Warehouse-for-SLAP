package com.company;

import java.util.LinkedList;

public class customer {

    double demand;//customer demand
    LinkedList<Double> distance_list=new LinkedList<Double>();//distances to warehouses

    customer(double demand){
        this.demand=demand;
    }

    public double getDemand() {
        return demand;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public LinkedList<Double> getDistance_list() {
        return distance_list;
    }
    public double getWarehousefromDistance_list(int warehouse){
        return distance_list.get(warehouse);
    }
    public void addDistance_list(double cost){
        distance_list.add(cost);
    }
}
