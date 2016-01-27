package ornithopter.tasktree;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import ornithopter.tasktree.functions.Action0;
import ornithopter.tasktree.functions.Action1;

/**
 * @author Ornithopter on 2015/11/15.
 */
public abstract class Task {
    /**
     *
     */
    TaskController taskController;

    /**
     * callback for client
     */
    private ExecutionCallback callback;

    /**
     *
     */
    private Thread executionThread;

    /* ================
     * internal callbacks for task connection
     * ================ */

    private Action1<Map<String, Object>> onSuccess;
    private Action0 onCancel;
    private Action1<Throwable> onError;

    private final List<TaskConnection<? extends Task>> prependedConnections = new LinkedList<>();
    private final List<TaskConnection<? extends Task>> appendedConnections = new LinkedList<>();

    /* ================
     * public api
     * ================ */

    /**
     * execute
     */
    public abstract void execute();

    /**
     * @param interrupt interrupt the working thread
     */
    public void cancel(boolean interrupt) {
        if (interrupt && executionThread != null) {
            executionThread.interrupt();
        }
        if (taskController != null) {
            taskController.cancel();
        }
    }

    /**
     * prepend a Task, which will be executed <b>BEFORE</b> the execution of this task
     * @param <T> type of the Task
     * @return the prepending task in {@code connection}
     */
    public <T extends Task> T prepend(TaskConnection<T> connection) {
        prependedConnections.add(connection);
        return connection.getTarget();
    }

    /**
     * append a Task, which will be executed <b>AFTER</b> the execution of this task
     * @param <T> type of the Task
     * @return the appending task in {@code connection}
     */
    public <T extends Task> T append(TaskConnection<T> connection) {
        appendedConnections.add(connection);
        return connection.getTarget();
    }

    /* ================
     * internal api
     * ================ */

    void fireSuccess() {
        callSuccess(callback);
        Map<String, Object> resultMap = getResultMap();
        startAppendedTask(resultMap);
        if (onSuccess != null) {
            onSuccess.call(resultMap);
        }
    }

    void fireError(Throwable e) {
        callback.fireError(e);
        if (onError != null) {
            onError.call(e);
        }
    }

    void fireCancel() {
        callback.fireCancel();
        if (onCancel != null) {
            onCancel.call();
        }
    }

    @SuppressWarnings("unchecked")
    void fireProgress(Object progress) {
        callback.fireProgress(progress);
    }

    protected final <T extends ExecutionCallback> void execute(T callback) {
        this.callback = callback;
        try {
            executionThread = Thread.currentThread();
            startPrependedTask();
            executeInternal();
            if (taskController == null) {
                fireSuccess();
            } else {
                // TODO: handle time out exception.
            }
        } catch (Throwable e) {
            if (onError != null) {
                onError.call(e);
            }
            callback.fireError(e);
        }
    }

    /* ================
     *
     * ================ */

    private void startPrependedTask() {
        final CountDownLatch latch = new CountDownLatch(prependedConnections.size());
        for (final TaskConnection<? extends Task> prependedConnection : prependedConnections) {
            final Task task = prependedConnection.getTarget();
            task.onSuccess = (new Action1<Map<String, Object>>() {
                @Override
                public void call(Map<String, Object> stringObjectMap) {
                    latch.countDown();
                    Task.this.readInputFromMap(prependedConnection.connector.mapResultForConsumer(stringObjectMap));
                }
            });
            task.onCancel = new Action0() {
                @Override
                public void call() {
                    latch.countDown();
                }
            };
            task.onError = (new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    latch.countDown();
                }
            });

            Executors.newCachedThreadPool().submit((Runnable) new Runnable() {
                @Override
                public void run() {
                    task.execute();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            if (taskController.isCancelPending()) {
                // TODO: 2016/1/10 check cancel state
            } else {
                fireError(e);
            }
        }
    }

    private void startAppendedTask(final Map<String, Object> resultMap) {
        for (final TaskConnection<? extends Task> appendedConnection : appendedConnections) {
            Executors.newCachedThreadPool().submit(new Runnable() {
                @Override
                public void run() {
                    appendedConnection.getTarget().readInputFromMap(appendedConnection.connector.mapResultForConsumer(resultMap));
                    appendedConnection.getTarget().execute();
                }
            });
        }
    }

    /* ================
     * override for task connection
     * ================ */

    protected void readInputFromMap(Map<String, Object> resultMap){

    }

    protected Map<String, Object> getResultMap(){
        return null;
    }

    protected boolean checkInputs(){
        return true;
    }

    protected abstract <T extends ExecutionCallback> void callSuccess(T executionCallback);

    protected abstract void executeInternal() throws Throwable;
}
