package ornithopter.tasktree;

import java.lang.ref.WeakReference;

/**
 * TaskBean
 *
 * @author Ornithopter on 2015/12/6.
 */
public class TaskBean {
    protected final WeakReference<? extends Task> taskWeakReference;

    public <T extends Task> TaskBean(T task) {
        this.taskWeakReference = new WeakReference<>(task);
    }

    /**
     * @return The task.
     */
    protected Task getTask() {
        return taskWeakReference.get();
    }
}
