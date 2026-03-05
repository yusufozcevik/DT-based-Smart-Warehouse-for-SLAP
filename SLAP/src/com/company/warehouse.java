package com.company;


public class warehouse {

    double cap_w;//capacity
    double s_w;//setup cost
    double remain_cap_w;//remained capacity

    warehouse(double cap_w, double s_w){
        this.cap_w=cap_w;
        this.s_w=s_w;
        this.remain_cap_w=cap_w;
    }

    public double getCap_w() {
        return cap_w;
    }

    public double getS_w() {
        return s_w;
    }
    public double getRemain_cap_w() {
        return remain_cap_w;
    }

    public void setRemain_cap_w(double remain_cap_w) {
        this.remain_cap_w = remain_cap_w;
    }
    public void refresh_remained_capacity(){
        remain_cap_w=cap_w;
    }

}
