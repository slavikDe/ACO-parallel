package Sequential;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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

    private final Random random = new Random();
    private double alpha = 1;
    private double beta = 5;
    private double evaporation = 0.5;
    private double Q = 100.0 * 5;
    private double randomFactor = 0.1;
    private int maxIterations = 200;

    private int noOfCities;
    private int[][] graph;
    private double[][] trails;
    private List<Ant> ants;
    private double[] probabilities;

    private int currentIndex;

    private int[] bestTourOrder;
    private double bestTourLength;

    public AntColonyOptimization(int noOfCities, int minDistance, int maxDistance) {
        initializeParams(noOfCities, minDistance, maxDistance);
    }

    AntColonyOptimization(double al, double be, double ev, int q, double rf, int iter, int noOfCities, int minDistance, int maxDistance) {
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
        this.noOfCities = noOfCities;
        int numberOfAnts = noOfCities;
        ants = new ArrayList<>(numberOfAnts);
        trails = new double[noOfCities][noOfCities];
        probabilities = new double[noOfCities];
        for (int i = 0; i < numberOfAnts; i++)
            ants.add(new Ant(noOfCities));
    }

    private int[][] generateRandomCity(int numberOfCities, int minDistance, int maxDistance) {
        int[][] randomCity = new int[numberOfCities][numberOfCities];

        for (int i = 0; i < numberOfCities; i++) {
            for (int j = 0; j < numberOfCities; j++) {
                if (i == j) randomCity[i][j] = 0;
                else randomCity[i][j] = random.nextInt(maxDistance - minDistance + 1) + minDistance;
            }
        }
        return randomCity;
    }

    public void startAntOptimization() {
        int attempts = 20;

        for (int i = 0; i < attempts; i++) {
            System.out.println("\nAttempt #" + (i+1));
            solve();
//            System.out.println("Best Tour Order: " + Arrays.toString(bestTourOrder) + "\n");
        }

//        System.out.print("Length: " + bestTourLength + " Naive Solution: " + IntStream.of(graph[0]).sum() + " ");
    }

    private void solve() {
        resetAnts();
        clearTrails();

        for (int i = 0; i < maxIterations; i++) {
            currentIndex = 0;
            for (Ant ant : ants) {
                ant.clear();
                ant.setFirstCity(random.nextInt(noOfCities));
            }
//            currentIndex = 1;
            moveAnts();
            updateTrails();
            updateBest();
        }

        System.out.println("Best tour length: " + bestTourLength);
        System.out.println("Best tour order: " + Arrays.toString(bestTourOrder) + '\n');
    }

    private void resetAnts() {
        for (Ant ant : ants) {
            ant.clear();
//            ant.visitCity(random.nextInt(noOfCities));
        }
    }

    private void clearTrails() {
        for (int i = 0; i < noOfCities; i++) {
            for (int j = 0; j < noOfCities; j++) {
                trails[i][j] = 1;
            }
        }
    }

    private void moveAnts() {
        probabilities = new double[noOfCities];
        for (int i = 0; i < noOfCities - 1; i++) {
            for (Ant ant : ants) {
                ant.visitCity(selectNextCity(ant));
            }
            currentIndex++;
        }
    }

    private void updateTrails() {
        for (int i = 0; i < noOfCities; i++) {
            for (int j = 0; j < noOfCities; j++)
                trails[i][j] *= evaporation;
        }
        for (Ant a : ants) {
            double contribution = Q / a.trailLength(graph);
            for (int i = 0; i < noOfCities - 1; i++)
                trails[a.trail[i]][a.trail[i + 1]] += contribution;
            trails[a.trail[noOfCities - 1]][a.trail[0]] += contribution;
        }
    }

    private void updateBest() {
        if (bestTourOrder == null) {
            bestTourOrder = ants.getFirst().trail;
            bestTourLength = ants.getFirst().trailLength(graph);
        }

        for (Ant a : ants) {
            if (a.trailLength(graph) < bestTourLength) {
                bestTourLength = a.trailLength(graph);
                bestTourOrder = a.trail.clone();
            }
        }
    }

    private int selectNextCity(Ant ant) {
        if (random.nextDouble() < randomFactor) {
            List<Integer> notVisitedCities = new ArrayList<>();
            for (int i = 0; i < noOfCities; i++) {
                if (!ant.visited(i)) {
                    notVisitedCities.add(i);
                }
            }
            if (!notVisitedCities.isEmpty()) {
                int index = random.nextInt(notVisitedCities.size());
                return notVisitedCities.get(index);
            }
        }
        calculateProbabilities(ant);
        double r = random.nextDouble();
        double total = 0;
        for (int i = 0; i < noOfCities; i++) {
            total += probabilities[i];
            if (total >= r) {
                return i;
            }
        }
        for (int i = 0; i < noOfCities; i++) {
            if (!ant.visited(i)) {
                return i;
            }
        }
        throw new RuntimeException("There are no other cities");
    }

    private void calculateProbabilities(Ant ant) {
        int currentCity = ant.trail[currentIndex];
        double pheromone = 0.0;
        for (int j = 0; j < noOfCities; j++) {
            if (!ant.visited(j)) {
                pheromone += Math.pow(trails[currentCity][j], alpha) * Math.pow(1.0 / graph[currentCity][j], beta);
            }
        }

        for (int j = 0; j < noOfCities; j++) {
            if (ant.visited(j)) {
                probabilities[j] = 0.0;
            } else {
                double numerator = Math.pow(trails[currentCity][j], alpha) * Math.pow(1.0 / graph[currentCity][j], beta);
                probabilities[j] = numerator / pheromone;
            }
        }
    }

    public void setGraph(int[][] graph) {
        this.graph = graph;
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

    public int naiveSolution() {
        return IntStream.of(graph[0]).sum();
    }
}