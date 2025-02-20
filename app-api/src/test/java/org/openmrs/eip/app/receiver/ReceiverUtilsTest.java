package org.openmrs.eip.app.receiver;

import static com.jayway.jsonpath.Option.DEFAULT_PATH_LEAF_TO_NULL;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.management.entity.receiver.SyncedMessage.SyncOutcome.CONFLICT;
import static org.openmrs.eip.app.management.entity.receiver.SyncedMessage.SyncOutcome.ERROR;
import static org.openmrs.eip.app.management.entity.receiver.SyncedMessage.SyncOutcome.SUCCESS;
import static org.openmrs.eip.app.receiver.ReceiverUtils.generateEvictionPayload;
import static org.openmrs.eip.app.receiver.ReceiverUtils.generateSearchIndexUpdatePayload;
import static org.openmrs.eip.component.SyncOperation.c;
import static org.openmrs.eip.component.SyncOperation.d;

import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.openmrs.eip.app.management.entity.receiver.SyncMessage;
import org.openmrs.eip.app.management.entity.receiver.SyncedMessage;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.component.camel.utils.CamelUtils;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.DrugOrderModel;
import org.openmrs.eip.component.model.OrderModel;
import org.openmrs.eip.component.model.PatientIdentifierModel;
import org.openmrs.eip.component.model.PatientModel;
import org.openmrs.eip.component.model.PersonAddressModel;
import org.openmrs.eip.component.model.PersonAttributeModel;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.PersonNameModel;
import org.openmrs.eip.component.model.TestOrderModel;
import org.openmrs.eip.component.model.UserModel;
import org.openmrs.eip.component.model.VisitModel;
import org.openmrs.eip.component.repository.PatientIdentifierRepository;
import org.openmrs.eip.component.repository.PersonAttributeRepository;
import org.openmrs.eip.component.repository.PersonNameRepository;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.BeanUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, CamelUtils.class, BeanUtils.class })
public class ReceiverUtilsTest {
	
	@Mock
	private ProducerTemplate mockTemplate;
	
	@Mock
	private PersonNameRepository mockNameRepo;
	
	@Mock
	private PatientIdentifierRepository mockIdRepo;
	
	@Mock
	private PersonAttributeRepository mockAttribRepo;
	
