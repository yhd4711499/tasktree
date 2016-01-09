package ornithopter.demo.tasktree;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Output;
import ornithopter.tasktree.annotations.Task;

/**
 * @author Ornithopter on 2016/1/9.
 */
@Task(rx = false)
public class Login {
    @Input String username;
    @Input String password;

    @Output UserInfo userInfo;

    @Inject TaskController<String> taskController;

    @Execution void execute() throws InterruptedException {
        taskController.progress("connecting...");
        Thread.sleep(2000);
        taskController.progress("authenticating...");
        Thread.sleep(2000);
        userInfo = new UserInfo();
        userInfo.username = username;
        userInfo.token = "********";
        taskController.success();
    }

    public static class UserInfo {
        String username;
        String token;
    }
}
