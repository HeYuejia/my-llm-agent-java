//public class RetryTask {
//    private final String operationId;
//    private final RetryableOperation operation;
//    private final int maxRetries;
//    private final long retryIntervalMillis;
//    private int currentRetryCount;
//    private final RetryStrategy retryStrategy;
//
//    public RetryTask(String operationId, RetryableOperation operation,
//                    int maxRetries, long retryIntervalMillis,
//                    RetryStrategy retryStrategy) {
//        this.operationId = operationId;
//        this.operation = operation;
//        this.maxRetries = maxRetries;
//        this.retryIntervalMillis = retryIntervalMillis;
//        this.currentRetryCount = 0;
//        this.retryStrategy = retryStrategy;
//    }
//
//    public void execute() {
//        try {
//            operation.execute();
//        } catch (Exception e) {
//            handleFailure(e);
//        }
//    }
//
//    void handleFailure(Exception e) {
//        currentRetryCount++;
//
//        if (currentRetryCount >= maxRetries) {
//            sendFinalFailureAlert(operationId, e);
//            return;
//        }
//
//        retryStrategy.scheduleRetry(this, retryIntervalMillis);
//    }
//
//    private void sendFinalFailureAlert(String operationId, Exception e) {
//        // 发送告警逻辑
//        System.out.println("Alert: Operation " + operationId + " failed after " +
//                          maxRetries + " attempts. Error: " + e.getMessage());
//    }
//}
//
//
//
