package ornithopter.tasktree.compiler;

import ornithopter.tasktree.functions.Action;

/**
 * @author Ornithopter on 2015/12/6.
 */
public class LanguageInfoRepo {
    public static final String FUNC_PACKAGE_RX = "rx.functions";
    public static final String FUNC_PACKAGE_TASK_TREE = Action.class.getPackage().getName();
}
