package org.openmrs.eip.app.management.entity.receiver;

import java.time.LocalDateTime;

import org.openmrs.eip.app.SyncOperationConverter;
import org.openmrs.eip.app.management.entity.AbstractEntity;
import org.openmrs.eip.component.SyncOperation;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Encapsulates info about a sync message received by the receiver
 */
@Entity
@Table(name = "receiver_sync_msg")
public class SyncMessage extends AbstractEntity {
	
	public static final long serialVersionUID = 1;
	
	//Unique identifier for the entity usually a uuid or name for an entity like a privilege that has no uuid
	@NotNull
	@Column(nullable = false, updatable = false)
	private String identifier;
	
	@NotNull
	@Column(name = "entity_payload", columnDefinition = "text", nullable = false, updatable = false)
	private String entityPayload;
	
	@NotNull
	@Column(name = "model_class_name", nullable = false, updatable = false)
	private String modelClassName;
	
	@NotNull
	@Column(nullable = false, updatable = false, length = 1)
	@Convert(converter = SyncOperationConverter.class)
	private SyncOperation operation;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "site_id", nullable = false, updatable = false)
	private SiteInfo site;
	
	@NotNull
	@Column(name = "is_snapshot", nullable = false, updatable = false)
	private Boolean snapshot = false;
	
	@Column(name = "message_uuid", length = 38, updatable = false)
	private String messageUuid;
	
	@NotNull
	@Column(name = "date_sent_by_sender", nullable = false, updatable = false)
	private LocalDateTime dateSentBySender;
	
	/**
	 * Gets the identifier
	 *
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	/**
	 * Sets the identifier
	 *
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * Gets the entityPayload
	 *
	 * @return the entityPayload
	 */
	public String getEntityPayload() {
		return entityPayload;
	}
	
	/**
	 * Sets the entityPayload
	 *
	 * @param entityPayload the entityPayload to set
	 */
	public void setEntityPayload(String entityPayload) {
		this.entityPayload = entityPayload;
	}
	
	/**
	 * Gets the modelClassName
	 *
	 * @return the modelClassName
	 */
	public String getModelClassName() {
		return modelClassName;
	}
	
	/**
	 * Sets the modelClassName
	 *
	 * @param modelClassName the modelClassName to set
	 */
	public void setModelClassName(String modelClassName) {
		this.modelClassName = modelClassName;
	}
	
	/**
	 * Gets the operation
	 *
	 * @return the operation
	 */
	public SyncOperation getOperation() {
		return operation;
	}
	
	/**
	 * Sets the operation
	 *
	 * @param operation the operation to set
	 */
	public void setOperation(SyncOperation operation) {
		this.operation = operation;
	}
	
	/**
	 * Gets the site
	 *
	 * @return the site
	 */
	public SiteInfo getSite() {
		return site;
	}
	
	/**
	 * Sets the site
	 *
	 * @param site the site to set
	 */
	public void setSite(SiteInfo site) {
		this.site = site;
	}
	
	/**
	 * Gets the snapshot
	 *
	 * @return the snapshot
	 */
	public Boolean getSnapshot() {
		return snapshot;
	}
	
	/**
	 * Sets the snapshot
	 *
	 * @param snapshot the snapshot to set
	 */
	public void setSnapshot(Boolean snapshot) {
		this.snapshot = snapshot;
	}
	
	public String getMessageUuid() {
		return messageUuid;
	}
	
	public void setMessageUuid(String messageUuid) {
		this.messageUuid = messageUuid;
	}
	
	/**
	 * Gets the dateSentBySender
	 *
	 * @return the dateSentBySender
	 */
	public LocalDateTime getDateSentBySender() {
		return dateSentBySender;
	}
	
	/**
	 * Sets the dateSentBySender
	 *
	 * @param dateSentBySender the dateSentBySender to set
	 */
	public void setDateSentBySender(LocalDateTime dateSentBySender) {
		this.dateSentBySender = dateSentBySender;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " {id=" + getId() + ", identifier=" + identifier + ", modelClassName="
		        + modelClassName + ", operation=" + operation + ", site=" + site + ", snapshot=" + snapshot
		        + ", messageUuid=" + messageUuid + ", dateSentBySender=" + dateSentBySender + "}";
	}
	
}
