import java.util.concurrent.Callable;
import org.max.distributed.algorithms.lock.PostgresqlDistributedLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class PostgresqlDistributedLockMain {

    public static void main(String[] args) throws Exception {

        final int threadsCount = 10;

        ExecutorService pool = Executors.newFixedThreadPool(threadsCount);

        AtomicInteger criticalSectionThCount = new AtomicInteger(0);

        PostgresqlDistributedLock lock = new PostgresqlDistributedLock();

        List<Callable<Integer>> allTasks = new ArrayList<>();

        for (int i = 0; i < threadsCount; ++i) {
            allTasks.add(() -> {

                int maxThInCriticalSection = 0;


                for (int it = 0; it < 100; ++it) {
                    lock.lock();
                    try {
//                            System.out.printf("[thread-%d]lock ACQUIRED\n", Thread.currentThread().getId());
                        maxThInCriticalSection = Math.max(maxThInCriticalSection,
                            criticalSectionThCount.incrementAndGet());
                    }
                    finally {
                        criticalSectionThCount.decrementAndGet();
//                            System.out.printf("[thread-%d]lock RELEASED\n", Thread.currentThread().getId());
                        lock.unlock();
                    }
                }


                return maxThInCriticalSection;

            });
        }

        List<Future<Integer>> allResults = pool.invokeAll(allTasks);

        int maxDetected = 0;
        for (Future<Integer> curFuture : allResults) {
            maxDetected = Math.max(maxDetected, curFuture.get());
        }

        pool.shutdownNow();

        System.out.printf("Max threads count detected in critical section: %d\n", maxDetected);

        System.out.println("Main done...");
    }

}



