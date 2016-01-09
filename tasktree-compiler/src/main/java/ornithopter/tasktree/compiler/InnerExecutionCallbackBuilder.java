package ornithopter.tasktree.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import ornithopter.tasktree.ExecutionCallback;
import ornithopter.tasktree.compiler.utils.StringUtils;

import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * @author Ornithopter on 2015/11/22.
 */
class InnerExecutionCallbackBuilder {

    public TypeSpec build(Context ctx) {
        ClassName executionCallbackClassName = ClassName.get(ExecutionCallback.class);
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(ctx.innerCallbackClassName.simpleName())
                .addModifiers(PUBLIC);

        TypeMirror taskControllerType = ctx.getTaskControllerTypeArgument();
        TypeName progressType = taskControllerType == null ?
                TypeName.VOID.box() : TypeName.get(taskControllerType);
        ParameterizedTypeName superClassTypeName = ParameterizedTypeName.get(
                executionCallbackClassName,
                progressType);
        typeSpecBuilder.addSuperinterface(superClassTypeName);

        MethodSpec.Builder onProgressBuilder;
        String onProgressParameterName = "progress";
        onProgressBuilder = MethodSpec.methodBuilder("fireProgress")
                .addModifiers(PUBLIC)
                .addParameter(
                        ParameterSpec.builder(progressType, onProgressParameterName).build())
                .returns(void.class);

        MethodSpec.Builder onCanceledBuilder = MethodSpec.methodBuilder("fireCancel")
                .addModifiers(PUBLIC)
                .returns(void.class);

        MethodSpec.Builder onErrorBuilder = MethodSpec.methodBuilder("fireError")
                .addModifiers(PUBLIC)
                .addParameter(Throwable.class, "e")
                .returns(void.class);

        List<ParameterSpec> args = new ArrayList<>();
        List<String> argNames = new ArrayList<>();
        for (Element outputField :
                ctx.outputFields) {
            String argName = outputField.getSimpleName().toString();
            args.add(ParameterSpec.builder(
                    TypeName.get(outputField.asType()), argName)
                    .build()
            );
            argNames.add(argName);
        }
        MethodSpec.Builder onSuccessBuilder = MethodSpec.methodBuilder("onSuccess")
                .addModifiers(PUBLIC)
                .addParameters(args)
                .returns(void.class);

        if (ctx.rxEnabled) {
            onSuccessBuilder
                    .beginControlFlow("if ($L != null)", ctx.rxSubscriberFieldName);

            for (String argName :
                    argNames) {
                onSuccessBuilder.addStatement("$L.$L = $L", ctx.resultFieldName, argName, argName);
            }

            onSuccessBuilder.addStatement("$L.onNext($L)", ctx.rxSubscriberFieldName, ctx.resultFieldName);
            onSuccessBuilder.addStatement("$L.onCompleted()", ctx.rxSubscriberFieldName);

            onSuccessBuilder.endControlFlow();

            onErrorBuilder
                    .beginControlFlow("if ($L != null)", ctx.rxSubscriberFieldName)
                    .addStatement("$L.onError($L)", ctx.rxSubscriberFieldName, "e")
                    .endControlFlow();
        } else {
            if (taskControllerType != null) {
                onProgressBuilder
                        .beginControlFlow("if (progressCallback != null)")
                        .addStatement("progressCallback.call($L)", onProgressParameterName)
                        .endControlFlow();
            }

            onCanceledBuilder
                    .beginControlFlow("if (canceledCallback != null)")
                    .addStatement("canceledCallback.call()")
                    .endControlFlow();

            onErrorBuilder
                    .beginControlFlow("if (errorCallback != null)")
                    .addStatement("errorCallback.call(e)")
                    .endControlFlow();

            onSuccessBuilder
                    .beginControlFlow("if (successCallback != null)")
                    .addStatement("successCallback.call($L)", StringUtils.join(argNames, ","))
                    .endControlFlow();
        }

        List<MethodSpec> methodSpecs = new ArrayList<>();
        methodSpecs.add(onSuccessBuilder.build());
        methodSpecs.add(onErrorBuilder.build());
        methodSpecs.add(onProgressBuilder.build());
        methodSpecs.add(onCanceledBuilder.build());

        typeSpecBuilder.addMethods(methodSpecs);


        return typeSpecBuilder.build();
    }
}
