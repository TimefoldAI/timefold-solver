package ai.timefold.solver.core.impl.io.jaxb;

import java.util.Locale;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class JaxbLocaleAdapter extends XmlAdapter<String, Locale> {

    @Override
    public @Nullable Locale unmarshal(@Nullable String localeString) {
        if (localeString == null) {
            return null;
        }
        return Locale.forLanguageTag(localeString);
    }

    @Override
    public @Nullable String marshal(@Nullable Locale locale) {
        if (locale == null) {
            return null;
        }
        return locale.toLanguageTag();
    }
}
