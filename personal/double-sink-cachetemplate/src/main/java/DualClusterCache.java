public interface DualClusterCache {
    // 读取操作
    String get(String key) throws Exception;
    
    // 写入操作
    void set(String key, String value, BackupMode mode) throws Exception;
    
    // 删除操作
    void delete(String key, BackupMode mode) throws Exception;
    
    // 备份模式枚举
    enum BackupMode {
        SYNC,  // 同步备份
        ASYNC  // 异步备份
    }
}