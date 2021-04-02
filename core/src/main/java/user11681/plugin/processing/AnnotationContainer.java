package user11681.plugin.processing;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import user11681.plugin.AnnotationRequirementException;
import user11681.plugin.processing.annotation.Alias;
import user11681.plugin.processing.annotation.CompatibleWith;
import user11681.plugin.processing.annotation.Expandable;
import user11681.plugin.processing.annotation.Flat;
import user11681.plugin.processing.annotation.IncompatibleWith;
import user11681.plugin.processing.annotation.Key;
import user11681.plugin.processing.annotation.LazyArray;
import user11681.plugin.processing.annotation.Pair;
import user11681.plugin.processing.annotation.Requires;
import user11681.plugin.processing.annotation.Wrapper;
import user11681.plugin.processing.transformer.AnnotationTransformer;
import user11681.plugin.processing.transformer.ArrayTransformer;
import user11681.plugin.processing.transformer.EnumTransformer;
import user11681.plugin.processing.transformer.Transformer;
import user11681.plugin.processing.transformer.TypeTransformer;

@SuppressWarnings("unchecked")
public class AnnotationContainer {
    public static final List<Transformer> transformers = new ArrayList<>(Arrays.asList(
        AnnotationTransformer.instance,
        ArrayTransformer.instance,
        EnumTransformer.instance,
        TypeTransformer.instance
    ));

    public final AnnotationMirror mirror;
    public final TypeElement type;
    public final Element element;
    public final Map<String, ExecutableElement> elements = new LinkedHashMap<>();

    public AnnotationContainer(TypeElement element, AnnotationMirror mirror) {
        this.mirror = mirror;
        this.type = (TypeElement) mirror.getAnnotationType().asElement();
        this.element = element;

        for (Element member : this.type.getEnclosedElements()) {
            if (member instanceof ExecutableElement) {
                this.elements.put(member.getSimpleName().toString(), (ExecutableElement) member);
            }
        }

        this.checkRequirements();
    }

    public static Map<Object, Object> flatten(List<Map<Object, Object>> list, String keyName, String valueName) {
        Map<Object, Object> flatMap = new LinkedHashMap<>(list.size(), 1);

        for (Map<Object, Object> entry : list) {
            flatMap.put(entry.get(keyName), entry.get(valueName));
        }

        return flatMap;
    }

    public static Object getValue(ExpansionOptions options, AnnotationValue annotationValue) {
        Object value = annotationValue.getValue();

        for (Transformer transformer : transformers) {
            Optional<Object> newValue = transformer.transform(options, value);

            if (newValue.isPresent()) {
                return newValue.get();
            }
        }

        return value;
    }

    protected static boolean expand(ExpansionOptions options, Element elmement, ExecutableElement element) {
        return options.shouldExpand(elmement, element.getSimpleName().toString()) || element.getAnnotation(Expandable.class) != null || elmement.getAnnotation(Expandable.class) != null;
    }

    public boolean isType(Class<? extends Annotation> type) {
        return this.element instanceof TypeElement && type.getCanonicalName().equals(((TypeElement) this.element).getQualifiedName().toString());
    }

    protected static <A extends Annotation> void handleAnnotation(Element element, Class<A> type, Consumer<A> handler) {
        A annotation = element.getAnnotation(type);

        if (annotation != null) {
            handler.accept(annotation);
        }
    }

