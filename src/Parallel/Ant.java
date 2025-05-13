package Parallel;

public class Ant {
    protected int trailSize;
    protected int[] trail;
    protected boolean[] visited;
    private int currentCity;
    private int numVisitedCities = 0;

    public Ant(int tourSize) {
        this.trailSize = tourSize;
        this.trail = new int[tourSize];
        this.visited = new boolean[tourSize];
    }

    protected void visitCity(int cityTo) {
        trail[numVisitedCities++] = cityTo;
        visited[cityTo] = true;
        currentCity = cityTo;
    }

    protected boolean visited(int i) {
        return visited[i];
    }

    protected int trailLength(int[][] graph) {
        int length = graph[trail[trailSize - 1]][trail[0]];
        for (int i = 0; i < trailSize - 1; i++)
            length += graph[trail[i]][trail[i + 1]];
        return length;
    }

    protected void clear() {
        for (int i = 0; i < trailSize; i++)
            visited[i] = false;
        numVisitedCities = 0;
    }

    protected void setFirstCity(int city) {
        currentCity = city;
        visited[city] = true;
        trail[0] = city;
        numVisitedCities++;
    }

    protected int[] getTrail(){
        return trail;
    }

    protected boolean[] getVisited(){
        return visited;
    }

    protected int getCurrentCity() {
        return currentCity;
    }
}