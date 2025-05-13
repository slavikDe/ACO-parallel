package Sequential;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.LongStream;

public class Main {

    public static void main(String[] args) {
        seqBenchmark();
    }

    public static void manualTesting(int noOfCities) {
        AntColonyOptimization aco = new AntColonyOptimization(noOfCities);
        int naive = aco.naiveSolution();
//        aco.prettyPrint();
        long startTime = System.currentTimeMillis();
        aco.startAntOptimization();

        System.out.println("Global time: " + (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("Naive solution: " + naive);
    }

    public static void seqBenchmark() {
        int [] citySizes = {50, 75, 100, 150, 200};
        int cycles = 5;

        long[] results = new long[cycles ];

        System.out.println("Start time:" + LocalDateTime.now());
        System.out.println("Sequential with \n cities: " + Arrays.toString(citySizes));
        for(int noOfCities : citySizes){
           for (int i = 0; i < cycles; i++) {
               AntColonyOptimization aco = new AntColonyOptimization(noOfCities);

               long startTime = System.currentTimeMillis();
               aco.startAntOptimization();
               long endTime = System.currentTimeMillis();
               long result = endTime - startTime;

               results[i] = result;
               System.out.println("Global time: " + result + " ms");
           }

           System.out.printf("Cities: %d, avg Time: %d, cycles: 20, %n", noOfCities, LongStream.of(results).sum() / cycles );
        }
        System.out.println("End time:" + LocalDateTime.now());
    }
}