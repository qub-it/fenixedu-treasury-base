package org.fenixedu.commons.i18n;

import com.google.gson.JsonParser;

public class LocalizedStringSerializer {

	public static String externalize(final LocalizedString value) {
		return value.json().getAsString();
	}
	
	public static LocalizedString internalize(final String value) {
		return LocalizedString.fromJson(new JsonParser().parse(value));
	}
	
}
