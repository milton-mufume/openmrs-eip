package org.openmrs.eip.web.sender;

import static org.apache.camel.impl.engine.DefaultFluentProducerTemplate.on;
import static org.openmrs.eip.web.RestConstants.DEFAULT_MAX_COUNT;
import static org.openmrs.eip.web.RestConstants.FIELD_COUNT;
import static org.openmrs.eip.web.RestConstants.FIELD_ITEMS;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.eip.app.management.entity.SenderSyncMessage;
import org.openmrs.eip.web.RestConstants;
import org.openmrs.eip.web.contoller.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestConstants.API_PATH + "/dbsync/sender/sync")
public class SenderSyncMessageController extends BaseRestController {
	
	private static final Logger log = LoggerFactory.getLogger(SenderSyncMessageController.class);
	
	@Override
	public Class<?> getClazz() {
		return SenderSyncMessage.class;
	}
	
	@GetMapping
	public Map<String, Object> getAll() {
		if (log.isDebugEnabled()) {
			log.debug("Fetching sender sync messages");
		}
		
		Map<String, Object> results = new HashMap(2);
		Integer count = on(camelContext)
		        .to("jpa:" + getName() + "?query=SELECT count(*) FROM " + getName() + " WHERE status = 'NEW'")
		        .request(Integer.class);
		
		results.put(FIELD_COUNT, count);
		
		List<Object> items;
		if (count > 0) {
			items = on(camelContext).to("jpa:" + getName() + "?query=SELECT m FROM " + getName()
			        + " m WHERE m.status = 'NEW' &maximumResults=" + DEFAULT_MAX_COUNT).request(List.class);
			
			results.put(FIELD_ITEMS, items);
		} else {
			results.put(FIELD_ITEMS, Collections.emptyList());
		}
		
		return results;
	}
	
	@GetMapping("/{id}")
	public Object get(@PathVariable("id") Integer id) {
		if (log.isDebugEnabled()) {
			log.debug("Fetching sender sync message with id: " + id);
		}
		
		return doGet(id);
	}
	
}
