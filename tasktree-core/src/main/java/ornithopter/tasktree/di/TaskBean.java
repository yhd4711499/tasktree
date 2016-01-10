package ornithopter.tasktree.di;

import java.lang.ref.WeakReference;

import ornithopter.tasktree.Task;

/**
 * TaskBean. For dependency injection.
 *
 * @author Ornithopter on 2015/12/6.
 */
public class TaskBean {
    private final WeakReference<? extends Task> taskWeakReference;

    protected TaskBean(Task task) {
        this.taskWeakReference = new WeakReference<>(task);
    }

    /**
     * @return The task.
     */
    protected Task getTask() {
        return taskWeakReference.get();
    }
}
