package org.iplantc.service.jobs.model.enumerations;

public enum JobDependencyOperator
{
	GT, GTE, 
	LT, LTE, 
	EQ, NE, 
	IS_NULL, IS_NOT_NULL, 
	IN, NOT_IN, 
	AFTER, BEFORE, 
	BEGINS_WITH, ENDS_WITH, 
	CONTAINS, DOES_NOT_CONTAIN, 
	LIKE, NOT_LIKE;

}
