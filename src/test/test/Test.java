package test.test;

import main.java.cloud.RedisAccess;
import main.java.cloud.RedisAccessFactory;
import main.java.config.EngineConfig;
import main.java.util.RandomUtil;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        EngineConfig.setConfigPath("D:\\Eclipse\\workspace\\IDF_VC_Sim_Engine\\WebContent\\WEB-INF\\engine.config");

        String key = RandomUtil.genRandomStr();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(RandomUtil.genRandomStr());
        }
        String txt = sb.toString();

        try (RedisAccess access = RedisAccessFactory.getAccess()) {
            access.rpush(key, txt);
            Thread.sleep(40000);
            access.rpush(key, txt);

            String get = access.rpop(key);
            System.out.println("Get equal txt? " + txt.equals(get));
            get = access.rpop(key);
            System.out.println("Get equal txt? " + txt.equals(get));
        } finally {
            RedisAccessFactory.closePool();
        }
    }
}
