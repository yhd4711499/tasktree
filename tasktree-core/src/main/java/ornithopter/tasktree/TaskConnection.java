package ornithopter.tasktree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Map;

import ornithopter.tasktree.utils.MapUtil;

/**
 * @author Ornithopter on 2016/1/10.
 */
public class TaskConnection<T extends Task> {
    public TaskConnection(T target, Map<String, String> mapping) {
        this.target = target;
        this.connector = new TaskConnector(mapping);
    }

    public TaskConnection(T target, String... mapping) {
        this.target = target;
        this.connector = new TaskConnector(MapUtil.from(mapping));
    }

    @NotNull T getTarget() {
        return target;
    }

    private final T target;
    final TaskConnector connector;
}
