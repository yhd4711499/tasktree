package ornithopter.tasktree;

import org.jetbrains.annotations.Nullable;

import ornithopter.tasktree.functions.Func0;

/**
 * Control the workflow of a Task.
 * <p>
 * {@link #success} must be called to complete the execution.
 *
 * @author Ornithopter
 */
public class TaskController<T> extends TaskBean {

    private Func0<Boolean> cancelPendingFunc;
    private boolean cancelPendingFlag;

    /**
     * @param task          the task you want to control
     * @param cancelPending used as a delegate for rx task to check whether subscriber is unSubscribed or not.
     *                      pass null if this task is not a rx task.
     * @throws IllegalStateException if task has a taskController already.
     */
    @SuppressWarnings("unchecked")
    public <V extends Task> TaskController(V task, @Nullable Func0<Boolean> cancelPending) throws IllegalStateException {
        super(task);
        if (task.taskController != null) {
            throw new IllegalStateException("You can't replace the exist taskController in a task.");
        }
        task.taskController = this;
        this.cancelPendingFunc = cancelPending;
    }

    @SuppressWarnings("unchecked")
    public void progress(T progress) {
        Task task = getTask();
        if (task != null) {
            task.callback.fireProgress(progress);
        }
    }

    /**
     * @return the task is about to cancel.
     */
    public boolean isCancelPending() {
        if (cancelPendingFunc == null) {
            return cancelPendingFlag;
        }
        return cancelPendingFunc.call();
    }

    public void error(Throwable e) {
        Task task = getTask();
        if (task != null) {
            task.callback.fireError(e);
        }
    }

    public void success() {
        Task task = getTask();
        if (task != null) {
            task.succeedFromCallback();
        }
    }

    /**
     * request the task to cancel.
     */
    void cancel() {
        cancelPendingFunc = null;
        cancelPendingFlag = true;
    }

    /**
     * @return The task. Invoke {@link #error} and return null if task not exist.
     */
    @Override
    protected Task getTask() {
        Task task = super.getTask();
        if (task == null) {
            // TODO: 2015/11/29 specify an Exception if task not exists.
            error(null);
        }
        return task;
    }
}
