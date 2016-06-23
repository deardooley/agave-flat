package org.iplantc.service.metadata.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.iplantc.service.metadata.model.validation.constraints.ValidJsonSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;

public class ValidJsonSchemaValidator implements ConstraintValidator<ValidJsonSchema, Object> {
    
    @Override
    public void initialize(final ValidJsonSchema constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object target, final ConstraintValidatorContext constraintContext) {
        
        ObjectMapper mapper = new ObjectMapper();
        
        boolean isValid = false;
        
        if (target == null) {
            return true;
        }
        
        try 
        {
            JsonNode schemaNode = null;
            
            if (target instanceof String) {
                schemaNode = mapper.readTree((String) target);
            } 
            
            SyntaxValidator validator = JsonSchemaFactory.byDefault().getSyntaxValidator();

            isValid = validator.schemaIsValid(schemaNode);
        } 
        catch(Throwable e) {
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate( 
                        "The supplied JSON Schema definition is invalid. " +
                        "For more information on JSON Schema, please visit http://json-schema.org/")
                    .addConstraintViolation();
        }
        
        return isValid;
    }
}
