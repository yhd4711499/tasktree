package ornithopter.tasktree;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * @author Ornithopter on 2016/1/10.
 */
public class TaskConnection<T extends Task> {
    public TaskConnection(T target, Map<String, String> mapping) {
        this.target = new WeakReference<>(target);
        this.connector = new TaskConnector(mapping);
    }

    final WeakReference<T> target;
    final TaskConnector connector;
}
