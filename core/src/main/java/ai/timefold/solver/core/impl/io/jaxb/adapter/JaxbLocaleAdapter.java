package ai.timefold.solver.core.impl.io.jaxb.adapter;

import java.util.Locale;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class JaxbLocaleAdapter extends XmlAdapter<String, Locale> {

    @Override
    public Locale unmarshal(String localeString) {
        if (localeString == null) {
            return null;
        }
        return Locale.forLanguageTag(localeString);
    }

    @Override
    public String marshal(Locale locale) {
        if (locale == null) {
            return null;
        }
        return locale.toLanguageTag();
    }
}
