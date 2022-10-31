package org.openmrs.eip.web.contoller;

import static org.apache.camel.component.jpa.JpaConstants.JPA_PARAMETERS_HEADER;
import static org.apache.camel.impl.engine.DefaultFluentProducerTemplate.on;
import static org.openmrs.eip.web.RestConstants.DEFAULT_MAX_COUNT;
import static org.openmrs.eip.web.RestConstants.FIELD_COUNT;
import static org.openmrs.eip.web.RestConstants.FIELD_ITEMS;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseRestController {
	
	protected static final Logger log = LoggerFactory.getLogger(BaseRestController.class);
	
	@Autowired
	protected ProducerTemplate producerTemplate;
	
	@Autowired
	protected CamelContext camelContext;
	
	public Map<String, Object> doGetAll() {
		Map<String, Object> results = new HashMap(2);
		Integer count = on(camelContext).to("jpa:" + getName() + "?query=SELECT count(*) FROM " + getName())
		        .request(Integer.class);
		
		results.put(FIELD_COUNT, count);
		
		List<Object> items;
		if (count > 0) {
			items = on(camelContext)
			        .to("jpa:" + getName() + "?query=SELECT c FROM " + getName() + " c &maximumResults=" + DEFAULT_MAX_COUNT)
			        .request(List.class);
			
			results.put(FIELD_ITEMS, items);
		} else {
			results.put(FIELD_ITEMS, Collections.emptyList());
		}
		
		return results;
	}
	
	public Object doGet(Long id) {
		return producerTemplate.requestBody(
		    "jpa:" + getName() + "?query=SELECT c FROM " + getName() + " c WHERE c.id = " + id, null, getClazz());
	}
	
	public Map<String, Object> searchByDate(String dateProperty, Date startDate, Date endDate) {
		final String queryParamStartDate = "startDate";
		final String queryParamEndDate = "endDate";
		Map<String, Object> results = new HashMap(2);
		String whereClause = StringUtils.EMPTY;
		
		Map<String, Object> paramAndValueMap = new HashMap(2);
		if (startDate != null) {
			whereClause = " WHERE e." + dateProperty + " >= :" + queryParamStartDate;
			paramAndValueMap.put(queryParamStartDate, startDate);
		}
		
		if (endDate != null) {
			whereClause += (StringUtils.isBlank(whereClause) ? " WHERE " : " AND") + " e." + dateProperty + " <= :"
			        + queryParamEndDate;
			paramAndValueMap.put(queryParamEndDate, endDate);
		}
		
		Integer count = on(camelContext)
		        .to("jpa:" + getName() + "?query=SELECT count(*) FROM " + getName() + " e " + whereClause)
		        .withHeader(JPA_PARAMETERS_HEADER, paramAndValueMap).request(Integer.class);
		
		if (count == 0) {
			results.put(FIELD_COUNT, 0);
			results.put(FIELD_ITEMS, Collections.emptyList());
			return results;
		}
		
		List<Object> items = on(camelContext).to("jpa:" + getName() + "?query=SELECT e FROM " + getName() + " e "
		        + whereClause + " &maximumResults=" + DEFAULT_MAX_COUNT).withHeader(JPA_PARAMETERS_HEADER, paramAndValueMap)
		        .request(List.class);
		
		results.put(FIELD_COUNT, count);
		results.put(FIELD_ITEMS, items);
		
		return results;
	}
	
	public String getName() {
		return getClazz().getSimpleName();
	}
	
	public abstract Class<?> getClazz();
	
}
