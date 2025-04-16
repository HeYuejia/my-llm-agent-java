//import com.jd.jim.cli.redis.jedis.JedisCluster;
//
//import java.util.UUID;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicReference;
//
//public class RedisDualClusterCache implements DualClusterCache {
//    private final JedisCluster primaryCluster;
//    private final JedisCluster backupCluster;
//    private final ExecutorService asyncExecutor;
//    private final int maxRetryAttempts;
//    private final long retryIntervalMillis;
//    private final RetryStrategy retryStrategy;
//
//    public RedisDualClusterCache(JedisCluster primary, JedisCluster backup,
//                               int maxRetryAttempts, long retryIntervalMillis,
//                                 RetryStrategyType retryStrategyType) {
//        this.primaryCluster = primary;
//        this.backupCluster = backup;
//        this.maxRetryAttempts = maxRetryAttempts;
//        this.retryIntervalMillis = retryIntervalMillis;
//        this.asyncExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//        this.retryStrategy = RetryStrategyFactory.createStrategy(retryStrategyType);
//    }
//
//    @Override
//    public String get(String key) throws Exception {
//        try {
//            return primaryCluster.get(key);
//        } catch (Exception e) {
//            throw new Exception("Primary cluster read failed", e);
//        }
//    }
//
//    @Override
//    public void set(String key, String value, BackupMode mode) throws Exception {
//        // 重试主集群写入
//        executeWithRetry(() -> writeToPrimary(key, value, mode));
//    }
//
//    @Override
//    public void delete(String key, BackupMode mode) throws Exception {
//        executeWithRetry(() -> deleteFromPrimary(key, mode));
//    }
//
//    // 私有方法实现
//    private void writeToPrimary(String key, String value, BackupMode mode) throws Exception {
//        // 1. 写入主集群
//        primaryCluster.set(key, value);
//
//        // 2. 根据模式写入备份集群
//        if (mode == BackupMode.SYNC) {
//            syncWriteToBackup(key, value);
//        } else {
//            asyncWriteToBackup(key, value);
//        }
//    }
//
//    private void deleteFromPrimary(String key, BackupMode mode) throws Exception {
//        // 1. 从主集群删除
//        primaryCluster.del(key);
//
//        // 2. 根据模式从备份集群删除
//        if (mode == BackupMode.SYNC) {
//            syncDeleteFromBackup(key);
//        } else {
//            asyncDeleteFromBackup(key);
//        }
//    }
//
//    // 同步写入备份
//    private void syncWriteToBackup(String key, String value) throws Exception {
//        executeWithRetry(() -> {
//            backupCluster.set(key, value);
//            return null;
//        });
//    }
//
//    // 异步写入备份
//    private void asyncWriteToBackup(String key, String value) {
//        asyncExecutor.submit(() -> {
//            try {
//                syncWriteToBackup(key, value);
//            } catch (Exception e) {
//                // 异步操作失败处理，可以记录日志或触发告警
//                log.error("Async backup write failed for key: {}", key, e);
//            }
//        });
//    }
//
//    // 同步删除备份
//    private void syncDeleteFromBackup(String key) throws Exception {
//        executeWithRetry(() -> {
//            backupCluster.del(key);
//            return null;
//        });
//    }
//
//    // 异步删除备份
//    private void asyncDeleteFromBackup(String key) {
//        asyncExecutor.submit(() -> {
//            try {
//                syncDeleteFromBackup(key);
//            } catch (Exception e) {
//                log.error("Async backup delete failed for key: {}", key, e);
//            }
//        });
//    }
//
//    // 重试逻辑
//    private <T> T executeWithRetry(Callable<T> operation) throws Exception {
//        AtomicReference<T> result = new AtomicReference<>();
//        AtomicReference<Exception> lastException = new AtomicReference<>();
//
//        String operationId = UUID.randomUUID().toString();
//
//        RetryableOperation retryableOp = () -> {
//            try {
//                result.set(operation.call());
//            } catch (Exception e) {
//                lastException.set(e);
//                throw e;
//            }
//        };
//
//        RetryTask retryTask = new RetryTask(operationId, retryableOp,
//                maxRetryAttempts, retryIntervalMillis,
//                retryStrategy);
//
//        try {
//            // 立即执行第一次尝试
//            retryableOp.execute();
//            return result.get();
//        } catch (Exception e) {
//            // 第一次失败后进入重试流程
//            retryTask.handleFailure(e);
//            throw new Exception("Operation in progress with retries", e);
//        }
//    }
//
//    // 关闭资源
//    public void shutdown() {
//        asyncExecutor.shutdown();
//        if (retryStrategy instanceof DelayQueueRetryStrategy) {
//            ((DelayQueueRetryStrategy)retryStrategy).shutdown();
//        } else if (retryStrategy instanceof MqRetryStrategy) {
//            ((MqRetryStrategy)retryStrategy).shutdown();
//        }
//    }
//}