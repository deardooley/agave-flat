package org.teragrid.service.tgcdb.dto;

import org.json.JSONString;

public interface TgcdbDTO extends JSONString {

	public String toHtml();

	public String toCsv();

	public String toPerl();

}
