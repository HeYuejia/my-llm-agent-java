//public class RetryStrategyFactory {
//    public static RetryStrategy createStrategy(RetryStrategyType type, Object... args) {
//        switch (type) {
//            case DELAY_QUEUE:
//                return new DelayQueueRetryStrategy();
//            case MESSAGE_QUEUE:
//                return new MqRetryStrategy((String)args[0], (String)args[1]);
//            default:
//                throw new IllegalArgumentException("Unknown retry strategy type: " + type);
//        }
//    }
//}