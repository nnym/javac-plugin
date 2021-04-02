package user11681.plugin.processing.annotation;

@IncompatibleWith(CompatibleWith.class)
public @interface IncompatibleWith {
    Class<?>[] value();
}
