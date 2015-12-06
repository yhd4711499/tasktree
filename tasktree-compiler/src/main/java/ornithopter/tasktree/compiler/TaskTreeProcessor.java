package ornithopter.tasktree.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Output;
import ornithopter.tasktree.annotations.Task;
import ornithopter.tasktree.compiler.utils.StringUtils;
import ornithopter.tasktree.functions.Func1;
import rx.Observable;
import rx.Subscriber;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class TaskTreeProcessor extends AbstractProcessor {

    private static final String TASK_CLASS_POSTFIX = "Task";

    private Context context;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        filer = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Task.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Task.class)) {
            if (!SuperficialValidation.validateElement(element)) continue;

            context = new Context();
            context.taskBaseClassName = ClassName.get(ornithopter.tasktree.Task.class);
            context.processingEnv = processingEnv;

            Task annotation = element.getAnnotation(Task.class);
            context.rxEnabled = annotation.rx();
            try {
                context.packageElement = element.getEnclosingElement().asType();
                context.wrappedTaskClassName = ClassName.get(context.packageElement.toString(), element.getSimpleName().toString() + TASK_CLASS_POSTFIX);
                TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(context.wrappedTaskClassName.simpleName())
                        .addModifiers(PUBLIC);

                List<Element> inputFields = getFieldsWithAnnotation(element, Input.class);
                List<Element> outputFields = getFieldsWithAnnotation(element, Output.class);
                List<Element> injectFields = getFieldsWithAnnotation(element, Inject.class);

                context.inputFields = inputFields;
                context.outputFields = outputFields;
                context.injectFields = injectFields;
                context.taskImplFieldName = "taskImpl";
                context.taskImplElement = element;
                context.innerCallbackClassName = context.wrappedTaskClassName.nestedClass("InnerExecutionCallback");
                context.taskControllerField = context.getInjectFieldOrDefault(TaskController.class);

                context.rxSubscriberFieldName = "subscriber";
                context.resultFieldName = "result";
                context.resultClassName = context.wrappedTaskClassName.nestedClass("Result");

                processTaskField(typeSpecBuilder);
                processSuperClass(typeSpecBuilder);
                processCallSuccessMethod(typeSpecBuilder);
                processOnSuccessMethod(typeSpecBuilder);
                processExecuteMethod(typeSpecBuilder);
                processBuildMethod(typeSpecBuilder);

                if (context.rxEnabled) {
                    processAsObservableMethod(typeSpecBuilder);
                    processResultTypeAndField(typeSpecBuilder);
                }

                JavaFile.builder(context.packageElement.toString(), typeSpecBuilder.build())
                        .addFileComment("Generated code from TaskTree. Do not modify!")
                        .build().writeTo(filer);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(ERROR, "failed to generate code:" + e, element);
                e.printStackTrace();
            }
        }

        return true;
    }

    private void processSuperClass(TypeSpec.Builder typeSpecBuilder) throws Exception {
        typeSpecBuilder.addType(new InnerExecutionCallbackBuilder().build(context));
        List<TypeName> typeNames = new ArrayList<>();
        typeNames.add(context.wrappedTaskClassName.nestedClass(context.innerCallbackClassName.simpleName()));
        if (context.taskControllerField != null) {
            List<? extends TypeMirror> typeParameters = ((DeclaredType) context.taskControllerField.asType()).getTypeArguments();
            TypeName typeName;
            if (typeParameters.size() == 0) {
                typeName = TypeName.OBJECT;
            } else if (typeParameters.size() == 1) {
                typeName = TypeName.get(typeParameters.get(0));
            } else {
                throw new Exception("only one parameter type is allowed!");
            }
            typeNames.add(typeName);
        } else {
            typeNames.add(TypeName.OBJECT);
        }
        ParameterizedTypeName superClassTypeName;
        if (typeNames.size() == 2) {
            superClassTypeName = ParameterizedTypeName.get(context.taskBaseClassName, typeNames.get(0), typeNames.get(1));
        } else {
            superClassTypeName = ParameterizedTypeName.get(context.taskBaseClassName, typeNames.get(0));
        }

        context.wrappedTaskSuperClassTypeName = superClassTypeName;

        typeSpecBuilder.superclass(superClassTypeName);
    }

    private void processTaskField(TypeSpec.Builder typeSpecBuilder) {
        Element element = context.taskImplElement;
        FieldSpec fieldSpec = FieldSpec.builder(
                TypeName.get(element.asType()),
                context.taskImplFieldName,
                Modifier.PRIVATE).initializer("new $L()", element.asType()).build();


        typeSpecBuilder.addField(fieldSpec);
    }

    private void processBuildMethod(TypeSpec.Builder typeSpecBuilder) {
        List<ParameterSpec> args = new ArrayList<>();

        for (Element inputField :
                context.inputFields) {
            ParameterSpec parameterSpec = ParameterSpec.builder(
                    TypeName.get(inputField.asType()),
                    inputField.getSimpleName().toString())
                    .build();
            args.add(parameterSpec);
        }

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(PUBLIC, STATIC)
                .addParameters(args)
                .returns(context.wrappedTaskClassName);

        String taskInstanceName = "task";
        methodBuilder.addStatement("$L $L = new $L()", context.wrappedTaskClassName, taskInstanceName, context.wrappedTaskClassName);
        for (ParameterSpec param :
                args) {
            methodBuilder.addStatement("$L.$L.$L = $L", taskInstanceName, context.taskImplFieldName, param.name, param.name);
        }
        methodBuilder.addStatement("return $L", taskInstanceName);

        typeSpecBuilder.addMethod(methodBuilder.build());
    }

    private void processCallSuccessMethod(TypeSpec.Builder typeSpecBuilder) {
        List<String> args = new ArrayList<>();
        List<Element> outputFields = context.outputFields;
        args.addAll(outputFields.stream().map(outputField -> context.taskImplFieldName + "." + outputField.getSimpleName().toString()).collect(Collectors.toList()));

        MethodSpec method = MethodSpec.methodBuilder("callSuccess")
                .addModifiers(PROTECTED)
                .addParameter(context.innerCallbackClassName, "callback")
                .returns(void.class)
                .addStatement("callback.onSuccess($L)", StringUtils.join(args, ", "))
                .build();

        typeSpecBuilder.addMethod(method);
    }

    private void processExecuteMethod(TypeSpec.Builder typeSpecBuilder) {
        Element element = context.taskImplElement;
        Class<Execution> clazz = Execution.class;
        List<? extends Element> enclosedElements = ElementFilter.methodsIn(element.getEnclosedElements());
        ExecutableElement executionElement = null;
        for (Element enclosedElement :
                enclosedElements) {
            if (executionElement != null) {
                throw new IllegalArgumentException("only one Execution method is allowed!");
            }
            if (enclosedElement.getAnnotation(clazz) != null) {
                if (isInaccessibleViaGeneratedCode(clazz, "methods", enclosedElement)
                        || isBindingInWrongPackage(clazz, enclosedElement)) {
                    continue;
                }
                if (enclosedElement instanceof ExecutableElement) {
                    executionElement = (ExecutableElement) enclosedElement;
                } else {
                    throw new IllegalArgumentException("only method can be annotated with Execution!");
                }
            }
        }
        if (executionElement == null) {
            throw new IllegalArgumentException("one Execution method must be annotated!");
        }

        MethodSpec.Builder executeInternalBuilder = MethodSpec.methodBuilder("executeInternal")
                .addModifiers(PROTECTED)
                .returns(void.class);

        executeInternalBuilder.addStatement("$L.$L()", context.taskImplFieldName, executionElement.getSimpleName());

        typeSpecBuilder.addMethod(executeInternalBuilder.build());

        MethodSpec.Builder executeBuilder = MethodSpec.methodBuilder("execute")
                .addModifiers(PUBLIC)
                .returns(void.class);

        if (context.taskControllerField != null) {
            if (context.rxEnabled && context.getTaskControllerType() != null) {
                String funcTypeName = context.taskBaseClassName.simpleName() + "<?," + context.getTaskControllerType() + ">";
                CodeBlock codeBlock = CodeBlock.builder().add("" +
                        "new $L<$L, Boolean>() {\n" +
                        "            @Override\n" +
                        "            public Boolean call($L task) {\n" +
                        "                if (task != null) {\n" +
                        "                    return (($L)task).subscriber.isUnsubscribed();\n" +
                        "                } else {\n" +
                        "                    return false;\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }", ClassName.get(Func1.class), funcTypeName, funcTypeName, context.wrappedTaskClassName).build();
                executeBuilder.addStatement("$L.$L = new $L(this, $L)", context.taskImplFieldName, context.taskControllerField.getSimpleName(), context.taskControllerField.asType(), codeBlock);
            } else {
                executeBuilder.addStatement("$L.$L = new $L(this, null)", context.taskImplFieldName, context.taskControllerField.getSimpleName(), context.taskControllerField.asType());
            }
        }

        executeBuilder.addStatement("execute(new $L())", context.innerCallbackClassName);
        typeSpecBuilder.addMethod(executeBuilder.build());
    }

    private void processOnSuccessMethod(TypeSpec.Builder typeSpecBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onSuccess");
        TypeName[] outputTypes = new TypeName[context.outputFields.size()];
        int typeCount = outputTypes.length;
        for (int i = 0; i < typeCount; i++) {
            outputTypes[i] = TypeName.get(context.outputFields.get(i).asType());
        }
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                context.getFunctionClass("Action" + typeCount),
                outputTypes);
        methodBuilder.addParameter(parameterizedTypeName, "callback")
                .addModifiers(PUBLIC)
                .addStatement("successCallback = callback")
                .addStatement("return this")
                .returns(context.wrappedTaskClassName);
        typeSpecBuilder.addMethod(methodBuilder.build());

        typeSpecBuilder.addField(parameterizedTypeName, "successCallback");
    }

    private void processAsObservableMethod(TypeSpec.Builder builder) {
        ClassName observableClassName = ClassName.get(Observable.class);
        ClassName onSubscribeClassName = ClassName.get(Observable.OnSubscribe.class);
        ClassName subscriberClassName = ClassName.get(Subscriber.class);

        builder.addField(subscriberClassName, context.rxSubscriberFieldName);

        ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(ClassName.get(Observable.class), context.resultClassName);

        builder.addMethod(MethodSpec.methodBuilder("asObservable")
                .addModifiers(Modifier.PUBLIC)
                .addCode("return $L.create(new $L<$L>() {\n" +
                                "  @Override\n" +
                                "  public void call($L<? super $L> subscriber) {\n" +
                                "    $L.this.$L = subscriber;\n" +
                                "    execute();\n" +
                                "  }\n" +
                                "});\n"
                        , observableClassName, onSubscribeClassName, context.resultClassName
                        , subscriberClassName, context.resultClassName
                        , context.wrappedTaskClassName, context.rxSubscriberFieldName)
                .returns(returnTypeName)
                .build()
        );
    }

    private void processResultTypeAndField(TypeSpec.Builder builder) {
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(context.resultClassName.simpleName())
                .addModifiers(PUBLIC);

        List<Element> outputFields = context.outputFields;
        for (Element field :
                outputFields) {
            typeSpecBuilder.addField(TypeName.get(field.asType()), field.getSimpleName().toString(), Modifier.PUBLIC);
        }

        builder.addType(typeSpecBuilder.build());

        builder.addField(
                FieldSpec.builder(context.resultClassName, context.resultFieldName, Modifier.PRIVATE)
                        .initializer("new $L()", context.resultClassName)
                        .build()
        );
    }

    private List<Element> getFieldsWithAnnotation(Element element, Class<? extends Annotation> clazz) {
        List<Element> inputFields = new ArrayList<>();
        List<? extends Element> enclosedElements = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (Element enclosedElement :
                enclosedElements) {
            if (enclosedElement.getAnnotation(clazz) != null) {
                if (isInaccessibleViaGeneratedCode(clazz, "fields", enclosedElement)
                        || isBindingInWrongPackage(clazz, enclosedElement)) {
                    continue;
                }
                inputFields.add(enclosedElement);
            }
        }
        return inputFields;
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    /*
        private void logParsingError(Element element, Class<? extends Annotation> annotation,
                                     Exception e) {
            StringWriter stackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(stackTrace));
            error(element, "Unable to parse @%s binding.\n\n%s", annotation.getSimpleName(), stackTrace);
        }
    */
    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }
}
