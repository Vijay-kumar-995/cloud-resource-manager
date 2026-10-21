package cgroup;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.ReadOnlyFileSystemException;

public class CgroupWriter {
    private final CgroupPathResolver paths;

    public CgroupWriter(CgroupPathResolver paths) {
        this.paths = paths;
    }

    public void setCpuMax(String containerId, long quotaUsec, long periodUsec) throws IOException {
        try {
            if (paths.isV2()) {
                Files.writeString(paths.cpuMaxPath(containerId), (quotaUsec < 0 ? "max" : quotaUsec)
                        + " " + periodUsec);
            } else {
                Files.writeString(paths.cpuPeriodPath(containerId), Long.toString(periodUsec));
                Files.writeString(paths.cpuMaxPath(containerId), Long.toString(quotaUsec));
            }
        } catch (AccessDeniedException exception) {
            throw permissionError(exception);
        } catch (ReadOnlyFileSystemException exception) {
            throw permissionError(new AccessDeniedException(exception.getMessage()));
        }
    }

    public void setMemoryMax(String containerId, long bytes) throws IOException {
        try {
            Files.writeString(paths.memoryMaxPath(containerId), bytes < 0 ? "max" : Long.toString(bytes));
        } catch (AccessDeniedException exception) {
            throw permissionError(exception);
        } catch (ReadOnlyFileSystemException exception) {
            throw permissionError(new AccessDeniedException(exception.getMessage()));
        }
    }

    private IOException permissionError(AccessDeniedException cause) {
        return new IOException("Cannot write cgroup limits. Run the resource manager with sudo.", cause);
    }
}
