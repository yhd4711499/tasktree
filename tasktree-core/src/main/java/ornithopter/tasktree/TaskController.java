package ornithopter.tasktree;

import org.jetbrains.annotations.Nullable;

import ornithopter.tasktree.functions.Func1;

/**
 * Control the workflow of a Task.
 * <p>
 * {@link #success} must be called to complete the execution.
 *
 * @author Ornithopter
 */
public class TaskController<T> extends TaskBean<T> {

    private Func1<Task<?, T>, Boolean> cancelPendingFunc;
    private boolean cancelPendingFlag;

    /**
     * @param task          the task you want to control
     * @param cancelPending used as a delegate for rx task to check whether subscriber is unSubscribed or not.
     *                      passing null if this task is not a rx task.
     * @throws IllegalStateException if task has a taskController already.
     */
    public TaskController(Task<?, T> task, @Nullable Func1<Task<?, T>, Boolean> cancelPending) throws IllegalStateException {
        super(task);
        if (task.taskController != null) {
            throw new IllegalStateException("You can't replace the exist taskController in a task.");
        }
        task.taskController = this;
        this.cancelPendingFunc = cancelPending;
    }

    public void progress(T progress) {
        Task<?, T> task = getTask();
        if (task != null && task.progressCallback != null) {
            try {
                task.progressCallback.call(progress);
            } catch (Throwable throwable) {
                // todo: handle progress error
                throwable.printStackTrace();
            }
        }
    }

    /**
     * @return the task about to cancel.
     */
    public boolean isCancelPending() {
        if (cancelPendingFunc == null) {
            return cancelPendingFlag;
        }
        return cancelPendingFunc.call(getTask());
    }

    public void error(Throwable e) {

    }

    public void success() {
        Task<?, T> task = getTask();
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
    protected Task<?, T> getTask() {
        Task<?, T> task = super.getTask();
        if (task == null) {
            // // TODO: 2015/11/29 specify an Exception when task not exists.
            error(null);
        }
        return task;
    }
}
