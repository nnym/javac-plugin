package user11681.plugin.processing.annotation;

@IncompatibleWith(IncompatibleWith.class)
public @interface CompatibleWith {
    Class<?>[] value();
}
