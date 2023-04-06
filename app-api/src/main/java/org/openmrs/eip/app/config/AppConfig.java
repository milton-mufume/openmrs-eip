package org.openmrs.eip.app.config;

import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;
import static org.openmrs.eip.app.SyncConstants.PROP_THREAD_NUMBER;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class AppConfig {
	
	@Bean(BEAN_NAME_SYNC_EXECUTOR)
	public ThreadPoolExecutor getSyncExecutor(@Value("${" + PROP_THREAD_NUMBER + ":}") Integer threadCount) {
		if (threadCount == null) {
			threadCount = Runtime.getRuntime().availableProcessors();
		}
		
		return (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
	}
	
}
