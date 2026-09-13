import java.io.BufferedReader;
import java.io.FileReader;


public class TestProc{
public  static long getAvailableMemoryMB() throws Exception{
    BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
    String line;

    while ((line = reader.readLine()) != null) {
        if (line.startsWith("MemAvailable")) {
            String value = line.split("\\s+")[1];

            long memoryKB = Long.parseLong(value);
            long memoryMB = memoryKB/1024;

            reader.close();

            return memoryMB;
        }

    }
    reader.close();
    return 0;
}
public static void main(String[] args)throws Exception{
long memory = getAvailableMemoryMB();
System.out.println("Available Memory: "+ memory + " MB");
}
}
