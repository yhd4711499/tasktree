package ornithopter.tasktree;

/**
 * @author Ornithopter on 2015/11/15.
 */
public interface ExecutionCallback<TProgress> {
    /**
     * task made a new progress
     *
     * @param progress progress
     */
    void onProgress(TProgress progress);

    /**
     * task was canceled
     */
    void onCanceled();

    /**
     * task was interrupted by en error
     *
     * @param e error
     */
    void onError(Throwable e);

//    /**
//     * task succeed with result data
//     * @param data result data
//     */
//    void onSuccess(Object... data);
}
