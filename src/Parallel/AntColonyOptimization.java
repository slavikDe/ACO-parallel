package Parallel;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
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

//    public final Random random = new Random();
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
    private final ReentrantLock lock = new ReentrantLock();

    public AntColonyOptimization(int noOfCities, int minDistance, int maxDistance) {
        initializeParams(noOfCities, minDistance, maxDistance);
    }

    AntColonyOptimization(double tr, double al, double be, double ev, int q, double af, double rf, int iter, int noOfCities, int minDistance, int maxDistance) {
        c = tr;
        alpha = al;
        beta = be;
        evaporation = ev;
        Q = q;
        randomFactor = rf;
        maxIterations = iter;
        initializeParams(noOfCities, minDistance, maxDistance);
    }

    private void initializeParams(int noOfCities, int minDistance, int maxDistance) {
        graph = generateRandomCity(noOfCities, minDistance, maxDistance);
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
        List<AntWorker> tasks = createTasks();

        // create ES
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // load tasks
        for(int iteration = 0; iteration < maxIterations; iteration++) {
            List<Future<?>> futures = new ArrayList<>();
            for(AntWorker task : tasks) {
               futures.add(executorService.submit(task));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            updateTrails();
            updateBest();

            if (iteration < maxIterations - 1) {
                resetAnts();
            }
       }

        // shutdown
        shutdownAndAwaitTermination(executorService);

//        System.out.println("Best tour length: " + bestTourLength);
//        System.out.println("Best tour order: " + Arrays.toString(bestTourOrder));
    }

    private List<AntWorker> createTasks() {
        List<AntWorker> tasks = new ArrayList<>();
        for(int i = 0; i < numberOfAnts; i++) {
            tasks.add(new AntWorker(ants.get(i)));
        }

        return tasks;
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
//            System.out.println("\nAttempt #" + (i+1));
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

    private int[][] generateRandomCity(int numberOfCities, int minDistance, int maxDistance) {
        Random random = new Random();
        int[][] randomCity = new int[numberOfCities][numberOfCities];

        for (int i = 0; i < numberOfCities; i++) {
            for (int j = 0; j < numberOfCities; j++) {
                if (i == j) randomCity[i][j] = 0;
                else randomCity[i][j] = random.nextInt(maxDistance - minDistance + 1) + minDistance;
            }
        }
        return randomCity;
    }

    public int naiveSolution() {
        return IntStream.of(graph[0]).sum();
    }

    private  int selectNextCity(Ant ant) {
        Random localRandom = new Random();

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

        lock.lock();
        try {
            double[] probabilities = calculateProbabilities(ant);
            double randomValue = localRandom.nextDouble();
            double total = 0;
            for (int i = 0; i < numberOfCities; i++) {
                total += probabilities[i];
                if (total >= randomValue) {
                    return i;
                }
            }

            for (int i = 0; i < numberOfCities; i++) {
                if (!ant.visited(i)) {
                    return i;
                }
            }
            throw new RuntimeException("There are no other cities");
        }finally {
            lock.unlock();
        }
    }

    private synchronized double[] calculateProbabilities(Ant ant) {
        int i = ant.getCurrentCity();
        double pheromone = 0.0;
        for (int j = 0; j < numberOfCities; j++) {
            if (i == j) continue;
            if (!ant.visited(j)) {
                pheromone += Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
            }
        }

        double[] probabilities = new double[numberOfCities];
        for (int j = 0; j < numberOfCities; j++) {
            if (ant.visited(j) || j == i) {
                probabilities[j] = 0.0;
            } else {
                double numerator = Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
                probabilities[j] = numerator / pheromone;

            }
        }
        return probabilities;
    }

    private class AntWorker implements Runnable {
        private final Ant ant;

        public AntWorker(Ant ant) {
            this.ant = ant;
        }

        @Override
        public void run() {
            for (int i = 0; i < numberOfCities - 1; i++) {
                int nextCity = selectNextCity(ant);
                ant.visitCity(nextCity);
            }
        }
    }
}
