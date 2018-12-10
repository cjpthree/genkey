package com.jarxi.genkey.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SnowFlakeTest {
    long times = 2 ^ 20;

    @Test
    public void contextLoads() {
    }

    // 连续生产少于MAX_SEQUENCE个id，不会重复
    @Test
    public void testRepetitionLess() {
        SnowFlake snowFlake = new SnowFlake(3, 2);
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < times; i++) {
            set.add(snowFlake.nextId());
        }
        assertThat((long)set.size(), equalTo(times));
    }

    // 每ms生产id不超过MAX_SEQUENCE个，不会重复
    @Test
    public void testRepetitionMore() {
        long itimes = 10;
        long jtimes = times;
        SnowFlake snowFlake = new SnowFlake(3, 2);
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < itimes; i++) {
            for (long j = 0; j < jtimes; j++) {
                set.add(snowFlake.nextId());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertThat((long)set.size(), equalTo(itimes * jtimes));
    }

    // 多个线程并发，workid不同，每ms生产id不超过MAX_SEQUENCE个，生成的id就不会重复
    @Test
    public void testConcurrent() {
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        Queue<Long> queue = new ConcurrentLinkedQueue<>();
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

    // 测试时钟调整的影响，设置时钟不能影响currentTimeMillis(),此测试暂时无用
    @Test
    public void testClockRepetition() {
        long itimes = 100;
        long jtimes = times;
        SnowFlake snowFlake = new SnowFlake(3, 2);
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < itimes; i++) {
            for (long j = 0; j < jtimes; j++) {
                set.add(snowFlake.nextId());
            }
            Date date = new Date();
            long time = date.getTime();
            time = time - 1 * 1000; // 返回1毫秒之前
            date.setTime(time);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertThat((long)set.size(), equalTo(itimes * jtimes));
    }
}
