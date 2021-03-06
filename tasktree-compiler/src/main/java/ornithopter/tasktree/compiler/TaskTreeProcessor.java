package ornithopter.tasktree.compiler;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;
import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import ornithopter.tasktree.ExecutionCallback;
import ornithopter.tasktree.TaskConnection;
import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Output;
import ornithopter.tasktree.annotations.Task;
import ornithopter.tasktree.compiler.utils.StringUtils;
import ornithopter.tasktree.functions.Func0;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.FINAL;
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
                context.packageElement = element.getEnclosingElement();
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

                context.taskKeyDefineClassName = context.wrappedTaskClassName.nestedClass("Keys");

                processTaskField(typeSpecBuilder);

                processSuperClass(typeSpecBuilder);

                processCallSuccessMethod(typeSpecBuilder);
                processReadResultMapMethod(typeSpecBuilder);
                processGetResultMapMethod(typeSpecBuilder);

                processExecuteMethod(typeSpecBuilder);
//                processFireProgressMethod(typeSpecBuilder);

                processBuildManuallyMethod(typeSpecBuilder);
                processBuildFromMapMethod(typeSpecBuilder);
                processBuildConnectionMethod(typeSpecBuilder);

                processKeyDefine(typeSpecBuilder);

                if (context.rxEnabled) {
                    processAsObservableMethod(typeSpecBuilder);
                    processAsFuncMethod(typeSpecBuilder);
                    processResultTypeAndField(typeSpecBuilder);
                } else {
                }
                processOnSuccessMethod(typeSpecBuilder);
                processOnErrorMethod(typeSpecBuilder);
                processOnProgressMethod(typeSpecBuilder);
                processOnStartedMethod(typeSpecBuilder);
                processOnCanceledMethod(typeSpecBuilder);

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

    private void processSuperClass(TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addType(new InnerExecutionCallbackBuilder().build(context));
//        List<TypeName> typeNames = new ArrayList<>();
//        typeNames.add(context.wrappedTaskClassName.nestedClass(context.innerCallbackClassName.simpleName()));
//        if (context.taskControllerField != null) {
//            TypeName typeName;
//            TypeMirror taskControllerType = context.getTaskControllerTypeArgument();
//            if (taskControllerType == null) {
//                typeName = TypeName.OBJECT;
//            } else {
//                typeName = TypeName.get(taskControllerType);
//            }
//            typeNames.add(typeName);
//        } else {
//            typeNames.add(TypeName.OBJECT);
//        }
//        ParameterizedTypeName superClassTypeName;
//        if (context.useShortTask) {
//            superClassTypeName = ParameterizedTypeName.get(context.taskBaseClassName, typeNames.get(1));
//        } else {
//            if (typeNames.size() == 2) {
//                superClassTypeName = ParameterizedTypeName.get(context.taskBaseClassName, typeNames.get(0), typeNames.get(1));
//            } else {
//                superClassTypeName = ParameterizedTypeName.get(context.taskBaseClassName, typeNames.get(0));
//            }
//        }

        context.wrappedTaskSuperClassTypeName = context.taskBaseClassName;

        typeSpecBuilder.superclass(context.taskBaseClassName);
    }

    private void processTaskField(TypeSpec.Builder typeSpecBuilder) {
        Element element = context.taskImplElement;
        FieldSpec fieldSpec = FieldSpec.builder(
                TypeName.get(element.asType()),
                context.taskImplFieldName,
                Modifier.PRIVATE).initializer("new $T()", element.asType()).build();
        typeSpecBuilder.addField(fieldSpec);
    }

    private void processBuildManuallyMethod(TypeSpec.Builder typeSpecBuilder) {
        List<ParameterSpec> args = new ArrayList<>();

        for (Element inputField : context.inputFields) {
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
        methodBuilder.addStatement("$T $L = new $T()",
                context.wrappedTaskClassName, taskInstanceName, context.wrappedTaskClassName);
        for (ParameterSpec param : args) {
            methodBuilder.addStatement("$L.$L.$L = $L",
                    taskInstanceName, context.taskImplFieldName, param.name, param.name);
        }
        methodBuilder.addStatement("return $L", taskInstanceName);

        typeSpecBuilder.addMethod(methodBuilder.build());
    }

    private void processBuildFromMapMethod(TypeSpec.Builder typeSpecBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), "map")
                .returns(context.wrappedTaskClassName);

        String taskInstanceName = "task";
        methodBuilder.addStatement("$T $L = new $T()",
                context.wrappedTaskClassName, taskInstanceName, context.wrappedTaskClassName);
        methodBuilder.addStatement("$L.readInputFromMap($L)", taskInstanceName, "map");
        methodBuilder.addStatement("return $L", taskInstanceName);
        typeSpecBuilder.addMethod(methodBuilder.build());
    }

    private void processBuildConnectionMethod(TypeSpec.Builder typeSpecBuilder) {
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(TaskConnection.class), context.wrappedTaskClassName);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("connect")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "mapping")
                .returns(returnType);
        methodBuilder.addStatement("return new $T(new $T(), $L)", returnType, context.wrappedTaskClassName, "mapping");
        typeSpecBuilder.addMethod(methodBuilder.build());
    }

    private void processCallSuccessMethod(TypeSpec.Builder typeSpecBuilder) {
        List<String> args = new ArrayList<>();
        List<Element> outputFields = context.outputFields;
        args.addAll(Stream.of(outputFields).map(new Function<Element, String>() {
            @Override
            public String apply(Element outputField) {
                return context.taskImplFieldName + "." + outputField.getSimpleName().toString();
            }
        }).collect(Collectors.<String>toList()));

        MethodSpec.Builder builder = MethodSpec.methodBuilder("callSuccess")
                .addModifiers(PROTECTED)
                .addParameter(TypeName.get(ExecutionCallback.class), "callback")
                .returns(void.class);

        if (context.rxEnabled) {
            builder.addStatement("$L.completed = true", context.resultFieldName);
        }
        builder.addStatement("(($T) callback).onSuccess($L)", context.innerCallbackClassName, StringUtils.join(args, ", "));

        typeSpecBuilder.addMethod(builder.build());
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
            throw new IllegalArgumentException("an Execution method must be annotated!");
        }

        MethodSpec.Builder executeInternalBuilder = MethodSpec.methodBuilder("executeInternal")
                .addAnnotation(Override.class)
                .addException(Throwable.class)
                .addModifiers(PROTECTED)
                .returns(void.class);

