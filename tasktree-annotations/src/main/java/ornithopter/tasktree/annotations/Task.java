package ornithopter.tasktree.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A task will be generated with the name {ThiClassName}Task <p>
 *
 * for example, A class named "FooTask" will be generated if class "Foo" is annotated by this. <p>
 *
 * You can specify input and output values by {@link Input} and {@link Output}. <p>
 *
 * In addition, you can gain access to control the workflow of task operation by adding a field with type <var>ornithopter.tasktree.Workflow</var> (in module :tasktree-core)
 * with {@link Inject} annotation.
 *
 * <p>
 *
 * @author Ornithopter
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Task {
    /**
     * If true, <var>asObservable</var> method will be added to turn this Task into rx.Observable. Make sure rxjava is added as dependency.
     *
     * <p><b>You should NOT using onSuccess, onProgress, onError and onCancel when using asObservable !</b>
     * @return support rxjava or not.
     */
    boolean rx();
}
