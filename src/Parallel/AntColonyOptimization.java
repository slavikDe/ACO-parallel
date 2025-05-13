package Parallel;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class AntColonyOptimization {
    /*
     * default
     * private double c = 1.0;             //number of trails
     * private double alpha = 1;           //pheromone importance
     * private double beta = 5;            //distance priority
     * private double evaporation = 0.5;
     * private double Q = 500;             //pheromone left on trail per ant
     * private double antFactor = 0.8;     //no of ants per node
     * private double randomFactor = 0.01; //introducing randomness
     * private int maxIterations = 1000;
     */

    public final Random random = new Random();
    private double c = 1.0;
    private double alpha = 1;
    private double beta = 5;
    private double evaporation = 0.5;
    private double Q = 100.0 * 5;
    private double randomFactor = 0.01;
    private int maxIterations = 300;
    private int numberOfAnts;
    private int numberOfCities;
    private int[][] graph;
    private double[][] trails;
    private List<Ant> ants;

    private int[] bestTourOrder;
    private double bestTourLength ;

    private int numberOfThreads = 2; // default min value

    public AntColonyOptimization(int noOfCities) {
        initializeParams(noOfCities);
    }

    AntColonyOptimization(double tr, double al, double be, double ev, int q, double af, double rf, int iter, int noOfCities) {
        c = tr;
        alpha = al;
        beta = be;
        evaporation = ev;
        Q = q;
        randomFactor = rf;
        maxIterations = iter;
        initializeParams(noOfCities);
    }

    private void initializeParams(int noOfCities) {
        graph = generateRandomCity(noOfCities);
        this.numberOfCities = noOfCities;
        trails = new double[noOfCities][noOfCities];
        numberOfAnts = noOfCities ;
        ants = new ArrayList<>(numberOfAnts);
        for (int i = 0; i < numberOfAnts; i++)
            ants.add(new Ant(noOfCities));
    }

    private void solve()  {
        resetAnts();
        clearTrails();

        // create tasks
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads, updateTrailsAndBest());
        List<Future<?>> futures = new ArrayList<>();

        int defaultAntsPerThread = numberOfAnts / numberOfThreads;
        int remainder = numberOfAnts % numberOfThreads;
        int antsOffset = 0;

        for (int i = 0; i < numberOfThreads; i++) {
            int antsPerThread = defaultAntsPerThread + (i < remainder ? 1 : 0);
            int finalAntsOffset = antsOffset;

            AntsRunner antsRunner = new AntsRunner(barrier, antsPerThread, finalAntsOffset);
            futures.add(executorService.submit(antsRunner));

            antsOffset += antsPerThread;
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // shutdown
        shutdownAndAwaitTermination(executorService);

        System.out.println("\nBest tour length: " + bestTourLength);
        System.out.println("\nBest tour order: " + Arrays.toString(bestTourOrder));
    }

    private Runnable updateTrailsAndBest(){
        return () -> {
            updateTrails();
            updateBest();
        };
    }

    private void shutdownAndAwaitTermination(ExecutorService executorService) {
        executorService.shutdown();
        try {
            int EXECUTOR_SERVICE_TIMEOUT_SECONDS = 3;
            if (!executorService.awaitTermination(EXECUTOR_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(EXECUTOR_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    System.err.println("Executor did not terminate");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void startAntOptimization() {
        int attempts = 5;
        for (int i = 0; i < attempts; i++) {
            System.out.println("Attempt #" + (i+1));
            solve();

        }
//        System.out.print("\nLength: " + bestTourLength + " Naive Solution: " + IntStream.of(graph[0]).sum() + " ");
    }

    private void clearTrails() {
        for (int i = 0; i < numberOfCities; i++) {
            for (int j = 0; j < numberOfCities; j++) {
                trails[i][j] = c;
            }
        }
    }

    private void updateTrails() {
        for (int i = 0; i < numberOfCities; i++) {
            for (int j = 0; j < numberOfCities; j++)
                trails[i][j] *= evaporation;
        }
        for (Ant a : ants) {
            double contribution = Q / a.trailLength(graph);
            for (int i = 0; i < numberOfCities - 1; i++)
                trails[a.trail[i]][a.trail[i + 1]] += contribution;
            trails[a.trail[numberOfCities - 1]][a.trail[0]] += contribution;
        }
    }

    private void updateBest() {
        if (bestTourOrder == null) {
            bestTourOrder = ants.getFirst().trail;
            bestTourLength = ants.getFirst().trailLength(graph);
        }

        for (Ant a : ants) {
            double length = a.trailLength(graph);
            if (length < bestTourLength) {
                bestTourLength = length;
                bestTourOrder = a.trail.clone();
            }
        }
    }

    private void resetAnts() {
        for (Ant ant : ants)  ant.clear();
    }

    public void prettyPrint() {
        IntStream.range(0, graph.length).forEach(i -> System.out.print("\t" + i));
        System.out.println("\n\t" + "----".repeat(graph.length));

        for (int i = 0; i < graph.length; i++) {
            System.out.print(i + " | ");
            for (int j = 0; j < graph[i].length; j++) {
                System.out.print(graph[i][j] + "\t");
            }
            System.out.println();
        }
    }

    public void setThreads(int nThreads) {
        this.numberOfThreads = nThreads;
    }

    private int[][] generateRandomCity(int numberOfCities) {
        int maxDistanceBetweenCities = 100;
        int[][] randomCity = new int[numberOfCities][numberOfCities];

        for (int i = 0; i < numberOfCities; i++) {
            for (int j = 0; j < numberOfCities; j++) {
                if (i == j) randomCity[i][j] = 0;
                else randomCity[i][j] = random.nextInt(maxDistanceBetweenCities) + 1;
            }
        }
        return randomCity;
    }

    public int naiveSolution() {
        return IntStream.of(graph[0]).sum();
    }

    private class

    AntsRunner implements Runnable {
        private final CyclicBarrier barrier;
        private final List<Ant> localAnts;
        private final Random localRandom = new Random();
        private final double[] localProbabilities;

        AntsRunner(CyclicBarrier barrier, int antsPerThread, int antsOffset) {
            this.localAnts = ants.subList(antsOffset, antsOffset + antsPerThread);
            this.barrier = barrier;
            this.localProbabilities = new double[numberOfCities];
        }

        @Override
        public void run() {
            for (int i = 0; i < maxIterations; i++) {
                for(Ant ant : localAnts) {
                    ant.clear();
                    ant.setFirstCity(localRandom.nextInt(numberOfCities));
                }

                moveLocalAnts();
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void moveLocalAnts() {
            for(int i = 0; i < numberOfCities - 1; i++) {
                for(Ant ant : localAnts) {
                    int nextCity = selectNextCity(ant);
                    ant.visitCity(nextCity);
                }
            }
        }

        private int selectNextCity(Ant ant) {
            if (localRandom.nextDouble() < randomFactor) {
                List<Integer> notVisitedCities = new ArrayList<>();
                for (int i = 0; i < numberOfCities; i++) {
                    if (!ant.visited(i)) {
                        notVisitedCities.add(i);
                    }
                }
                if (!notVisitedCities.isEmpty()) {
                    int index = localRandom.nextInt(notVisitedCities.size());
                    return notVisitedCities.get(index);
                }
            }

            calculateProbabilities(ant);
            double r = localRandom.nextDouble();
            double total = 0;
            for (int i = 0; i < numberOfCities; i++) {
                total += localProbabilities[i];
                if (total >= r) {
                    return i;
                }
            }

            for (int i = 0; i < numberOfCities; i++) {
                if (!ant.visited(i)) {
                    return i;
                }
            }
            throw new RuntimeException("There are no other cities");
        }

        private synchronized void calculateProbabilities(Ant ant) {
            int i = ant.getCurrentCity();
            double pheromone = 0.0;
            for (int j = 0; j < numberOfCities; j++) {
                if (i == j) continue;
                if (!ant.visited(j)) {
                    pheromone += Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
                }
            }
            for (int j = 0; j < numberOfCities; j++) {
                if (ant.visited(j) || j == i) {
                    localProbabilities[j] = 0.0;
                } else {
                    double numerator = Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
                    localProbabilities[j] = numerator / pheromone;

                }
            }
        }
    }
}