    public <T> T elements(ExpansionOptions options) {
        final Map<ExecutableElement, AnnotationValue> elements = (Map<ExecutableElement, AnnotationValue>) this.mirror.getElementValues();

        for (ExecutableElement element : this.elements.values()) {
            if (!elements.containsKey(element) && expand(options, this.element, element)) {
                elements.put(element, element.getDefaultValue());
            }
        }

        final Container<LinkedHashMap<String, Object>> newElements = new Container<>(new LinkedHashMap<>(elements.size()));

        for (Map.Entry<ExecutableElement, AnnotationValue> entry : elements.entrySet()) {
            ExecutableElement element = entry.getKey();

            final Container<Object> value = new Container<>(getValue(options, entry.getValue()));

            handleAnnotation(element, Pair.class, (Pair pair) -> {
                List<LinkedHashMap<String, Object>> values;

                if (value.cast() instanceof List) {
                    values = value.cast();
                } else {
                    values = new ArrayList<>();
                    values.add(value.cast());
                }

                for (LinkedHashMap<String, Object> annotation : values) {
                    annotation.put((String) annotation.remove(pair.key()), annotation.remove(pair.value()));
                }
            });

/*
            handleAnnotation(element, Merge.class, (Merge merge) -> {
                if (this.element != null) {
                    if (this.element.getAnnotation(Repeatable.class) != null) {

                    } else if (element.getReturnType().getAnnotation(Repeatable.class) != null) {
                        if (element.getSimpleName().contentEquals("value")) {
//                        Map<String, List<Object>> flattened = new LinkedHashMap<>();
                            List<Object> flattened = new ArrayList<>();

                            for (Map<String, Object> repeatableAnnotation : (List<Map<String, Object>>) value.cast()) {
                                for (Map.Entry<String, Object> repeatableEntry : repeatableAnnotation.entrySet()) {
                                    if ()
//                                Object repeatableValue = repeatableEntry.getValue();
//                                String repeatableKey = repeatableEntry.getKey();
//
//                                if (!merge.nest() && repeatableValue instanceof List) {
//                                    if (!flattened.containsKey(repeatableKey)) {
//                                        flattened.put(repeatableKey, (List<Object>) repeatableValue);
//                                    } else {
//                                        flattened.get(repeatableKey).addAll((List<Object>) repeatableValue);
//                                    }
//                                } else {
//                                    flattened.computeIfAbsent(repeatableKey, ignored -> new ArrayList<>()).add(repeatableValue);
//                                }
                                }
                            }

                            value.set(flattened);
                        }
                    } else if (merge.forceArray()) {
                        value.set(new ArrayList<>(Collections.singletonList(value.cast())));
                    }
                } else if (merge.forceArray()) {
                    value.set(new ArrayList<>(Collections.singletonList(value.cast())));
                }
            });
*/

            handleAnnotation(element, Key.class, (Key key) -> value.set(Map.of(key.value(), value.cast())));

            handleAnnotation(element, LazyArray.class, (LazyArray array) -> {
                List<Object> list = value.cast();

                if (list.size() == 1) {
                    value.set(list.get(0));
                }
            });

            handleAnnotation(element, Flat.class, (Flat flat) -> {
                if (flat != null) {
                    value.set(flatten(value.cast(), flat.key(), flat.value()));
                }
            });

            Alias alias = element.getAnnotation(Alias.class);

            newElements.get().put(alias == null ? element.getSimpleName().toString() : alias.value(), value.cast());
        }

        if (this.element != null) {
            handleAnnotation(this.element, Pair.class, (Pair pair) -> newElements.get().put((String) newElements.get().remove(pair.key()), newElements.get().remove(pair.value())));

            handleAnnotation(this.element, Key.class, (Key key) -> {
                var newElementsCopy = newElements.get();

                newElements.set(new LinkedHashMap<>());
                newElements.get().put(key.value(), newElementsCopy);
            });

            Wrapper wrapper = this.element.getAnnotation(Wrapper.class);

            if (wrapper != null) {
                Set<Map.Entry<String, Object>> entries = newElements.get().entrySet();

                if (newElements.get().size() != 1) {
                    List<Object> values = new ArrayList<>();

                    for (Map.Entry<String, Object> entry : entries) {
                        values.add(entry.getValue());
                    }

                    return (T) values;
                }

                return (T) entries.iterator().next().getValue();
            }
        }

        return newElements.cast();
    }

    protected void checkRequirements() {
        if (this.element != null) {
            DeclaredType annotationType = this.mirror.getAnnotationType();
            CompatibleWith compatibleWith = annotationType.getAnnotation(CompatibleWith.class);
            IncompatibleWith incompatibleWith = annotationType.getAnnotation(IncompatibleWith.class);
            Requires requires = annotationType.getAnnotation(Requires.class);

            Function<Class<?>, String> toCanonicalName = Class::getCanonicalName;

            Set<String> compatible = compatibleWith == null ? null : Arrays.stream(compatibleWith.value()).map(toCanonicalName).collect(Collectors.toUnmodifiableSet());
            Set<String> incompatible = incompatibleWith == null ? null : Arrays.stream(incompatibleWith.value()).map(toCanonicalName).collect(Collectors.toUnmodifiableSet());
            Set<String> required = requires == null ? null : Arrays.stream(requires.value()).map(toCanonicalName).collect(Collectors.toSet());

            for (AnnotationMirror annotation : this.element.getAnnotationMirrors()) {
                if (annotation != this.mirror) {
                    for (AnnotationMirror otherAnnotation : annotation.getAnnotationType().getAnnotationMirrors()) {
                        TypeElement otherType = (TypeElement) otherAnnotation.getAnnotationType().asElement();
                        String annotationTypeName = otherType.getQualifiedName().toString();

                        if (incompatible != null && incompatible.contains(annotationTypeName)
                            || compatible != null && !compatible.contains(annotationTypeName)) {
                            throw AnnotationRequirementException.incompatible((TypeElement) annotationType.asElement(), otherType, this.element);
                        }

                        if (required != null) {
                            required.remove(annotationTypeName);
                        }
                    }
                }
            }

            if (required != null && !required.isEmpty()) {
                throw AnnotationRequirementException.required((TypeElement) annotationType.asElement(), required.toArray(new String[0]), this.element);
            }
        }
    }
}
