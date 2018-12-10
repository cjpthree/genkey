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
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JSnowFlakeTest {
    private long times = 2 ^ 20;

    @Test
    public void contextLoads() {
    }

    // 连续生产不超过getMaxSequence个id，不会重复
    @Test
    public void testRepetitionLess() {
        JSnowFlake jsnowFlake = new JSnowFlake();
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < times; i++) {
            set.add(jsnowFlake.nextId());
        }
        assertThat((long)set.size(), equalTo(times));
    }

    // 每秒钟生产id不超过getMaxSequence个，不会重复
    @Test
    public void testRepetitionMore() {
        JSnowFlake jsnowFlake = new JSnowFlake();
        long itimes = 10;
        long jtimes = times;
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < itimes; i++) {
            for (long j = 0; j < jtimes; j++) {
                set.add(jsnowFlake.nextId());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertThat((long)set.size(), equalTo(itimes * jtimes));
    }

    // 多个线程并发，workid不同，每秒钟生产id不超过getMaxSequence个，生成的id就不会重复
    @Test
    public void testConcurrent() {
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        Queue<Long> queue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < threads; i++) {
            Runnable runnable = new Runnable() {
                int workid = 0;
                public void run() {
                    JSnowFlake jsnowFlake = new JSnowFlake(workid);
                    for (long i = 0; i < times; i++) {
                        queue.add(jsnowFlake.nextId());
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
        long itimes = 10;
        long jtimes = times;
        long newMillis = System.currentTimeMillis();
        long oldMillis = newMillis;
        JSnowFlake jsnowFlake = new JSnowFlake();
        Set<Long> set = new TreeSet<>();
        for (long i = 0; i < itimes; i++) {
            for (long j = 0; j < jtimes; j++) {
                set.add(jsnowFlake.nextId());
            }
            Date date = new Date();
            newMillis = System.currentTimeMillis();
            if (oldMillis > newMillis) {
                System.out.println("wrong time");
            }
            oldMillis = newMillis;
            long time = date.getTime();
            time = time - 1 * 1000; // 返回1秒之前
            date.setTime(time);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertThat((long)set.size(), equalTo(itimes * jtimes));
    }
}