public class Workload {
    private static volatile long counter;

    public static void main(String[] args) throws Exception {
        String type = args.length == 0 ? "cpu" : args[0];
        if (type.equals("memory")) {
            byte[][] memory = new byte[80][];
            for (int i = 0; i < memory.length; i++) {
                memory[i] = new byte[1024 * 1024];
                memory[i][0] = 1;
            }
            while (true) Thread.sleep(1000);
        }
        while (true) counter++;
    }
}
