package ornithopter.tasktree;


/**
 * @author Ornithopter on 2015/11/15.
 */
public abstract class Task {
    protected TaskController taskController;

    /*package local*/ ExecutionCallback callback;

    private Task prependedTask;
    private Task appendedTask;

    public Task getPrependedTask() {
        return prependedTask;
    }

    public Task getAppendedTask() {
        return appendedTask;
    }

    /**
     * 执行
     */
    public abstract void execute();

    public Task prepend(Task task) {
        prependedTask = task;
        return this;
    }

    public Task append(Task task) {
        appendedTask = task;
        return this;
    }

    public void cancel() {
        if (taskController != null) {
            taskController.cancel();
        }
    }

    protected abstract <T extends ExecutionCallback> void callSuccess(T executionCallback);

    protected abstract void executeInternal();

    protected <T extends ExecutionCallback> void execute(T callback) {
        this.callback = callback;
        try {
            executeInternal();
            if (taskController == null) {
                callSuccess(callback);
            } else {
                // TODO: handle time out exception.
            }
        } catch (Throwable e) {
            callback.fireError(e);
        }
    }

    /*package local*/ void succeedFromCallback() {
        callSuccess(this.callback);
    }
}
