//package Parallel;
//
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.stream.IntStream;
//
//public class AntColonyOptimization {
//    private double c = 1.0;
//    private double alpha = 1;
//    private double beta = 5;
//    private double evaporation = 0.5;
//    private double Q = 100.0 * 5;
//    private double randomFactor = 0.01;
//    private int maxIterations = 300;
//    private int numberOfAnts;
//    private int numberOfCities;
//    private int[][] graph;
//    private double[][] trails;
//    private List<Ant> ants;
//
//    private int[] bestTourOrder;
//    private double bestTourLength;
//
//    private int numberOfThreads = 2;
//
//    public AntColonyOptimization(int noOfCities, int minDistance, int maxDistance) {
//        initializeParams(noOfCities, minDistance, maxDistance);
//    }
//
//    AntColonyOptimization(double tr, double al, double be, double ev, int q, double af, double rf, int iter, int noOfCities, int minDistance, int maxDistance) {
//        c = tr;
//        alpha = al;
//        beta = be;
//        evaporation = ev;
//        Q = q;
//        randomFactor = rf;
//        maxIterations = iter;
//        initializeParams(noOfCities, minDistance, maxDistance);
//    }
//
//    private void initializeParams(int noOfCities, int minDistance, int maxDistance) {
//        graph = generateRandomCity(noOfCities, minDistance, maxDistance);
//        this.numberOfCities = noOfCities;
//        trails = new double[noOfCities][noOfCities];
//        numberOfAnts = noOfCities;
//        ants = new ArrayList<>(numberOfAnts);
//        for (int i = 0; i < numberOfAnts; i++)
//            ants.add(new Ant(noOfCities));
//    }
//
//    private void solve() {
//        resetAnts();
//        clearTrails();
//
//        // Create thread pools for parallel processing
//        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
//
//        for (int iteration = 0; iteration < maxIterations; iteration++) {
//            // Reset ants for new iteration
//            resetAnts();
//
//            // Create individual ant tasks
//            List<Future<?>> antFutures = new ArrayList<>();
//            for (Ant ant : ants) {
//                antFutures.add(executorService.submit(new IndividualAntRunner(ant)));
//            }
//
//            // Wait for all ants to complete their tours
//            for (Future<?> future : antFutures) {
//                try {
//                    future.get();
//                } catch (InterruptedException | ExecutionException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            // Update trails and best solution (sequential operations)
//            updateTrails();
//            updateBest();
//        }
//
//        shutdownAndAwaitTermination(executorService);
//    }
//
//    private void shutdownAndAwaitTermination(ExecutorService executorService) {
//        executorService.shutdown();
//        try {
//            int EXECUTOR_SERVICE_TIMEOUT_SECONDS = 3;
//            if (!executorService.awaitTermination(EXECUTOR_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//                if (!executorService.awaitTermination(EXECUTOR_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
//                    System.err.println("Executor did not terminate");
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    public void startAntOptimization() {
//        int attempts = 5;
//        for (int i = 0; i < attempts; i++) {
//            solve();
//        }
//    }
//
//    private void clearTrails() {
//        for (int i = 0; i < numberOfCities; i++) {
//            for (int j = 0; j < numberOfCities; j++) {
//                trails[i][j] = c;
//            }
//        }
//    }
//
//    private void updateTrails() {
//        // Evaporate existing pheromones
//        for (int i = 0; i < numberOfCities; i++) {
//            for (int j = 0; j < numberOfCities; j++)
//                trails[i][j] *= evaporation;
//        }
//
//        // Add new pheromones from each ant's trail
//        for (Ant a : ants) {
//            double contribution = Q / a.trailLength(graph);
//            for (int i = 0; i < numberOfCities - 1; i++)
//                trails[a.trail[i]][a.trail[i + 1]] += contribution;
//            trails[a.trail[numberOfCities - 1]][a.trail[0]] += contribution;
//        }
//    }
//
//    private void updateBest() {
//        if (bestTourOrder == null) {
//            bestTourOrder = ants.get(0).trail;
//            bestTourLength = ants.get(0).trailLength(graph);
//        }
//
//        for (Ant a : ants) {
//            double length = a.trailLength(graph);
//            if (length < bestTourLength) {
//                bestTourLength = length;
//                bestTourOrder = a.trail.clone();
//            }
//        }
//    }
//
//    private void resetAnts() {
//        Random random = new Random();
//        for (Ant ant : ants) {
//            ant.clear();
//            ant.setFirstCity(random.nextInt(numberOfCities));
//        }
//    }
//
//    public void prettyPrint() {
//        IntStream.range(0, graph.length).forEach(i -> System.out.print("\t" + i));
//        System.out.println("\n\t" + "----".repeat(graph.length));
//
//        for (int i = 0; i < graph.length; i++) {
//            System.out.print(i + " | ");
//            for (int j = 0; j < graph[i].length; j++) {
//                System.out.print(graph[i][j] + "\t");
//            }
//            System.out.println();
//        }
//    }
//
//    public void setThreads(int nThreads) {
//        this.numberOfThreads = nThreads;
//    }
//
//    private int[][] generateRandomCity(int numberOfCities, int minDistance, int maxDistance) {
//        Random random = new Random();
//        int[][] randomCity = new int[numberOfCities][numberOfCities];
//
//        for (int i = 0; i < numberOfCities; i++) {
//            for (int j = 0; j < numberOfCities; j++) {
//                if (i == j) randomCity[i][j] = 0;
//                else randomCity[i][j] = random.nextInt(maxDistance - minDistance + 1) + minDistance;
//            }
//        }
//        return randomCity;
//    }
//
//    public int naiveSolution() {
//        return IntStream.of(graph[0]).sum();
//    }
//
//    // Individual ant runner that processes one ant's complete tour
//    private class IndividualAntRunner implements Runnable {
//        private final Ant ant;
//        private final Random localRandom;
//        private final double[] localProbabilities;
//
//        public IndividualAntRunner(Ant ant) {
//            this.ant = ant;
//            this.localRandom = new Random();
//            this.localProbabilities = new double[numberOfCities];
//        }
//
//        @Override
//        public void run() {
//            // Complete the tour for this ant
//            for (int step = 0; step < numberOfCities - 1; step++) {
//                int nextCity = selectNextCity();
//                ant.visitCity(nextCity);
//            }
//        }
//
//        private int selectNextCity() {
//            // Random selection with some probability
//            if (localRandom.nextDouble() < randomFactor) {
//                List<Integer> notVisitedCities = new ArrayList<>();
//                for (int i = 0; i < numberOfCities; i++) {
//                    if (!ant.visited(i)) {
//                        notVisitedCities.add(i);
//                    }
//                }
//                if (!notVisitedCities.isEmpty()) {
//                    int index = localRandom.nextInt(notVisitedCities.size());
//                    return notVisitedCities.get(index);
//                }
//            }
//
//            // Probability-based selection
//            calculateProbabilities();
//            double r = localRandom.nextDouble();
//            double total = 0;
//            for (int i = 0; i < numberOfCities; i++) {
//                total += localProbabilities[i];
//                if (total >= r) {
//                    return i;
//                }
//            }
//
//            // Fallback: return first unvisited city
//            for (int i = 0; i < numberOfCities; i++) {
//                if (!ant.visited(i)) {
//                    return i;
//                }
//            }
//            throw new RuntimeException("There are no other cities");
//        }
//
//        private void calculateProbabilities() {
//            int currentCity = ant.getCurrentCity();
//            double pheromoneSum = 0.0;
//
//            // Calculate sum of pheromone * heuristic for normalization
//            for (int j = 0; j < numberOfCities; j++) {
//                if (currentCity != j && !ant.visited(j)) {
//                    pheromoneSum += Math.pow(trails[currentCity][j], alpha)
//                            * Math.pow(1.0 / graph[currentCity][j], beta);
//                }
//            }
//
//            // Calculate probabilities
//            for (int j = 0; j < numberOfCities; j++) {
//                if (ant.visited(j) || j == currentCity) {
//                    localProbabilities[j] = 0.0;
//                } else {
//                    double numerator = Math.pow(trails[currentCity][j], alpha)
//                            * Math.pow(1.0 / graph[currentCity][j], beta);
//                    localProbabilities[j] = numerator / pheromoneSum;
//                }
//            }
//        }
//    }
//}