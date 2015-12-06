package ornithopter.tasktree.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import ornithopter.tasktree.TaskController;

/**
 * @author Ornithopter on 2015/11/22.
 */
public class Context {

     /* =============== information of annotated task =============== */
    /**
     * package for class annotated with {@link ornithopter.tasktree.annotations.Task}
     */
    public TypeMirror packageElement;
    /**
     * generated class name of class annotated with {@link ornithopter.tasktree.annotations.Task}
     */
    public ClassName wrappedTaskClassName;
    /**
     * type name of super class of generated class
     */
    public ParameterizedTypeName wrappedTaskSuperClassTypeName;
    /**
     * rx support
     */
    public boolean rxEnabled;

    /* =============== rx =============== */
    public String rxSubscriberFieldName;

    public String resultFieldName;

    public ClassName resultClassName;

    public boolean asyncFinish;

    /* =============== annotated elements =============== */
    /**
     * all fields with {@link ornithopter.tasktree.annotations.Output}
     */
    public List<Element> outputFields;
    /**
     * all fields with {@link ornithopter.tasktree.annotations.Inject}
     */
    public List<Element> inputFields;
    /**
     * all fields with {@link ornithopter.tasktree.annotations.Inject}
     */
    public List<Element> injectFields;
    /**
     * {@link TaskController} field annotated with {@link ornithopter.tasktree.annotations.Inject}
     */
    public Element taskControllerField;
    /**
     * class annotated with {@link ornithopter.tasktree.annotations.Task}
     */
    public Element taskImplElement;

    /* =============== inner =============== */
    /**
     * an instance of class annotated by {@link ornithopter.tasktree.annotations.Task}
     */
    public String taskImplFieldName;
    /**
     * class name for inner execution callback
     */
    public ClassName innerCallbackClassName;

    /**
     * class name for {@link ornithopter.tasktree.Task}
     */
    public ClassName taskBaseClassName;
    /* =============== language utils =============== */
    public ProcessingEnvironment processingEnv;

    public <T> List<Element> getInjectField(Class<T> clazz) {
        ImmutableList.Builder<Element> builder = ImmutableList.builder();
        for (Element element :
                injectFields) {
            Types typeUtils = getTypeUtils();
            TypeMirror erasure = typeUtils.erasure(element.asType());
            TypeMirror target = processingEnv.getElementUtils().getTypeElement(clazz.toString()).asType();
            if (typeUtils.contains(erasure, target)) {
                builder.add(element);
            }
        }
        return builder.build();
    }

    public <T> Element getInjectFieldOrDefault(Class<T> clazz) {
        for (Element element :
                injectFields) {
            Types typeUtils = getTypeUtils();
            TypeMirror erasure = typeUtils.erasure(element.asType());
            TypeMirror target = typeUtils.erasure(getTypeElement(clazz).asType());
            if (typeUtils.contains(erasure, target)) {
                return element;
            }
        }
        return null;
    }

    public  <T> TypeElement getTypeElement(Class<T> clazz) {
        return processingEnv.getElementUtils().getTypeElement(clazz.getCanonicalName());
    }

    public Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }

    public TypeMirror getFirstTypeArgumentOrDefault(DeclaredType declaredType) {
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() == 0) {
            return null;
        }
        return typeArguments.get(0);
    }

    /**
     * @return null if no
     */
    public @Nullable TypeMirror getTaskControllerType() {
        if (taskControllerField == null) {
            return null;
        }
        return getFirstTypeArgumentOrDefault((DeclaredType) taskControllerField.asType());
    }

    public String getFunctionsPackageName() {
        return rxEnabled ? LanguageInfoRepo.FUNC_PACKAGE_RX : LanguageInfoRepo.FUNC_PACKAGE_TASK_TREE;
    }

    public ClassName getFunctionClass(String className) {
        return ClassName.get(getFunctionsPackageName(), className);
    }

}
