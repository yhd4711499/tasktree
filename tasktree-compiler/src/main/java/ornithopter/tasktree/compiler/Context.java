package ornithopter.tasktree.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Task;

/**
 * @author Ornithopter on 2015/11/22.
 */
public class Context {

     /* =============== information of annotated task =============== */
    /**
     * package for class annotated with {@link Task}
     */
    public Element packageElement;
    /**
     * generated class name of class annotated with {@link ornithopter.tasktree.annotations.Task}
     */
    public ClassName wrappedTaskClassName;
    /**
     * generated class name for holding naming of input and output fields.
     */
    public ClassName taskKeyDefineClassName;
    /**
     * type name of super class of generated class
     */
    public TypeName wrappedTaskSuperClassTypeName;
    /**
     * rx support
     */
    public boolean rxEnabled;
    /**
     *
     */
    public boolean useShortTask;

    /* =============== rx =============== */
    public String rxSubscriberFieldName;

    public String resultFieldName;

    public ClassName resultClassName;

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

    /**
     * @return list of elements which is annotated with {@link Inject} and also subtype of {@code clazz}.
     * <p>empty list if no suitable elements.
     * <p>Note: this list is immutable!
     */
    public List<Element> getInjectField(Class<?> clazz) {
        ImmutableList.Builder<Element> builder = ImmutableList.builder();
        injectFields.stream().filter(elm -> isSubType(clazz, elm)).forEach(builder::add);
        return builder.build();
    }

    /**
     * @return true if type of {@code elm} is subtype of {@code clazz}.
     */
    public boolean isSubType(Class<?> clazz, Element elm) {
        Types typeUtils = getTypeUtils();
        TypeMirror target = typeUtils.erasure(getTypeElement(clazz).asType());
        return typeUtils.isSubtype(elm.asType(), target);
    }

    /**
     * @return element which is annotated with {@link Inject} and also subtype of {@code clazz}.
     * <p>null if not found.
     */
    public Element getInjectFieldOrDefault(Class<?> clazz) {
        Optional<Element> optional = getInjectField(clazz).stream().findFirst();
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * find the super type of {@code typeMirror}
     * @return super type with {@code qualifiedClassName}. null if not found.
     */
    public TypeMirror findTargetSuperType(TypeMirror typeMirror, String qualifiedClassName) {
        Types typeUtils = getTypeUtils();
        if (typeUtils.erasure(typeMirror).toString().equals(qualifiedClassName)) {
            return typeMirror;
        }
        Optional<? extends TypeMirror> first = typeUtils.directSupertypes(typeMirror).stream()
                .filter((t)-> typeUtils.erasure(t).toString().equals(qualifiedClassName))
                .findFirst();
        if (first.isPresent()) {
            return first.get();
        } else {
            return null;
        }
    }

    /**
     * @return converted {@link TypeElement} from {@link Class}
     */
    public TypeElement getTypeElement(Class clazz) {
        return processingEnv.getElementUtils().getTypeElement(clazz.getCanonicalName());
    }

    /**
     * @return first type argument
     */
    public TypeMirror getFirstTypeArgumentOrDefault(DeclaredType declaredType) {
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() == 0) {
            return null;
        }
        return typeArguments.get(0);
    }

    /**
     * @return the type argument of {@link TaskController}. null if no {@link TaskController} is present.
     */
    public @Nullable TypeMirror getTaskControllerTypeArgument() {
        if (taskControllerField == null) {
            return null;
        }
        return getFirstTypeArgumentOrDefault(
                (DeclaredType) findTargetSuperType(
                        taskControllerField.asType(),
                        TaskController.class.getCanonicalName())
        );
    }

    public String getFunctionsPackageName() {
        return rxEnabled ? LanguageInfoRepo.FUNC_PACKAGE_RX : LanguageInfoRepo.FUNC_PACKAGE_TASK_TREE;
    }

    public ClassName getFunctionClass(String className) {
        return ClassName.get(getFunctionsPackageName(), className);
    }

    private Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }

}
