package ornithopter.tasktree;

import java.lang.ref.WeakReference;

/**
 * TaskBean
 *
 * @author Ornithopter on 2015/12/6.
 */
public class TaskBean<T> {
    protected final WeakReference<Task<T>> taskWeakReference;

    public TaskBean(Task<T> task) {
        this.taskWeakReference = new WeakReference<>(task);
    }

    /**
     * @return The task.
     */
    protected Task<T> getTask() {
        return taskWeakReference.get();
    }
}
