package Sequential;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class ACOV01 {
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
    private double Q = 100.0 * 1;
    private double antFactor = 1.0;
    private double randomFactor = 0.01;
    private int maxIterations = 300;
    private int noOfCities;
    private int[][] graph;
    private double[][] trails;
    private List<Ant> ants;
    private double[] probabilities;

    private int currentIndex;

    private int[] bestTourOrder;
    private double bestTourLength;

    public ACOV01(int noOfCities) {
        initializeParams(noOfCities);
    }

    ACOV01(double al, double be, double ev, int q, double af, double rf, int iter, int noOfCities) {
        alpha = al;
        beta = be;
        evaporation = ev;
        Q = q;
        antFactor = af;
        randomFactor = rf;
        maxIterations = iter;
        initializeParams(noOfCities);
    }

    private void initializeParams(int noOfCities) {
        graph = generateRandomCity(noOfCities);
        this.noOfCities = noOfCities;
        int numberOfAnts = (int) (noOfCities * antFactor);
        ants = new ArrayList<>(numberOfAnts);
        trails = new double[noOfCities][noOfCities];
        probabilities = new double[noOfCities];

        for (int i = 0; i < numberOfAnts; i++)
            ants.add(new Ant(noOfCities));

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

    public void startAntOptimization() {
        int attempts = 5;
        for (int i = 0; i < attempts; i++) {
//            System.out.println("\nAttempt #" + (i + 1));
            long startTime = System.currentTimeMillis();
            solve();
//            System.out.println("Attempt time: " + (System.currentTimeMillis() - startTime) + " ms\n");
        }
    }

    private void solve() {
        resetAnts();
        clearTrails();

        for (int i = 0; i < maxIterations; i++) {
            currentIndex = 0;
            for (Ant ant : ants) {
                ant.clear();
            }
            moveAnts();
            updateTrails();
            updateBest();
        }

        System.out.println("\nBest tour length: " + bestTourLength);
//        System.out.println("\nBest tour order: " + Arrays.toString(bestTourOrder));
    }

    private void resetAnts() {
        for (Ant ant : ants) {
            ant.clear();
            ant.visitCity(random.nextInt(noOfCities));
        }
        currentIndex = 0;
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
        for (int i = currentIndex; i < noOfCities - 1; i++) {
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
        throw new RuntimeException("There are no other cities");
    }

    private void calculateProbabilities(Ant ant) {
        int i = ant.trail[currentIndex];
        double pheromone = 0.0;
        for (int j = 0; j < noOfCities; j++) {
            if(j == i) continue;
            if (!ant.visited(j)) {
                double valueToAdd = Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
                pheromone += valueToAdd;
                if(Double.isNaN(valueToAdd) || Double.isInfinite(valueToAdd) || Double.isNaN(pheromone) || Double.isInfinite(pheromone) ){
                    throw new RuntimeException("There is a NaN or infinite value");
                }
                if (pheromone == 0.0) {
                    System.out.println("0.0 pheromone");
                }
            }
        }
        for (int j = 0; j < noOfCities; j++) {
            if (ant.visited(j) || j == i) {
                probabilities[j] = 0.0;
            } else {
                double numerator = Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
                if(Double.isNaN(numerator) || Double.isInfinite(numerator) ){
                    throw new RuntimeException("There is a NaN or infinite value");
                }
                probabilities[j] = numerator / pheromone;
            }
        }
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