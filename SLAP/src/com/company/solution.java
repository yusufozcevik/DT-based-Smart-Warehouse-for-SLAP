package com.company;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class solution implements Runnable {
    int index;//for each thread
    int list_index;//for each thread

    static Scanner sc2;

    static LinkedList<LinkedList<Integer>> pBest_positions = new LinkedList<>();
    static LinkedList<Double> pBest_costs = new LinkedList<>();
    static LinkedList<LinkedList<Double>> velocity = new LinkedList<>();
    static LinkedList<Integer> gBest_position = new LinkedList<>();
    static double gBest_cost = Double.MAX_VALUE;

    static boolean forklift_only=false;
    static boolean drone_only=false;
    static boolean forklift_drone=true;

    static int N=0;//number_of_warehouse
    static int M=0;//number_of_customer
    static int vmax,vmaxd,vmaxf=0;
    static double Phover,Plifting,Ptr,Ptrd,Ptrf=0.0;
    static int ForkliftBatteryTime=48000;//8hours
    static int DroneBatteryTime=1800;//30min
    static LinkedList<warehouse> warehouse_list=new LinkedList<warehouse>();
    static LinkedList<customer> customer_list=new LinkedList<customer>();
    static int chromozome_number;//number of chromozome in chromozome pool
    static LinkedList<LinkedList<Integer>> chromozome=new LinkedList<LinkedList<Integer>>();//chromozome_number * M dimensional pool
    //M is length of chromozome
    static LinkedList<Double> chromozome_costs=new LinkedList<Double>();
    static LinkedList<Double> chromozome_times=new LinkedList<Double>();
    static LinkedList<Double> chromozome_replacement=new LinkedList<Double>();
    static LinkedList<LinkedList<Double>> chromozome_BlockUtilization= new LinkedList<LinkedList<Double>>();
    static LinkedList<Integer> mating_pool=new LinkedList<Integer>();//Keep only chromozome index in chromozome_list
    static LinkedList<LinkedList<Integer>> new_generation=new LinkedList<LinkedList<Integer>>();
    static LinkedList<Double>  new_generation_costs=new LinkedList<Double>();
    static LinkedList<Double>  new_generation_times=new LinkedList<Double>();
    static LinkedList<Double>  new_generation_replacement=new LinkedList<Double>();
    static LinkedList<LinkedList<Double>>  new_generation_BlockUtilization=new LinkedList<LinkedList<Double>>();
    static Semaphore sem = new Semaphore(1);

    static final Random generator = new Random();

    public static void main(String[] args)throws IOException, ParseException{
        long rgenseed = System.currentTimeMillis();
        //long rgenseed = 1462550264496L;
        generator.setSeed(rgenseed);

        FileWriter wc2 = new FileWriter("output.txt",true);//to keep random seed values
        BufferedWriter bw = new BufferedWriter(wc2);
        bw.write("s:"+rgenseed+" result:");


        int item=0;
        sc2 = new Scanner(new File(args[0]));

        while (sc2.hasNextLine()) {


            if(item==0){
                Scanner s2 = new Scanner(sc2.nextLine());
                String s = s2.next();
                N=Integer.parseInt(s);

                s=s2.next();
                M=Integer.parseInt(s);
                if(drone_only) vmax=4; //4 m/s for drone,
                if(forklift_only) vmax=3; //3 m/s for forklift
                if(forklift_drone) {vmaxd=4;vmaxf=3;}
                double a=0.5*9.807*0.5*9.807*0.5*9.807;
                double b=4*2*3*(0.02)*0.02*1.225;
                Phover=Math.sqrt(a*150000/b);//for drone
                Plifting=4;//m*g*v joule/sn //for forklift

                chromozome_number=50;


            }else if(item<(N+1)){
                Scanner s2 = new Scanner(sc2.nextLine());
                String s = s2.next();

                Double cap=Double.parseDouble(s);

                s = s2.next();

                Double setup=Double.parseDouble(s);

                warehouse w=new warehouse(cap,setup);
                warehouse_list.add(w);


            }else{
                Scanner s2 = new Scanner(sc2.nextLine());
                String s = s2.next();

                double demand=Double.parseDouble(s);
                customer c=new customer(demand);
                customer_list.add(c);

                s2 = new Scanner(sc2.nextLine());
                for(int i=0;i<N;i++){
                    s = s2.next();

                    double cost=Double.parseDouble(s);
                    c.addDistance_list(cost/1000);

                }

                item++;
            }
            item++;
        }
        long tStart=System.currentTimeMillis();
        //run_conventional();
        run_genetic();
        //run_pso();


        long tEnd=System.currentTimeMillis();
        System.out.print("\nTime:");
        System.out.print(tEnd-tStart);

    }

    public static void run_conventional() {
        initialize_pool();
        calculate_costs(1,false);//chromozome
        int best=find_best(chromozome,chromozome_costs);
        get_feasible(best,chromozome);
        LinkedList<Double> t=calculate_chromozome_cost(chromozome.get(best));
        chromozome_costs.set(best,t.get(0));//chromozome
        chromozome_times.set(best,t.get(1));//chromozome
        chromozome_replacement.set(best, t.get(2));//chromozome

        for(int i=0;i<N;i++){
            chromozome_BlockUtilization.get(best).set(i,t.get(i+3));
        }
        print_best_result(best);
    }
    public static void run_pso() {
        initialize_pso();
        calculate_costs(1, true);
        update_best_values();

        for (int iteration = 0; iteration < 10; iteration++) {
            update_particles();
            calculate_costs(1, true);
            update_best_values();
            System.out.println("Iteration: " + iteration + " Best Cost: " + gBest_cost);
        }

        print_gBest_result();
    }
    public static void print_gBest_result() {
        System.out.println("\n--- PSO GLOBAL BEST RESULT ---");

        LinkedList<Double> metrics = calculate_chromozome_cost(gBest_position);

        double energy = metrics.get(0);
        double time = metrics.get(1);
        double replacement = metrics.get(2);

        System.out.printf("Best Energy (Cost): %.2f\n", energy);
        System.out.printf("Total Time: %.2f mins\n", time);
        System.out.printf("Battery Replacements: %.0f\n", replacement);

        System.out.println("Customer Assignments (Warehouse IDs):");
        for (int j = 0; j < M; j++) {
            System.out.print(gBest_position.get(j) + " ");
        }

        System.out.println("\nWarehouse Block Utilization:");
        for (int i = 0; i < N; i++) {
            System.out.printf("%.1f ", metrics.get(3 + i));
        }
        System.out.println("\n------------------------------");
    }
    public static void initialize_pso() {
        initialize_pool();

        for (int i = 0; i < chromozome_number; i++) {

            LinkedList<Double> vRow = new LinkedList<>();
            for (int j = 0; j < M; j++) {
                vRow.add(generator.nextDouble());
            }
            velocity.add(vRow);

            pBest_positions.add(new LinkedList<>(chromozome.get(i)));
            pBest_costs.add(Double.MAX_VALUE);
        }
    }
    public static void update_particles() {
        double w = 0.7;  // (Inertia weight)
        double c1 = 1.5; //(Cognitive)
        double c2 = 1.5; // (Social)

        for (int i = 0; i < chromozome_number; i++) {
            LinkedList<Integer> currentPos = chromozome.get(i);
            LinkedList<Integer> pBest = pBest_positions.get(i);
            LinkedList<Double> currentVel = velocity.get(i);

            for (int j = 0; j < M; j++) {
                double r1 = generator.nextDouble();
                double r2 = generator.nextDouble();

                double newVel = (w * currentVel.get(j)) +
                        (c1 * r1 * (pBest.get(j) - currentPos.get(j))) +
                        (c2 * r2 * (gBest_position.get(j) - currentPos.get(j)));

                currentVel.set(j, newVel);

                if (generator.nextDouble() < sigmoid(newVel)) {
                    if (generator.nextDouble() < 0.5) {
                        currentPos.set(j, gBest_position.get(j));
                    } else {
                        currentPos.set(j, pBest.get(j));
                    }
                }
            }
            get_feasible(i, chromozome);
        }
    }
    public static void update_best_values() {
        for (int i = 0; i < chromozome_number; i++) {
            double currentCost = chromozome_costs.get(i);

            // pBest Güncelleme
            if (currentCost < pBest_costs.get(i)) {
                pBest_costs.set(i, currentCost);
                pBest_positions.set(i, new LinkedList<>(chromozome.get(i)));
            }

            // gBest Güncelleme
            if (currentCost < gBest_cost) {
                gBest_cost = currentCost;
                gBest_position = new LinkedList<>(chromozome.get(i));
            }
        }
    }

    private static double sigmoid(double v) {
        return 1 / (1 + Math.exp(-v));
    }
    public static void run_genetic() {
        initialize_pool();
        calculate_costs(1,false);//chromozome

        for(int iteration=0;iteration<10000;iteration++){
            ParentSelection(); //order-based, ranking
            Recombination();//Cross-over
            Mutation(new_generation);//one point p_m
            for(int i=0;i<chromozome_number;i++)
                get_feasible(i,new_generation);
            calculate_costs(2,false);//chrozome
            Survivor_Selection();
            System.out.print("i:"+iteration+"\n");
            print_pool(chromozome,chromozome_costs, chromozome_times, chromozome_replacement,chromozome_BlockUtilization);
        }
        int best=find_best(chromozome,chromozome_costs);
        get_feasible(best,chromozome);
        LinkedList<Double> t=calculate_chromozome_cost(chromozome.get(best));
        chromozome_costs.set(best,t.get(0));//chromozome
        chromozome_times.set(best,t.get(1));//chromozome
        chromozome_replacement.set(best, t.get(2));//chromozome

        for(int i=0;i<N;i++){
            chromozome_BlockUtilization.get(best).set(i,t.get(i+3));
        }

        print_best_result(best);
    }
    public static void initialize_pool(){

        for(int i=0;i<chromozome_number;i++){
            LinkedList<Integer> temp=new LinkedList<Integer>();//new generation initialization
            for(int j=0;j<M;j++){
                int w=new Integer(0);
                temp.add(w);
            }
            new_generation.add(temp);
            Double t=new Double(0);
            Double t1=new Double(0);
            Double t2=new Double(0);

            new_generation_costs.add(t);//new_generation_costs initialization
            new_generation_times.add(t1);//new_generation_costs initialization
            new_generation_replacement.add(t2);//new_generation_costs initialization
            LinkedList<Double> t3=new LinkedList<Double>();
            for(int k=0;k<N;k++) {
                Double t4=new Double(0);
                t3.add(t4);
            }
            new_generation_BlockUtilization.add(t3);

            temp=new LinkedList<Integer>();//chromozome initialization

            for(int j=0;j<M;j++){
                int w=new Integer(0);
                temp.add(w);
            }
            Double ct=new Double(0);
            Double ct1=new Double(0);
            Double ct2=new Double(0);

            chromozome_costs.add(ct);//chromozome_costs initialization
            chromozome_times.add(ct1);//chromozome_costs initialization
            chromozome_replacement.add(ct2);//chromozome_costs initialization
            LinkedList<Double> ct5=new LinkedList<Double>();
            for(int k=0;k<N;k++) {
                double ct6=new Double(0);
                ct5.add(ct6);
            }
            chromozome_BlockUtilization.add(ct5);
            for(int j=0;j<M;j++){// customer j to w
                int w= generator.nextInt(N);
                boolean assigned=false;

                double demand=customer_list.get(j).getDemand();

                while(!assigned){

                    double remained_capacity=warehouse_list.get(w).getRemain_cap_w();

                    if(demand<=remained_capacity){
                        temp.set(j,w);//index,value, assign j to w
                        remained_capacity=remained_capacity-demand;
                        warehouse_list.get(w).setRemain_cap_w(remained_capacity);//capacity is updated
                        assigned=true;

                    }else{
                        w= generator.nextInt(N);
                    }
                }

            }
            chromozome.add(temp);

            for(int w=0;w<N;w++)
                warehouse_list.get(w).refresh_remained_capacity();
        }
    }

    public static void print_pool(LinkedList<LinkedList<Integer>> temp,LinkedList<Double> temp_costs, LinkedList<Double> temp_times, LinkedList<Double> temp_replacement,LinkedList<LinkedList<Double>> temp_BlockUtilization  ){

        for(int i=0;i<chromozome_number;i++){
            System.out.print("chromozome"+i+": ");
            for(int j=0;j<M;j++){
                System.out.print(temp.get(i).get(j)+" ");
            }
            System.out.print("cost:"+temp_costs.get(i)+" time:"+temp_times.get(i)+" replacement:"+ temp_replacement.get(i));
            System.out.print("\nBlockUtilization: ");

            System.out.print(temp_BlockUtilization.get(i));
            System.out.print("\n");
        }
    }
    public static void add_penalty(int list_index){

        for(int i=0;i<chromozome_number;i++){

            for(int w=0;w<N;w++){
                warehouse_list.get(w).refresh_remained_capacity();
                double remained_capacity=warehouse_list.get(w).getRemain_cap_w();

                for(int j=0;j<M;j++){
                    int w2;
                    if(list_index==1)
                        w2=chromozome.get(i).get(j);
                    else w2=new_generation.get(i).get(j);

                    if(w==w2){
                        double demand=customer_list.get(j).getDemand();

                        remained_capacity=remained_capacity-demand;

                    }
                }

                double cost;
                if(list_index==1)
                    cost=chromozome_costs.get(i);
                else cost=new_generation_costs.get(i);

                if(remained_capacity<0){

                    cost=cost+(0-remained_capacity);

                }
                if(list_index==1)
                    chromozome_costs.set(i,cost);// set chromozome i with cost
                else new_generation_costs.set(i,cost);// set chromozome i with cost

            }

        }
    }
    public static void calculate_costs(int list_index,boolean penalty_exist){
        ExecutorService executor=Executors.newFixedThreadPool(10);

        for(int i=0;i<chromozome_number;i++){
            Runnable x=new solution(i,list_index);
            executor.execute(x);
        }
        executor.shutdown();
        while (!executor.isTerminated());


        if(penalty_exist)
            add_penalty(list_index);//add penalty to infeasible solutions

    }
    public static LinkedList<Double> calculate_chromozome_cost(LinkedList<Integer> temp_chromozome){

        double cost=new Double(0);
        double costd=new Double(0);
        double costf=new Double(0);
        double energy=new Double(0);
        double v,vd,vf=new Double(vmax);
        LinkedList<Double> BlockUtilization= new LinkedList<Double>();

        for(int i=0;i<N;i++) {
            Double t = new Double(0);
            BlockUtilization.add(t);
        }

        for(int j=0;j<M;j++){
            int w=temp_chromozome.get(j);
            double utilization=BlockUtilization.get(w)+1.0;
            BlockUtilization.set(w,utilization);

            double distance_cost=customer_list.get(j).getDistance_list().get(w);//distance cost between customer j to warehouse w
            //System.out.print(distance_cost+" ");
            Double k = BlockUtilization.get(w);
            if(forklift_only)
                if(k>=5)
                    cost = cost + distance_cost*100;//distance cost is added for forklift limit
            if(drone_only)
                if(k>=5)
                    cost = cost + distance_cost*(k-5); //height cost is added for drone

            int M=generator.nextInt(k.intValue());
            if(forklift_drone) {
                if (M >= 5) {
                    costf = costf + distance_cost*M*10;//distance cost is added for forklift limit
                    costd = costd + distance_cost * (M - 5); //height cost is added for drone
                }

            }

        }

        for (int i=0;i<N;i++) {
            Double k = BlockUtilization.get(i);

             if(drone_only) cost = cost + k*45*5; //for drone, hovering duration is added.
             if(forklift_only) cost= cost + k*90*5;//for forklift, lifting duration is added.
            if(forklift_drone){
                costd= costd + k*45*5; //for drone, hovering duration is added.
                costf= costf + k*90*5;//for forklift, lifting duration is added.
            }
        }
        double time=0.0;
        double timed=0.0;
        double timef=0.0;
        if(forklift_drone){
            timed=costd/vmaxd;
            timef=costf/vmaxf;
            vd=costd/timed;
            vf=costf/timef;
            Ptrd=5/vmaxd*vd;
            Ptrf=5/vmaxf*vf;
        }else{
        time=cost/vmax;
        v=cost/time;
        Ptr=5/vmax*v;
        }


        double replacement=0.0;
        double replacementd=0.0;
        double replacementf=0.0;
        if(forklift_only)
            replacement=Math.floor(time/ForkliftBatteryTime);//Forklift 8 hours
        if(drone_only)
            replacement=Math.floor(time/DroneBatteryTime); //Drone 30min
        if(forklift_drone){
            replacementd=Math.floor(timed/DroneBatteryTime);
            replacementf=Math.floor(timef/ForkliftBatteryTime);
        }
        if(replacement>0 && drone_only ) time=time+replacement*10*60;//recharging 10 mins for drone
        if(replacement>0 && forklift_only )time=time+replacement*120*60;//recharhing 120 mins for forklift
        if(forklift_drone ) {
            if(replacementd>0) timed=timed+replacementd*10*60;//recharging 10 mins for drone
            if(replacementf>0) timef=timef+replacementf*120*60;//recharhing 120 mins for forklift
        }

        if(drone_only) energy=time*(Phover+Ptr);//watt, joule/sn for drone
        if(forklift_only) energy=time*(Plifting+Ptr);//for forklift
        if(forklift_drone) energy= timef*(Plifting+Ptrf) + timed*(Phover+Ptrd);

        energy=energy/60;//joule/mins

        LinkedList<Double> result= new LinkedList<>();
        result.add(0,energy);
        if(forklift_drone) {
            result.add(1,(timed+timef)/60);
            result.add(2,(replacementd+replacementf));
        }
        else {
            result.add(1,time/60);
            result.add(2,replacement);
        }


        for(int i=0;i<N;i++)
            result.add(3+i,BlockUtilization.get(i));
        return result;

    }
    public static void sort_population(LinkedList<LinkedList<Integer>> temp,LinkedList<Double> temp_costs){
        //Insertion sort
        for(int i=1;i<chromozome_number;i++){
            double cost=temp_costs.get(i);
            int j;
            for(j=i-1;j>=0 && cost<temp_costs.get(j);j--);

            Double t=new Double(cost);
            temp_costs.remove(i);
            temp_costs.add(j+1,t);

            LinkedList<Integer> temp_chromozome=new LinkedList<Integer>();
            for(int k=0;k<M;k++){
                int number=new Integer(temp.get(i).get(k));
                temp_chromozome.add(k,number);
            }
            temp.remove(i);
            temp.add(j+1,temp_chromozome);
        }

    }
    public static void ParentSelection(){//order-based ranking

        sort_population(chromozome,chromozome_costs);
        for(int i=0;i<chromozome_number;i++){
            mating_pool.add(i,i);
        }

    }
    public static void Recombination(){//crossover (uniform, 2 point)

        for(int i=0;i<chromozome_number-1;i=i+2){//uniform cross-over

            for(int j=0;j<M;j++){

                int probability=generator.nextInt(2);
                if(probability>0){//take from first parent
                    new_generation.get(i).set(j,chromozome.get(mating_pool.get(i)).get(j));
                    new_generation.get(i+1).set(j,chromozome.get(mating_pool.get(i+1)).get(j));
                }else{//take from second parent
                    new_generation.get(i).set(j,chromozome.get(mating_pool.get(i+1)).get(j));
                    new_generation.get(i+1).set(j,chromozome.get(mating_pool.get(i)).get(j));
                }
            }
        }
        if(chromozome_number%2==1)
            new_generation.get(chromozome_number-1).set(M-1,chromozome.get(mating_pool.get(chromozome_number-1)).get(M-1));


    }
    public static void get_feasible(int best,LinkedList<LinkedList<Integer>> temp){

        int i=best;

        for(int w=0;w<N;w++)
            warehouse_list.get(w).refresh_remained_capacity();

        for(int j=0;j<M;j++){
            double demand=customer_list.get(j).getDemand();
            int w=temp.get(i).get(j);

            double remained_capacity=warehouse_list.get(w).getRemain_cap_w();

            if(demand<=remained_capacity){

                remained_capacity=remained_capacity-demand;
                warehouse_list.get(w).setRemain_cap_w(remained_capacity);//capacity is updated

            }else{
                boolean j_feasible=false;
                while(!j_feasible){

                    for(int k=0;k<N;k++){
                        if(k!=w){

                            remained_capacity=warehouse_list.get(k).getRemain_cap_w();
                            if(demand<=remained_capacity){

                                temp.get(i).set(j,k); //assign j to warehouse k
                                remained_capacity=remained_capacity-demand;
                                warehouse_list.get(k).setRemain_cap_w(remained_capacity);//capacity is updated
                                j_feasible=true;
                            }
                        }
                        if(j_feasible) break;
                    }

                }
            }
        }

    }
    public static void Mutation(LinkedList<LinkedList<Integer>> temp){//Random resetting
        for(int i=0;i<chromozome_number;i++){//one point
            int j=generator.nextInt(M);
            int probability=generator.nextInt(2);//mutation rate p_m
            //int probability=1;
            int value=generator.nextInt(N);

            if(probability>0){

                temp.get(i).set(j,value);

            }

        }
    }

    public static void print_best_result(int index){

        System.out.printf("Energy: %f Time:%f Replacement:%f\n",chromozome_costs.get(index),chromozome_times.get(index),chromozome_replacement.get(index));
        //System.out.print(chromozome_costs.get(index)+"\n");
        for(int j=0;j<M;j++){
            System.out.print(chromozome.get(index).get(j)+" ");
        }
        System.out.print("\nBlockUtilization:");
        for(int j=0;j<N;j++) {
            System.out.print(chromozome_BlockUtilization.get(index).get(j)+" ");
        }

    }
    public static int find_worst(LinkedList<LinkedList<Integer>> temp,LinkedList<Double> temp_costs){
        double min_cost=temp_costs.get(0);
        int index=0;
        for(int i=1;i<chromozome_number;i++){
            if(min_cost<temp_costs.get(i)){
                min_cost=temp_costs.get(i);
                index=i;
            }
        }
        return index;
    }
    public static int find_best(LinkedList<LinkedList<Integer>> temp,LinkedList<Double> temp_costs){
        double max_cost=0;
        int index=0;
        boolean first_time=true;
        double t=-1;
        for(int i=0;i<chromozome_number;i++){
            if(first_time){
                if(temp_costs.get(i)!=t){
                    max_cost=temp_costs.get(i);
                    index=i;
                    first_time=false;
                }
            }else{
                if(temp_costs.get(i)!=t & max_cost>temp_costs.get(i)){
                    max_cost=temp_costs.get(i);
                    index=i;
                }
            }
        }
        return index;
    }
    public static void Survivor_Selection(){

        for(int iter=0;iter<chromozome_number/2;iter++){

            int best_chromozome=find_best(chromozome,chromozome_costs);
            int worst_new_generation=find_worst(new_generation,new_generation_costs);

            if(chromozome_costs.get(best_chromozome)<new_generation_costs.get(worst_new_generation)){//best chromozome are replaced with worst new_generation

                for(int j=0;j<M;j++){
                    new_generation.get(worst_new_generation).set(j,chromozome.get(best_chromozome).get(j));

                }
                new_generation_costs.set(worst_new_generation,chromozome_costs.get(best_chromozome));
                new_generation_times.set(worst_new_generation,chromozome_times.get(best_chromozome));
                new_generation_replacement.set(worst_new_generation,chromozome_replacement.get(best_chromozome));
                for(int j=0;j<N;j++)
                    new_generation_BlockUtilization.get(worst_new_generation).set(j,chromozome_BlockUtilization.get(best_chromozome).get(j));

            }
        }

        for(int i=0;i<chromozome_number;i++){
            for(int j=0;j<M;j++)
                chromozome.get(i).set(j,new_generation.get(i).get(j));
            chromozome_costs.set(i,new_generation_costs.get(i));
            chromozome_times.set(i,new_generation_times.get(i));
            chromozome_replacement.set(i,new_generation_replacement.get(i));
            for(int j=0;j<N;j++)
                chromozome_BlockUtilization.get(i).set(j,new_generation_BlockUtilization.get(i).get(j));
        }
    }
    public static void print_mating_pool(){
        for(int i=0;i<chromozome_number;i++){
            System.out.print("chromozome:"+mating_pool.get(i)+ "\n");
        }
    }

    public solution(int index,int list_index){
        this.index=index;
        this.list_index=list_index;
    }
    public void run() {

        if(list_index==1) {
            LinkedList<Double> t=calculate_chromozome_cost(chromozome.get(index));
            Double cost=t.get(0);
            Double time=t.get(1);
            Double replacement=t.get(2);
            LinkedList<Double> BlockUtilization= new LinkedList<Double>();
            for(int i=0;i<N;i++)
                BlockUtilization.add(i,t.get(3+i));
            try {
                sem.acquire();
                chromozome_costs.set(index,cost);
                chromozome_times.set(index,time);
                chromozome_replacement.set(index,replacement);
                for(int i=0;i<N;i++)
                    chromozome_BlockUtilization.get(index).set(i,BlockUtilization.get(i));
                sem.release();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }// set chromozome i with cost
        else {
            LinkedList<Double> t=calculate_chromozome_cost(new_generation.get(index));
            Double cost=t.get(0);
            Double time=t.get(1);
            Double replacement=t.get(2);
            LinkedList<Double> BlockUtilization= new LinkedList<Double>();
            for(int i=0;i<N;i++)
                BlockUtilization.add(i,t.get(3+i));
            try {
                sem.acquire();
                new_generation_costs.set(index,cost);
                new_generation_times.set(index,time);
                new_generation_replacement.set(index,replacement);
                for(int i=0;i<N;i++)
                    new_generation_BlockUtilization.get(index).set(i,BlockUtilization.get(i));
                sem.release();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }



        }// set chromozome i with cost

    }
}

