package de.dlr.ivf.tapas;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.HashMap;
import java.util.Map;

public class RandomThreadTest {

    /**
     * the random generator
     */
    private static final MersenneTwister generator;
    private static int resolution =1024;
    private static int tries =160000;
    private static int threads =16;


    Map<Integer,Integer> threadCollisions = new HashMap<>();

    class RandomThread extends Thread {


        public Integer[] occuancy = new Integer[0];
        public RandomThread(int resolution){
            this.occuancy = new Integer[resolution];
            //init
            for (int i = 0; i < resolution; i++)
                this.occuancy[i] = 0;
        }

        @Override
        public void run() {
            double val;
            int index;
            for(int i = 0; i< tries; i++){
                val =RandomThreadTest.random();
                index = (int)(val*this.occuancy.length);
                this.occuancy[index]++;
            }
        }
    }

    static {
        generator = new MersenneTwister();//new Random(RANDOM_SEED_NUMBER);
    }

    /**
     *
     *
     * @return random value
     */
    public static double random() {
        return generator.nextDouble();
    }

    public static double randomGaussian() {
        return generator.nextGaussian();
    }


    public static void main(String[] args){
        RandomThreadTest worker = new RandomThreadTest();


       Thread[] thread = new Thread[threads];
       //init the threads
       for(int i = 0; i< threads; i++){
           thread[i] = worker.new RandomThread(resolution);
       }
       //start them
       try {
           for (int i = 0; i < threads; i++) {
               thread[i].start();
           }
       }catch (Throwable e){
           e.printStackTrace();
       }
    }


}
