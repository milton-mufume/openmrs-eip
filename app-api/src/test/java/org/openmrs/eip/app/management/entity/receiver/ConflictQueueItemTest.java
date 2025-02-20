package org.openmrs.eip.app.management.entity.receiver;

import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.model.PersonModel;
import org.springframework.beans.BeanUtils;

public class ConflictQueueItemTest {
	
	@Test
	public void shouldCreateAConflictItemFromASyncMessage() throws Exception {
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(SyncMessage.class);
		final Date dateCreated = new Date();
		SyncMessage msg = new SyncMessage();
		msg.setId(1L);
		msg.setDateCreated(new Date());
		msg.setIdentifier("uuid");
		msg.setEntityPayload("payload");
		msg.setModelClassName(PersonModel.class.getName());
		msg.setSite(new SiteInfo());
		msg.setSnapshot(true);
		msg.setMessageUuid("message-uuid");
		msg.setDateSentBySender(LocalDateTime.now());
		msg.setOperation(SyncOperation.u);
		msg.setDateCreated(dateCreated);
		long timestamp = System.currentTimeMillis();
		
		ConflictQueueItem conflict = new ConflictQueueItem(msg);
		
		Assert.assertNull(conflict.getId());
		assertTrue(conflict.getDateCreated().getTime() == timestamp || conflict.getDateCreated().getTime() > timestamp);
		assertEquals(dateCreated, conflict.getDateReceived());
		Set<String> ignored = new HashSet();
		ignored.add("id");
		ignored.add("class");
		ignored.add("dateCreated");
		for (PropertyDescriptor descriptor : descriptors) {
			if (ignored.contains(descriptor.getName())) {
				continue;
			}
			
			String getter = descriptor.getReadMethod().getName();
			assertEquals(invokeMethod(msg, getter), invokeMethod(conflict, getter));
		}
	}
	
	@Test
	public void shouldCreateAConflictItemFromARetryItem() throws Exception {
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(ReceiverRetryQueueItem.class);
		ReceiverRetryQueueItem retry = new ReceiverRetryQueueItem();
		retry.setId(1L);
		retry.setDateCreated(new Date());
		retry.setIdentifier("uuid");
		retry.setEntityPayload("payload");
		retry.setModelClassName(PersonModel.class.getName());
		retry.setSite(new SiteInfo());
		retry.setSnapshot(true);
		retry.setMessageUuid("message-uuid");
		retry.setDateSentBySender(LocalDateTime.now());
		retry.setDateReceived(new Date());
		long timestamp = System.currentTimeMillis();
		
		ConflictQueueItem conflict = new ConflictQueueItem(retry);
		
		Assert.assertNull(conflict.getId());
		assertTrue(conflict.getDateCreated().getTime() == timestamp || conflict.getDateCreated().getTime() > timestamp);
		Set<String> ignored = new HashSet();
		ignored.add("id");
		ignored.add("class");
		ignored.add("dateCreated");
		ignored.add("attemptCount");
		ignored.add("dateChanged");
		ignored.add("exceptionType");
		ignored.add("message");
		for (PropertyDescriptor descriptor : descriptors) {
			if (ignored.contains(descriptor.getName())) {
				continue;
			}
			
			String getter = descriptor.getReadMethod().getName();
			assertEquals(invokeMethod(retry, getter), invokeMethod(conflict, getter));
		}
	}
	
}
