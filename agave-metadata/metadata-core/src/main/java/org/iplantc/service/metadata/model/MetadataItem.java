/**
 *
 */
package org.iplantc.service.metadata.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.model.validation.constraints.MetadataSchemaComplianceConstraint;
import org.iplantc.service.notification.model.Notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.DBObject;

/**
 * @author dooley
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@MetadataSchemaComplianceConstraint(valueField="value",
  schemaIdField="schemaId",
  message="The value does not comply with the provided metadata schema")
public class MetadataItem {

    @Id
    private String _id;

    @NotNull(message="No name attribute specified. Please provide a valid name for this metadata item.")
    @NotEmpty(message="Empty name attribute specified. Please provide a valid name for this metadata item.")
    @Length(min=1,max=256)
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private String name;

    @Length(max=16384, message="Metadata value must be less than 16385")
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private JsonNode value;

    @Length(max=64, message="Metadata schemaId must be a valid schema uuid")
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private String schemaId;

    @NotNull
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private String tenantId;

    @NotNull
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private String internalUsername;

    @NotNull
    @Length(min=1,max=32, message="Metadata owner must be less than 33 characters.")
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private String owner;

    @Length(max=64, message="Metadata uuid must be a valid uuid.")
    @NotNull
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private String uuid;

    @Past
    @NotNull
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private Date created;

    @NotNull
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private Date lastUpdated;

    @JsonInclude(Include.NON_NULL)
    private String error;

    @JsonView({MetadataViews.Resource.Summary.class})
    private String _links;

    @JsonUnwrapped
    @JsonView({MetadataViews.Resource.Summary.class, MetadataViews.Request.class})
    private MetadataAssociationList associations = new MetadataAssociationList();

    @JsonView({MetadataViews.Resource.ACL.class, MetadataViews.Request.class})
    private List<PermissionGrant> acl = new ArrayList<PermissionGrant>();

    @JsonIgnore
    @JsonView({MetadataViews.Resource.Notifications.class, MetadataViews.Request.class})
    private List<Notification> notifications = new ArrayList<Notification>();

    public MetadataItem() {
        this.uuid = new AgaveUUID(UUIDType.METADATA).toString();
        this.tenantId = TenancyHelper.getCurrentTenantId();
        this.created = new Date();
        this.lastUpdated = new Date();
    }

//    public MetadataItem(DBObject mongoObj) {
//        this();
//        if (mongoObj == null || mongoObj.size() == 0) {
//            return;
//        } else {
//            setUuid(mongoObj.getString("uuid"));
//            setName(mongoObj.getString("name"));
//            setValue(mongoObj.get("value").toString());
//            setSchemaId(mongoObj.getString("schemaId"));
//            setTenantId(mongoObj.getString("tenantId"));
//            setOwner(mongoObj.getString("owner"));
//            setUuid(mongoObj.getString("uuid"));
//            MetadataAssociationList obj = new MetadataAssociationList(mongoObj.get("associatedUuids"));
//            this.associations
//            setAssociatedUuids();
//            setCreated(new DateTime(mongoObj.getString("created")).toDate());
//            setLastUpdated(new DateTime(mongoObj.getString("lastUpdated")).toDate());
//        }
//    }

    /**
     * @return the name
     */
    public synchronized String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public synchronized void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public synchronized JsonNode getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public synchronized void setValue(JsonNode value) {
        this.value = value;
    }

    /**
     * @return the schemaId
     */
    public synchronized String getSchemaId() {
        return schemaId;
    }

    /**
     * @param schemaId the schemaId to set
     */
    public synchronized void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    /**
     * @return the tenantId
     */
    public synchronized String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId the tenantId to set
     */
    public synchronized void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * @return the owner
     */
    public synchronized String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public synchronized void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the internalUsername
     */
    public String getInternalUsername() {
        return internalUsername;
    }

    /**
     * @param internalUsername the internalUsername to set
     */
    public void setInternalUsername(String internalUsername) {
        this.internalUsername = internalUsername;
    }

    /**
     * @return the uuid
     */
    public synchronized String getUuid() {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public synchronized void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the error
     */
    public synchronized String getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public synchronized void setError(String error) {
        this.error = error;
    }

    /**
     * @return the associatedUuids
     */
    public synchronized MetadataAssociationList getAssociations() {
        return associations;
    }

    /**
     * @param associatedUuids the associatedUuids to set
     */
    public synchronized void setAssociations(MetadataAssociationList associations) {
        this.associations = associations;
    }

    /**
     * @return the permissions
     */
    public synchronized List<PermissionGrant> getAcl() {
        return acl;
    }

    /**
     * @param permissions the permissions to set
     */
    public synchronized void setAcl(List<PermissionGrant> acl) {
        this.acl = acl;
    }

    public synchronized void addPermissionGrant(PermissionGrant permissionGrant) {
        this.acl.add(permissionGrant);
    }

    /**
     * @return the notifications
     */
    public synchronized List<Notification> getNotifications() {
        return notifications;
    }

    /**
     * @param notifications the notifications to set
     */
    public synchronized void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    /**
     * @return the created
     */
    public synchronized Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public synchronized void setCreated(Date created) {
        this.created = created;
    }

    /**
     * @return the lastUpdated
     */
    public synchronized Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public synchronized void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

//    @JsonIgnore
//    public ObjectNode toObjectNode() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JodaModule());
//
//        ObjectNode json = mapper.createObjectNode()
//                .put("name", getName())
//                .put("value", getValue())
//                .put("schemaId", getSchemaId())
//                .put("owner", getOwner())
//                .put("uuid", getUuid())
//                .putPOJO("created", getCreated())
//                .putPOJO("lastUpdated", getLastUpdated())
//                .putPOJO("associatedUuids", getAssociatedIds());
//
//        ObjectNode hal = mapper.createObjectNode();
//        hal.put("self", mapper.createObjectNode().put("href",
//                TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE + "data/" + getUuid())));
//
//        if (!getAssociatedUuids().isEmpty())
//        {
//            hal.putAll(getAssociatedUuids().getReferenceGroupMap());
//        }
//
//        if (StringUtils.isNotEmpty(getSchemaId()))
//        {
//            hal.put(UUIDType.SCHEMA.name().toLowerCase(),
//                    mapper.createObjectNode().put("href",
//                            TenancyHelper.resolveURLToCurrentTenant(getSchemaId())));
//        }
//        json.put("_links", hal);
//
//        return json;
//    }

//    @JsonValue
//    public String toString() {
//        return toObjectNode().toString();
//    }

//    public DBObject toDBObject() {
//        DBObject json = new BasicDBObject()
//                .append("name", getName())
//                .append("value", getValue())
//                .append("schemaId", getSchemaId())
//                .append("owner", getOwner())
//                .append("uuid", getUuid())
//                .append("tenantId", getTenantId())
//                .append("created", new DateTime(getCreated()).toString())
//                .append("lastUpdated", new DateTime(getLastUpdated()).toString())
//                .append("associatedUuids", new BasicDBList().addAll(getAssociatedUuids().getRawUuid()));
//        return json;
//    }

}
