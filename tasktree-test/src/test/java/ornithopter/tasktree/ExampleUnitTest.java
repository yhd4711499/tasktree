package ornithopter.tasktree;


import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import ornithopter.demo.tasktree.HelloworldRxTask;
import ornithopter.demo.tasktree.HelloworldTask;
import rx.Subscriber;


/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void test() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
        HelloworldTask.build(name, 6)
                .onSuccess(s -> {
                    Assert.assertEquals(s, "Hello yhd. You're younger than me.");
                    signal.countDown();
                })
                .onError(throwable -> {
                    signal.countDown();
                    Assert.assertNull(throwable);
                })
                .onStarted(() -> System.out.println("HelloworldTask started."))
                .onProgress((progress) -> Assert.assertEquals(progress, "I'm thinking..."))
                .execute();
        signal.await();
    }

    @Test
    public void testRx() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
        HelloworldRxTask.build(name, 6).asObservable().subscribe(new Subscriber<HelloworldRxTask.Result>() {
            @Override
            public void onStart() {
                System.out.println("HelloworldRxTask started.");
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(HelloworldRxTask.Result result) {
                Assert.assertEquals(result.greetings, name);
                signal.countDown();
            }
        });
        signal.await();
    }

    @Test
    public void testChained() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
        HelloworldTask.build(name, 6)
                .onSuccess(s -> {
                    Assert.assertEquals(s, name);
                    HelloworldTask.build(name + " again", 6)
                            .onSuccess(s2 -> {
                                Assert.assertEquals(s2, name + " again");
                                signal.countDown();
                            })
                            .onStarted(() -> System.out.println("HelloworldTask started."))
                            .onProgress((progress) -> Assert.assertEquals(progress, "I'm thinking..."))
                            .execute();
                })
                .onStarted(() -> System.out.println("HelloworldTask started."))
                .onError(throwable -> signal.countDown())
                .onProgress((progress) -> Assert.assertEquals(progress, "I'm thinking..."))
                .execute();
        signal.await();
    }
}