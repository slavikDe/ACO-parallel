package Sequential;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.LongStream;

public class Main {
    public static void main(String[] args) {
        int numCities = 4;
        seqBenchmark();
    }

    public static void manualTesting(int noOfCities) {
        int minDistance = 20, maxDistance = 100;

        int[][] graph = new int[][]{
                // Mumbai, Delhi, Bengaluru, Chennai
                {   0,   1400,     980,     1330 }, // Mumbai
                {1400,      0,    2150,     2200 }, // Delhi
                { 980,   2150,       0,      350 }, // Bengaluru
                {1330,   2200,     350,        0 }  // Chennai
        };

        AntColonyOptimization aco = new AntColonyOptimization(noOfCities, minDistance, maxDistance);
        aco.setGraph(graph);
        int naive = aco.naiveSolution();
        aco.prettyPrint();
        long startTime = System.currentTimeMillis();
        aco.startAntOptimization();

        System.out.println("Global time: " + (System.currentTimeMillis() - startTime) + " ms");
//        System.out.println("Naive solution: " + naive);
    }

    public static void seqBenchmark() {
        int minDistance = 20, maxDistance = 100;
        int [] citySizes = {50, 75, 100, 150, 200};
        int cycles = 5;

        long[] results = new long[cycles];

        System.out.println("Start time:" + LocalDateTime.now());
        System.out.print("Sequential with \n cities: " + Arrays.toString(citySizes) +'\n');
        for(int noOfCities : citySizes){
           for (int i = 0; i < cycles; i++) {
               AntColonyOptimization aco =  new AntColonyOptimization(noOfCities, minDistance, maxDistance);

               long startTime = System.currentTimeMillis();
               aco.startAntOptimization();
               long endTime = System.currentTimeMillis();
               long elapsedTime = endTime - startTime;

               if (i > 0){
                   results[i - 1] = elapsedTime;
                   System.out.print("\n- Global time: " + elapsedTime + " ms");
               }
               else System.out.print("- Warm up");
           }

           System.out.printf("Cities: %d, avg Time: %d, cycles: 20, %n", noOfCities, LongStream.of(results).sum() / cycles );
        }
        System.out.println("End time:" + LocalDateTime.now());
    }
}