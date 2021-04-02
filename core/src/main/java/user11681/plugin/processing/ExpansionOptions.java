package user11681.plugin.processing;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import user11681.plugin.ElementUtil;

@SuppressWarnings({"unchecked", "UnusedReturnValue"})
public class ExpansionOptions {
    protected final Map<String, List<String>> types = new HashMap<>();

    protected boolean all;
    protected boolean serialize;

    public boolean shouldExpand(Element type) {
        return this.all || this.types.containsKey(ElementUtil.name(type));
    }

    public boolean shouldExpand(Element type, String element) {
        if (this.all) {
            return true;
        }

        List<String> elements = this.types.get(ElementUtil.name(type));

        return elements != null && (elements.isEmpty() || elements.contains(element));
    }

    public boolean shouldExpand(String type) {
        return this.types.containsKey(type);
    }

    public boolean shouldExpand(String type, String element) {
        return this.all || this.types.containsKey(type) && this.types.get(type).contains(element);
    }

    public boolean shouldSerialize() {
        return this.serialize;
    }

    public ExpansionOptions all() {
        this.all = true;

        return this;
    }

    public ExpansionOptions serialize() {
        this.serialize = true;

        return this;
    }

    public ExpansionOptions expand(Class<Annotation>... types) {
        for (Class<Annotation> type : types) {
            this.expand(type, new String[0]);
        }

        return this;
    }

    public ExpansionOptions expand(Class<Annotation> type, String... elements) {
        this.expand(type.getCanonicalName(), elements);

        return this;
    }

    public ExpansionOptions expand(TypeElement... types) {
        for (TypeElement type : types) {
            this.expand(type, new String[0]);
        }

        return this;
    }

    public ExpansionOptions expand(TypeElement type, String... elements) {
        this.expand(type.getQualifiedName().toString(), elements);

        return this;
    }

    public ExpansionOptions expand(String... types) {
        for (String type : types) {
            this.expand(type, new String[0]);
        }

        return this;
    }

    public ExpansionOptions expand(String type, String... elements) {
        Collections.addAll(this.types.computeIfAbsent(type, ignored -> new ArrayList<>()), elements);

        return this;
    }
}
