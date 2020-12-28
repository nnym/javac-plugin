package user11681.plugin;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import net.gudenau.lib.unsafe.Unsafe;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.reflect.Invoker;
import user11681.reflect.Pointer;
import user11681.shortcode.Shortcode;

public abstract class AbstractCompilerPlugin implements Plugin, TaskListener {
    protected static final ClassLoader loader = AbstractCompilerPlugin.class.getClassLoader();

    protected static final Class<?> TreeMaker = Classes.load("com.sun.tools.javac.tree.TreeMaker");
    protected static final Class<?> JCTree = Classes.load("com.sun.tools.javac.tree.JCTree");
    protected static final Class<?> JCExpressionStatement = Classes.load(JCTree.getName() + "$JCExpressionStatement");
    protected static final Class<?> JavacTaskImpl = Classes.load("com.sun.tools.javac.api.JavacTaskImpl");
    protected static final Class<?> Context = Classes.load("com.sun.tools.javac.util.Context");
    protected static final Class<?> JavacProcessingEnvironment = Classes.load("com.sun.tools.javac.processing.JavacProcessingEnvironment");

    protected static final Pointer expr = new Pointer().instanceField(JCExpressionStatement, "expr");
    protected static final Pointer fileManager = new Pointer().instanceField(JavacTaskImpl, "fileManager");

    protected final String name;
    protected final List<String> annotationTypes = new ArrayList<>();

    protected boolean initialized;

    protected JavacTask task;
    protected JavaFileManager javaFileManager;
    protected Object context;
    protected TaskEvent event;
    protected CompilationUnitTree compilationUnit;
    protected ProcessingEnvironment environment;
    protected Messager messager;
    protected Elements elements;
    protected Types types;
    protected Filer filer;

    protected AbstractCompilerPlugin(String name) {
        this.name = name;
    }

    protected void processAnnotation(Annotation annotation) throws Throwable {}

    protected void afterCompilation() throws Throwable {}

    protected Class<?>[] getAnnotationTypes() throws Throwable {
        return new Class<?>[0];
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> getExplicitProperties(AnnotationMirror annotation) {
        final Map<String, Object> values = (Map<String, Object>) (Object) annotation.getElementValues();
        final Set<? extends Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> entries = ((Map<? extends ExecutableElement, ? extends AnnotationValue>) (Object) values).entrySet();

        values.clear();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : entries) {
            values.put(entry.getKey().getSimpleName().toString(), entry.getValue().getValue());
        }

        return values;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void init(JavacTask task, String... args) {
        if (this.initialized) try {
            this.task = task;
            this.javaFileManager = fileManager.get(task);
            this.context = Accessor.getObject(task, "context");
            this.environment = (ProcessingEnvironment) Invoker.findStatic(JavacProcessingEnvironment, "instance", JavacProcessingEnvironment, Context).invoke(this.context);
            this.messager = this.environment.getMessager();
            this.elements = this.environment.getElementUtils();
            this.types = this.environment.getTypeUtils();
            this.filer = this.environment.getFiler();

            for (Class<?> type : this.getAnnotationTypes()) {
                this.annotationTypes.add(type.getName());
            }
        } catch (Throwable throwable) {
            throw Unsafe.throwException(throwable);
        } finally {
            this.initialized = true;
        }

        task.addTaskListener(this);
    }

    @Override
    public boolean autoStart() {
        return true;
    }

    @Override
    public void started(TaskEvent event) {
        this.event = event;
        this.compilationUnit = event.getCompilationUnit();

        try {
            if (event.getKind() == TaskEvent.Kind.ANNOTATION_PROCESSING) {
                this.processAnnotations();
            }
        } catch (Throwable throwable) {
            throw this.throwException(throwable);
        }
    }

    @Override
    public void finished(TaskEvent event) {
        this.event = event;

        try {
            if (event.getKind() == TaskEvent.Kind.COMPILATION) {
                this.afterCompilation();
            }
        } catch (Throwable throwable) {
            throw this.throwException(throwable);
        }
    }

    protected void processAnnotations() throws Throwable {
        this.processTypeRecursively(this.event.getTypeElement());
    }

    private void processTypeRecursively(TypeElement type) throws Throwable {
        this.processType(type);

        for (Element element : type.getEnclosedElements()) {
            if (element instanceof TypeElement) {
                processTypeRecursively((TypeElement) element);
            }
        }
    }

    protected void processType(TypeElement type) throws Throwable {
        for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
            if (this.annotationTypes.contains(this.getBinaryName(this.getAnnotationType(annotation)))) {
                this.processAnnotation(new Annotation((TypeElement) annotation.getAnnotationType().asElement(), type, getExplicitProperties(annotation)));
            }
        }
    }

    protected InputStream getClassInput(String name) throws IOException {
        final InputStream stream = loader.getResourceAsStream(Shortcode.getLocation(name));

        if (stream != null) {
            return stream;
        }

        return this.javaFileManager.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT, name, JavaFileObject.Kind.CLASS, null).openInputStream();
    }

    protected OutputStream getClassOutput(String name) throws IOException {
        return this.getOutputClass(name).openOutputStream();
    }

    protected JavaFileObject getOutputClass(String name) throws IOException {
        return this.javaFileManager.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT, name, JavaFileObject.Kind.CLASS, null);
    }

    protected RuntimeException throwException(Throwable throwable) {
        this.error(throwable);

        throw Unsafe.throwException(throwable);
    }

    protected String getBinaryName(TypeElement type) {
        return this.elements.getBinaryName(type).toString();
    }

    protected TypeElement getAnnotationType(AnnotationMirror annotation) {
        return (TypeElement) annotation.getAnnotationType().asElement();
    }

    protected <T> void note(T... arguments) {
        this.printMessage(Diagnostic.Kind.NOTE, arguments);
    }

    protected <T> void note(String format, T... arguments) {
        this.printMessage(Diagnostic.Kind.NOTE, format, arguments);
    }

    protected <T> void warning(T... arguments) {
        this.printMessage(Diagnostic.Kind.WARNING, arguments);
    }

    protected <T> void warning(String format, T... arguments) {
        this.printMessage(Diagnostic.Kind.WARNING, format, arguments);
    }

    protected void error(Throwable throwable) {
        final Throwable cause = throwable.getCause();

        if (cause != null) {
            this.error(cause);
        }

        this.error(throwable.toString());
        this.error(throwable.getStackTrace());
    }

    protected <T> void error(T... arguments) {
        this.printMessage(Diagnostic.Kind.ERROR, arguments);
    }

    protected <T> void error(String format, T... arguments) {
        this.printMessage(Diagnostic.Kind.ERROR, format, arguments);
    }

    protected <T> void printMessage(Diagnostic.Kind kind, T... arguments) {
        for (Object argument : arguments) {
            this.printMessage(kind, String.valueOf(argument));
        }
    }

    protected <T> void printMessage(Diagnostic.Kind kind, String format, T... arguments) {
        this.messager.printMessage(kind, String.format(format, (Object[]) arguments));
    }
}
