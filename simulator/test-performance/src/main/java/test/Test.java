package test;

/**
 * Created by matthias on 21.11.17.
 */
public abstract class Test implements Runnable{
    protected TestConfig config;

    public Test(TestConfig config) {
        this.config = config;
    }

    public abstract void shutdown();
}
