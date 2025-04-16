@FunctionalInterface
public interface RetryableOperation {
    void execute() throws Exception;
}