	private ParseContext jsonPathContext = JsonPath
	        .using(Configuration.builder().options(DEFAULT_PATH_LEAF_TO_NULL).build());
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(CamelUtils.class);
		PowerMockito.mockStatic(BeanUtils.class);
		when(SyncContext.getBean(ProducerTemplate.class)).thenReturn(mockTemplate);
		when(SyncContext.getBean(PersonNameRepository.class)).thenReturn(mockNameRepo);
		when(SyncContext.getBean(PatientIdentifierRepository.class)).thenReturn(mockIdRepo);
		when(SyncContext.getBean(PersonAttributeRepository.class)).thenReturn(mockAttribRepo);
		when(mockTemplate.getCamelContext()).thenReturn(new DefaultCamelContext());
	}
	
	@After
	public void tearDown() {
		Whitebox.setInternalState(ReceiverUtils.class, "nameRepo", (Object) null);
		Whitebox.setInternalState(ReceiverUtils.class, "idRepo", (Object) null);
		Whitebox.setInternalState(ReceiverUtils.class, "attribRepo", (Object) null);
	}
	
	@Test
	public void isCached_shouldReturnTrueForCachedEntities() {
		assertTrue(ReceiverUtils.isCached(PersonModel.class.getName()));
		assertTrue(ReceiverUtils.isCached(PersonNameModel.class.getName()));
		assertTrue(ReceiverUtils.isCached(PersonAddressModel.class.getName()));
		assertTrue(ReceiverUtils.isCached(PersonAttributeModel.class.getName()));
		assertTrue(ReceiverUtils.isCached(UserModel.class.getName()));
		assertTrue(ReceiverUtils.isCached(PatientModel.class.getName()));
	}
	
	@Test
	public void isCached_shouldReturnFalseForNonCachedEntities() {
		assertFalse(ReceiverUtils.isCached(PatientIdentifierModel.class.getName()));
	}
	
	@Test
	public void isIndexed_shouldReturnTrueForIndexedEntities() {
		assertTrue(ReceiverUtils.isIndexed(PersonNameModel.class.getName()));
		assertTrue(ReceiverUtils.isIndexed(PersonAttributeModel.class.getName()));
		assertTrue(ReceiverUtils.isIndexed(PatientIdentifierModel.class.getName()));
		assertTrue(ReceiverUtils.isIndexed(PersonModel.class.getName()));
		assertTrue(ReceiverUtils.isIndexed(PatientModel.class.getName()));
	}
	
	@Test
	public void isIndexed_shouldReturnFalseForNonIndexedEntities() {
		assertFalse(ReceiverUtils.isIndexed(PersonAddressModel.class.getName()));
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromASyncMessageForACachedAndIndexedEntity() throws Exception {
		SyncMessage syncMessage = new SyncMessage();
		syncMessage.setModelClassName(PersonModel.class.getName());
		long timestamp = System.currentTimeMillis();
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(syncMessage, SUCCESS);
		
		assertNull(msg.getId());
		assertTrue(msg.getDateCreated().getTime() == timestamp || msg.getDateCreated().getTime() > timestamp);
		assertEquals(syncMessage.getDateCreated(), msg.getDateReceived());
		assertFalse(msg.isResponseSent());
		assertTrue(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
		assertTrue(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
		assertEquals(SUCCESS, msg.getOutcome());
		PowerMockito.verifyStatic(BeanUtils.class);
		BeanUtils.copyProperties(syncMessage, msg, "id", "dateCreated");
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromASyncMessageForACachedButNotIndexedEntity() {
		SyncMessage syncMessage = new SyncMessage();
		syncMessage.setModelClassName(PersonAddressModel.class.getName());
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(syncMessage, SUCCESS);
		
		assertEquals(SUCCESS, msg.getOutcome());
		assertTrue(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
		assertFalse(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromASyncMessageForAnIndexedButNotCachedEntity() {
		SyncMessage syncMessage = new SyncMessage();
		syncMessage.setModelClassName(PatientIdentifierModel.class.getName());
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(syncMessage, SUCCESS);
		
		assertEquals(SUCCESS, msg.getOutcome());
		assertTrue(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
		assertFalse(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromASyncMessageForAFailedItem() {
		SyncMessage syncMessage = new SyncMessage();
		syncMessage.setModelClassName(PersonModel.class.getName());
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(syncMessage, ERROR);
		
		assertEquals(ERROR, msg.getOutcome());
		assertTrue(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
		assertTrue(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromASyncMessageForAConflictedItem() {
		SyncMessage syncMessage = new SyncMessage();
		syncMessage.setModelClassName(PersonModel.class.getName());
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(syncMessage, CONFLICT);
		
		assertEquals(CONFLICT, msg.getOutcome());
		assertTrue(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
		assertTrue(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForAPersonEntity() {
		final String uuid = "person-uuid";
		
		String json = generateEvictionPayload(PersonModel.class.getName(), uuid, c).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
		assertNull(docContext.read("subResource"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForADeletedPerson() {
		final String uuid = "person-uuid";
		
		String json = generateEvictionPayload(PersonModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertNull(docContext.read("uuid"));
		assertNull(docContext.read("subResource"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForAPatient() {
		final String uuid = "patient-uuid";
		
		String json = generateEvictionPayload(PatientModel.class.getName(), uuid, c).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
		assertNull(docContext.read("subResource"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForADeletedPatient() {
		final String uuid = "patient-uuid";
		
		String json = generateEvictionPayload(PatientModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertNull(docContext.read("uuid"));
		assertNull(docContext.read("subResource"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForAPersonName() {
		final String uuid = "name-uuid";
		
		String json = generateEvictionPayload(PersonNameModel.class.getName(), uuid, c).toString();
		
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("name", JsonPath.read(json, "subResource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForADeletedPersonName() {
		final String uuid = "name-uuid";
		SyncedMessage msg = new SyncedMessage();
		msg.setIdentifier(uuid);
		msg.setOperation(SyncOperation.d);
		msg.setModelClassName(PersonNameModel.class.getName());
		
		String json = generateEvictionPayload(PersonNameModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("name", JsonPath.read(json, "subResource"));
		assertNull(docContext.read("uuid"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForAPersonAttribute() {
		final String uuid = "attribute-uuid";
		
		String json = generateEvictionPayload(PersonAttributeModel.class.getName(), uuid, c).toString();
		
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("attribute", JsonPath.read(json, "subResource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForADeletedPersonAttribute() {
		final String uuid = "attribute-uuid";
		
		String json = generateEvictionPayload(PersonAttributeModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("attribute", JsonPath.read(json, "subResource"));
		assertNull(docContext.read("uuid"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForAPersonAddress() {
		final String uuid = "address-uuid";
		
		String json = generateEvictionPayload(PersonAddressModel.class.getName(), uuid, c).toString();
		
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("address", JsonPath.read(json, "subResource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForADeletedPersonAddress() {
		final String uuid = "address-uuid";
		
		String json = generateEvictionPayload(PersonAddressModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("address", JsonPath.read(json, "subResource"));
		assertNull(docContext.read("uuid"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForAUser() {
		final String uuid = "user-uuid";
		
		String json = generateEvictionPayload(UserModel.class.getName(), uuid, c).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("user", JsonPath.read(json, "resource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
		assertNull(docContext.read("subResource"));
	}
	
	@Test
	public void generateEvictionPayload_shouldGenerateCacheEvictionJsonForADeletedUser() {
		final String uuid = "user-uuid";
		
		String json = generateEvictionPayload(UserModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("user", JsonPath.read(json, "resource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
		assertNull(docContext.read("subResource"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForAPersonEntity() {
		final String personUuid = "person-uuid";
		final String nameUuid1 = "name-uuid-1";
		final String nameUuid2 = "name-uuid-2";
		final String idUuid1 = "id-uuid-1";
		final String idUuid2 = "id-uuid-2";
		final String attribUuid1 = "attrib-uuid-1";
		final String attribUuid2 = "attrib-uuid-2";
		when(mockNameRepo.getPersonNameUuids(personUuid)).thenReturn(asList(nameUuid1, nameUuid2));
		when(mockIdRepo.getPatientIdentifierUuids(personUuid)).thenReturn(asList(idUuid1, idUuid2));
		when(mockAttribRepo.getPersonAttributeUuids(personUuid)).thenReturn(asList(attribUuid1, attribUuid2));
		
		List<String> payloads = (List) generateSearchIndexUpdatePayload(PersonModel.class.getName(), personUuid, c);
		
		assertEquals(6, payloads.size());
		assertEquals("person", JsonPath.read(payloads.get(0), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(0), "subResource"));
		assertEquals(nameUuid1, JsonPath.read(payloads.get(0), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(1), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(1), "subResource"));
		assertEquals(nameUuid2, JsonPath.read(payloads.get(1), "uuid"));
		
		assertEquals("patient", JsonPath.read(payloads.get(2), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(2), "subResource"));
		assertEquals(idUuid1, JsonPath.read(payloads.get(2), "uuid"));
		assertEquals("patient", JsonPath.read(payloads.get(3), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(3), "subResource"));
		assertEquals(idUuid2, JsonPath.read(payloads.get(3), "uuid"));
		
		assertEquals("person", JsonPath.read(payloads.get(4), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(4), "subResource"));
		assertEquals(attribUuid1, JsonPath.read(payloads.get(4), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(5), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(5), "subResource"));
		assertEquals(attribUuid2, JsonPath.read(payloads.get(5), "uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForADeletedPerson() {
		final String personUuid = "person-uuid";
		final String nameUuid1 = "name-uuid-1";
		final String nameUuid2 = "name-uuid-2";
		final String idUuid1 = "id-uuid-1";
		final String idUuid2 = "id-uuid-2";
		final String attribUuid1 = "attrib-uuid-1";
		final String attribUuid2 = "attrib-uuid-2";
		when(mockNameRepo.getPersonNameUuids(personUuid)).thenReturn(asList(nameUuid1, nameUuid2));
		when(mockIdRepo.getPatientIdentifierUuids(personUuid)).thenReturn(asList(idUuid1, idUuid2));
		when(mockAttribRepo.getPersonAttributeUuids(personUuid)).thenReturn(asList(attribUuid1, attribUuid2));
		
		List<String> payloads = (List) generateSearchIndexUpdatePayload(PersonModel.class.getName(), personUuid, d);
		
		assertEquals(6, payloads.size());
		assertEquals("person", JsonPath.read(payloads.get(0), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(0), "subResource"));
		assertEquals(nameUuid1, JsonPath.read(payloads.get(0), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(1), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(1), "subResource"));
		assertEquals(nameUuid2, JsonPath.read(payloads.get(1), "uuid"));
		
		assertEquals("patient", JsonPath.read(payloads.get(2), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(2), "subResource"));
		assertEquals(idUuid1, JsonPath.read(payloads.get(2), "uuid"));
		assertEquals("patient", JsonPath.read(payloads.get(3), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(3), "subResource"));
		assertEquals(idUuid2, JsonPath.read(payloads.get(3), "uuid"));
		
		assertEquals("person", JsonPath.read(payloads.get(4), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(4), "subResource"));
		assertEquals(attribUuid1, JsonPath.read(payloads.get(4), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(5), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(5), "subResource"));
		assertEquals(attribUuid2, JsonPath.read(payloads.get(5), "uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForAPatient() {
		final String patientUuid = "patient-uuid";
		final String nameUuid1 = "name-uuid-1";
		final String nameUuid2 = "name-uuid-2";
		final String idUuid1 = "id-uuid-1";
		final String idUuid2 = "id-uuid-2";
		final String attribUuid1 = "attrib-uuid-1";
		final String attribUuid2 = "attrib-uuid-2";
		when(mockNameRepo.getPersonNameUuids(patientUuid)).thenReturn(asList(nameUuid1, nameUuid2));
		when(mockIdRepo.getPatientIdentifierUuids(patientUuid)).thenReturn(asList(idUuid1, idUuid2));
		when(mockAttribRepo.getPersonAttributeUuids(patientUuid)).thenReturn(asList(attribUuid1, attribUuid2));
		
		List<String> payloads = (List) generateSearchIndexUpdatePayload(PatientModel.class.getName(), patientUuid, c);
		
		assertEquals(6, payloads.size());
		assertEquals("person", JsonPath.read(payloads.get(0), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(0), "subResource"));
		assertEquals(nameUuid1, JsonPath.read(payloads.get(0), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(1), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(1), "subResource"));
		assertEquals(nameUuid2, JsonPath.read(payloads.get(1), "uuid"));
		
		assertEquals("patient", JsonPath.read(payloads.get(2), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(2), "subResource"));
		assertEquals(idUuid1, JsonPath.read(payloads.get(2), "uuid"));
		assertEquals("patient", JsonPath.read(payloads.get(3), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(3), "subResource"));
		assertEquals(idUuid2, JsonPath.read(payloads.get(3), "uuid"));
		
		assertEquals("person", JsonPath.read(payloads.get(4), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(4), "subResource"));
		assertEquals(attribUuid1, JsonPath.read(payloads.get(4), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(5), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(5), "subResource"));
		assertEquals(attribUuid2, JsonPath.read(payloads.get(5), "uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForADeletedPatient() {
		final String patientUuid = "patient-uuid";
		final String nameUuid1 = "name-uuid-1";
		final String nameUuid2 = "name-uuid-2";
		final String idUuid1 = "id-uuid-1";
		final String idUuid2 = "id-uuid-2";
		final String attribUuid1 = "attrib-uuid-1";
		final String attribUuid2 = "attrib-uuid-2";
		SyncedMessage msg = new SyncedMessage();
		msg.setIdentifier(patientUuid);
		msg.setModelClassName(PatientModel.class.getName());
		when(mockNameRepo.getPersonNameUuids(patientUuid)).thenReturn(asList(nameUuid1, nameUuid2));
		when(mockIdRepo.getPatientIdentifierUuids(patientUuid)).thenReturn(asList(idUuid1, idUuid2));
		when(mockAttribRepo.getPersonAttributeUuids(patientUuid)).thenReturn(asList(attribUuid1, attribUuid2));
		
		List<String> payloads = (List) generateSearchIndexUpdatePayload(PatientModel.class.getName(), patientUuid, d);
		
		assertEquals(6, payloads.size());
		assertEquals("person", JsonPath.read(payloads.get(0), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(0), "subResource"));
		assertEquals(nameUuid1, JsonPath.read(payloads.get(0), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(1), "resource"));
		assertEquals("name", JsonPath.read(payloads.get(1), "subResource"));
		assertEquals(nameUuid2, JsonPath.read(payloads.get(1), "uuid"));
		
		assertEquals("patient", JsonPath.read(payloads.get(2), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(2), "subResource"));
		assertEquals(idUuid1, JsonPath.read(payloads.get(2), "uuid"));
		assertEquals("patient", JsonPath.read(payloads.get(3), "resource"));
		assertEquals("identifier", JsonPath.read(payloads.get(3), "subResource"));
		assertEquals(idUuid2, JsonPath.read(payloads.get(3), "uuid"));
		
		assertEquals("person", JsonPath.read(payloads.get(4), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(4), "subResource"));
		assertEquals(attribUuid1, JsonPath.read(payloads.get(4), "uuid"));
		assertEquals("person", JsonPath.read(payloads.get(5), "resource"));
		assertEquals("attribute", JsonPath.read(payloads.get(5), "subResource"));
		assertEquals(attribUuid2, JsonPath.read(payloads.get(5), "uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForAPersonName() {
		final String uuid = "name-uuid";
		
		String json = generateSearchIndexUpdatePayload(PersonNameModel.class.getName(), uuid, c).toString();
		
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("name", JsonPath.read(json, "subResource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForADeletedPersonName() {
		final String uuid = "name-uuid";
		
		String json = generateSearchIndexUpdatePayload(PersonNameModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("name", JsonPath.read(json, "subResource"));
		assertNull(docContext.read("uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForAPersonAttribute() {
		final String uuid = "attribute-uuid";
		
		String json = generateSearchIndexUpdatePayload(PersonAttributeModel.class.getName(), uuid, c).toString();
		
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("attribute", JsonPath.read(json, "subResource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForADeletedPersonAttribute() {
		final String uuid = "attribute-uuid";
		
		String json = generateSearchIndexUpdatePayload(PersonAttributeModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("person", JsonPath.read(json, "resource"));
		assertEquals("attribute", JsonPath.read(json, "subResource"));
		assertNull(docContext.read("uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForAPatientIdentifier() {
		final String uuid = "id-uuid";
		
		String json = generateSearchIndexUpdatePayload(PatientIdentifierModel.class.getName(), uuid, c).toString();
		
		assertEquals("patient", JsonPath.read(json, "resource"));
		assertEquals("identifier", JsonPath.read(json, "subResource"));
		assertEquals(uuid, JsonPath.read(json, "uuid"));
	}
	
	@Test
	public void generateSearchIndexUpdatePayload_shouldGenerateSearchIndexUpdateJsonForADeletedPatientIdentifier() {
		final String uuid = "id-uuid";
		
		String json = generateSearchIndexUpdatePayload(PatientIdentifierModel.class.getName(), uuid, d).toString();
		
		DocumentContext docContext = jsonPathContext.parse(json);
		assertEquals("patient", JsonPath.read(json, "resource"));
		assertEquals("identifier", JsonPath.read(json, "subResource"));
		assertNull(docContext.read("uuid"));
	}
	
	@Test
	public void isSubclass_shouldReturnTrueForAModelClassNameForASubclass() {
		assertTrue(ReceiverUtils.isSubclass(PatientModel.class.getName()));
		assertTrue(ReceiverUtils.isSubclass(DrugOrderModel.class.getName()));
		assertTrue(ReceiverUtils.isSubclass(TestOrderModel.class.getName()));
	}
	
	@Test
	public void isSubclass_shouldReturnFalseForAModelClassNameForANonSubclass() {
		assertFalse(ReceiverUtils.isSubclass(PersonModel.class.getName()));
		assertFalse(ReceiverUtils.isSubclass(OrderModel.class.getName()));
		assertFalse(ReceiverUtils.isSubclass(VisitModel.class.getName()));
	}
	
	@Test
	public void getParentModelClassName_shouldReturnForAParentClassName() {
		assertEquals(PersonModel.class.getName(), ReceiverUtils.getParentModelClassName(PatientModel.class.getName()));
		assertEquals(OrderModel.class.getName(), ReceiverUtils.getParentModelClassName(DrugOrderModel.class.getName()));
		assertEquals(OrderModel.class.getName(), ReceiverUtils.getParentModelClassName(TestOrderModel.class.getName()));
	}
	
	@Test
	public void getParentModelClassName_shouldFailForAClassNameWithNoParent() {
		final String modelClass = VisitModel.class.getName();
		Exception thrown = Assert.assertThrows(EIPException.class, () -> ReceiverUtils.getParentModelClassName(modelClass));
		assertEquals("No parent class found for model class: " + modelClass, thrown.getMessage());
	}
	
	@Test
	public void getParentModelClassName_shouldFailForAClassNameWithNoParentWithSubclasses() {
		final String modelClass = PersonModel.class.getName();
		Exception thrown = Assert.assertThrows(EIPException.class, () -> ReceiverUtils.getParentModelClassName(modelClass));
		assertEquals("No parent class found for model class: " + modelClass, thrown.getMessage());
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromAConflictForACachedAndIndexedEntity() throws Exception {
		ConflictQueueItem conflict = new ConflictQueueItem();
		conflict.setModelClassName(PersonModel.class.getName());
		conflict.setMessageUuid("message-uuid");
		long timestamp = System.currentTimeMillis();
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(conflict);
		
		assertNull(msg.getId());
		assertTrue(msg.getDateCreated().getTime() == timestamp || msg.getDateCreated().getTime() > timestamp);
		assertEquals(conflict.getDateCreated(), msg.getDateReceived());
		assertTrue(msg.isResponseSent());
		assertTrue(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
		assertTrue(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
		assertEquals(SUCCESS, msg.getOutcome());
		PowerMockito.verifyStatic(BeanUtils.class);
		BeanUtils.copyProperties(conflict, msg, "id", "dateCreated");
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromAConflictForACachedButNotIndexedEntity() {
		ConflictQueueItem conflict = new ConflictQueueItem();
		conflict.setModelClassName(PersonAddressModel.class.getName());
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(conflict);
		
		assertEquals(SUCCESS, msg.getOutcome());
		assertTrue(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
		assertFalse(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
	}
	
	@Test
	public void createSyncedMessage_shouldCreateASyncedMessageFromAConflictForAnIndexedButNotCachedEntity() {
		ConflictQueueItem conflict = new ConflictQueueItem();
		conflict.setModelClassName(PatientIdentifierModel.class.getName());
		
		SyncedMessage msg = ReceiverUtils.createSyncedMessage(conflict);
		
		assertEquals(SUCCESS, msg.getOutcome());
		assertTrue(msg.isIndexed());
		assertFalse(msg.isSearchIndexUpdated());
		assertFalse(msg.isCached());
		assertFalse(msg.isEvictedFromCache());
	}
	
}
