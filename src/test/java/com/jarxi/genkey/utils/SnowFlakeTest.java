package com.jarxi.genkey.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SnowFlakeTest {
    long times = SnowFlake.MAX_SEQUENCE + 100;

    @Test
    public void contextLoads() {
    }

    // 连续生产超过MAX_SEQUENCE个id，不会重复
    @Test
    public void testRepetitionLess() {
        SnowFlake snowFlake = new SnowFlake(3, 2);
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < times; i++) {
            set.add(snowFlake.nextId());
        }
        assertThat((long)set.size(), equalTo(times));
    }

    // 多个线程并发，多个ID生成器，workid不同，生成的id不会重复
    @Test
    public void testConcurrent() {
        int threads = 3;
        final CountDownLatch latch = new CountDownLatch(threads);
        final Queue<Long> queue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < threads; i++) {
            Runnable runnable = new Runnable() {
                int workid = 0;
                public void run() {
                    SnowFlake snowFlake = new SnowFlake(workid, 3);
                    for (long i = 0; i < times; i++) {
                        queue.add(snowFlake.nextId());
                    }
                    latch.countDown(); // 执行完毕，计数器减1
                }
                public Runnable accept(int workid) { // 接收从外部传递的参数
                    this.workid = workid;
                    return this;
                }
            }.accept(i);
            new Thread(runnable).start();
        }
        try {
            latch.await(); // 主线程等待
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Set<Long> set = new TreeSet<>();
        for(Long item: queue) {
            set.add(item);
        }
        assertThat((long)queue.size(), equalTo(threads * times));
        assertThat((long)set.size(), equalTo(threads * times));
    }

    // 多个线程并发，只有一个ID生成器，生成的id不会重复
    @Test
    public void testSameWorkidConcurrent() {
        int threads = 3;
        final CountDownLatch latch = new CountDownLatch(threads);
        final Queue<Long> queue = new ConcurrentLinkedQueue<>();
        final SnowFlake snowFlake = new SnowFlake(3, 2);
        for (int i = 0; i < threads; i++) {
            Runnable runnable = new Runnable() {
                public void run() {
                for (long i = 0; i < times; i++) {
                    queue.add(snowFlake.nextId());
                }
                latch.countDown(); // 执行完毕，计数器减1
                }
            };
            new Thread(runnable).start();
        }
        try {
            latch.await(); // 主线程等待
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Set<Long> set = new TreeSet<>();
        for(Long item: queue) {
            set.add(item);
        }
        assertThat((long)queue.size(), equalTo(threads * times));
        assertThat((long)set.size(), equalTo(threads * times));
    }
}
