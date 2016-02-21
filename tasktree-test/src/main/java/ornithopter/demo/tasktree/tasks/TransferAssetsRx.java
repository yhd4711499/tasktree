package ornithopter.demo.tasktree.tasks;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Optional;
import ornithopter.tasktree.annotations.Task;

/**
 * @author Ornithopter on 2016/1/10.
 */
@Task(rx = true)
class TransferAssetsRx {
    @Input
    LoginRx.UserInfo userInfoFrom;

    @Input
    LoginRx.UserInfo userInfoTo;

    @Optional
    @Input
    String backupRequestUrl;

    @Inject
    TaskController<String> taskController;

    @Execution
    void execute() throws InterruptedException {
        if (userInfoFrom == null) {
            taskController.error(new IllegalArgumentException("userInfoFrom"));
            return;
        }
        if (userInfoTo == null) {
            taskController.error(new IllegalArgumentException("userInfoTo"));
            return;
        }
        taskController.progress("connecting...");
        Thread.sleep(500);
        taskController.progress("transferring: " + userInfoFrom.username + " -> " + userInfoTo.username + "...");
        Thread.sleep(500);
        taskController.success();
    }
}
