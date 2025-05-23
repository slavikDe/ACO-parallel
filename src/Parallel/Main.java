package Parallel;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.LongStream;

public class Main {
    public static void main(String[] args) {
        manualTesting(100);
    }

    public static void manualTesting(int noOfCities) {

        AntColonyOptimization aco = new AntColonyOptimization(noOfCities);
        int naive = aco.naiveSolution();
        aco.prettyPrint();
        long startTime = System.currentTimeMillis();
        aco.startAntOptimization();

        System.out.println("Global time: " + (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("Naive solution: " + naive);
    }

    public static void parallelBenchmark() {
        int threadsCount = 20;

        int [] citySizes = {50, 75, 100, 150, 200};

        int cycles = 20;

        long[] results = new long[cycles];
        System.out.println("Start time: " + LocalDateTime.now());
        System.out.print("Parallel with \n cities: " + Arrays.toString(citySizes) +'\n');
        for (int noOfCities : citySizes) {
            for (int i = 0; i < cycles; i++) {
                AntColonyOptimization aco = new AntColonyOptimization(noOfCities);
                aco.setThreads(threadsCount);

                long startTime = System.currentTimeMillis();
                aco.startAntOptimization();
                long endTime = System.currentTimeMillis();
                long result = endTime - startTime;

                if (i > 0){
                    results[i - 1] = result;
                    System.out.print("\n- Global time: " + result + " ms");
                }
                else System.out.print("- Warm up");

            }
            System.out.printf("%nCities: %d, threads: %d, avg Time: %d, cycles: 20, %n", noOfCities, threadsCount, LongStream.of(results).sum() / (cycles - 1) );
        }
        System.out.println("End time:" + LocalDateTime.now());

    }
}