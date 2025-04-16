//import java.util.concurrent.DelayQueue;
//import java.util.concurrent.Delayed;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public interface RetryStrategy {
//    void scheduleRetry(RetryTask task, long delayMillis);
//}
//
//// DelayQueue实现
//public class DelayQueueRetryStrategy implements RetryStrategy {
//    private final DelayQueue<DelayedRetryTask> delayQueue;
//    private final ExecutorService executor;
//
//    public DelayQueueRetryStrategy() {
//        this.delayQueue = new DelayQueue<>();
//        this.executor = Executors.newSingleThreadExecutor();
//        startConsumer();
//    }
//
//    @Override
//    public void scheduleRetry(RetryTask task, long delayMillis) {
//        long triggerTime = System.currentTimeMillis() + delayMillis;
//        delayQueue.put(new DelayedRetryTask(task, triggerTime));
//    }
//
//    private void startConsumer() {
//        executor.submit(() -> {
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    DelayedRetryTask delayedTask = delayQueue.take();
//                    delayedTask.getTask().execute();
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        });
//    }
//
//    private static class DelayedRetryTask implements Delayed {
//        private final RetryTask task;
//        private final long triggerTime;
//
//        public DelayedRetryTask(RetryTask task, long triggerTime) {
//            this.task = task;
//            this.triggerTime = triggerTime;
//        }
//
//        public RetryTask getTask() {
//            return task;
//        }
//
//        @Override
//        public long getDelay(TimeUnit unit) {
//            long diff = triggerTime - System.currentTimeMillis();
//            return unit.convert(diff, TimeUnit.MILLISECONDS);
//        }
//
//        @Override
//        public int compareTo(Delayed o) {
//            return Long.compare(this.triggerTime, ((DelayedRetryTask)o).triggerTime);
//        }
//    }
//
//    public void shutdown() {
//        executor.shutdownNow();
//    }
//}
//
//// MQ实现
//public class MqRetryStrategy implements RetryStrategy {
//    private final MessageQueueProducer mqProducer;
//    private final MessageQueueConsumer mqConsumer;
//
//    public MqRetryStrategy(String topic, String groupId) {
//        this.mqProducer = new MessageQueueProducer(topic);
//        this.mqConsumer = new MessageQueueConsumer(topic, groupId, this::handleMessage);
//        this.mqConsumer.start();
//    }
//
//    @Override
//    public void scheduleRetry(RetryTask task, long delayMillis) {
//        RetryMessage message = new RetryMessage(task, System.currentTimeMillis() + delayMillis);
//        mqProducer.send(message);
//    }
//
//    private void handleMessage(RetryMessage message) {
//        if (message.getTriggerTime() <= System.currentTimeMillis()) {
//            message.getTask().execute();
//        } else {
//            // 如果还没到触发时间，重新放入队列
//            mqProducer.send(message);
//        }
//    }
//
//    public void shutdown() {
//        mqProducer.close();
//        mqConsumer.close();
//    }
//}
//
//// MQ消息结构
//public class RetryMessage implements Serializable {
//    private final RetryTask task;
//    private final long triggerTime;
//
//    // 构造函数、getters等
//}