//        if (!context.rxEnabled) {
        executeInternalBuilder
                .beginControlFlow("if ($L != null)", "startedCallback")
                .addStatement("$L.call()", "startedCallback")
                .endControlFlow();
//        }

        executeInternalBuilder.addStatement("$L.$L()", context.taskImplFieldName, executionElement.getSimpleName());

        typeSpecBuilder.addMethod(executeInternalBuilder.build());

        MethodSpec.Builder executeBuilder = MethodSpec.methodBuilder("execute")
                .addModifiers(PUBLIC)
                .returns(void.class);

        if (context.taskControllerField != null) {
            if (context.rxEnabled && context.getTaskControllerTypeArgument() != null) {
                TypeSpec anony = TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(ParameterizedTypeName.get(
                                ClassName.get(Func0.class),
                                TypeName.get(Boolean.class)))
                        .addMethod(MethodSpec.methodBuilder("call")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(Boolean.class)
                                .beginControlFlow("if (subscriber != null)")
                                    .addStatement("return subscriber.isUnsubscribed()")
                                .endControlFlow()
                                .beginControlFlow("else")
                                    .addStatement("return false")
                                .endControlFlow()
                                .build())
                        .build();

                executeBuilder.addStatement("$L.$L = new $T(this, $L)",
                        context.taskImplFieldName, context.taskControllerField.getSimpleName(), context.taskControllerField.asType(), anony);
            } else {
                executeBuilder.addStatement("$L.$L = new $T(this, null)",
                        context.taskImplFieldName, context.taskControllerField.getSimpleName(), context.taskControllerField.asType());
            }
        }

        executeBuilder.addStatement("execute(new $T())", context.innerCallbackClassName);
        typeSpecBuilder.addMethod(executeBuilder.build());
    }

    private void processOnSuccessMethod(TypeSpec.Builder typeSpecBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onSuccess");
        TypeName[] outputTypes = new TypeName[context.outputFields.size()];
        TypeName callbackTypeName;
        int typeCount = outputTypes.length;
        for (int i = 0; i < typeCount; i++) {
            outputTypes[i] = TypeName.get(context.outputFields.get(i).asType());
        }
        if (typeCount == 0) {
            callbackTypeName = context.getFunctionClass("Action0");
        } else {
            callbackTypeName = ParameterizedTypeName.get(
                    context.getFunctionClass("Action" + typeCount),
                    outputTypes);

        }

        methodBuilder.addParameter(callbackTypeName, "callback")
                .addModifiers(PUBLIC)
                .addStatement("successCallback = callback")
                .addStatement("return this")
                .returns(context.wrappedTaskClassName);
        typeSpecBuilder.addMethod(methodBuilder.build());

        typeSpecBuilder.addField(callbackTypeName, "successCallback");
    }

    private void processOnProgressMethod(TypeSpec.Builder typeSpecBuilder) {
        TypeMirror taskControllerTypeArgument = context.getTaskControllerTypeArgument();
        if (taskControllerTypeArgument == null) {
            return;
        }
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onProgress");

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                context.getFunctionClass("Action1"),
                TypeName.get(taskControllerTypeArgument));

        methodBuilder.addParameter(parameterizedTypeName, "callback")
                .addModifiers(PUBLIC)
                .addStatement("progressCallback = callback")
                .addStatement("return this")
                .returns(context.wrappedTaskClassName);

        typeSpecBuilder.addMethod(methodBuilder.build());

        typeSpecBuilder.addField(parameterizedTypeName, "progressCallback");
    }

    private void processOnErrorMethod(TypeSpec.Builder typeSpecBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onError");

        ParameterizedTypeName action1 = ParameterizedTypeName.get(context.getFunctionClass("Action1"), ClassName.get(Throwable.class));
        methodBuilder.addParameter(action1, "callback")
                .addModifiers(PUBLIC)
                .addStatement("errorCallback = callback")
                .addStatement("return this")
                .returns(context.wrappedTaskClassName);

        typeSpecBuilder.addMethod(methodBuilder.build());
        typeSpecBuilder.addField(action1, "errorCallback");
    }

    private void processOnStartedMethod(TypeSpec.Builder typeSpecBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onStarted");

        methodBuilder.addParameter(context.getFunctionClass("Action0"), "callback")
                .addModifiers(PUBLIC)
                .addStatement("startedCallback = callback")
                .addStatement("return this")
                .returns(context.wrappedTaskClassName);

        typeSpecBuilder.addMethod(methodBuilder.build());
        typeSpecBuilder.addField(context.getFunctionClass("Action0"), "startedCallback");
    }

    private void processOnCanceledMethod(TypeSpec.Builder typeSpecBuilder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("onCancel");

        methodBuilder.addParameter(context.getFunctionClass("Action0"), "callback")
                .addModifiers(PUBLIC)
                .addStatement("canceledCallback = callback")
                .addStatement("return this")
                .returns(context.wrappedTaskClassName);

        typeSpecBuilder.addMethod(methodBuilder.build());
        typeSpecBuilder.addField(context.getFunctionClass("Action0"), "canceledCallback");
    }

    private void processAsObservableMethod(TypeSpec.Builder typeSpecBuilder) {
        ClassName observableClassName = ClassName.get("rx", "Observable");
        ClassName onSubscribeClassName = ClassName.get("rx", "Observable.OnSubscribe");
        ClassName subscriberClassName = ClassName.get("rx", "Subscriber");

        typeSpecBuilder.addField(subscriberClassName, context.rxSubscriberFieldName);

        ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(observableClassName, context.resultClassName);

        typeSpecBuilder.addMethod(MethodSpec.methodBuilder("asObservable")
                .addModifiers(Modifier.PUBLIC)
                .addCode("return $L.create(new $T<$T>() {\n" +
                                "  @Override\n" +
                                "  public void call($T<? super $T> subscriber) {\n" +
                                "    $T.this.$L = subscriber;\n" +
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

    private void processAsFuncMethod(TypeSpec.Builder typeSpecBuilder) {
        ClassName functionClass = context.getFunctionClass("Func" + (context.inputFields.size()));

        List<TypeName> typeNames = Stream.of(context.inputFields).map(new Function<Element, TypeName>() {
            @Override
            public TypeName apply(Element value) {
                return TypeName.get(value.asType()).box();
            }
        }).collect(Collectors.<TypeName>toList());
        typeNames = new ArrayList<>(typeNames);
        typeNames.add(context.resultClassName);
        TypeName[] array = typeNames.toArray(new TypeName[typeNames.size()]);

        ParameterizedTypeName typeName = ParameterizedTypeName.get(functionClass, array);

        List<ParameterSpec> params = Stream.of(context.inputFields).map(new Function<Element, ParameterSpec>() {
            @Override
            public ParameterSpec apply(Element value) {
                return ParameterSpec.builder(TypeName.get(value.asType()).box(), value.getSimpleName().toString()).build();
            }
        }).collect(Collectors.<ParameterSpec>toList());

        String taskVarName = "task";

        TypeSpec anony = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(typeName)
                .addMethod(MethodSpec.methodBuilder("call")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameters(params)
                        .returns(context.resultClassName)
                        .addStatement("$T $L = build($L)", context.wrappedTaskClassName, taskVarName, Stream.of(context.inputFields).map(new Function<Element, Name>() {
                            @Override
                            public Name apply(Element value) {
                                return value.getSimpleName();
                            }
                        }).collect(Collectors.joining(",")))
                        .addStatement("$L.execute()", taskVarName)
                        .addStatement("return $L.result", taskVarName)
                        .build())
                .build();

        typeSpecBuilder.addMethod(
                MethodSpec.methodBuilder("asFunc")
                        .addModifiers(PUBLIC, STATIC)
                        .returns(typeName)
                        .addStatement("return $L", anony)
                        .build()
        );
    }

    private void processReadResultMapMethod(TypeSpec.Builder typeSpecBuilder) {
        final String paramName = "map";
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("readInputFromMap")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), paramName)
                .returns(TypeName.VOID)
                .addStatement("$T obj", Object.class);
        Stream.of(context.inputFields)
                .forEach(new Consumer<Element>() {
                    @Override
                    public void accept(Element element) {
                        methodBuilder
                                .addStatement("obj = $L.get($S)", paramName, element.getSimpleName())
                                .beginControlFlow("if (obj != null)")
                                .addStatement(
                                        "$L.$L = ($T)obj",
                                        context.taskImplFieldName,
                                        element.getSimpleName(),
                                        element.asType())
                                .endControlFlow();
                    }
                });
        typeSpecBuilder.addMethod(methodBuilder.build());
    }

    private void processGetResultMapMethod(TypeSpec.Builder typeSpecBuilder) {
        final String paramName = "map";
        ParameterizedTypeName mapType = ParameterizedTypeName.get(Map.class, String.class, Object.class);
        ParameterizedTypeName hashMapType = ParameterizedTypeName.get(HashMap.class, String.class, Object.class);
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getResultMap")
                .addAnnotation(Override.class)
                .addModifiers(PROTECTED)
                .returns(mapType);
        methodBuilder.addStatement("$T $L = new $T()", hashMapType, paramName, hashMapType);
        Stream.of(context.outputFields)
                .forEach(new Consumer<Element>() {
                    @Override
                    public void accept(Element element) {
                        methodBuilder.addStatement(
                                "$L.put($S, $L.$L)",
                                paramName,
                                element.getSimpleName(),
                                context.taskImplFieldName,
                                element.getSimpleName());
                    }
                });
        methodBuilder.addStatement("return $L", paramName);
        typeSpecBuilder.addMethod(methodBuilder.build());
    }

//    private void processCheckInputsMethod(TypeSpec.Builder typeSpecBuilder) {
//        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("checkInputs")
//                .addAnnotation(Override.class)
//                .addModifiers(PROTECTED)
//                .returns(TypeName.BOOLEAN);
//
//    }

    private void processKeyDefine(TypeSpec.Builder typeSpecBuilder){
        final List<FieldSpec> fieldSpecs = new ArrayList<>(context.inputFields.size() + context.outputFields.size());

        Consumer<Element> consumer = new Consumer<Element>() {
            @Override
            public void accept(Element element) {
                fieldSpecs.add(
                        FieldSpec.builder(String.class, element.getSimpleName().toString(), PUBLIC, STATIC, FINAL)
                                .initializer("$S", element.getSimpleName())
                                .build()
                );
            }
        };

        Stream.of(context.inputFields).forEach(consumer);
        Stream.of(context.outputFields).forEach(consumer);

        TypeSpec.Builder keysBuilder = TypeSpec.classBuilder(context.taskKeyDefineClassName.simpleName())
                .addModifiers(PUBLIC, FINAL)
                .addFields(fieldSpecs);

        typeSpecBuilder.addType(keysBuilder.build());
    }

    private void processResultTypeAndField(TypeSpec.Builder builder) {
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(context.resultClassName.simpleName())
                .addModifiers(PUBLIC);

        List<Element> outputFields = context.outputFields;
        for (Element field :
                outputFields) {
            typeSpecBuilder.addField(TypeName.get(field.asType()), field.getSimpleName().toString(), Modifier.PUBLIC);
        }

        TypeMirror progressType = context.getTaskControllerTypeArgument();
        if (progressType != null) {
            typeSpecBuilder.addField(TypeName.get(progressType), "progress", PUBLIC);
            typeSpecBuilder.addField(TypeName.BOOLEAN, "completed", PUBLIC);
        }

        builder.addType(typeSpecBuilder.build());

        builder.addField(
                FieldSpec.builder(context.resultClassName, context.resultFieldName, Modifier.PRIVATE)
                        .initializer("new $T()", context.resultClassName)
                        .build()
        );
    }

    private List<Element> getFieldsWithAnnotation(Element element, Class<? extends Annotation> clazz) {
        List<Element> fields = new ArrayList<>();
        List<? extends Element> enclosedElements = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (Element enclosedElement :
                enclosedElements) {
            if (enclosedElement.getAnnotation(clazz) != null) {
                if (isInaccessibleViaGeneratedCode(clazz, "fields", enclosedElement)
                        || isBindingInWrongPackage(clazz, enclosedElement)) {
                    continue;
                }
                fields.add(enclosedElement);
            }
        }
        return fields;
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
