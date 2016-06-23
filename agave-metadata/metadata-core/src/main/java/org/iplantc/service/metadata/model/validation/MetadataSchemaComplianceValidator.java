package org.iplantc.service.metadata.model.validation;

import java.util.Iterator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.dao.MetadataSchemaDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.exceptions.MetadataValidationException;
import org.iplantc.service.metadata.managers.MetadataSchemaPermissionManager;
import org.iplantc.service.metadata.model.MetadataSchemaItem;
import org.iplantc.service.metadata.model.validation.constraints.MetadataSchemaComplianceConstraint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;

public class MetadataSchemaComplianceValidator implements ConstraintValidator<MetadataSchemaComplianceConstraint, Object> {

    private static final Logger log = Logger.getLogger(MetadataSchemaComplianceValidator.class);
    private String valueFieldName;
    private String schemaFieldName;

    @Override
    public void initialize(final MetadataSchemaComplianceConstraint constraintAnnotation) {
        valueFieldName = constraintAnnotation.valueField();
        schemaFieldName = constraintAnnotation.schemaIdField();
    }

    @Override
    public boolean isValid(Object target, final ConstraintValidatorContext constraintContext) {
        
//        boolean isValid = false;
//        
//        try {
//            final Object metadataValue = BeanUtils.getProperty(target, valueFieldName);
//            final Object schemaId = BeanUtils.getProperty(target, schemaFieldName);
//            
//            if (StringUtils.isEmpty((String)schemaId)) {
//                isValid = true;
//            } 
//            else {
                
//                MetadataSchemaItem schemaItem = MetadataSchemaDao.getInstance().findByUuidAndTenant((String)schemaId, TenancyHelper.getCurrentTenantId());
//                if (schemaItem == null) {
//                    isValid = true;
//                }
//                else 
//                {
//                    MetadataSchemaPermissionManager schemaPM = new MetadataSchemaPermissionManager((String)schemaId, schemaItem.getOwner());
//                    if (schemaPM.canRead(TenancyHelper.getCurrentTenantId())) 
//                    {
//                        ObjectMapper mapper = new ObjectMapper();
//                        
//                        JsonNode jsonSchemaNode = mapper.valueToTree(schemaItem);
//                        
//                        JsonNode metadataValueNode = null;
//                        if (metadataValue instanceof String) {
//                            metadataValueNode = mapper.readTree((String)metadataValue);
//                        } else if (!(metadataValue instanceof JsonNode)) {
//                            metadataValueNode = mapper.valueToTree(metadataValue);
//                        } else {
//                            metadataValueNode = (JsonNode)metadataValue;
//                        }
//                        
//                        JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
//                        ProcessingReport report = validator.validate(jsonSchemaNode, metadataValueNode);
//                        
//                        isValid = report.isSuccess();
//                        
//                        if (!isValid) 
//                        {   
//                            StringBuilder sb = new StringBuilder();
//                            for (Iterator<ProcessingMessage> reportMessageIterator = report.iterator(); reportMessageIterator.hasNext();) {
//                                sb.append(reportMessageIterator.next().toString() + "\n"); 
//                            }
//                            throw new MetadataValidationException(
//                                    "Metadata value does not conform to schema. \n" + sb.toString());
//                        }
//                    }
//                    else {
//                        throw new PermissionException("User does not have permission to read metadata schema");
//                    }
//                }       
//            } 
//        }
//        catch (MetadataValidationException | PermissionException e) {
//            log.error("Failed to validate metadata against schema", e);
//            constraintContext.disableDefaultConstraintViolation();
//            constraintContext.buildConstraintViolationWithTemplate( 
//                        e.getMessage())
//                    .addConstraintViolation();
//        }
//        catch(MetadataException e) {
//            log.error("Failed to fetch metadata schema permissions", e);
//            constraintContext.disableDefaultConstraintViolation();
//            constraintContext.buildConstraintViolationWithTemplate( 
//                        "Unable to fetch metadata schema permissions")
//                    .addConstraintViolation();
//        }
//        catch (Exception e) {
//            log.error("Unexpected error while validating metadata value against schema.", e);
//            constraintContext.disableDefaultConstraintViolation();
//            constraintContext.buildConstraintViolationWithTemplate( 
//                        "Unexpected error while validating metadata value against schema.")
//                    .addConstraintViolation();
//        }
//        
//        return isValid;
                return true;
    }
}
