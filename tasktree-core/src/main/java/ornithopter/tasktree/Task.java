package ornithopter.tasktree;


import ornithopter.tasktree.functions.Action0;
import ornithopter.tasktree.functions.Action1;

/**
 * @author Ornithopter on 2015/11/15.
 */
public abstract class Task<TProgress> {

    protected Action1<TProgress> progressCallback;
    protected Action0 startedCallback;
    protected Action0 canceledCallback;
    protected Action1<Throwable> errorCallback;

    protected TaskController<TProgress> taskController;

    private ExecutionCallback<TProgress> callback;

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

    public Task<TProgress> onProgress(Action1<TProgress> callback) {
        progressCallback = callback;
        return this;
    }

    public Task<TProgress> onStarted(Action0 callback) {
        startedCallback = callback;
        return this;
    }

    public Task<TProgress> onCanceled(Action0 callback) {
        canceledCallback = callback;
        return this;
    }

    public Task<TProgress> onError(Action1<Throwable> callback) {
        errorCallback = callback;
        return this;
    }

    public Task<TProgress> prepend(Task task) {
        prependedTask = task;
        return this;
    }

    public Task<TProgress> append(Task task) {
        appendedTask = task;
        return this;
    }

    public void cancel() {
        if (taskController != null) {
            taskController.cancel();
        }
    }

    protected abstract void callSuccess(ExecutionCallback executionCallback);

    protected abstract void executeInternal();

    protected <T extends ExecutionCallback<TProgress>> void execute(T callback) {
        this.callback = callback;
        try {
            if (startedCallback != null) {
                startedCallback.call();
            }
            executeInternal();
            if (taskController != null) {
                // TODO: handle time out exception.
            } else {
                callSuccess(callback);
            }
        } catch (Throwable e) {
            callback.onError(e);
        }
    }

    /*package local*/ void succeedFromCallback() {
        callSuccess(this.callback);
    }
}
