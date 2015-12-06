package ornithopter.tasktree;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.concurrent.CountDownLatch;

import ornithopter.demo.tasktree.HelloworldRxTask;
import ornithopter.demo.tasktree.HelloworldTask;


/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    @SmallTest
    public void test() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
        HelloworldTask.build(name, 6).onSuccess(s -> {
            assertEquals(s, name);
            signal.countDown();
        }).onProgress((progress)->assertEquals(progress, "I'm thinking...")).execute();
        signal.await();
    }

    @SmallTest
    public void testRx() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
        HelloworldRxTask.build(name, 6).asObservable().subscribe(result -> {
            assertEquals(result.greetings, name);
            signal.countDown();
        });
        signal.await();
    }
}