package user11681.plugin;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import net.gudenau.lib.unsafe.Unsafe;
import user11681.plugin.processing.AnnotationContainer;
import user11681.reflect.Classes;
import user11681.reflect.Invoker;
import user11681.reflect.Pointer;

@SuppressWarnings({"unchecked", "SameParameterValue"})
public abstract class AbstractCompilerPlugin extends AbstractProcessor implements Plugin, TaskListener {
    protected static final ClassLoader loader = AbstractCompilerPlugin.class.getClassLoader();

    protected static final Class<?> BasicJavacTask = Classes.load("com.sun.tools.javac.api.BasicJavacTask");
    protected static final Class<?> TreeMaker = Classes.load("com.sun.tools.javac.tree.TreeMaker");
    protected static final Class<?> JCTree = Classes.load("com.sun.tools.javac.tree.JCTree");
    protected static final Class<?> JCExpressionStatement = Classes.load(JCTree.getName() + "$JCExpressionStatement");
    protected static final Class<?> JavacTaskImpl = Classes.load("com.sun.tools.javac.api.JavacTaskImpl");
    protected static final Class<?> Context = Classes.load("com.sun.tools.javac.util.Context");
    protected static final Class<?> JavacProcessingEnvironment = Classes.load("com.sun.tools.javac.processing.JavacProcessingEnvironment");
    protected static final Class<?> JavaCompiler = Classes.load("com.sun.tools.javac.main.JavaCompiler");

    protected static final Pointer compilerPointer = new Pointer().instanceField(JavacProcessingEnvironment, "compiler");
    protected static final Pointer contextPointer = new Pointer().instanceField(JavacProcessingEnvironment, "context");
    protected static final Pointer exprPointer = new Pointer().instanceField(JCExpressionStatement, "expr");
    protected static final Pointer fileManagerPointer = new Pointer().instanceField(JavacProcessingEnvironment, "fileManager");
    protected static final Pointer htPointer = new Pointer().instanceField(Context, "ht");
    protected static final Pointer JavacTaskImpl$compilerPointer = new Pointer().instanceField(JavacTaskImpl, "compiler");
    protected static final Pointer JavacTaskImpl$contextPointer = new Pointer().instanceField(BasicJavacTask, "context");
    protected static final Pointer procEnvImplPointer = new Pointer().instanceField(JavaCompiler, "procEnvImpl");

    protected static final MethodHandle JavacProcessingEnvironment$instance = Invoker.findStatic(JavacProcessingEnvironment, "instance", JavacProcessingEnvironment, Context);

    protected static final Object notFound = null;

    protected final String name;
    protected final Set<String> annotationTypes = new HashSet<>();

    protected boolean initialized;

    protected JavacTask task;
    protected Object compiler;
    protected JavaFileManager files;
    protected Object context;
    protected TaskEvent event;
    protected CompilationUnitTree compilationUnit;
    protected ProcessingEnvironment environment;
    protected Messager messager;
    protected Elements elements;
    protected Types types;
    protected Filer filer;
    protected Map<Object, Object> contextMap;

    public AbstractCompilerPlugin(String name) {
        this.name = name;

        for (Class<?> type : this.getAnnotationTypes()) {
            this.annotationTypes.add(type.getCanonicalName());
        }
    }

    protected void processAnnotation(Element annotatedElement, AnnotationContainer annotation) throws Throwable {}

    protected void done() throws Throwable {}

    protected List<Class<?>> getAnnotationTypes() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void init(JavacTask task, String... args) {
        this.task = task;

        if (!this.initialized) {
            try {
                this.init((ProcessingEnvironment) JavacProcessingEnvironment$instance.invoke((Object) JavacTaskImpl$contextPointer.getObject(task)));
            } catch (Throwable throwable) {
                throw Unsafe.throwException(throwable);
            }

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
            if (event.getKind() == TaskEvent.Kind.ANALYZE) {
                this.processAnnotations();
            }
        } catch (Throwable throwable) {
            throw this.throwException(throwable);
        }
    }

    @Override
    public void finished(TaskEvent event) {
        this.event = event;

        if (event.getKind() == TaskEvent.Kind.COMPILATION) try {
            this.done();
        } catch (Throwable throwable) {
            throw this.throwException(throwable);
        }
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return this.annotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public void init(ProcessingEnvironment environment) {
        this.environment = environment;
        this.compiler = compilerPointer.getObject(environment);
        this.context = contextPointer.getObject(environment);
        this.contextMap = htPointer.getObject(context);
        this.files = fileManagerPointer.getObject(environment);
        this.messager = environment.getMessager();
        this.elements = environment.getElementUtils();
        this.types = environment.getTypeUtils();
        this.filer = environment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement type : annotations) {
                this.processTypeRecursively(type);
            }
        } catch (Throwable throwable) {
            throw this.throwException(throwable);
        }

        return false;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return null;
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
            TypeElement annotationType = this.getAnnotationType(annotation);

            if (this.annotationTypes.contains(annotationType.getQualifiedName().toString())) {
                this.processAnnotation(type, new AnnotationContainer(annotationType, annotation));
            }
        }
    }

    protected JavaFileObject getInputClass(String name) throws IOException {
        for (StandardLocation standardLocation : StandardLocation.values()) {
            JavaFileObject output = this.files.getJavaFileForInput(standardLocation, name, JavaFileObject.Kind.CLASS);

            if (output != null) {
                return output;
            }
        }

        return (JavaFileObject) notFound;
    }

    protected FileObject getInputResource(String path, StandardLocation location) throws IOException {
        ResourceLocation resource = new ResourceLocation(path);

        return this.files.getFileForInput(location, resource.packageName, resource.relativeName);
    }

    protected FileObject getInputResource(String path) throws IOException {
        ResourceLocation resource = new ResourceLocation(path);

        for (StandardLocation location : StandardLocation.values()) {
            FileObject file = this.files.getFileForInput(location, resource.packageName, resource.relativeName);

            if (file != null) {
                return file;
            }
        }

        return (FileObject) notFound;
    }

    protected FileObject getOutputResource(String path) throws IOException {
        ResourceLocation resource = new ResourceLocation(path);

        return this.files.getFileForOutput(StandardLocation.CLASS_OUTPUT, resource.packageName, resource.relativeName, null);
    }

    protected OutputStream getClassOutput(String name) throws IOException {
        return this.getOutputClass(name).openOutputStream();
    }

    protected JavaFileObject getOutputClass(String name) throws IOException {
        return this.files.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT, name, JavaFileObject.Kind.CLASS, null);
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
        Throwable cause = throwable.getCause();

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
