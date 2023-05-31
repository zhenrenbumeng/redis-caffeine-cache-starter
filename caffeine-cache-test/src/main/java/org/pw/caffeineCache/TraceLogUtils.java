package org.pw.caffeineCache;

import java.util.UUID;

public class TraceLogUtils {
    /**
     * 日志跟踪id名。
     */
    public static final String LOG_TRACE_ID = "trace_id";

    public static String getTraceId() {
        return UUID.randomUUID().toString()
          // .replaceAll("-", "_")
          ;
    }

}
