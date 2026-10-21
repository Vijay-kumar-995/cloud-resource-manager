package enforcer;

import java.util.List;

public interface DisplayMetricsProvider {
    List<DisplaySample> read() throws Exception;
}
