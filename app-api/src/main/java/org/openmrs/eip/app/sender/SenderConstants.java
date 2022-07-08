package org.openmrs.eip.app.sender;

public class SenderConstants {
	
	public static final String EX_PROP_EVENT = "event";
	
	public static final String EX_PROP_DBZM_EVENT = "dbzmEvent";
	
	public static final String EX_PROP_IS_SUBCLASS = "is-subclass";
	
	public static final String EX_PROP_RETRY_ITEM_ID = "retry-item-id";
	
	public static final String EX_PROP_RETRY_ITEM = "retry-item";
	
	public static final String EX_PROP_FAILED_ENTITIES = "failed-entities";
	
	public static final String ROUTE_ID_DB_EVENT_PROCESSOR = "db-event-processor";
	
	public static final String URI_DB_EVENT_PROCESSOR = "direct:" + ROUTE_ID_DB_EVENT_PROCESSOR;
	
	public static final String ROUTE_ID_DBZM_EVENT_PROCESSOR = "debezium-event-processor";
	
	public static final String URI_DBZM_EVENT_PROCESSOR = "direct:" + ROUTE_ID_DBZM_EVENT_PROCESSOR;
	
	public static final String ROUTE_ID_DBSYNC = "out-bound-db-sync";
	
	public static final String URI_DBSYNC = "direct:" + ROUTE_ID_DBSYNC;
	
	public static final String ROUTE_ID_DBZM_EVENT_READER = "debezium-event-reader";
	
	public static final String URI_DBZM_EVENT_READER = "direct:" + ROUTE_ID_DBZM_EVENT_READER;
	
	public static final String ROUTE_ID_RETRY = "sender-retry";
	
	public static final String URI_RETRY = "direct:" + ROUTE_ID_RETRY;
	
	public static final String ROUTE_ID_ERROR_HANDLER = "outbound-error-handler";
	
	public static final String URI_ERROR_HANDLER = "direct:" + ROUTE_ID_ERROR_HANDLER;
	
	public static final String ERROR_HANDLER_REF = "outBoundErrorHandler";
	
}
