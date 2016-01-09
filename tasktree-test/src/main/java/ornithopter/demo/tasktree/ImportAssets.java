package ornithopter.demo.tasktree;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Task;

/**
 * @author Ornithopter on 2016/1/9.
 */
@Task(rx = false)
public class ImportAssets {
    @Input
    Login.UserInfo userInfo;

    @Inject
    TaskController<String> taskController;

    @Execution
    void execute() throws InterruptedException {
        taskController.progress("connecting...");
        Thread.sleep(2000);
        taskController.progress("importing...");
        Thread.sleep(2000);
        taskController.success();
    }
}
