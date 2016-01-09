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
    void fireProgress(TProgress progress);

    /**
     * task was canceled
     */
    void fireCancel();

    /**
     * task was interrupted by en error
     *
     * @param e error
     */
    void fireError(Throwable e);
}
