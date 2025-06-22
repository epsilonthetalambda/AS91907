public record Tick(int normal, int infected, int immune) { // Stores the totals of one simulation tick
    public int get(Person.State state) { // Returns the value corresponding to a given state
        return switch (state) {
            case NORMAL -> normal;
            case INFECTED -> infected;
            case IMMUNE -> immune;
        };
    }
    public Tick(int[] count) {
        this(count[0], count[1], count[2]);
    }
    @Override
    public String toString() { // Outputs a CSV line corresponding to the tick's totals
        return normal + "," + infected + ',' + immune + System.lineSeparator();
    }
}
