package ornithopter.tasktree;

import org.jetbrains.annotations.Nullable;

import ornithopter.tasktree.functions.Func0;

/**
 * TaskController is used as a notifier and status checking tool.
 * No logic for task execution here.
 */

/**
 * Control the workflow of a Task.
 * <p>
 * {@link #success} must be called to complete the execution.
 * <p>
 * @author Ornithopter
 */
public class TaskController<T> extends ornithopter.tasktree.di.TaskBean {

    /**
     * whether the task is about to cancel.
     */
    private Func0<Boolean> cancelPendingFunc;
    /**
     * whether the task is about to cancel.
     */
    private boolean cancelPendingFlag;

    /**
     * @param task          the task you want to control
     * @param cancelPending used as a delegate for rx task to check whether subscriber is unSubscribed or not.
     *                      pass null if this task is not a rx task.
     * @throws IllegalStateException if task has a taskController already.
     */
    public TaskController(Task task, @Nullable Func0<Boolean> cancelPending) throws IllegalStateException {
        super(task);
        if (task.taskController != null) {
            throw new IllegalStateException("You can't replace the exist taskController in a task.");
        }
        task.taskController = this;
        this.cancelPendingFunc = cancelPending;
    }

    /**
     * @return whether the task is about to cancel.
     */
    public boolean isCancelPending() {
        if (cancelPendingFunc == null) {
            return cancelPendingFlag;
        }
        return cancelPendingFunc.call();
    }

    /**
     * Indicate that some progress have been made in this task.
     * @param progress progress
     */
    public void progress(T progress) {
        Task task = getTask();
        if (task != null) {
            task.fireProgress(progress);
        }
    }

    /**
     * Indicate an error has occurred and task is about to finish.
     * <p>
     * you should not call any method after.
     * @param e error
     */
    public void error(Throwable e) {
        Task task = getTask();
        if (task != null) {
            task.fireError(e);
        }
    }

    /**
     * Indicate the task succeeded
     * <p>
     * you should not call any method after.
     */
    public void success() {
        Task task = getTask();
        if (task != null) {
            task.fireSuccess();
        }
    }

    /**
     * request the task to cancel.
     * <p>
     * you should not call any method after.
     */
    void cancel() {
        cancelPendingFunc = null;
        cancelPendingFlag = true;
        Task task = getTask();
        if (task != null) {
            task.fireCancel();
        }
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
